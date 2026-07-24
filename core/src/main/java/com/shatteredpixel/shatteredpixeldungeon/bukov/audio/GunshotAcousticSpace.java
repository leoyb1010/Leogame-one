package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/**
 * Authored gunshot decay selected from the local enclosure around the source.
 */
public enum GunshotAcousticSpace {
	INDOOR(Assets.Sounds.Bukov.GUNSHOT_TAIL_INDOOR),
	CORRIDOR(Assets.Sounds.Bukov.GUNSHOT_TAIL_CORRIDOR),
	OPEN(Assets.Sounds.Bukov.GUNSHOT_TAIL_OPEN);

	private final String[] tailAssets;

	GunshotAcousticSpace(String[] tailAssets) {
		this.tailAssets = tailAssets;
	}

	public String tailAsset(int sequence) {
		return tailAssets[GunshotVariantResolver.index(sequence)];
	}
}
