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

    public Timestamp getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Replica ID who was the source of the update request.
     */
    private int replicaId;

    public int getReplicaId() {
        return replicaId;
    }

    private Request updateRequest;

    public Request getUpdateRequest() {
        return updateRequest;
    }

    public UpdateLogEntry(int replicaId, Timestamp updateTimestamp, Request updateRequest) {
        this.replicaId = replicaId;
        this.updateTimestamp = updateTimestamp;
        this.updateRequest = updateRequest;
    }

    @Override
    public int compareTo(UpdateLogEntry entry) {
        return updateTimestamp.compareTo(entry.getUpdateTimestamp());
    }
}
