package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.TextureFilm;

/**
 * Dedicated raid-item renderer backed only by Bukov's original pixel atlas.
 *
 * This deliberately does not use an {@code Item.image()} value: several
 * Bukov host items still carry compatibility image IDs from the legacy
 * dungeon inventory. Mapping by runtime type/category prevents those legacy
 * CROSSBOW, CHEST and DOCUMENT frames from entering the Bukov player path.
 */
public final class BukovItemSprite extends ItemSprite {

	public static final String ATLAS = "sprites/bukov/items_interactions.png";
	public static final int FRAME_SIZE = 16;
	public static final int FRAME_COUNT = 72;

	private static final TextureFilm FILM =
			new TextureFilm(FRAME_SIZE * FRAME_COUNT, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);

	public enum Frame {
		FIREARM_NEEDLE_9(0),
		FIREARM_SHUTTLE_9(1),
		FIREARM_WARD_556(2),
		FIREARM_MOUNTAIN_762(3),
		FIREARM_BOLT_12(4),
		FIREARM_LONGSTREET_762(5),
		FIREARM_SENTINEL_9(6),
		FIREARM_SPARROW_9(7),
		FIREARM_HIVE_9(8),
		FIREARM_WHISPER_9(9),
		FIREARM_JACKAL_9(10),
		FIREARM_RIVER_556(11),
		FIREARM_FOUNDRY_762(12),
		FIREARM_CARBINE_556(13),
		FIREARM_BREAKER_12(14),
		FIREARM_RAINSTORM_12(15),
		FIREARM_WATCHTOWER_556(16),
		FIREARM_FRONTIER_762(17),
		AMMO_9_TRAINING(18),
		AMMO_9_STANDARD(19),
		AMMO_9_SUBSONIC(20),
		AMMO_556_STANDARD(21),
		AMMO_556_ARMOR_PIERCING(22),
		AMMO_762_STANDARD(23),
		AMMO_762_EXPANDING(24),
		AMMO_12G_BUCKSHOT(25),
		ARMOR_SOFT_VEST(26),
		ARMOR_PATROL_VEST(27),
		ARMOR_CERAMIC_RIG(28),
		BACKPACK_SCOUT_PACK(29),
		BACKPACK_FIELD_PACK(30),
		MEDICAL_BANDAGE(31),
		MEDICAL_PAINKILLER(32),
		MEDICAL_FIRST_AID(33),
		MEDICAL_TOURNIQUET(34),
		MEDICAL_ANTISEPTIC(35),
		MEDICAL_SPLINT(36),
		MEDICAL_STIM(37),
		TOOL_SET(38),
		LOOT_CANNED_FOOD(39),
		LOOT_WATER_FILTER(40),
		LOOT_BOLTS(41),
		LOOT_SCRAP_METAL(42),
		LOOT_CLOTH_ROLL(43),
		LOOT_BATTERY(44),
		LOOT_CERAMIC_SHARD(45),
		LOOT_RUBBER_HOSE(46),
		LOOT_SEALED_COFFEE(47),
		LOOT_COPPER_WIRE(48),
		LOOT_ELECTRIC_MOTOR(49),
		LOOT_BEARING(50),
		LOOT_CIRCUIT_BOARD(51),
		LOOT_FUEL_CAN(52),
		LOOT_WELDING_ROD(53),
		LOOT_PRESSURE_GAUGE(54),
		LOOT_RELAY_MODULE(55),
		LOOT_COPPER_COIL(56),
		LOOT_MACHINE_OIL(57),
		LOOT_GOLD_WATCH(58),
		LOOT_ENCRYPTED_DRIVE(59),
		LOOT_ANTIQUE_COIN(60),
		LOOT_CAMERA_LENS(61),
		LOOT_MILITARY_CHIP(62),
		LOOT_RADIO_CRYSTAL(63),
		LOOT_OPTICAL_SENSOR(64),
		LOOT_OFFICER_BADGE(65),
		LOOT_COMMAND_KEY(66),
		LOOT_PROTOTYPE_CORE(67),
		MISSION_ARCHIVE(68),
		FIXED_EXTRACTION(69),
		CONDITIONAL_EXTRACTION(70),
		PUMP_STATION(71),

		// Backward-compatible semantic aliases for existing call sites.
		AMMO_TRAINING(18),
		AMMO_STANDARD(19),
		AMMO_SUBSONIC(20),
		AMMO_ARMOR_PIERCING(22),
		AMMO_EXPANDING(24),
		MEDICAL_TRAUMA_POUCH(33),
		MEDICAL_SUTURE(34),
		MEDICAL_INJECTOR(37),
		SALVAGE(42),
		HIGH_VALUE(59),
		ARMOR_OR_BACKPACK(27),
		INDUSTRIAL_CRATE(42);

		private final int index;

		Frame(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}
	}

	public BukovItemSprite() {
		super();
		useAtlas();
		view(Frame.INDUSTRIAL_CRATE);
	}

	public BukovItemSprite(Item item) {
		this();
		view(item);
	}

	public BukovItemSprite(Heap heap) {
		this();
		link(heap);
	}

	private void useAtlas() {
		if (texture == null || texture.width != FRAME_SIZE * FRAME_COUNT
				|| texture.height != FRAME_SIZE) {
			texture(ATLAS);
		}
	}

	public BukovItemSprite view(Frame frame) {
		if (frame == null) {
			throw new IllegalArgumentException("frame is required");
		}
		useAtlas();
		frame(FILM.get(frame.index()));
		glow(null);
		perspectiveRaise = 5 / 16f;
		return this;
	}

	@Override
	public BukovItemSprite view(Item item) {
		return view(frameFor(item));
	}

	@Override
	public BukovItemSprite view(Heap heap) {
		Frame mapped = frameFor(heap);
		view(mapped);
		alpha(heap != null && heap.hidden ? 0.15f : 1f);
		return this;
	}

	@Override
	public BukovItemSprite view(int ignoredLegacyImage, Glowing ignoredLegacyGlow) {
		// ItemSprite invokes this virtual method from its constructor. The
		// dedicated atlas is installed immediately afterwards by useAtlas().
		return view(Frame.INDUSTRIAL_CRATE);
	}

	public static Frame frameFor(Heap heap) {
		if (heap == null) {
			return Frame.INDUSTRIAL_CRATE;
		}
		switch (heap.type) {
			case CHEST:
			case LOCKED_CHEST:
			case CRYSTAL_CHEST:
				return Frame.INDUSTRIAL_CRATE;
			case HEAP:
			case FOR_SALE:
				return frameFor(heap.peek());
			default:
				// Bukov does not render tomb/skeleton/remains silhouettes.
				return Frame.INDUSTRIAL_CRATE;
		}
	}

	public static Frame frameFor(Item item) {
		if (item instanceof Firearm) {
			return frameForDefinition(
					"firearm:" + ((Firearm)item).definitionId());
		}
		if (item instanceof AmmoStack) {
			return frameForDefinition(
					"ammo:" + ((AmmoStack)item).definitionId());
		}
		if (item instanceof BukovMissionArchive) {
			return Frame.MISSION_ARCHIVE;
		}
		if (item instanceof BukovLootItem) {
			BukovLootItem loot = (BukovLootItem)item;
			Frame authored = authoredFrame(loot.bukovDefinitionId());
			if (authored != null) {
				return authored;
			}
			BukovLootItem.Category category = loot.category();
			if (category == BukovLootItem.Category.MEDICAL) {
				String id = loot.bukovDefinitionId();
				if (id != null && (id.contains("bandage")
						|| id.contains("gauze"))) {
					return Frame.MEDICAL_BANDAGE;
				}
				if (id != null && (id.contains("suture")
						|| id.contains("stitch"))) {
					return Frame.MEDICAL_SUTURE;
				}
				if (id != null && (id.contains("splint")
						|| id.contains("fracture"))) {
					return Frame.MEDICAL_SPLINT;
				}
				if (id != null && (id.contains("inject")
						|| id.contains("stabil"))) {
					return Frame.MEDICAL_INJECTOR;
				}
				return Frame.MEDICAL_TRAUMA_POUCH;
			}
			if (category == BukovLootItem.Category.TOOL) {
				return Frame.SALVAGE;
			}
			if (category == BukovLootItem.Category.HIGH_VALUE
					|| category == BukovLootItem.Category.BOSS) {
				return Frame.HIGH_VALUE;
			}
			return Frame.SALVAGE;
		}
		return Frame.INDUSTRIAL_CRATE;
	}

	/**
	 * Maps the durable raid definition used by hub/backpack rows to the same
	 * dedicated atlas frame used by its host Item instance.
	 */
	public static Frame frameForDefinition(String definitionId) {
		String id = definitionId == null
				? "" : definitionId.trim().toLowerCase(java.util.Locale.ROOT);
		Frame authored = authoredFrame(id);
		if (authored != null) {
			return authored;
		}
		if (id.startsWith("firearm:")) {
			return Frame.FIREARM_NEEDLE_9;
		}
		if (id.startsWith("ammo:")) {
			return Frame.AMMO_9_STANDARD;
		}
		if (id.contains("archive")) return Frame.MISSION_ARCHIVE;
		if (id.contains("bandage") || id.contains("gauze")) {
			return Frame.MEDICAL_BANDAGE;
		}
		if (id.contains("suture") || id.contains("stitch")) {
			return Frame.MEDICAL_SUTURE;
		}
		if (id.contains("splint") || id.contains("fracture")) {
			return Frame.MEDICAL_SPLINT;
		}
		if (id.contains("inject") || id.contains("stabil")) {
			return Frame.MEDICAL_INJECTOR;
		}
		if (id.contains("med") || id.contains("trauma")) {
			return Frame.MEDICAL_TRAUMA_POUCH;
		}
		if (id.contains("armor") || id.contains("vest")
				|| id.contains("backpack") || id.contains("rig")) {
			return Frame.ARMOR_OR_BACKPACK;
		}
		if (id.contains("valuable") || id.contains("electronics")
				|| id.contains("boss")) {
			return Frame.HIGH_VALUE;
		}
		return Frame.SALVAGE;
	}

	private static Frame authoredFrame(String definitionId) {
		if (definitionId == null) {
			return null;
		}
		String id = definitionId.trim().toLowerCase(
				java.util.Locale.ROOT);
		switch (id) {
			case "firearm:needle_9":
				return Frame.FIREARM_NEEDLE_9;
			case "firearm:shuttle_9":
				return Frame.FIREARM_SHUTTLE_9;
			case "firearm:ward_556":
				return Frame.FIREARM_WARD_556;
			case "firearm:mountain_762":
				return Frame.FIREARM_MOUNTAIN_762;
			case "firearm:bolt_12":
				return Frame.FIREARM_BOLT_12;
			case "firearm:longstreet_762":
				return Frame.FIREARM_LONGSTREET_762;
			case "firearm:sentinel_9":
				return Frame.FIREARM_SENTINEL_9;
			case "firearm:sparrow_9":
				return Frame.FIREARM_SPARROW_9;
			case "firearm:hive_9":
				return Frame.FIREARM_HIVE_9;
			case "firearm:whisper_9":
				return Frame.FIREARM_WHISPER_9;
			case "firearm:jackal_9":
				return Frame.FIREARM_JACKAL_9;
			case "firearm:river_556":
				return Frame.FIREARM_RIVER_556;
			case "firearm:foundry_762":
				return Frame.FIREARM_FOUNDRY_762;
			case "firearm:carbine_556":
				return Frame.FIREARM_CARBINE_556;
			case "firearm:breaker_12":
				return Frame.FIREARM_BREAKER_12;
			case "firearm:rainstorm_12":
				return Frame.FIREARM_RAINSTORM_12;
			case "firearm:watchtower_556":
				return Frame.FIREARM_WATCHTOWER_556;
			case "firearm:frontier_762":
				return Frame.FIREARM_FRONTIER_762;
			case "ammo:ammo_9_training":
				return Frame.AMMO_9_TRAINING;
			case "ammo:ammo_9_standard":
				return Frame.AMMO_9_STANDARD;
			case "ammo:ammo_9_subsonic":
				return Frame.AMMO_9_SUBSONIC;
			case "ammo:ammo_556_standard":
				return Frame.AMMO_556_STANDARD;
			case "ammo:ammo_556_armor_piercing":
				return Frame.AMMO_556_ARMOR_PIERCING;
			case "ammo:ammo_762_standard":
				return Frame.AMMO_762_STANDARD;
			case "ammo:ammo_762_expanding":
				return Frame.AMMO_762_EXPANDING;
			case "ammo:ammo_12g_buckshot":
				return Frame.AMMO_12G_BUCKSHOT;
			case "armor:soft_vest":
				return Frame.ARMOR_SOFT_VEST;
			case "armor:patrol_vest":
				return Frame.ARMOR_PATROL_VEST;
			case "armor:ceramic_rig":
				return Frame.ARMOR_CERAMIC_RIG;
			case "backpack:scout_pack":
				return Frame.BACKPACK_SCOUT_PACK;
			case "backpack:field_pack":
				return Frame.BACKPACK_FIELD_PACK;
			case "bandage":
				return Frame.MEDICAL_BANDAGE;
			case "painkiller":
				return Frame.MEDICAL_PAINKILLER;
			case "first_aid":
				return Frame.MEDICAL_FIRST_AID;
			case "tourniquet":
				return Frame.MEDICAL_TOURNIQUET;
			case "antiseptic":
				return Frame.MEDICAL_ANTISEPTIC;
			case "splint":
				return Frame.MEDICAL_SPLINT;
			case "stim":
				return Frame.MEDICAL_STIM;
			case "tool_set":
				return Frame.TOOL_SET;
			case "canned_food":
				return Frame.LOOT_CANNED_FOOD;
			case "water_filter":
				return Frame.LOOT_WATER_FILTER;
			case "bolts":
				return Frame.LOOT_BOLTS;
			case "scrap_metal":
				return Frame.LOOT_SCRAP_METAL;
			case "cloth_roll":
				return Frame.LOOT_CLOTH_ROLL;
			case "battery":
				return Frame.LOOT_BATTERY;
			case "ceramic_shard":
				return Frame.LOOT_CERAMIC_SHARD;
			case "rubber_hose":
				return Frame.LOOT_RUBBER_HOSE;
			case "sealed_coffee":
				return Frame.LOOT_SEALED_COFFEE;
			case "copper_wire":
				return Frame.LOOT_COPPER_WIRE;
			case "electric_motor":
				return Frame.LOOT_ELECTRIC_MOTOR;
			case "bearing":
				return Frame.LOOT_BEARING;
			case "circuit_board":
				return Frame.LOOT_CIRCUIT_BOARD;
			case "fuel_can":
				return Frame.LOOT_FUEL_CAN;
			case "welding_rod":
				return Frame.LOOT_WELDING_ROD;
			case "pressure_gauge":
				return Frame.LOOT_PRESSURE_GAUGE;
			case "relay_module":
				return Frame.LOOT_RELAY_MODULE;
			case "copper_coil":
				return Frame.LOOT_COPPER_COIL;
			case "machine_oil":
				return Frame.LOOT_MACHINE_OIL;
			case "gold_watch":
				return Frame.LOOT_GOLD_WATCH;
			case "encrypted_drive":
				return Frame.LOOT_ENCRYPTED_DRIVE;
			case "antique_coin":
				return Frame.LOOT_ANTIQUE_COIN;
			case "camera_lens":
				return Frame.LOOT_CAMERA_LENS;
			case "military_chip":
				return Frame.LOOT_MILITARY_CHIP;
			case "radio_crystal":
				return Frame.LOOT_RADIO_CRYSTAL;
			case "optical_sensor":
				return Frame.LOOT_OPTICAL_SENSOR;
			case "officer_badge":
				return Frame.LOOT_OFFICER_BADGE;
			case "command_key":
				return Frame.LOOT_COMMAND_KEY;
			case "prototype_core":
				return Frame.LOOT_PROTOTYPE_CORE;
			case "mission:maintenance_archive":
				return Frame.MISSION_ARCHIVE;
			default:
				return null;
		}
	}
}
