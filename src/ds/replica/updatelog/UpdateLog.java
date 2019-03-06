package ds.replica.updatelog;

import ds.core.Timestamp;
import ds.frontend.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The update log component of a replica contains all updates known by the replica
 * that have not been seen by all replicas within the network.
 */
public class UpdateLog {
    private List<UpdateLogEntry> entries;

    public UpdateLog() {
        entries = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Checks whether any update within the log is stable compared to a value timestamp
     *
     * @param valueTimestamp
     * @return any update is stable compared with valueTimestamp
     */
    public boolean hasStableEntry(Timestamp valueTimestamp) {
        return entries.stream().anyMatch(entry ->
                entry.getUpdateRequest().isStable(valueTimestamp));
    }

    /**
     * Returns all entries that are considered stable compared to a valueTimestamp
     *
     * @param valueTimestamp
     * @return all entries within log that are stable
     */
    public PriorityQueue<UpdateLogEntry> getStableEntries(Timestamp valueTimestamp) {
        // Entry is stable <=> entry.request.timestamp <= valueTimestamp (value has caught up to update)
        PriorityQueue<UpdateLogEntry> stableEntries = new PriorityQueue<>();

        entries.stream()
                .filter(entry -> entry.getUpdateRequest().isStable(valueTimestamp))
                .forEach(stableEntries::add);

        return stableEntries;
    }

    /**
     * Returns whether a entry has been seen by every replica in the network
     *
     * @param timestampTable known timestamps of all replicas
     * @param entry          to check whether it's empty or not
     * @return whether entry is outdated and can be removed from the network
     */
    private boolean isEntryOutdated(List<Timestamp> timestampTable, UpdateLogEntry entry) {
        int creatorId = entry.getReplicaId();
        Timestamp entryTimestamp = entry.getUpdateTimestamp();

        // Are all replica managers up to date?
        return timestampTable.stream().allMatch(timestamp ->
                timestamp.get(creatorId) >= entryTimestamp.get(creatorId));
    }

    public void removeOutdatedEntries(List<Timestamp> timestampTable) {
        entries.removeIf(entry -> isEntryOutdated(timestampTable, entry));
    }

    private boolean logContainsEntryWithTimestamp(Request request) {
        return entries.stream()
                .anyMatch(entry -> entry.getUpdateRequest().getUid().equals(request.getUid()));
    }

    public void add(int replicaId, Timestamp updateTimestamp, Request request) {
        if (logContainsEntryWithTimestamp(request)) {
            return;
        }

        entries.add(new UpdateLogEntry(replicaId, updateTimestamp, request));
    }

    public void add(UpdateLogEntry entry) {
        if (logContainsEntryWithTimestamp(entry.getUpdateRequest())) {
            return;
        }

        entries.add(entry);
    }

    public List<UpdateLogEntry> anyEntryThat(Predicate<UpdateLogEntry> acceptPredicate) {
        return entries.stream()
                .filter(acceptPredicate)
                .collect(Collectors.toList());
    }
}
