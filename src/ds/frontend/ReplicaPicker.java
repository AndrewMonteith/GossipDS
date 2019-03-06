package ds.frontend;

import ds.core.StubLoader;
import ds.replica.ReplicaApi;
import ds.replica.ReplicaStatus;

import java.io.NotActiveException;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

/**
 * ReplicaPicker provides a less naive way of picking a replicas to communicate with.
 * The simple rules are:
 * - It will continue to communicate with the same replicas whilst requests are successful.
 * - If one of these same replicas becomes overloaded or offline it will try to move to another replica
 * - If no replicas are active it will choose overloaded replicas.
 * - If all replicas are offline then it throws an error.
 */
class ReplicaPicker {
    private Deque<Integer> lastContactedReplicas = new ArrayDeque<>();
    private StubLoader stubLoader;

    ReplicaPicker() throws RemoteException {
        stubLoader = new StubLoader();
        resetLastContactedState();
    }

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
            } catch (RemoteException | NotActiveException ignored) {
            }
        }

        return replicas;
    }

    /**
     * Returns a list with capacity at most number which contains replicas following the rules
     * specified in the class doc. comment.
     *
     * @param number target number of replicas to pick
     * @return list of replica stubs
     * @throws RemoteException
     */
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

    ReplicaApi getReplica(int replicaId) throws NotActiveException, RemoteException {
        return stubLoader.getReplicaStub(replicaId);
    }
}
