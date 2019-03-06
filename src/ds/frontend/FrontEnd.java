package ds.frontend;

import ds.client.RequestParameters;
import ds.core.Timestamp;
import ds.movies.MovieDetails;
import ds.replica.ReplicaApi;
import ds.replica.ReplicaStatus;

import java.io.NotActiveException;
import java.rmi.RemoteException;
import java.util.function.Function;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

/**
 * Frontend of a distributed network.
 * Contains only required functionality for a DS with a single frontend.
 */
public class FrontEnd implements FrontEndApi {
    private ReplicaPicker replicaPicker;

    private Timestamp feTimestamp = new Timestamp(NUMBER_OF_REPLICAS);

    public FrontEnd() throws RemoteException {
        replicaPicker = new ReplicaPicker();
    }

    private Request createRequestFromParameters(RequestParameters parameters) {
        return new Request(parameters, feTimestamp.copy());
    }

    @Override
    public MovieDetails query(RequestParameters parameters) throws RemoteException {
        ReplicaApi replica = replicaPicker.pickReplicasToContact(1).get(0);

        QueryResponse response = replica.query(createRequestFromParameters(parameters));

        feTimestamp.merge(response.getTimestamp());

        return response.getMovieDetails();
    }

    public void resetReplicaPickerState() {
        replicaPicker.resetLastContactedState();
    }

    private boolean makeMutationRequests(RequestParameters parameters,
                                         MutationRequestMaker requestMaker)
            throws RemoteException {

        Timestamp originalTimestamp = feTimestamp.copy();

        Function<ReplicaApi, MutationResponse> makeRequestSafe = replica -> {
            Request request = new Request(parameters, originalTimestamp);

            try {
                return requestMaker.sendRequest(replica, request);
            } catch (RemoteException e) {
                System.out.println("Failed to contact replica previously though reachable");
            }

            return null;
        };

        /*
          Notice below we pick 2 replicas to send the update request to
          This means that we can handle 1 fault in the network.
         */
        return replicaPicker.pickReplicasToContact(2).stream()
                .map(makeRequestSafe)
                .peek(response -> feTimestamp.merge(response.getTimestamp()))
                .allMatch(MutationResponse::isSuccess);
    }

    @Override
    public boolean update(RequestParameters parameters) throws RemoteException {
        return makeMutationRequests(parameters, ReplicaApi::update);
    }

    @Override
    public boolean submit(RequestParameters parameters) throws RemoteException {
        return makeMutationRequests(parameters, ReplicaApi::submit);
    }

    @Override
    public void changeReplicaStatus(int replicaId, ReplicaStatus status) throws RemoteException {
        try {
            replicaPicker.getReplica(replicaId).setReplicaStatus(status);
        } catch (NotActiveException ignored) {
        }
    }

    @FunctionalInterface
    private interface MutationRequestMaker {
        MutationResponse sendRequest(ReplicaApi replica, Request request) throws RemoteException;
    }
}
