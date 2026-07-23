package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Standalone test entry point. Run through scripts/bukov_realtime_camera_test.sh
 * without Gradle, libGDX, or a graphics context.
 */
public final class RealtimeCameraFollowStandaloneTest {

	public static void main(String[] args) {
		staysStillInsideDeadZone();
		tracksAcrossManyScreens();
		convergesNearlyEquallyAtDifferentFrameRates();
		System.out.println("PASS: Bukov realtime camera follow");
	}

	private static void staysStillInsideDeadZone() {
		RealtimeCameraFollow follow = new RealtimeCameraFollow(12f, 8f, 8f);
		follow.reset(100f, 100f);
		follow.update(111.9f, 107.9f, 1f / 60f);
		assertNear(100f, follow.centerX(), 0.0001f, "x dead zone");
		assertNear(100f, follow.centerY(), 0.0001f, "y dead zone");
	}

	private static void tracksAcrossManyScreens() {
		RealtimeCameraFollow follow = new RealtimeCameraFollow(12f, 8f, 8f);
		follow.reset(0f, 0f);
		float previous = follow.centerX();
		for (int frame = 0; frame < 180; frame++) {
			follow.update(2_000f, 1_000f, 1f / 60f);
			if (follow.centerX() < previous) {
				throw new AssertionError("camera must move monotonically");
			}
			previous = follow.centerX();
		}
		assertNear(1_988f, follow.centerX(), 0.01f,
				"x must converge after crossing many view widths");
		assertNear(992f, follow.centerY(), 0.01f,
				"y must converge after crossing many view heights");
	}

	private static void convergesNearlyEquallyAtDifferentFrameRates() {
		float at30 = simulate(30);
		float at120 = simulate(120);
		assertNear(at30, at120, 0.02f,
				"exponential response must be frame-rate independent");
	}

	private static float simulate(int fps) {
		RealtimeCameraFollow follow = new RealtimeCameraFollow(12f, 8f, 8f);
		follow.reset(0f, 0f);
		for (int frame = 0; frame < fps; frame++) {
			follow.update(500f, 0f, 1f / fps);
		}
		return follow.centerX();
	}

	private static void assertNear(
			float expected,
			float actual,
			float tolerance,
			String label) {
		if (Math.abs(expected - actual) > tolerance) {
			throw new AssertionError(
					label + ": expected " + expected + ", got " + actual);
		}
	}
}
