package ds.replica;

import ds.core.Timestamp;
import ds.frontend.MutationResponse;
import ds.frontend.QueryResponse;
import ds.frontend.Request;
import ds.replica.updatelog.UpdateLogEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ReplicaApi extends Remote {
    /**
     * Query replica for the status, used only for simulation purposes
     */
    ReplicaStatus requestStatus() throws RemoteException;

    /**
     * Update status of replica whether you want to knock if offline or online again
     *
     * @param status new status of the replica
     * @throws RemoteException if fail to reach replica
     */
    void setReplicaStatus(ReplicaStatus status) throws RemoteException;

    /**
     * Makes replica process the gossip message from another replica
     *
     * @param message what the replica will process
     * @throws RemoteException
     */
    void processGossipMessage(GossipMessage message) throws RemoteException;


    /**
     * returns all updates from a replicas's update log that is before timestamp
     * used so a replica can update it's update log if it's trying to catchup
     *
     * @param timestamp to compare against each update log entry
     * @return all updates that are before timestamp
     * @throws RemoteException
     */
    List<UpdateLogEntry> findAllRequiredUpdates(Timestamp timestamp) throws RemoteException;

    QueryResponse query(Request request) throws RemoteException;

    MutationResponse update(Request request) throws RemoteException;

    MutationResponse submit(Request request) throws RemoteException;
}
