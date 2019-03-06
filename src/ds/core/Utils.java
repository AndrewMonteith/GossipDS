package ds.core;

class Utils {
    private Utils() {
    }

    static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed:" + message);
        }
    }
}
