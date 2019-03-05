package ds.replica;

import java.io.Serializable;

public enum ReplicaStatus implements Serializable  {
    ACTIVE,
    OVERLOADED,
    OFFLINE;

    public static ReplicaStatus fromInteger(int id) {
        switch (id) {
            case 0: return ACTIVE;
            case 1: return OVERLOADED;
            case 2: return OFFLINE;
            default: throw new IllegalArgumentException("invalid id");
        }
    }
}
