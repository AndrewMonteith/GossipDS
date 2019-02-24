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

public class StubLoader {
    private Map<Integer, ReplicaApi> replicaStubs = new HashMap<>();
    private Map<Integer, ReplicaStatus> replicaStatuses = new HashMap<>();

    private Registry registry;

    private ReplicaApi loadReplicaStub(int replicaId) throws RemoteException, NotBoundException {
        return (ReplicaApi) registry.lookup(formatReplicaRegistryIdentifier(replicaId));
    }

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

    public ReplicaStatus getStatusFor(int replicaId) {
        return replicaStatuses.get(replicaId);
    }

    public static String formatReplicaRegistryIdentifier(int id) {
        return "replica-" + id;
    }

    public StubLoader() throws RemoteException {
        registry = LocateRegistry.getRegistry("localhost", 13007);
    }
}
