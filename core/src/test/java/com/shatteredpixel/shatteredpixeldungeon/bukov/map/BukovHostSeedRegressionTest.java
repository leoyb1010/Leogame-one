package com.shatteredpixel.shatteredpixeldungeon.bukov.map;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRouteMetrics;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms.BukovEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms.BukovExtractionAnchorRoom;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovHostSeedRegressionTest {

	private static final long[] HOST_SEEDS = {
			2164875390089L,
			351225973746L,
			1L, 2L, 3L, 7L, 11L, 42L, 99L, 256L,
			1024L, 4096L, 94823742L, 117013337L, 314159265L,
			987654321L, 2147483647L, -1L, -42L, Long.MAX_VALUE
	};

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;

	@Before
	public void captureHostGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		if (Game.version == null) Game.version = "test";
	}

	@After
	public void restoreHostGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Game.version = previousVersion;
	}

	@Test
	public void twentyHostSeedsProduceAValidAdapterWithinRetryBudget() {
		for (long seed : HOST_SEEDS) adaptWithinRetryBudget(seed);
	}

	@Test
	public void connectionRoomsDoNotInflatePlayableBudgetForKnown36RoomSeed() {
		BukovRoomGraphAdapter.AdaptedMap adapted =
				adaptWithinRetryBudget(2164875390089L);

		assertTrue(adapted.layout.marks.size() > 34);
		assertTrue(adapted.layout.playableRoomCount() >= 26);
		assertTrue(adapted.layout.playableRoomCount() <= 34);
		int structural = 0;
		for (BukovRaidLayout.Mark mark : adapted.layout.marks) {
			if (mark.structuralTransit) structural++;
		}
		assertEquals(adapted.layout.marks.size(),
				adapted.layout.playableRoomCount() + structural);
	}

	@Test
	public void knownRiskTieSeedSelectsStrictDeterministicTriplet() {
		BukovRoomGraphAdapter.AdaptedMap first =
				adaptWithinRetryBudget(351225973746L);
		BukovRoomGraphAdapter.AdaptedMap second =
				adaptWithinRetryBudget(351225973746L);

		BukovRaidLayout.Route safe = route(first.layout, BukovRaidLayout.RouteRisk.SAFE);
		BukovRaidLayout.Route balanced =
				route(first.layout, BukovRaidLayout.RouteRisk.BALANCED);
		BukovRaidLayout.Route high =
				route(first.layout, BukovRaidLayout.RouteRisk.HIGH_RISK);
		assertTrue(high.roomIds.size() < safe.roomIds.size());
		assertTrue(BukovRouteMetrics.averageThreat(first.layout, safe.roomIds)
				< BukovRouteMetrics.averageThreat(first.layout, balanced.roomIds));
		assertTrue(BukovRouteMetrics.averageThreat(first.layout, balanced.roomIds)
				< BukovRouteMetrics.averageThreat(first.layout, high.roomIds));
		for (BukovRaidLayout.Route riskRoute : first.layout.routes) {
			assertEquals(riskRoute.roomIds,
					route(second.layout, riskRoute.risk).roomIds);
		}
	}

	@Test
	public void fiveHundredSeedsPerModeRemainReachableAndInsideModeSize()
			throws Exception {
		long[] totalRooms = new long[BukovRaidMode.values().length];
		long[] totalSafeRouteRooms =
				new long[BukovRaidMode.values().length];
		for (BukovRaidMode mode : BukovRaidMode.values()) {
			for (int index = 0; index < 500; index++) {
				long seed = 0x9E3779B97F4A7C15L * (index + 1L);
				BukovRoomGraphAdapter.AdaptedMap adapted =
						adaptWithinRetryBudget(seed, mode);
				assertTrue(mode + " seed=" + seed + " rooms="
								+ adapted.layout.playableRoomCount(),
						mode.acceptsContentRoomCount(
								adapted.layout.playableRoomCount()));
				assertEquals(mode + " seed=" + seed,
						3, adapted.layout.routes.size());
				assertTrue(mode + " seed=" + seed,
						adapted.layout.extractions.size() >= 2);
				totalRooms[mode.ordinal()] +=
						adapted.layout.playableRoomCount();
				totalSafeRouteRooms[mode.ordinal()] += route(
						adapted.layout,
						BukovRaidLayout.RouteRisk.SAFE).roomIds.size();
				if (mode == BukovRaidMode.BOSS_CONTRACT) {
					assertTrue("boss branch must be on high-risk route",
							route(
									adapted.layout,
									BukovRaidLayout.RouteRisk.HIGH_RISK)
									.roomIds.contains(roomId(
											adapted.layout,
											BukovRaidLayout.Zone.BOSS)));
				}
			}
		}
		assertTrue(totalRooms[BukovRaidMode.QUICK_SWEEP.ordinal()]
				< totalRooms[BukovRaidMode.SCAVENGER.ordinal()]);
		assertTrue(totalRooms[BukovRaidMode.SCAVENGER.ordinal()]
				< totalRooms[BukovRaidMode.EXPEDITION.ordinal()]);
		assertTrue(totalRooms[BukovRaidMode.EXPEDITION.ordinal()]
				< totalRooms[BukovRaidMode.BOSS_CONTRACT.ordinal()]);
		assertTrue(totalSafeRouteRooms[BukovRaidMode.QUICK_SWEEP.ordinal()]
				< totalSafeRouteRooms[BukovRaidMode.EXPEDITION.ordinal()]);
	}

	@Test
	public void sameModeAndSeedIsDeterministicAndBossRouteUsesBossBranch() {
		for (BukovRaidMode mode : BukovRaidMode.values()) {
			BukovRoomGraphAdapter.AdaptedMap first =
					adaptWithinRetryBudget(94823742L, mode);
			BukovRoomGraphAdapter.AdaptedMap second =
					adaptWithinRetryBudget(94823742L, mode);
			assertEquals(signature(first.layout), signature(second.layout));
			if (mode == BukovRaidMode.BOSS_CONTRACT) {
				String bossRoom = roomId(
						first.layout, BukovRaidLayout.Zone.BOSS);
				assertTrue(route(
						first.layout,
						BukovRaidLayout.RouteRisk.HIGH_RISK)
						.roomIds.contains(bossRoom));
			}
		}
	}

	private static BukovRoomGraphAdapter.AdaptedMap adaptWithinRetryBudget(long seed) {
		return adaptWithinRetryBudget(seed, BukovRaidMode.EXPEDITION);
	}

	private static BukovRoomGraphAdapter.AdaptedMap adaptWithinRetryBudget(
			long seed, BukovRaidMode mode) {
		if (Game.version == null) Game.version = "test";
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = seed;
		Random.pushGenerator(seed + 1);
		try {
			SpecialRoom.initForRun();
			SecretRoom.initForRun();
		} finally {
			Random.popGenerator();
		}
		List<String> diagnostics = Collections.emptyList();

		Random.pushGenerator(Dungeon.seedCurDepth());
		try {
			for (int attempt = 0; attempt < 32; attempt++) {
				HostLevel level = new HostLevel(mode);
				if (!level.buildOnce()) continue;
				BukovRoomGraphAdapter.AdaptedMap adapted =
						BukovRoomGraphAdapter.adapt(
								level,
								Dungeon.seedCurDepth(),
								"fog_depot",
								mode);
				if (adapted.readyForRaid()) return adapted;
				diagnostics = adapted.diagnostics;
			}
		} finally {
			Random.popGenerator();
		}
		fail("seed=" + seed + " failed 32 attempts: " + diagnostics);
		return null;
	}

	private static String signature(BukovRaidLayout layout) {
		StringBuilder result = new StringBuilder();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			result.append(mark.roomId()).append(':')
					.append(mark.zone).append(':')
					.append(mark.semanticId).append('|');
		}
		for (BukovRaidLayout.Route route : layout.routes) {
			result.append(route.risk).append(route.roomIds).append('|');
		}
		for (com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition
				extraction : layout.extractions) {
			result.append(extraction.id).append(':')
					.append(extraction.roomId).append('|');
		}
		return result.toString();
	}

	private static String roomId(
			BukovRaidLayout layout, BukovRaidLayout.Zone zone) {
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (mark.zone == zone) return mark.roomId();
		}
		throw new AssertionError("Missing " + zone);
	}

	private static BukovRaidLayout.Route route(
			BukovRaidLayout layout, BukovRaidLayout.RouteRisk risk) {
		for (BukovRaidLayout.Route route : layout.routes) {
			if (route.risk == risk) return route;
		}
		throw new AssertionError("Missing " + risk);
	}

	private static final class HostLevel extends SewerLevel {

		private final BukovRaidMode mode;

		private HostLevel(BukovRaidMode mode) {
			this.mode = mode;
		}

		@Override
		protected Builder builder() {
			FigureEightBuilder builder = new FigureEightBuilder()
					.setLoopShape(2, mode.loopShapeIntensity, 0f);
			builder.setExtraConnectionChance(mode.extraConnectionChance);
			builder.setTunnelLength(
					mode.pathTunnelChances(),
					mode.branchTunnelChances());
			return builder;
		}

		@Override
		protected int standardRooms(boolean forceMax) {
			return mode.standardRoomBudget;
		}

		@Override
		protected int specialRooms(boolean forceMax) {
			return BukovLevel.SPECIAL_ROOM_BUDGET;
		}

		@Override
		protected ArrayList<Room> initRooms() {
			ArrayList<Room> result = new ArrayList<>();
			result.add(roomEntrance = new BukovEntranceRoom());
			result.add(roomExit = new BukovExtractionAnchorRoom());
			for (int index = 0;
					index < mode.standardRoomBudget;
					index++) {
				EmptyRoom room = new EmptyRoom();
				room.setSizeCat(1);
				result.add(room);
			}
			return result;
		}

		boolean buildOnce() {
			Builder graphBuilder = builder();
			ArrayList<Room> initialRooms = initRooms();
			Random.shuffle(initialRooms);
			do {
				for (Room room : initialRooms) {
					room.neigbours.clear();
					room.connected.clear();
				}
				rooms = graphBuilder.build(new ArrayList<>(initialRooms));
			} while (rooms == null);
			return true;
		}
	}
}
