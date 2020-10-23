package fr.roboteek.robot.util.commons;

public class RandomUtils {

    public static int nextInt(final int startInclusive, final int endInclusive) {
        return org.apache.commons.lang3.RandomUtils.nextInt(0, endInclusive - startInclusive) + startInclusive;
    }

    public static long nextLong(final long startInclusive, final long endInclusive) {
        return org.apache.commons.lang3.RandomUtils.nextLong(0, endInclusive - startInclusive) + startInclusive;
    }

    public static double nextDouble(final double startInclusive, final double endInclusive) {
        return org.apache.commons.lang3.RandomUtils.nextDouble(0, endInclusive - startInclusive) + startInclusive;
    }

    public static boolean nextBoolean() {
        return org.apache.commons.lang3.RandomUtils.nextBoolean();
    }
}
