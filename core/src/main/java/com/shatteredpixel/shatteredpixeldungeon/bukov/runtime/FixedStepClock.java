package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Converts variable render deltas into deterministic fixed simulation steps.
 */
public final class FixedStepClock {

	@FunctionalInterface
	public interface Stepper {
		void step(float deltaSeconds);
	}

	private final float stepSeconds;
	private final float maxFrameSeconds;
	private final int maxStepsPerFrame;
	private float accumulator;

	public FixedStepClock(float updatesPerSecond, float maxFrameSeconds, int maxStepsPerFrame) {
		if (updatesPerSecond <= 0f) {
			throw new IllegalArgumentException("updatesPerSecond must be > 0");
		}
		if (maxFrameSeconds <= 0f) {
			throw new IllegalArgumentException("maxFrameSeconds must be > 0");
		}
		if (maxStepsPerFrame <= 0) {
			throw new IllegalArgumentException("maxStepsPerFrame must be > 0");
		}
		stepSeconds = 1f / updatesPerSecond;
		this.maxFrameSeconds = maxFrameSeconds;
		this.maxStepsPerFrame = maxStepsPerFrame;
	}

	public float advance(float frameSeconds, Stepper stepper) {
		if (stepper == null) {
			throw new IllegalArgumentException("stepper is required");
		}
		accumulator += Math.max(0f, Math.min(frameSeconds, maxFrameSeconds));
		int steps = 0;
		while (accumulator >= stepSeconds && steps < maxStepsPerFrame) {
			stepper.step(stepSeconds);
			accumulator -= stepSeconds;
			steps++;
		}
		if (steps == maxStepsPerFrame && accumulator >= stepSeconds) {
			accumulator %= stepSeconds;
		}
		return alpha();
	}

	public float stepSeconds() {
		return stepSeconds;
	}

	public float alpha() {
		return accumulator / stepSeconds;
	}

	public void reset() {
		accumulator = 0f;
	}
}
