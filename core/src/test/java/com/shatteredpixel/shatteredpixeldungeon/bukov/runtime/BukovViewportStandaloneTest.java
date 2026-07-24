package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Standalone regression test for the production viewport guard. This can run
 * without Gradle, libGDX or an OpenGL context.
 */
public final class BukovViewportStandaloneTest {

	public static void main(String[] args) {
		followsAcrossMoreThanOneScreen();
		clampsAtAuthoredMapEdges();
		centersMapsSmallerThanTheViewport();
		alignsScrollToPhysicalPixels();
		keepsValidSmoothFollowPosition();
		System.out.println("PASS: Bukov viewport guard");
	}

	private static void followsAcrossMoreThanOneScreen() {
		float scroll = 0f;
		for (float focus = 80f; focus <= 880f; focus += 8f) {
			scroll = BukovViewport.resolveScroll(
					scroll, focus, 240f, 1_024f, 5f);
		}
		assertTrue(scroll > 600f,
				"camera must cross several view widths with the operator");
	}

	private static void clampsAtAuthoredMapEdges() {
		assertNear(0f, BukovViewport.resolveScroll(
				-400f, 5f, 240f, 1_024f, 4f), 0.0001f,
				"left edge");
		assertNear(784f, BukovViewport.resolveScroll(
				2_000f, 1_020f, 240f, 1_024f, 4f), 0.0001f,
				"right edge");
	}

	private static void centersMapsSmallerThanTheViewport() {
		assertNear(-80f, BukovViewport.resolveScroll(
				0f, 40f, 240f, 80f, 4f), 0.0001f,
				"small map centering");
	}

	private static void alignsScrollToPhysicalPixels() {
		float scroll = BukovViewport.resolveScroll(
				123.123f, 240f, 240f, 1_024f, 5f);
		assertNear(
				Math.round(scroll * 5f),
				scroll * 5f,
				0.0001f,
				"pixel alignment");
	}

	private static void keepsValidSmoothFollowPosition() {
		assertNear(120f, BukovViewport.resolveScroll(
				120f, 240f, 240f, 1_024f, 4f), 0.0001f,
				"smooth follower ownership");
	}

	private static void assertTrue(boolean condition, String label) {
		if (!condition) {
			throw new AssertionError(label);
		}
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
