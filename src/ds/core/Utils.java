package ds.core;

public class Utils {
    private Utils() {}

    public static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed:" + message);
        }
    }
}
