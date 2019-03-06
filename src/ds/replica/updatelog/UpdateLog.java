package ds.replica.updatelog;

import ds.core.Timestamp;
import ds.frontend.Request;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UpdateLog {
    private List<UpdateLogEntry> entries;

    public boolean hasStableEntry(Timestamp valueTimestamp) {
        return entries.stream().anyMatch(entry ->
                entry.getUpdateRequest().isStable(valueTimestamp));
    }

    public PriorityQueue<UpdateLogEntry> getStableEntries(Timestamp valueTimestamp) {
        // Entry is stable <=> entry.request.timestamp <= valueTimestamp (value has caught up to update)
        PriorityQueue<UpdateLogEntry> stableEntries = new PriorityQueue<>();

        entries.stream()
                .filter(entry -> entry.getUpdateRequest().isStable(valueTimestamp))
                .forEach(stableEntries::add);

        return stableEntries;
    }

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

    public boolean add(UpdateLogEntry entry) {
        if (logContainsEntryWithTimestamp(entry.getUpdateRequest())) {
            return false;
        }

        return entries.add(entry);
    }

    public List<UpdateLogEntry> anyEntryThat(Predicate<UpdateLogEntry> acceptPredicate) {
        return entries.stream()
                .filter(acceptPredicate)
                .collect(Collectors.toList());
    }

    public UpdateLog() {
        entries = Collections.synchronizedList(new ArrayList<>());
    }
}
