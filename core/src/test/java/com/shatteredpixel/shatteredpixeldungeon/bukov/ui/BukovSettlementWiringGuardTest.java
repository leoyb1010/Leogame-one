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
		assertTrue(scene.contains("确认并返回藏身处")
				|| read("src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java")
						.contains("确认并返回藏身处"));
		assertTrue(read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java")
				.contains("沿用配装"));
		String settlement = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java");
		assertTrue(settlement.contains("BukovSettlementRevealModel"));
		assertTrue(settlement.contains("reveal.advance(Game.elapsed)"));
		assertTrue(settlement.contains("[ 撤离确认 ]"));
		assertTrue(settlement.contains("[ 行动损失 ]"));
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
