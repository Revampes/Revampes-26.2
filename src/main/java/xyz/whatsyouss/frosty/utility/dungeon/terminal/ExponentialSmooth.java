package xyz.whatsyouss.frosty.utility.dungeon.terminal;

/**
 * Exponential smoothing in wall-clock time.
 *
 * <p>Port of yqloss' {@code ExponentialSmooth}: every animated value in the Better Terminal
 * UI (slot colours, hover lightness, press size, the title cross-fade) is an instance of this,
 * so the motion matches the original frame-rate independently instead of being tick-bound.
 */
public final class ExponentialSmooth {

    private double value;
    private long last = Long.MIN_VALUE;

    public ExponentialSmooth(double initial) {
        this.value = initial;
    }

    public double value() {
        return value;
    }

    /** Approaches {@code target}; the first call after a reset snaps straight to it. */
    public double approach(double target, double speed) {
        long now = System.nanoTime();
        long diff = now - (last == Long.MIN_VALUE ? now : last);
        last = now;
        value = target - (target - value) * Math.pow(1.0 - speed, diff / 50_000_000.0);
        return value;
    }

    /** Like {@link #approach} but snaps on the very first call. */
    public double approachOrSet(double target, double speed) {
        long now = System.nanoTime();
        long diff = now - (last == Long.MIN_VALUE ? now : last);
        value = last == Long.MIN_VALUE
                ? target
                : target - (target - value) * Math.pow(1.0 - speed, diff / 50_000_000.0);
        last = now;
        return value;
    }

    public void set(double target) {
        value = target;
        last = System.nanoTime();
    }

    public void reset(double target) {
        value = target;
        last = Long.MIN_VALUE;
    }
}
