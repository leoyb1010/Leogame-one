package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/** Guaranteed, non-droppable first-raid objective document. */
public final class BukovMissionArchive extends Item
		implements BukovEconomicItem {

	public BukovMissionArchive() {
		image = ItemSpriteSheet.DOCUMENT_HOLDER;
		stackable = false;
		unique = true;
	}

	@Override
	public String bukovDefinitionId() {
		return FirstRaidMission.ARCHIVE_DEFINITION_ID;
	}

	@Override
	public float bukovUnitWeight() {
		// Objective pickup must succeed even at the normal weight cap.
		return 0f;
	}

	@Override
	public int bukovUnitValue() {
		return 0;
	}

	@Override
	public String name() {
		return BukovMessages.get(
				"bukov.economy.content.mission_archive_name");
	}

	@Override
	public String desc() {
		return BukovMessages.get(
				"bukov.economy.content.mission_archive_desc");
	}

	@Override
	public int value() {
		return 0;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		// This host item normally lives in the raid ledger, but returning no
		// actions also prevents accidental loss if a future inventory adapter
		// chooses to display it in the regular bag.
		return new ArrayList<>();
	}

	@Override
	public boolean isSimilar(Item item) {
		return item == this;
	}
}
