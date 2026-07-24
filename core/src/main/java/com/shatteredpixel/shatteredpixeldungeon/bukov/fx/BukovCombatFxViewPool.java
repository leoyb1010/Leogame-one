package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;

/**
 * Fixed-capacity, scene-owned pool for all seven authored firearm views.
 *
 * Simulation has already committed before an event reaches this class. When a
 * category is saturated, its oldest view slot is restarted for the newest
 * event; no queue state, damage, ammo, collision, or RNG can observe that
 * choice.
 */
public final class BukovCombatFxViewPool extends Group {

	private final BukovMuzzleFx[] muzzles;
	private final BukovShellFx[] shells;
	private final BukovTracerFx[] tracers;
	private final BukovImpactFx[] impacts;
	private final BukovBloodMistFx[] bloodMists;
	private final BukovBulletMarkFx[] bulletMarks;
	private final BukovExplosionFx[] explosions;
	private final long[] muzzleStamps;
	private final long[] shellStamps;
	private final long[] tracerStamps;
	private final long[] impactStamps;
	private final long[] bloodMistStamps;
	private final long[] bulletMarkStamps;
	private final long[] explosionStamps;
	private long activationClock;
	private long reusedOldest;

	/**
	 * Loads all capacities once at scene construction. No token file access or
	 * array allocation occurs while presenting combat events.
	 */
	public BukovCombatFxViewPool(BukovUiTokens tokens) {
		this(tokens, Capacities.from(tokens));
	}

	private BukovCombatFxViewPool(
			BukovUiTokens tokens, Capacities capacities) {
		if (tokens == null) {
			throw new IllegalArgumentException("UI tokens are required");
		}
		if (capacities == null) {
			throw new IllegalArgumentException("capacities are required");
		}
		muzzles = new BukovMuzzleFx[capacities.muzzleFlash];
		shells = new BukovShellFx[capacities.shell];
		tracers = new BukovTracerFx[capacities.tracer];
		impacts = new BukovImpactFx[capacities.impactSpark];
		bloodMists = new BukovBloodMistFx[capacities.bloodMist];
		bulletMarks = new BukovBulletMarkFx[capacities.bulletMark];
		explosions = new BukovExplosionFx[capacities.explosion];
		muzzleStamps = new long[muzzles.length];
		shellStamps = new long[shells.length];
		tracerStamps = new long[tracers.length];
		impactStamps = new long[impacts.length];
		bloodMistStamps = new long[bloodMists.length];
		bulletMarkStamps = new long[bulletMarks.length];
		explosionStamps = new long[explosions.length];
		for (int index = 0; index < muzzles.length; index++) {
			muzzles[index] = new BukovMuzzleFx(tokens);
			add(muzzles[index]);
		}
		for (int index = 0; index < shells.length; index++) {
			shells[index] = new BukovShellFx(tokens);
			add(shells[index]);
		}
		for (int index = 0; index < tracers.length; index++) {
			tracers[index] = new BukovTracerFx(tokens);
			add(tracers[index]);
		}
		for (int index = 0; index < impacts.length; index++) {
			impacts[index] = new BukovImpactFx(tokens);
			add(impacts[index]);
		}
		for (int index = 0; index < bloodMists.length; index++) {
			bloodMists[index] = new BukovBloodMistFx(tokens);
			add(bloodMists[index]);
		}
		for (int index = 0; index < bulletMarks.length; index++) {
			bulletMarks[index] = new BukovBulletMarkFx(tokens);
			add(bulletMarks[index]);
		}
		for (int index = 0; index < explosions.length; index++) {
			explosions[index] = new BukovExplosionFx(tokens);
			add(explosions[index]);
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
		float flashScale =
				BukovAccessibilityPresentation.flashScale(
						SPDSettings.bukovReduceFlashes());
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
							event.intensity(),
							flashScale)) {
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
			case BLOOD_MIST:
				if (vectorVisible(directionX, directionY)) {
					int bloodMist = acquire(bloodMists, bloodMistStamps);
					if (bloodMists[bloodMist].reset(
							toX,
							toY,
							directionX,
							directionY,
							event.hostile(),
							event.intensity())) {
						stamp(bloodMistStamps, bloodMist);
					}
				}
				break;
			case BULLET_MARK:
				if (vectorVisible(directionX, directionY)) {
					int bulletMark = acquire(bulletMarks, bulletMarkStamps);
					if (bulletMarks[bulletMark].reset(
							toX,
							toY,
							directionX,
							directionY,
							event.hostile(),
							event.intensity())) {
						stamp(bulletMarkStamps, bulletMark);
					}
				}
				break;
			case EXPLOSION:
				if (finite(toX) && finite(toY)) {
					int explosion = acquire(explosions, explosionStamps);
					if (explosions[explosion].reset(
							toX,
							toY,
							event.hostile(),
							event.intensity(),
							flashScale)) {
						stamp(explosionStamps, explosion);
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
				+ activeCount(impacts)
				+ activeCount(bloodMists)
				+ activeCount(bulletMarks)
				+ activeCount(explosions);
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

	/**
	 * Pure capacity snapshot used both by the scene and by validation tests.
	 */
	static final class Capacities {

		private static final int MAX_PER_TYPE = 256;

		final int muzzleFlash;
		final int tracer;
		final int shell;
		final int impactSpark;
		final int bloodMist;
		final int bulletMark;
		final int explosion;

		private Capacities(
				int muzzleFlash,
				int tracer,
				int shell,
				int impactSpark,
				int bloodMist,
				int bulletMark,
				int explosion) {
			this.muzzleFlash = bounded(muzzleFlash, "muzzleFlash");
			this.tracer = bounded(tracer, "tracer");
			this.shell = bounded(shell, "shell");
			this.impactSpark = bounded(impactSpark, "impactSpark");
			this.bloodMist = bounded(bloodMist, "bloodMist");
			this.bulletMark = bounded(bulletMark, "bulletMark");
			this.explosion = bounded(explosion, "explosion");
		}

		static Capacities from(BukovUiTokens tokens) {
			if (tokens == null) {
				throw new IllegalArgumentException("UI tokens are required");
			}
			return new Capacities(
					tokens.vfxPoolCapacity("muzzleFlash"),
					tokens.vfxPoolCapacity("tracer"),
					tokens.vfxPoolCapacity("shell"),
					tokens.vfxPoolCapacity("impactSpark"),
					tokens.vfxPoolCapacity("bloodMist"),
					tokens.vfxPoolCapacity("bulletMark"),
					tokens.vfxPoolCapacity("explosion"));
		}

		int forType(CombatFxEvent.Type type) {
			if (type == null) {
				throw new IllegalArgumentException("type is required");
			}
			switch (type) {
				case MUZZLE_FLASH:
					return muzzleFlash;
				case TRACER:
					return tracer;
				case SHELL:
					return shell;
				case IMPACT:
					return impactSpark;
				case BLOOD_MIST:
					return bloodMist;
				case BULLET_MARK:
					return bulletMark;
				case EXPLOSION:
					return explosion;
				default:
					throw new AssertionError("all combat FX types are mapped");
			}
		}

		private static int bounded(int value, String label) {
			if (value <= 0 || value > MAX_PER_TYPE) {
				throw new IllegalArgumentException(
						label + " capacity must be between 1 and "
								+ MAX_PER_TYPE);
			}
			return value;
		}
	}
}
