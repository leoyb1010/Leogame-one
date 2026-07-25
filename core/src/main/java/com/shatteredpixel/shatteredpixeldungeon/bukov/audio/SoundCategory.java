package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

public enum SoundCategory {
	PLAYER_GUNSHOT,
	ENEMY_GUNSHOT,
	FOOTSTEP,
	COMBAT_FEEDBACK,
	BOSS_CUE,
	EXTRACTION_CUE,
	UI,
	AMBIENCE;

	public boolean visualizable() {
		return this != UI && this != AMBIENCE;
	}
}
