package ds.frontend;

import ds.client.RequestParameters;
import ds.movies.MovieDetails;
import ds.replica.ReplicaStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FrontEndApi extends Remote {
    /**
     * Queries the distributed network
     *
     * @param parameters of the request
     * @return details about the movie, including ratings, genres, name, ...
     * @throws RemoteException
     */
    MovieDetails query(RequestParameters parameters) throws RemoteException;

    /**
     * Updates an existing rating about a movie on the distributed network
     *
     * @param parameters of the request
     * @return whether the update was a success
     * @throws RemoteException
     */
    boolean update(RequestParameters parameters) throws RemoteException;

    /**
     * Submits a new rating about a movie on the distributed network
     * You cannot override an existing rating.
     *
     * @param parameters of the request
     * @return whether the submission was a success
     * @throws RemoteException
     */
    boolean submit(RequestParameters parameters) throws RemoteException;

    /**
     * Changes the replica status of a replica on the network
     * @param replicaId of the replica to change
     * @param status new status of the replica
     * @throws RemoteException
     */
    void changeReplicaStatus(int replicaId, ReplicaStatus status) throws RemoteException;
}
