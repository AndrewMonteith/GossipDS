package ds.frontend;

import ds.core.Timestamp;
import ds.client.RequestParameters;

import java.io.Serializable;
import java.util.UUID;

public class Request implements Serializable {
    /**
     * What the actual request is wanting to do.
     */
    private RequestParameters parameters;

    public RequestParameters getParameters() {
        return parameters;
    }

    /**
     * Timestamp sent by the frontend to one of the replicas.
     * Describes the timestamp that a replica should be in to be considered valid.
     */
    private Timestamp timestamp;

    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Unique ID that describes the operation we want to the data
     */
    private UUID uid;
    public UUID getUid() {
        return uid;
    }

    public Request(RequestParameters parameters, Timestamp timestamp) {
        this.parameters = parameters;
        this.timestamp = timestamp;
        uid = UUID.randomUUID();
    }

    public boolean isStable(Timestamp valueTimestamp) {
        return timestamp.isBeforeOrEqual(valueTimestamp);
    }
}
