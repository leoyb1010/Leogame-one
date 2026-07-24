package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/**
 * One-shot in-process proof that BukovDeploymentScene just created the host
 * map which GameScene is about to bind to a new checkpoint.
 *
 * The token is deliberately not persisted. After a crash, a host map without
 * a checkpoint is therefore classified as an orphan and archived instead of
 * receiving another loadout.
 */
public final class BukovDeploymentHandoff {

	private static long authorizedSeed;
	private static boolean authorized;

	private BukovDeploymentHandoff() {
	}

	public static synchronized void authorizeFreshHost(long seed) {
		authorizedSeed = seed;
		authorized = true;
	}

	public static synchronized boolean consumeFreshHost(long seed) {
		boolean matches = authorized && authorizedSeed == seed;
		authorized = false;
		authorizedSeed = 0L;
		return matches;
	}

	public static synchronized void clear() {
		authorized = false;
		authorizedSeed = 0L;
	}
}
