package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Guards the production GameScene route, not only the renderer-free raid model.
 */
public class BukovFirstRaidProductionWiringTest {

	@Test
	public void gameSceneRunsLiveWorldHudAndSettlementInOneFrameLoop()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");

		assertTrue(scene.contains("bukovWorld = new BukovRealtimeWorld("));
		assertTrue(scene.contains("bukovWorld.installEquippedGear("));
		assertTrue(scene.contains(
				"bukovRealtime = new RealtimeRaidSystem("));
		assertTrue(scene.contains("bukovHud = new BukovRaidHud().bind("));
		assertTrue(scene.contains(
				"bukovWorld.touchControls(bukovTouchControls)"));

		String update = between(
				scene,
				"if (BukovMode.active() && bukovRealtime != null) {",
				"if (BukovMode.active()) {",
				scene.indexOf("protected void update()"));
		assertOrdered(
				update,
				"BukovGameSceneFrameLoop.update(",
				"bukovTouchControls.liveActionAvailability(",
				"updateBukovLifecycle()");

		assertTrue(scene.contains("RaidResult result = bukovRaid.settleDeath()"));
		assertTrue(scene.contains("RaidResult result = bukovRaid.settleSuccess()"));
		assertTrue(scene.contains("show(new WndBukovSettlement("));
		assertTrue(scene.contains("hub.repeatLastLoadout()"));
		assertTrue(scene.contains(
				"ShatteredPixelDungeon.switchScene(BukovHubScene.class)"));
	}

	@Test
	public void archiveGateDoorAndExtractionPromptsReachTheLiveHud()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String collision = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/LevelCollisionMap.java");
		String gate = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/MissionGateTerrain.java");
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");

		assertTrue(world.contains(
				"missionEnabled = raid != null && raid.firstRaidMissionActive()"));
		assertTrue(world.contains(
				"BukovRaidHudState.Interaction.SEARCH"));
		assertTrue(world.contains(
				"\"bukov.raid.runtime.search_archive\""));
		assertTrue(world.contains(
				"BukovRaidHudState.Interaction.PICKUP"));
		assertTrue(world.contains(
				"\"bukov.raid.runtime.pickup_archive\""));
		assertTrue(world.contains(
				"\"bukov.raid.runtime.gate_locked_hint\""));
		assertTrue(world.contains(
				"raid.completeEvent(FirstRaidMission.EVENT_ID)"));
		assertTrue(world.contains("missionGateUnlocked = true"));
		assertTrue(world.contains("applyMissionGateTerrain()"));
		assertTrue(world.contains(
				"ExtractionIntentResolver.wantsToStart("));
		assertTrue(world.contains(
				"ExtractionIntentResolver.resolve("));
		assertTrue(world.contains(
				"BukovRaidHudState.Interaction.EXTRACT"));

		assertTrue(collision.contains(
				"terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR"));
		assertTrue(collision.contains(
				"Level.set(cell, Terrain.OPEN_DOOR, level)"));
		assertTrue(gate.contains(
				"unlocked ? Terrain.OPEN_DOOR : Terrain.LOCKED_DOOR"));
		assertTrue(gate.contains("cellRefresh.refresh(cell)"));

		assertTrue(hud.contains("hudSource.readRaidHudState(live)"));
		assertTrue(hud.contains("BukovHudFormat.interaction("));
		assertTrue(hud.contains("live.interactionProgress()"));
		assertTrue(hud.contains("interactionActionAvailable("));
	}

	@Test
	public void visibleIosInteractSupportsHoldAndCancelsWithModalState()
			throws Exception {
		String input = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeInput.java");
		String controls = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovTouchControls.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");

		assertTrue(input.contains(
				"mobile.actionHeld(\n"
						+ "\t\t\t\t\t\t\t\tBukovTouchState.Action.INTERACT)"));
		assertTrue(input.contains(
				"touchControls.consumePressed(\n"
						+ "\t\t\t\t\t\t\t\tBukovTouchState.Action.INTERACT)"));

		String cancellation = between(
				input,
				"public void cancelTouches() {",
				"public InputFrame poll(",
				0);
		assertTrue(cancellation.contains("touch.reset()"));
		assertTrue(cancellation.contains("touchControls.resetInput()"));

		assertTrue(controls.contains("state.beginAction(action, event.id)"));
		assertTrue(controls.contains("state.endPointer(event.id)"));
		assertTrue(controls.contains("if (disabled) {\n"
				+ "\t\t\t\tresetInteraction();"));
		assertTrue(scene.contains(
				"bukovTouchControls.inputBlocked(\n"
						+ "\t\t\t\t\tshowingWindow()"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}

	private static String between(
			String source,
			String start,
			String end,
			int fromIndex) {
		int startIndex = source.indexOf(start, Math.max(0, fromIndex));
		assertTrue("missing start marker: " + start, startIndex >= 0);
		int endIndex = source.indexOf(end, startIndex + start.length());
		assertTrue("missing end marker: " + end, endIndex > startIndex);
		return source.substring(startIndex, endIndex);
	}

	private static void assertOrdered(String source, String... markers) {
		int cursor = -1;
		for (String marker : markers) {
			int next = source.indexOf(marker, cursor + 1);
			assertTrue("missing or out-of-order marker: " + marker, next > cursor);
			cursor = next;
		}
	}
}
