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

class ReplicaPicker {
    private Deque<Integer> lastContactedReplicas = new ArrayDeque<>();
    private StubLoader stubLoader;

    private boolean isReplicaInStatus(int id, ReplicaStatus status) throws RemoteException {
        try {
            stubLoader.getReplicaStub(id);

            return stubLoader.getCachedStatusFor(id) == status;
        } catch (NotActiveException ignored) {
            return status == ReplicaStatus.OFFLINE;
        }

    }

    private List<Integer> chooseReplicasWithStatus(int number, ReplicaStatus status) throws RemoteException {
        List<Integer> acceptedReplicaIds = new ArrayList<>();

        for (int i = 0; i < lastContactedReplicas.size() && acceptedReplicaIds.size() < number; ++i) {
            int replicaId = lastContactedReplicas.removeFirst();

            if (isReplicaInStatus(replicaId, status)) {
                acceptedReplicaIds.add(replicaId);
            } else {
                lastContactedReplicas.addLast(replicaId);
            }
        }

        return acceptedReplicaIds;
    }

    private List<ReplicaApi> loadReplicasFromIds(List<Integer> replicaIds) {
        List<ReplicaApi> replicas = new ArrayList<>();

        for (int replicaId : replicaIds) {
            try {
                replicas.add(stubLoader.getReplicaStub(replicaId));
            } catch (RemoteException | NotActiveException ignored) {}
        }

        return replicas;
    }

    synchronized List<ReplicaApi> pickReplicasToContact(int number) throws RemoteException {
        List<Integer> replicaIds = chooseReplicasWithStatus(number, ReplicaStatus.ACTIVE);

        if (replicaIds.isEmpty()) {
            replicaIds = chooseReplicasWithStatus(number, ReplicaStatus.OVERLOADED);
        }

        if (replicaIds.isEmpty()) {
            throw new RemoteException("all replicas were offline, please consider restarting one.");
        }

        replicaIds.forEach(lastContactedReplicas::addFirst);

        return loadReplicasFromIds(replicaIds);
    }

    void resetLastContactedState() {
        lastContactedReplicas.clear();
        for (int i = 0; i < NUMBER_OF_REPLICAS; ++i) {
            lastContactedReplicas.addLast(i);
        }
    }

    ReplicaPicker() throws RemoteException {
        stubLoader = new StubLoader();
        resetLastContactedState();
    }

    ReplicaApi getReplica(int replicaId) throws NotActiveException, RemoteException {
        return stubLoader.getReplicaStub(replicaId);
    }
}
