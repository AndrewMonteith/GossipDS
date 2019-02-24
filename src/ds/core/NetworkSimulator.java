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

public class NetworkSimulator {
    public static final int NUMBER_OF_REPLICAS = 3;

    private Registry registry;

    private NetworkSimulator() throws RemoteException {
        registry = LocateRegistry.createRegistry(13007);
    }

    private Map<Integer, ReplicaApi> launchReplicas() throws RemoteException, AlreadyBoundException {
        Timer t = new Timer();
        Map<Integer, ReplicaApi> replicas = new HashMap<>();

        for (int i = 0; i < NUMBER_OF_REPLICAS; ++i) {
            System.out.printf("Launching replica %d\n", i);
            Replica replica = new Replica(i);

            ReplicaApi replicaStub = (ReplicaApi) UnicastRemoteObject.exportObject(replica, 0);

            registry.bind("replica-" + i, replicaStub);

            t.scheduleAtFixedRate(replica, 5000, 5000);

            replicas.put(i, replica);
        }

        return replicas;
    }

    private FrontEndApi launchFrontend() throws RemoteException, AlreadyBoundException {
        FrontEnd fe = new FrontEnd();

        FrontEndApi frontEndStub = (FrontEndApi) UnicastRemoteObject.exportObject(fe, 0);

        registry.bind("frontend", frontEndStub);

        return fe;
    }

    private static void safeSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) { }
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

        safeSleep(7500); // wait for gossip to update everyone

        replicas.get(0).setReplicaStatus(ReplicaStatus.OFFLINE);
        replicas.get(1).setReplicaStatus(ReplicaStatus.OFFLINE);

        assertCondition(serviceApi.query(new RequestParameters(1, 47)).getUserRanking() == 4.5f,
                "gossip message propagate updates");

        serviceApi.submit(new RequestParameters(30, 100, 2.5f));

        replicas.get(0).setReplicaStatus(ReplicaStatus.ACTIVE);
        replicas.get(1).setReplicaStatus(ReplicaStatus.ACTIVE);

        safeSleep(7500); // wait for gossip to update everyone
        replicas.get(2).setReplicaStatus(ReplicaStatus.OFFLINE);

        assertCondition(serviceApi.query(new RequestParameters(30, 100)).getUserRanking() == 2.5f,
                "gossip message propagates updates #2");
    }

    private static void doTestsWithFrontend(FrontEndApi serviceApi, Map<Integer, ReplicaApi> replicas) throws RemoteException {
        doTestsWithQuerying(serviceApi, replicas);
        doTestsWithUpdate(serviceApi, replicas);
        doTestsWithSubmit(serviceApi, replicas);
        doTestsWithGossipMessages(serviceApi, replicas);

        System.out.println(" --- FINISHED ALL TESTS ---");
    }

    public static void main(String[] args) {
        try {
            NetworkSimulator simulator = new NetworkSimulator();

            Map<Integer, ReplicaApi> replicas = simulator.launchReplicas();
            FrontEndApi frontEndStub = simulator.launchFrontend();

            doTestsWithFrontend(frontEndStub, replicas);
        } catch(RemoteException e) {
            System.out.println("Remote error occured when launching network");
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            System.out.println("Error when binding to RMI registry, are you already running an instance of the test network?");
        }
    }
}
