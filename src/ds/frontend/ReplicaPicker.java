package ds.frontend;

import ds.replica.ReplicaApi;
import ds.replica.ReplicaStatus;
import ds.core.StubLoader;

import java.io.NotActiveException;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

public class ReplicaPicker {
    private Deque<Integer> lastContactedReplicas = new ArrayDeque<>();
    private StubLoader stubLoader;

    private ReplicaApi getReplicaApiIfContactable(int id) throws RemoteException {
        try {
            return stubLoader.getReplicaStub(id);
        } catch (NotActiveException e) {
            return null;
        }
    }

    public synchronized List<ReplicaApi> pickReplicasToContact(int number) throws RemoteException {
        List<ReplicaApi> replicaStubs = new ArrayList<>();
        List<Integer> replicaIds = new ArrayList<>();

        for (int i = 0; i < lastContactedReplicas.size() && replicaStubs.size() < number; ++i) {
            int replicaId = lastContactedReplicas.removeFirst();
            ReplicaApi replica = getReplicaApiIfContactable(replicaId);

            if (replica != null && stubLoader.getStatusFor(replicaId) == ReplicaStatus.ACTIVE) {
                replicaStubs.add(replica);
                replicaIds.add(replicaId);
            } else {
                lastContactedReplicas.addLast(replicaId);
            }
        }

        if (replicaIds.isEmpty()) {
            throw new RemoteException("no replicas to contact, network is offline.");
        }

        replicaIds.forEach(lastContactedReplicas::addFirst);

        return replicaStubs;
    }

    public ReplicaPicker() throws RemoteException {
        for (int i = 0; i < NUMBER_OF_REPLICAS; ++i) {
            lastContactedReplicas.addLast(i);
            stubLoader = new StubLoader();
        }
    }

}
