package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovInteractionMarker;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovItemSprite;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class BukovItemVisualMappingTest {

	@Test
	public void frameContractIsStableAndComplete() {
		assertEquals(72, BukovItemSprite.FRAME_COUNT);
		assertEquals(0, BukovItemSprite.Frame.FIREARM_NEEDLE_9.index());
		assertEquals(5, BukovItemSprite.Frame.FIREARM_LONGSTREET_762.index());
		assertEquals(17, BukovItemSprite.Frame.FIREARM_FRONTIER_762.index());
		assertEquals(18, BukovItemSprite.Frame.AMMO_9_TRAINING.index());
		assertEquals(24, BukovItemSprite.Frame.AMMO_762_EXPANDING.index());
		assertEquals(33, BukovItemSprite.Frame.MEDICAL_FIRST_AID.index());
		assertEquals(37, BukovItemSprite.Frame.MEDICAL_STIM.index());
		assertEquals(68, BukovItemSprite.Frame.MISSION_ARCHIVE.index());
		assertEquals(69, BukovItemSprite.Frame.FIXED_EXTRACTION.index());
		assertEquals(70,
				BukovItemSprite.Frame.CONDITIONAL_EXTRACTION.index());
		assertEquals(71, BukovItemSprite.Frame.PUMP_STATION.index());
	}

	@Test
	public void commonBukovItemsNeverMapThroughLegacyImageIds() {
		assertEquals(
				BukovItemSprite.Frame.FIREARM_NEEDLE_9,
				BukovItemSprite.frameFor(new Firearm()));
		assertEquals(
				BukovItemSprite.Frame.AMMO_9_STANDARD,
				BukovItemSprite.frameFor(new AmmoStack()));
		assertEquals(
				BukovItemSprite.Frame.MISSION_ARCHIVE,
				BukovItemSprite.frameFor(new BukovMissionArchive()));
		assertEquals(
				BukovItemSprite.Frame.MEDICAL_TRAUMA_POUCH,
				BukovItemSprite.frameFor(new BukovLootItem().configure(
						"med:test",
						"Test Medkit",
						BukovLootItem.Category.MEDICAL,
						0.5f,
						120)));
		assertEquals(
				BukovItemSprite.Frame.SALVAGE,
				BukovItemSprite.frameFor(new BukovLootItem().configure(
						"loot:test",
						"Industrial Parts",
						BukovLootItem.Category.LOOT,
						1f,
						80)));
	}

	@Test
	public void firearmsAndAmmunitionHaveReadableDistinctSilhouettes() {
		assertEquals(
				BukovItemSprite.Frame.FIREARM_SHUTTLE_9,
				BukovItemSprite.frameFor(new Firearm().configure(
						"shuttle_9", "gun:shuttle", 20)));
		assertEquals(
				BukovItemSprite.Frame.FIREARM_WARD_556,
				BukovItemSprite.frameFor(new Firearm().configure(
						"ward_556", "gun:ward", 30)));
		assertEquals(
				BukovItemSprite.Frame.FIREARM_MOUNTAIN_762,
				BukovItemSprite.frameFor(new Firearm().configure(
						"mountain_762", "gun:mountain", 30)));
		assertEquals(
				BukovItemSprite.Frame.FIREARM_BOLT_12,
				BukovItemSprite.frameFor(new Firearm().configure(
						"bolt_12", "gun:bolt", 6)));
		assertEquals(
				BukovItemSprite.Frame.FIREARM_LONGSTREET_762,
				BukovItemSprite.frameFor(new Firearm().configure(
						"longstreet_762", "gun:longstreet", 10)));
		assertEquals(
				BukovItemSprite.Frame.AMMO_9_TRAINING,
				BukovItemSprite.frameFor(new AmmoStack().configure(
						"ammo_9_training", 12)));
		assertEquals(
				BukovItemSprite.Frame.AMMO_9_SUBSONIC,
				BukovItemSprite.frameFor(new AmmoStack().configure(
						"ammo_9_subsonic", 12)));
		assertEquals(
				BukovItemSprite.Frame.AMMO_556_ARMOR_PIERCING,
				BukovItemSprite.frameFor(new AmmoStack().configure(
						"ammo_556_armor_piercing", 12)));
		assertEquals(
				BukovItemSprite.Frame.AMMO_762_EXPANDING,
				BukovItemSprite.frameFor(new AmmoStack().configure(
						"ammo_762_expanding", 12)));
	}

	@Test
	public void durableBackpackDefinitionsUseTheSameDedicatedFrames() {
		assertEquals(
				BukovItemSprite.Frame.FIREARM_WARD_556,
				BukovItemSprite.frameForDefinition("firearm:ward_556"));
		assertEquals(
				BukovItemSprite.Frame.AMMO_556_ARMOR_PIERCING,
				BukovItemSprite.frameForDefinition(
						"ammo:ammo_556_armor_piercing"));
		assertEquals(
				BukovItemSprite.Frame.MEDICAL_BANDAGE,
				BukovItemSprite.frameForDefinition("bandage"));
		assertEquals(
				BukovItemSprite.Frame.MISSION_ARCHIVE,
				BukovItemSprite.frameForDefinition(
						"mission:maintenance_archive"));
		assertEquals(
				BukovItemSprite.Frame.ARMOR_PATROL_VEST,
				BukovItemSprite.frameForDefinition("armor:patrol_vest"));
		assertEquals(
				BukovItemSprite.Frame.SALVAGE,
				BukovItemSprite.frameForDefinition("duct_tape"));
	}

	@Test
	public void allAuthoredFirearmsOwnDistinctLogicalFrames() {
		String[] ids = {
				"needle_9", "shuttle_9", "ward_556", "mountain_762",
				"bolt_12", "longstreet_762", "sentinel_9", "sparrow_9",
				"hive_9", "whisper_9", "jackal_9", "river_556",
				"foundry_762", "carbine_556", "breaker_12",
				"rainstorm_12", "watchtower_556", "frontier_762"
		};
		Set<BukovItemSprite.Frame> frames = new HashSet<>();
		for (String id : ids) {
			frames.add(BukovItemSprite.frameForDefinition(
					"firearm:" + id));
		}
		assertEquals(18, frames.size());
	}

	@Test
	public void allChestVariantsUseIndustrialCrate() {
		for (Heap.Type type : new Heap.Type[] {
				Heap.Type.CHEST,
				Heap.Type.LOCKED_CHEST,
				Heap.Type.CRYSTAL_CHEST
		}) {
			Heap heap = new Heap();
			heap.type = type;
			assertEquals(
					BukovItemSprite.Frame.INDUSTRIAL_CRATE,
					BukovItemSprite.frameFor(heap));
		}
	}

	@Test
	public void interactionKindsOwnDedicatedAtlasFrames() {
		assertEquals(
				BukovItemSprite.Frame.FIXED_EXTRACTION,
				BukovInteractionMarker.Kind.FIXED_EXTRACTION.frame());
		assertEquals(
				BukovItemSprite.Frame.CONDITIONAL_EXTRACTION,
				BukovInteractionMarker.Kind.CONDITIONAL_EXTRACTION.frame());
		assertEquals(
				BukovItemSprite.Frame.PUMP_STATION,
				BukovInteractionMarker.Kind.PUMP_STATION.frame());
		assertEquals(
				BukovItemSprite.Frame.MISSION_ARCHIVE,
				BukovInteractionMarker.Kind.MISSION_ARCHIVE.frame());
	}

	@Test
	public void missionMarkerCentersOnItsWorldCell() {
		assertEquals(
				64f,
				BukovInteractionMarker.worldX(34, 10, 16f, 16f),
				0f);
		assertEquals(
				48f,
				BukovInteractionMarker.worldY(34, 10, 16f, 16f),
				0f);
	}

	@Test
	public void missionMarkerUsesOneHertzFifteenPercentBreathingRange() {
		float bright = BukovInteractionMarker.pulseAlpha(
				BukovInteractionMarker.Kind.MISSION_ARCHIVE,
				0.25f);
		float dim = BukovInteractionMarker.pulseAlpha(
				BukovInteractionMarker.Kind.MISSION_ARCHIVE,
				0.75f);
		float nextBright = BukovInteractionMarker.pulseAlpha(
				BukovInteractionMarker.Kind.MISSION_ARCHIVE,
				1.25f);

		assertEquals(1f, bright, 0.0001f);
		assertEquals(0.85f, dim, 0.0001f);
		assertEquals(bright, nextBright, 0.0001f);
		assertTrue(dim <= bright);
	}
}
