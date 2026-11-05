package dev.jax.jaxclient;

/** Exponentially smoothed value. Higher speed settles faster. */
final class Anim {
    private double value;
    private double target;
    private final double speed;

    Anim(double initial, double speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    void set(double newTarget) {
        target = newTarget;
    }

    void snap(double v) {
        value = v;
        target = v;
    }

    void update(double dt) {
        value += (target - value) * (1.0 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005) {
            value = target;
        }
    }

    double get() {
        return value;
    }
}
