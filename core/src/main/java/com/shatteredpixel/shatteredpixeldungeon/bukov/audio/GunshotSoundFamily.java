package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Six independently synthesized firearm body timbres. */
public enum GunshotSoundFamily {
	PISTOL(Assets.Sounds.Bukov.GUNSHOT_PISTOL),
	SMG(Assets.Sounds.Bukov.GUNSHOT_SMG),
	CARBINE(Assets.Sounds.Bukov.GUNSHOT_CARBINE),
	RIFLE(Assets.Sounds.Bukov.GUNSHOT_RIFLE),
	SHOTGUN(Assets.Sounds.Bukov.GUNSHOT_SHOTGUN),
	HEAVY(Assets.Sounds.Bukov.GUNSHOT_HEAVY);

	private final String asset;

	GunshotSoundFamily(String asset) {
		this.asset = asset;
	}

	public String asset() {
		return asset;
	}
}
