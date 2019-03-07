package ds.core;

import ds.replica.ReplicaApi;
import ds.replica.ReplicaStatus;

import java.io.NotActiveException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

/**
 * Lazy loader for ReplicaApi's based on an id.
 */
public class StubLoader {
    private Map<Integer, ReplicaApi> replicaStubs = new HashMap<>();
    private Map<Integer, ReplicaStatus> replicaStatuses = new HashMap<>();

    private Registry registry;

    public StubLoader() throws RemoteException {
        registry = LocateRegistry.getRegistry("localhost", 13007);
    }

    private static String formatReplicaRegistryIdentifier(int id) {
        return "replica-" + id;
    }

    private ReplicaApi loadReplicaStub(int replicaId) throws RemoteException, NotBoundException {
        return (ReplicaApi) registry.lookup(formatReplicaRegistryIdentifier(replicaId));
    }

    /**
     * Lazy loads a replica with a given id
     *
     * @param replicaId replica id to load
     * @return Replica stub to interact with replica
     * @throws RemoteException
     * @throws NotActiveException if the replica is offline
     */
    public ReplicaApi getReplicaStub(int replicaId) throws RemoteException, NotActiveException {
        try {
            replicaStubs.putIfAbsent(replicaId, loadReplicaStub(replicaId));

            ReplicaApi replica = replicaStubs.get(replicaId);
            ReplicaStatus replicaStatus = replica.requestStatus();

            replicaStatuses.put(replicaId, replicaStatus);

            if (replicaStatus == ReplicaStatus.OFFLINE) {
                throw new NotActiveException("Replica " + replicaId + " is offline");
            }

            return replica;
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ReplicaApi getCachedReplica(int replicaId) {
        return replicaStubs.get(replicaId);
    }

    /**
     * Returns last known status for a replica, may be old data since it's cached.
     *
     * @param replicaId
     * @return status of the given replicaId
     */
    public ReplicaStatus getCachedStatusFor(int replicaId) {
        return replicaStatuses.get(replicaId);
    }
}
