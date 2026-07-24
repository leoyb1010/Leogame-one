package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Converts variable render deltas into deterministic fixed simulation steps.
 */
public final class FixedStepClock {

	@FunctionalInterface
	public interface Stepper {
		void step(float deltaSeconds);
	}

	@FunctionalInterface
	public interface ConditionalStepper {
		boolean step(float deltaSeconds);
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
		return advanceWhile(frameSeconds, deltaSeconds -> {
			stepper.step(deltaSeconds);
			return true;
		});
	}

	/**
	 * Advances until the callback asks the clock to stop. Stopping clears the
	 * remaining frame backlog so a pause or terminal state cannot be replayed
	 * as catch-up simulation on the next rendered frame.
	 */
	public float advanceWhile(
			float frameSeconds,
			ConditionalStepper stepper) {
		if (stepper == null) {
			throw new IllegalArgumentException("stepper is required");
		}
		accumulator += Math.max(0f, Math.min(frameSeconds, maxFrameSeconds));
		int steps = 0;
		while (accumulator >= stepSeconds && steps < maxStepsPerFrame) {
			boolean keepAdvancing = stepper.step(stepSeconds);
			accumulator -= stepSeconds;
			steps++;
			if (!keepAdvancing) {
				accumulator = 0f;
				break;
			}
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
