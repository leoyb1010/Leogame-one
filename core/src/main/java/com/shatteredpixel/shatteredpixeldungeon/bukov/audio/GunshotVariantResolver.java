package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/** Deterministic audio-only variant selection that never consumes gameplay RNG. */
public final class GunshotVariantResolver {

	public static final int VARIANT_COUNT = 3;

	public static int index(int sequence) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.floorMod(sequence, VARIANT_COUNT);
	}

	private GunshotVariantResolver() {
	}
}
