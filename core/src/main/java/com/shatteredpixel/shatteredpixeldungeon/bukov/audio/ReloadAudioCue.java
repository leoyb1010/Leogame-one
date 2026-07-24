package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Mechanical phases shared by every detachable or internal reload motion. */
public enum ReloadAudioCue {
	MAG_OUT(1, Assets.Sounds.Bukov.RELOAD_MAG_OUT),
	MAG_IN(1 << 1, Assets.Sounds.Bukov.RELOAD_MAG_IN),
	CHARGE(1 << 2, Assets.Sounds.Bukov.RELOAD_CHARGE);

	public final int mask;
	private final String asset;

	ReloadAudioCue(int mask, String asset) {
		this.mask = mask;
		this.asset = asset;
	}

	public String asset() {
		return asset;
	}
}
