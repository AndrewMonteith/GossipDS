package ds.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The timestamp is used within the distributed network to describe the state of the network in terms of num. of updatelog
 * Ie a timestamp of (1, 1, 3) associated with some data could mean, this data is valid according to the state
 * replica 1 was in after 1 update, replica 2 was in after 1 update and replica 3 after 3 updatelog.
 * Timestamps are used primary for consistency
 */
public class Timestamp implements Comparable<Timestamp>, Serializable {
    private List<Integer> list;

    public Timestamp(int capacity) {
        list = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; ++i) {
            list.add(i, 0);
        }
    }

    public int getDimension() {
        return list.size();
    }

    public int get(int i) {
        return list.get(i);
    }

    public void set(int i, int k) {
        list.set(i, k);
    }

    public boolean isAfter(Timestamp timestamp) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i) > timestamp.get(i)) {
                return true;
            }
        }

        return false;
    }

    private boolean isEqual(Timestamp timestamp) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i) != timestamp.get(i)) {
                return false;
            }
        }

        return true;
    }

    private boolean isBefore(Timestamp timestamp) {
        return !isAfter(timestamp);
    }

    /**
     * Partial order <= for a timestamp
     * @param timestamp
     * @return this <= timestamp
     */
    public boolean isBeforeOrEqual(Timestamp timestamp) {
        return isBefore(timestamp) || isEqual(timestamp);
    }

    public void merge(Timestamp t) {
        for (int i = 0; i < list.size(); ++i) {
            list.set(i, Math.max(list.get(i), t.list.get(i)));
        }
    }

    public Timestamp copy() {
        Timestamp t = new Timestamp(list.size());
        t.merge(this);
        return t;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof Timestamp)) {
            return false;
        }

        return this.compareTo((Timestamp) t) == 0;
    }

    @Override
    public int compareTo(Timestamp timestamp) {
        if (isEqual(timestamp)) {
            return 0;
        } else if (isAfter(timestamp)) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(list.toArray());
    }
}

