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
}
