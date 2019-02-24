package ds.replica;

import java.io.Serializable;

public enum ReplicaStatus implements Serializable  {
    ACTIVE,
    OVERLOADED,
    OFFLINE
}
