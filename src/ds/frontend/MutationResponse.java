package ds.frontend;

import ds.core.Timestamp;

import java.io.Serializable;

public class MutationResponse implements Serializable {
    private boolean success;
    private Timestamp timestamp;

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    private MutationResponse(Timestamp timestamp, boolean success) {
        this.timestamp = timestamp;
        this.success = success;
    }

    public static MutationResponse wasSuccessful(Timestamp timestamp) {
        return new MutationResponse(timestamp, true);
    }

    public static MutationResponse wasFailure(Timestamp timestamp) {
        return new MutationResponse(timestamp, false);
    }
}
