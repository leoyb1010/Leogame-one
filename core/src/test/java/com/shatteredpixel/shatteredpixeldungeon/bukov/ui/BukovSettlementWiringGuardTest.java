package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Prevents settled raids from silently skipping the dedicated result screen. */
public class BukovSettlementWiringGuardTest {

	@Test
	public void successAndDeathBothOpenDedicatedSettlement() throws Exception {
		String scene = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		assertTrue(scene.contains("RaidResult result = bukovRaid.settleDeath()"));
		assertTrue(scene.contains("RaidResult result = bukovRaid.settleSuccess()"));
		assertTrue(scene.contains("new WndBukovSettlement("));
		assertTrue(scene.contains("bukovWorld.killCount()"));
		assertTrue(scene.contains("RepeatLastLoadout"));
		assertTrue(scene.contains("hub.repeatLastLoadout()"));
		assertTrue(scene.contains(
				"bukov.economy.settlement.confirm_return")
				|| read("src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java")
						.contains(
								"bukov.economy.settlement.confirm_return"));
		assertTrue(read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java")
				.contains("bukov.economy.settlement.repeat_loadout"));
		String settlement = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java");
		assertTrue(settlement.contains("BukovSettlementRevealModel"));
		assertTrue(settlement.contains("reveal.advance(Game.elapsed)"));
		assertTrue(settlement.contains(
				"bukov.economy.settlement.stamp_success"));
		assertTrue(settlement.contains(
				"bukov.economy.settlement.stamp_failed"));
		assertTrue(settlement.contains("BukovUiAssets.Stamp.EXTRACTED"));
		assertTrue(settlement.contains("BukovUiAssets.Stamp.LOST"));
		assertTrue(settlement.contains("if (skipReveal()) return;"));
		assertTrue(settlement.contains(
				"SPDSettings.bukovReduceMotion()"));
		assertTrue(settlement.contains(
				"tokens.motionMs(\"instant\")"));
		assertTrue(settlement.contains(
				"tokens.motionMs(\"ritual\")"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
