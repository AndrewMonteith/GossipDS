package ds.replica;

import ds.client.RequestParameters;
import ds.core.StubLoader;
import ds.core.Timestamp;
import ds.frontend.MutationResponse;
import ds.frontend.QueryResponse;
import ds.frontend.Request;
import ds.replica.updatelog.UpdateLog;
import ds.replica.updatelog.UpdateLogEntry;

import java.io.NotActiveException;
import java.rmi.RemoteException;
import java.util.*;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

/**
 * The replica class is the core component of the distributed system.
 */
public class Replica extends TimerTask implements ReplicaApi {
    private int replicaId;

    /**
     * Update log of the replica contains all known updates seen by the replica
     * that it believes has not been seen by every replica in the network
     */
    private UpdateLog updateLog = new UpdateLog();

    /**
     * Timestamp that reflects all entries in the update log
     */
    private Timestamp replicaTimestamp = new Timestamp(NUMBER_OF_REPLICAS);

    /**
     * Current status of the replica
     */
    private ReplicaStatus status = ReplicaStatus.ACTIVE;

    /**
     * Timestamp that reflects updates in value
     */
    private Timestamp valueTimestamp = new Timestamp(NUMBER_OF_REPLICAS);

    /**
     * Actual value we're querying and mutating
     */
    private ReplicaValue value = new ReplicaValue();

    private StubLoader stubLoader;

    /**
     * All operations we have executed
     * In real world we would clear these out say 60 seconds after having been added
     */
    private Set<UUID> executedOperations = new HashSet<>();

    /**
     * Estimated replica timestamps of all replicas which is updated
     * through gossip messages
     */
    private List<Timestamp> timestampTable = new ArrayList<>();

    public Replica(int replicaId) throws RemoteException {
        this.replicaId = replicaId;
        stubLoader = new StubLoader();

        for (int i = 0; i < NUMBER_OF_REPLICAS; ++i) {
            timestampTable.add(new Timestamp(NUMBER_OF_REPLICAS));
        }
    }

    @Override
    public ReplicaStatus requestStatus() throws RemoteException {
        return status;
    }

    @Override
    public void setReplicaStatus(ReplicaStatus status) throws RemoteException {
        System.out.printf("Replica %d is updating it's status to %s...\n", replicaId, status);

        this.status = status;
    }

    /**
     * Merges an update log from a list of entries, typically from a gossip message.
     * @param entries
     */
    private void mergeUpdateLog(List<UpdateLogEntry> entries) {
        entries.stream()
                .filter(entry -> !entry.getUpdateTimestamp().isBeforeOrEqual(replicaTimestamp))
                .forEach(updateLog::add);
    }

    /**
     * synchronizes timestamps with a gossip message
     * @param message
     */
    private void updateTimestamps(GossipMessage message) {
        replicaTimestamp.merge(message.getReplicaTimestamp());

        timestampTable.set(replicaId, replicaTimestamp);
        timestampTable.set(message.getSenderId(), message.getReplicaTimestamp());
    }

    private void clearUpdateLog() {
        updateLog.removeOutdatedEntries(timestampTable);
    }

    @Override
    public void processGossipMessage(GossipMessage message) throws RemoteException {
//        System.out.printf("Replica %d got gossip from %d of size %d\n", replicaId, message.getSenderId(), message.getUpdateLogEntries().size());
        mergeUpdateLog(message.getUpdateLogEntries());
        updateTimestamps(message);

        executeUpdatesUntilValueTimestampStabilises();

        clearUpdateLog();
    }

    @Override
    public List<UpdateLogEntry> findAllRequiredUpdates(Timestamp timestamp) throws RemoteException {
        return updateLog.anyEntryThat(entry -> timestamp.isAfter(entry.getUpdateRequest().getTimestamp()));
    }

    private boolean haveProcessedRequest(Request request) {
        return executedOperations.contains(request.getUid());
    }

    /**
     * Executes the update if it hasn't seen it before
     * @param entry to be executed
     * @return whether the update was successfully executed
     */
    private boolean executeEntry(UpdateLogEntry entry) {
        if (haveProcessedRequest(entry.getUpdateRequest())) {
            return false;
        }

        Request updateRequest = entry.getUpdateRequest();

        value.setRanking(updateRequest.getParameters());
        valueTimestamp.merge(entry.getUpdateTimestamp());
        executedOperations.add(updateRequest.getUid());

        return true;
    }

    private boolean executeStableUpdates() {
        return updateLog.getStableEntries(valueTimestamp).stream()
                .anyMatch(this::executeEntry);
    }

    private void executeUpdatesUntilValueTimestampStabilises() {
        while (updateLog.hasStableEntry(valueTimestamp) && executeStableUpdates()) ;
    }

    /**
     * Synchronizes the update log with a replica
     * @param replicaId replica to synchronize with
     * @param requiredTimestamp timestamp to catchup to
     * @throws NotActiveException if replica is offline
     * @throws RemoteException
     */
    private void catchupUpdateLog(int replicaId, Timestamp requiredTimestamp) throws NotActiveException, RemoteException {
        ReplicaApi replica = stubLoader.getReplicaStub(replicaId);

        List<UpdateLogEntry> entries = replica.findAllRequiredUpdates(requiredTimestamp);

        entries.forEach(updateLog::add);
    }

    /**
     * Makes value as up to date as possible with a given timestamp
     * @param requiredTimestamp to be up to date with
     * @throws RemoteException
     */
    private void catchupValue(Timestamp requiredTimestamp) throws RemoteException {
        for (int i = 0; i < valueTimestamp.getDimension(); ++i) {
            if (valueTimestamp.get(i) < requiredTimestamp.get(i)) {
                try {
                    catchupUpdateLog(i, requiredTimestamp);
                } catch (NotActiveException e) {
                    System.out.println("Failed to contact the replica who had our required updates\n" +
                            "in the real world we would ask other replicas for the other updates\n" +
                            "however I feel this is unlikely in our simulations and perhaps our the scope of this course");
                }
            }
        }

        executeUpdatesUntilValueTimestampStabilises();
    }

    private QueryResponse performQueryRequest(Request request) {
        return new QueryResponse(valueTimestamp, value.getDetailsForMovie(request.getParameters()));
    }

    @Override
    public QueryResponse query(Request request) throws RemoteException {
        System.out.printf("Replica %d is processing a query request...\n", replicaId);

        if (request.getTimestamp().isAfter(valueTimestamp)) {
            catchupValue(request.getTimestamp());
        }

        return performQueryRequest(request);
    }

    private void incrementReplicaTimestamp() {
        replicaTimestamp.set(replicaId, replicaTimestamp.get(replicaId) + 1);
        timestampTable.set(replicaId, replicaTimestamp);
    }

    private MutationResponse scheduleMutationRequest(Request request) {
        if (haveProcessedRequest(request)) {
            return MutationResponse.wasFailure(request.getTimestamp());
        }

        incrementReplicaTimestamp();

        Timestamp newFrontendTimestamp = request.getTimestamp().copy();
        newFrontendTimestamp.set(replicaId, replicaTimestamp.get(replicaId));

        updateLog.add(replicaId, newFrontendTimestamp, request);

        executeStableUpdates();

        return MutationResponse.wasSuccessful(newFrontendTimestamp);
    }

    private boolean isRequestAffectingExistingRating(Request request) {
        RequestParameters parameters = request.getParameters();

        return value.hasUserRankedMovie(parameters.getUserId(), parameters.getMovieId());
    }

    @Override
    public MutationResponse update(Request request) throws RemoteException {
        System.out.printf("Replica %d is processing a update request...\n", replicaId);

        if (!isRequestAffectingExistingRating(request)) { // only update existing ratings.
            return MutationResponse.wasFailure(request.getTimestamp());
        }

        return scheduleMutationRequest(request);
    }

    @Override
    public MutationResponse submit(Request request) throws RemoteException {
        System.out.printf("Replica %d is processing a submit request...\n", replicaId);

        if (isRequestAffectingExistingRating(request)) { // can only submit non-existent ratings
            return MutationResponse.wasFailure(request.getTimestamp());
        }

        return scheduleMutationRequest(request);
    }

    private void sendGossipMessage(int replicaNumber, GossipMessage message) {
        try {
            stubLoader.getReplicaStub(replicaNumber).processGossipMessage(message);
        } catch (RemoteException | NotActiveException e) {
            // In real life, perhaps we would count the number of fails
            // and if it reached some threshold then we would then stop sending gossip messages to it.
        }
    }

    private void broadcastGossipMessages() {
        if (status == ReplicaStatus.OFFLINE) {
            return;
        }
        System.out.printf("Replica %d is gossipping\n", replicaId);

        for (int replicaNumber = 0; replicaNumber < timestampTable.size(); ++replicaNumber) {
            if (replicaNumber == replicaId) {
                continue;
            }

            Timestamp estimatedTimestamp = timestampTable.get(replicaNumber);

            List<UpdateLogEntry> updatesReplicaNeeds = updateLog.anyEntryThat(
                    entry -> entry.getUpdateTimestamp().isAfter(estimatedTimestamp));

            sendGossipMessage(replicaNumber, new GossipMessage(replicaId, updatesReplicaNeeds, replicaTimestamp));
        }
    }

    @Override
    public void run() {
        broadcastGossipMessages();
    }
}
