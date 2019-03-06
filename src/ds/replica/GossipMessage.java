package ds.replica;


import ds.core.Timestamp;
import ds.replica.updatelog.UpdateLogEntry;

import java.io.Serializable;
import java.util.List;

/**
 * A gossip message sent between replicas R and R' contains 3 things:
 * - Sender if of R
 * - All entries R things R' has not seen
 * - Replica timestamp of R
 */
class GossipMessage implements Serializable {
    private int senderId;

    private List<UpdateLogEntry> updateLogEntries;

    private Timestamp replicaTimestamp;

    GossipMessage(int senderId, List<UpdateLogEntry> updateLogEntries, Timestamp replicaTimestamp) {
        this.senderId = senderId;
        this.updateLogEntries = updateLogEntries;
        this.replicaTimestamp = replicaTimestamp;
    }

    List<UpdateLogEntry> getUpdateLogEntries() {
        return updateLogEntries;
    }

    Timestamp getReplicaTimestamp() {
        return replicaTimestamp;
    }

    int getSenderId() {
        return senderId;
    }

    @Override
    public String toString() {
        return String.format("[ Gossip. Sender %d. Replica Timestamp: %s. #Updates %d ]",
                senderId, replicaTimestamp, updateLogEntries.size());
    }
}
