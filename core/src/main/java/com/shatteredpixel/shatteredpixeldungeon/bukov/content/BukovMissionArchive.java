package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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
		return "维修通道档案";
	}

	@Override
	public String desc() {
		return "记录了雾灯泵站维修通道的机械解锁顺序。任务物品，不可丢弃。";
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
