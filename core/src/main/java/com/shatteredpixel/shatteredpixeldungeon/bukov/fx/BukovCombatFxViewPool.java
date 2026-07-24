package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;

/**
 * Fixed-capacity, scene-owned pool for the four high-frequency firearm views.
 *
 * Simulation has already committed before an event reaches this class. When a
 * category is saturated, its oldest view slot is restarted for the newest
 * event; no queue state, damage, ammo, collision, or RNG can observe that
 * choice.
 */
public final class BukovCombatFxViewPool extends Group {

	public static final int MUZZLE_CAPACITY = 16;
	public static final int SHELL_CAPACITY = 16;
	public static final int TRACER_CAPACITY = 24;
	public static final int IMPACT_CAPACITY = 24;

	private final BukovMuzzleFx[] muzzles =
			new BukovMuzzleFx[MUZZLE_CAPACITY];
	private final BukovShellFx[] shells =
			new BukovShellFx[SHELL_CAPACITY];
	private final BukovTracerFx[] tracers =
			new BukovTracerFx[TRACER_CAPACITY];
	private final BukovImpactFx[] impacts =
			new BukovImpactFx[IMPACT_CAPACITY];
	private final long[] muzzleStamps = new long[MUZZLE_CAPACITY];
	private final long[] shellStamps = new long[SHELL_CAPACITY];
	private final long[] tracerStamps = new long[TRACER_CAPACITY];
	private final long[] impactStamps = new long[IMPACT_CAPACITY];
	private long activationClock;
	private long reusedOldest;

	public BukovCombatFxViewPool() {
		for (int index = 0; index < muzzles.length; index++) {
			muzzles[index] = new BukovMuzzleFx();
			add(muzzles[index]);
		}
		for (int index = 0; index < shells.length; index++) {
			shells[index] = new BukovShellFx();
			add(shells[index]);
		}
		for (int index = 0; index < tracers.length; index++) {
			tracers[index] = new BukovTracerFx();
			add(tracers[index]);
		}
		for (int index = 0; index < impacts.length; index++) {
			impacts[index] = new BukovImpactFx();
			add(impacts[index]);
		}
	}

	/**
	 * Presents one already-filtered cosmetic event in world-space pixels.
	 */
	public void present(CombatFxEvent event, float tileSize) {
		if (event == null || !finite(tileSize) || tileSize <= 0f) return;
		float fromX = event.fromX() * tileSize;
		float fromY = event.fromY() * tileSize;
		float toX = event.toX() * tileSize;
		float toY = event.toY() * tileSize;
		float directionX = event.toX() - event.fromX();
		float directionY = event.toY() - event.fromY();
		switch (event.type()) {
			case MUZZLE_FLASH:
				if (vectorVisible(directionX, directionY)) {
					int muzzle = acquire(muzzles, muzzleStamps);
					if (muzzles[muzzle].reset(
							fromX,
							fromY,
							directionX,
							directionY,
							event.hostile(),
							event.intensity())) {
						stamp(muzzleStamps, muzzle);
					}
				}
				break;
			case SHELL:
				if (vectorVisible(directionX, directionY)) {
					int shell = acquire(shells, shellStamps);
					if (shells[shell].reset(
							fromX,
							fromY,
							directionX,
							directionY,
							event.hostile(),
							event.intensity())) {
						stamp(shellStamps, shell);
					}
				}
				break;
			case TRACER:
				if (vectorVisible(toX - fromX, toY - fromY)) {
					int tracer = acquire(tracers, tracerStamps);
					if (tracers[tracer].reset(
							fromX,
							fromY,
							toX,
							toY,
							event.hostile(),
							event.intensity())) {
						stamp(tracerStamps, tracer);
					}
				}
				break;
			case IMPACT:
				if (finite(toX) && finite(toY)) {
					int impact = acquire(impacts, impactStamps);
					if (impacts[impact].reset(
							toX,
							toY,
							event.hostile(),
							event.intensity())) {
						stamp(impactStamps, impact);
					}
				}
				break;
			default:
				break;
		}
	}

	public int activeCount() {
		return activeCount(muzzles)
				+ activeCount(shells)
				+ activeCount(tracers)
				+ activeCount(impacts);
	}

	public long reusedOldest() {
		return reusedOldest;
	}

	static int oldestOrFree(boolean[] active, long[] stamps) {
		if (active == null || stamps == null
				|| active.length == 0 || active.length != stamps.length) {
			throw new IllegalArgumentException(
					"matching non-empty slot arrays are required");
		}
		int oldest = 0;
		for (int index = 0; index < active.length; index++) {
			if (!active[index]) return index;
			if (stamps[index] < stamps[oldest]) oldest = index;
		}
		return oldest;
	}

	private int acquire(Gizmo[] views, long[] stamps) {
		boolean foundFree = false;
		int selected = 0;
		for (int index = 0; index < views.length; index++) {
			if (!views[index].exists) {
				selected = index;
				foundFree = true;
				break;
			}
			if (stamps[index] < stamps[selected]) selected = index;
		}
		if (!foundFree) reusedOldest++;
		return selected;
	}

	private void stamp(long[] stamps, int index) {
		stamps[index] = ++activationClock;
	}

	private static int activeCount(Gizmo[] views) {
		int result = 0;
		for (Gizmo view : views) {
			if (view.exists) result++;
		}
		return result;
	}

	private static boolean vectorVisible(float x, float y) {
		return finite(x)
				&& finite(y)
				&& x * x + y * y > 0.0001f;
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}
}
