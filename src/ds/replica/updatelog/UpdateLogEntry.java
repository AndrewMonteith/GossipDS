package ds.replica.updatelog;

import ds.core.Timestamp;
import ds.frontend.Request;

import java.io.Serializable;

public class UpdateLogEntry implements Comparable<UpdateLogEntry>, Serializable {

    /**
     * The value timestamp will become up to date with this timestamp
     * after the mutation is applied to the value.
     */
    private Timestamp updateTimestamp;
    /**
     * Replica ID who was the source of the update request.
     */
    private int replicaId;
    private Request updateRequest;

    public UpdateLogEntry(int replicaId, Timestamp updateTimestamp, Request updateRequest) {
        this.replicaId = replicaId;
        this.updateTimestamp = updateTimestamp;
        this.updateRequest = updateRequest;
    }

    public Timestamp getUpdateTimestamp() {
        return updateTimestamp;
    }

    public int getReplicaId() {
        return replicaId;
    }

    public Request getUpdateRequest() {
        return updateRequest;
    }

    @Override
    public int compareTo(UpdateLogEntry entry) {
        return updateTimestamp.compareTo(entry.getUpdateTimestamp());
    }
}
