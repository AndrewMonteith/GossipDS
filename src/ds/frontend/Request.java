package ds.frontend;

import ds.client.RequestParameters;
import ds.core.Timestamp;

import java.io.Serializable;
import java.util.UUID;

/**
 * A generic request made to a replica.
 * We make no Java distinction between mutator requests and query requests
 * since there is no need in this toy example.
 */
public class Request implements Serializable {
    /**
     * What the actual request is wanting to do.
     */
    private RequestParameters parameters;
    /**
     * Timestamp sent by the frontend to one of the replicas.
     * Describes the timestamp that a replica should be in to be considered valid.
     */
    private Timestamp timestamp;
    /**
     * Unique ID that describes the operation we want to the data
     */
    private UUID uid;

    Request(RequestParameters parameters, Timestamp timestamp) {
        this.parameters = parameters;
        this.timestamp = timestamp;
        uid = UUID.randomUUID();
    }

    public RequestParameters getParameters() {
        return parameters;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public UUID getUid() {
        return uid;
    }

    public boolean isStable(Timestamp valueTimestamp) {
        return timestamp.isBeforeOrEqual(valueTimestamp);
    }
}
