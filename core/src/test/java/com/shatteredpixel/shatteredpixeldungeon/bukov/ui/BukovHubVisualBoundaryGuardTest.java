package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the hideout from regressing to classic dungeon menu widgets. */
public class BukovHubVisualBoundaryGuardTest {

	@Test
	public void hubUsesDedicatedTacticalComponentsAndCopy() throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/WndBukovHub.java")),
				StandardCharsets.UTF_8);
		String viewModel = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/BukovHubViewModel.java")),
				StandardCharsets.UTF_8);

		assertFalse(source.contains("RedButton"));
		assertFalse(source.contains("CheckBox"));
		assertFalse(source.contains("再次确认出击"));
		assertTrue(source.contains("HIDEOUT / LOADOUT"));
		assertTrue(source.contains("推荐配装"));
		assertTrue(source.contains("清空配装"));
		assertTrue(source.contains("确认出击"));
		assertTrue(source.contains("DeploymentConfirmWindow"));
		assertTrue(source.contains("FilterCycleButton"));
		assertTrue(source.contains("SortCycleButton"));
		assertTrue(source.contains("InventorySearchButton"));
		assertTrue(source.contains("WndBukovInventorySearch"));
		assertTrue(source.contains("item.rarity.colorToken"));
		assertTrue(source.contains("item.comparisonLabel()"));
		assertTrue(viewModel.contains("InventorySort"));
		assertTrue(viewModel.contains("matchesQuery("));
		assertTrue(viewModel.contains("主武器"));
		assertTrue(viewModel.contains("弹药"));
		assertTrue(viewModel.contains("医疗"));
	}

	@Test
	public void hubAttachesScrollPaneBeforeLayoutNeedsParentCamera()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/WndBukovHub.java")),
				StandardCharsets.UTF_8);

		int create = source.indexOf("itemScroll = new ScrollPane(list);");
		int attach = source.indexOf("add(itemScroll);", create);
		int layout = source.indexOf("itemScroll.setRect(", create);
		assertTrue(create >= 0);
		assertTrue(attach > create);
		assertTrue(layout > attach);
	}

	@Test
	public void mobileLoadoutAndSearchKeepCompactTouchAndKeyboardContracts()
			throws Exception {
		String hub = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/WndBukovHub.java")),
				StandardCharsets.UTF_8);
		String search = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
								+ "bukov/ui/WndBukovInventorySearch.java")),
				StandardCharsets.UTF_8);

		assertTrue(hub.contains("compactLandscape"));
		assertTrue(hub.contains("inventoryUtilityHeight"));
		assertTrue(hub.contains("eyebrow.visible = landscape"));
		assertTrue(search.contains("DeviceCompat.hasHardKeyboard()"));
		assertTrue(search.contains("boundOffsetWithMargin(0)"));
		assertTrue(search.contains("public void offset(int xOffset, int yOffset)"));
		assertTrue(search.contains(
				"this.initialQuery = normalize(initialQuery);"));
		assertTrue(search.contains("restore(initialQuery);"));
		assertTrue(search.contains(
				"hide();\n"
						+ "\t\tresult.apply(query);"));
		assertTrue(hub.contains(
				"focus.focus(inventoryItems.size() + 3);"));
		assertTrue(hub.contains("searchFocusFor(query)"));
		int searchReturn = hub.indexOf(
				"new WndBukovHub(",
				hub.indexOf("private void openInventorySearch()"));
		int returnEnd = hub.indexOf(
				"private int searchFocusFor",
				searchReturn);
		assertTrue(searchReturn >= 0);
		assertTrue(returnEnd > searchReturn);
		String returnChain = hub.substring(searchReturn, returnEnd);
		assertTrue(returnChain.contains("inventoryFilter"));
		assertTrue(returnChain.contains("inventorySort"));
		assertTrue(returnChain.contains("query"));
	}
}
