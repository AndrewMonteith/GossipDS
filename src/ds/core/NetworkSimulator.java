package ds.core;

import ds.client.RequestParameters;
import ds.frontend.FrontEnd;
import ds.frontend.FrontEndApi;
import ds.replica.Replica;
import ds.replica.ReplicaApi;
import ds.replica.ReplicaStatus;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import static ds.core.Utils.assertCondition;

/**
 * This class is responsible for simulating running the distributed network.
 * Ie this class provides all functionality required minus the client.
 */
public class NetworkSimulator {
    /**
     * The number of replicas we want to have in the distributed network.
     */
    public static final int NUMBER_OF_REPLICAS = 3;

    /**
     * Whether or not you want to run the provided unit tests.
     * <p>
     * The unit tests cover the following functionality:
     * - Whether querying the network works.
     * - Whether updating existing ratings in the network works.
     * - Whether submitting new ratings in the network works.
     * - Whether or not updates propagate via gossip messages
     * - Whether the network can handle 1 fault
     * - Whether a replica will always provide consistent data to the frontend
     */
    private static final boolean doFrontendTests = false;

    /**
     * How often a replica will gossip.
     */
    private static final long GOSSIP_PERIOD = 10000; // milliseconds

    private Registry registry;

    private NetworkSimulator() throws RemoteException {
        registry = LocateRegistry.createRegistry(13007);
    }

    private static void safeSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }

    private static void doTestsWithQuerying(FrontEndApi serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        assertCondition(serviceApi.query(new RequestParameters(1, 1)).getUserRanking() == 4.0f,
                "failed to query correctly vanilla data");

        assertCondition(serviceApi.query(new RequestParameters(-1, 264)).getMovie() == null,
                "safely returns no results for non existent movie");

    }

    private static void doTestsWithUpdate(FrontEndApi serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        assertCondition(serviceApi.update(new RequestParameters(1, 1, 3.0f)),
                "existing prexisting rating was accepted");

        assertCondition(serviceApi.query(new RequestParameters(1, 1)).getUserRanking() == 3.0f,
                "updating a preexisting rating was accepted");

        assertCondition(!serviceApi.update(new RequestParameters(1, 2320, 3.0f)),
                "can't update nonexistent rating");
    }

    private static void doTestsWithSubmit(FrontEndApi serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        assertCondition(serviceApi.submit(new RequestParameters(1, 2, 3.0f)),
                "submit request was accepted");

        assertCondition(serviceApi.query(new RequestParameters(1, 2)).getUserRanking() == 3.0f,
                "submit request actually happened");
    }

    private static void doTestsWithGossipMessages(FrontEndApi serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        serviceApi.update(new RequestParameters(1, 47, 4.5f));

        safeSleep(GOSSIP_PERIOD + 2500); // wait for gossip to update everyone

        replicas.get(0).setReplicaStatus(ReplicaStatus.OFFLINE);
        replicas.get(1).setReplicaStatus(ReplicaStatus.OFFLINE);

        assertCondition(serviceApi.query(new RequestParameters(1, 47)).getUserRanking() == 4.5f,
                "gossip message propagate updates");

        assertCondition(serviceApi.submit(new RequestParameters(30, 100, 2.5f)),
                "gossip test accepted submit");

        replicas.get(0).setReplicaStatus(ReplicaStatus.ACTIVE);
        replicas.get(1).setReplicaStatus(ReplicaStatus.ACTIVE);

        safeSleep(GOSSIP_PERIOD + 2500); // wait for gossip to update everyone
        replicas.get(2).setReplicaStatus(ReplicaStatus.OFFLINE);

        assertCondition(serviceApi.query(new RequestParameters(30, 100)).getUserRanking() == 2.5f,
                "gossip message propagates updates #2");


        replicas.get(2).setReplicaStatus(ReplicaStatus.ACTIVE);
    }

    private static void doMiniStressTest(FrontEnd serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        assertCondition(serviceApi.update(new RequestParameters(1, 47, 5.0f)),
                "accepted s.t. update #1");
        assertCondition(serviceApi.update(new RequestParameters(30, 100, 5.0f)),
                "accepted s.t. update #2");
        assertCondition(serviceApi.update(new RequestParameters(1, 2, 5.0f)),
                "accepted s.t. update #3");

        replicas.get(1).setReplicaStatus(ReplicaStatus.OVERLOADED);

        safeSleep(1000);

        assertCondition(serviceApi.query(new RequestParameters(30, 100)).getUserRanking() == 5.0f,
                "stress test checkpoint #1");

        replicas.get(1).setReplicaStatus(ReplicaStatus.OFFLINE);
        replicas.get(0).setReplicaStatus(ReplicaStatus.OFFLINE);

        assertCondition(serviceApi.update(new RequestParameters(1, 47, 2.0f)),
                "accepted s.t. update #4");
        assertCondition(serviceApi.update(new RequestParameters(30, 100, 2.0f)),
                "accepted s.t. update #5");
        assertCondition(serviceApi.update(new RequestParameters(1, 2, 2.0f)),
                "accepted s.t. update #6");

        safeSleep(1000);

        serviceApi.resetReplicaPickerState();
        replicas.get(1).setReplicaStatus(ReplicaStatus.ACTIVE);
        replicas.get(0).setReplicaStatus(ReplicaStatus.ACTIVE);

        assertCondition(serviceApi.query(new RequestParameters(1, 2)).getUserRanking() == 2.0f,
                "stress test checkpoint #2");
    }

    private static void doTestsWithFrontend(FrontEnd serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        doTestsWithQuerying(serviceApi, replicas);
        doTestsWithUpdate(serviceApi, replicas);
        doTestsWithSubmit(serviceApi, replicas);
        doTestsWithGossipMessages(serviceApi, replicas);

        serviceApi.resetReplicaPickerState(); // so we can reason about what to turn off during mini-stress test.

        doMiniStressTest(serviceApi, replicas);

        for (ReplicaApi replicaApi : replicas.values()) {
            replicaApi.setReplicaStatus(ReplicaStatus.ACTIVE);
        }

        System.out.println(" --- FINISHED ALL TESTS ---");
    }

    public static void main(String[] args) {
        try {
            NetworkSimulator simulator = new NetworkSimulator();

            Map<Integer, ReplicaApi> replicas = simulator.launchReplicas();
            FrontEnd frontEndStub = simulator.launchFrontend();

            if (doFrontendTests) {
                doTestsWithFrontend(frontEndStub, replicas);
            }
        } catch (RemoteException e) {
            System.out.println("Remote error occurred when launching network");
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            System.out.println("Error when binding to RMI registry, are you already running an instance of the test network?");
        }
    }

    private Map<Integer, ReplicaApi> launchReplicas() throws RemoteException, AlreadyBoundException {
        Timer t = new Timer();
        Map<Integer, ReplicaApi> replicas = new HashMap<>();

        for (int i = 0; i < NUMBER_OF_REPLICAS; ++i) {
            Replica replica = new Replica(i);

            ReplicaApi replicaStub = (ReplicaApi) UnicastRemoteObject.exportObject(replica, 0);

            registry.bind("replica-" + i, replicaStub);

            t.scheduleAtFixedRate(replica, GOSSIP_PERIOD, GOSSIP_PERIOD);

            replicas.put(i, replica);
            System.out.printf("Launched replica %d\n", i);
        }

        return replicas;
    }

    private FrontEnd launchFrontend() throws RemoteException, AlreadyBoundException {
        FrontEnd fe = new FrontEnd();

        FrontEndApi frontEndStub = (FrontEndApi) UnicastRemoteObject.exportObject(fe, 0);

        registry.bind("frontend", frontEndStub);

        System.out.println("Launched frontend");
        return fe;
    }
}
