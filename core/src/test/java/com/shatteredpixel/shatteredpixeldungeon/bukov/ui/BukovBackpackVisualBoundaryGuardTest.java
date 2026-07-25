package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the raid backpack from regressing to the inherited dungeon bag. */
public class BukovBackpackVisualBoundaryGuardTest {

	@Test
	public void backpackUsesOnlyDedicatedTacticalPresentation() throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/WndBukovBackpack.java")),
				StandardCharsets.UTF_8);
		String navigation = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/BukovNavigation.java")),
				StandardCharsets.UTF_8);

		assertFalse(source.contains("import com.shatteredpixel."
				+ "shatteredpixeldungeon.windows.WndBag"));
		assertFalse(source.contains("import com.shatteredpixel."
				+ "shatteredpixeldungeon.ui.ItemButton"));
		assertFalse(source.contains("import com.shatteredpixel."
				+ "shatteredpixeldungeon.sprites.ItemSprite"));
		assertFalse(source.contains("new ItemSprite("));
		assertTrue(source.contains("BukovItemSprite"));
		assertTrue(source.contains("frameForDefinition(item.definitionId)"));
		assertFalse(source.contains("RedButton"));
		assertTrue(source.contains(
				"BukovMessages.get(\"bukov.raid.backpack.title\")"));
		assertTrue(source.contains("pausedHintKey("));
		assertTrue(source.contains("DeviceCompat.isDesktop()"));
		assertTrue(source.contains("item.category.label"));
		assertTrue(source.contains("setBackpackOpen(true)"));
		assertTrue(source.contains("setBackpackOpen(false)"));
		assertTrue(source.contains(
				"\"bukov.raid.backpack.mission_no_drop\""));
		assertTrue(source.contains("BukovNavigation.inventory(event)"));
		assertTrue(navigation.contains("action == SPDAction.INVENTORY"));
	}
}
