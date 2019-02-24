package ds.frontend;

import ds.client.RequestParameters;
import ds.core.Timestamp;
import ds.movies.MovieDetails;
import ds.replica.ReplicaApi;

import java.rmi.RemoteException;
import java.util.function.Function;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

public class FrontEnd implements FrontEndApi {
    private ReplicaPicker replicaPicker;

    private Timestamp timestamp = new Timestamp(NUMBER_OF_REPLICAS);

    private Request createRequestFromParameters(RequestParameters parameters) {
        return new Request(parameters, timestamp.copy());
    }

    @Override
    public MovieDetails query(RequestParameters parameters) throws RemoteException {
        ReplicaApi replica = replicaPicker.pickReplicasToContact(1).get(0);

        QueryResponse response = replica.query(createRequestFromParameters(parameters));

        timestamp.merge(response.getTimestamp());

        return response.getMovieDetails();
    }

    @FunctionalInterface
    private interface MutationRequestMaker {
        MutationResponse sendRequest(ReplicaApi replica, Request request) throws RemoteException;
    }

    private boolean makeMutationRequests(RequestParameters parameters,
                                         MutationRequestMaker requestMaker)
            throws RemoteException
    {

        Timestamp originalTimestamp = timestamp.copy();
        Function<ReplicaApi, MutationResponse> makeRequestSafe = replica -> {
            Request request = new Request(parameters, originalTimestamp);

            try {
                return requestMaker.sendRequest(replica, request);
            } catch (RemoteException e) {
                System.out.println("Failed to contact replica previously though reachable");
            }

            return null;
        };

        return replicaPicker.pickReplicasToContact(2).stream()
                .map(makeRequestSafe)
                .peek(response -> timestamp.merge(response.getTimestamp()))
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

    public FrontEnd() throws RemoteException {
        replicaPicker = new ReplicaPicker();
    }
}
