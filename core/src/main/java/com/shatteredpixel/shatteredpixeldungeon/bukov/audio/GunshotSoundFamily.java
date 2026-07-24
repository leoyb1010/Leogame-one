package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Six independently synthesized firearm timbres with three authored variants. */
public enum GunshotSoundFamily {
	PISTOL(
			Assets.Sounds.Bukov.GUNSHOT_PISTOL_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_PISTOL_BODY),
	SMG(
			Assets.Sounds.Bukov.GUNSHOT_SMG_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_SMG_BODY),
	CARBINE(
			Assets.Sounds.Bukov.GUNSHOT_CARBINE_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_CARBINE_BODY),
	RIFLE(
			Assets.Sounds.Bukov.GUNSHOT_RIFLE_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_RIFLE_BODY),
	SHOTGUN(
			Assets.Sounds.Bukov.GUNSHOT_SHOTGUN_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_SHOTGUN_BODY),
	HEAVY(
			Assets.Sounds.Bukov.GUNSHOT_HEAVY_MECHANICAL,
			Assets.Sounds.Bukov.GUNSHOT_HEAVY_BODY);

	private final String[] mechanicalAssets;
	private final String[] bodyAssets;

	GunshotSoundFamily(String[] mechanicalAssets, String[] bodyAssets) {
		this.mechanicalAssets = mechanicalAssets;
		this.bodyAssets = bodyAssets;
	}

	public String mechanicalAsset(int sequence) {
		return mechanicalAssets[GunshotVariantResolver.index(sequence)];
	}

	public String bodyAsset(int sequence) {
		return bodyAssets[GunshotVariantResolver.index(sequence)];
	}
}
