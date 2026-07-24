package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

/** One-shot, non-modal teaching moments from plan section 79. */
public enum BukovTutorialEvent {
	FIREARM_PICKUP("bukov.raid.tutorial.firearm_pickup"),
	EMPTY_MAGAZINE("bukov.raid.tutorial.empty_magazine"),
	CONTAINER_OPENED("bukov.raid.tutorial.container_opened"),
	OVERWEIGHT("bukov.raid.tutorial.overweight"),
	BLEEDING("bukov.raid.tutorial.bleeding"),
	EXTRACTION_NEAR("bukov.raid.tutorial.extraction_near"),
	BOSS_WARNING("bukov.raid.tutorial.boss_warning"),
	FIRST_DEATH("bukov.raid.tutorial.first_death");

	private final String messageKey;

	BukovTutorialEvent(String messageKey) {
		this.messageKey = messageKey;
	}

	public String message() {
		return BukovMessages.get(messageKey);
	}

	String messageKey() {
		return messageKey;
	}
}
