package ds.core;

import java.io.Serializable;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ds.core.Utils.assertCondition;

/**
 * The timestamp is used within the distributed network to describe the state of the network in terms of num. of updatelog
 * Ie a timestamp of (1, 1, 3) associated with some data could mean, this data is valid according to the state
 * replica 1 was in after 1 update, replica 2 was in after 1 update and replica 3 after 3 updatelog.
 * Timestamps are used primary for consistency
 */
public class Timestamp implements Comparable<Timestamp>, Serializable {
    private List<Integer> list;

    public int getDimension() {
        return list.size();
    }

    public int get(int i) {
        return list.get(i);
    }

    public void set(int i, int k) {
        list.set(i, k);
    }

    public Timestamp(int capacity) {
        list = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; ++i) {
            list.add(i, 0);
        }
    }

    public Timestamp(List<Integer> list) { this.list = list; }

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

    public boolean isBefore(Timestamp timestamp) {
        return !isAfter(timestamp);
    }

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

        return this.compareTo((Timestamp)t) == 0;
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

    public Timestamp(int... nums) {
        this.list = new ArrayList<>();
        for (int i : nums) {
            list.add(i);
        }
    }

    public static void main(String[] args) {
        Timestamp t1 = new Timestamp(1, 2, 3);
        Timestamp t2 = new Timestamp(4, 5, 6);
        Timestamp t3 = new Timestamp(3, 5, 6);
        Timestamp t4 = new Timestamp(3, 5, 6);
        Timestamp t5 = new Timestamp(1, 0, 0);
        Timestamp t6 = new Timestamp(0, 1, 0);

        assertCondition(t1.isBefore(t2), "(1, 2, 3) <= (4, 5, 6)");
        assertCondition(!t3.isBefore(t1), "(3, 5, 6) </= (1, 2, 3)");
        assertCondition(t3.isBeforeOrEqual(t3), "(3, 5, 6) <= (3, 5, 6)");
        assertCondition(t3.isBeforeOrEqual(t2), "(3, 5, 6) <= (4, 5, 6)");
        assertCondition(t3.equals(t4), "(3, 5, 6) = (3, 5, 6)");
        assertCondition(t5.isAfter(t6), "(1, 0, 0) </= (0, 1, 0)");
    }
}

