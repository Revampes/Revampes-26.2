package xyz.revampes.Revamp.utility.dungeon.map;

public final class MapMath {

    private MapMath() {
    }

    public static double rescale(double value, double inMin, double inMax, double outMin, double outMax) {
        if (inMax - inMin == 0) return outMin;
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
