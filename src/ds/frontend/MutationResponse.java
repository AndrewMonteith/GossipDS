package ds.frontend;

import ds.core.Timestamp;

import java.io.Serializable;

/**
 * Response given to a frontend after a mutation request.
 */
public class MutationResponse implements Serializable {
    /**
     * Whether the request was a success
     */
    private boolean success;

    /**
     * New frontend timestamp
     */
    private Timestamp timestamp;

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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    boolean isSuccess() {
        return success;
    }
}
