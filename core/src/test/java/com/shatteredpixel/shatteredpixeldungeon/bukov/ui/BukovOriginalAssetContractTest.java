package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovOriginalAssetContractTest {

	@Test
	public void operatorUsesRuntimeCompatibleOriginalAtlasContract() throws Exception {
		assertPng("src/main/assets/sprites/bukov_operator.png", 384, 128);
		assertPng("src/main/assets/sprites/bukov_operator_lower.png", 384, 128);
		assertPng("src/main/assets/sprites/bukov_operator_upper.png", 384, 128);

		String generator = text("../scripts/generate_bukov_operator_sprite.mjs");
		String manifest = text(
				"src/main/assets/sprites/bukov_operator_manifest.json");
		assertTrue(generator.contains("const FRAME_W = 12"));
		assertTrue(generator.contains("const FRAME_H = 15"));
		assertTrue(generator.contains("const FRAME_COUNT = 32"));
		assertTrue(generator.contains("const DIRECTION_COUNT = 8"));
		assertTrue(generator.contains("const lowerPixels"));
		assertTrue(generator.contains("const upperPixels"));
		assertTrue(generator.contains("onLayer(lowerPixels"));
		assertTrue(manifest.contains("\"schemaVersion\": 2"));
		assertTrue(manifest.contains("\"frameCount\": 32"));
		assertTrue(manifest.contains(
				"\"lowerBody\": \"sprites/bukov_operator_lower.png\""));
		assertTrue(manifest.contains(
				"\"upperBodyWeapon\": \"sprites/bukov_operator_upper.png\""));
		assertTrue(manifest.contains("\"layerSha256\""));
		assertTrue(manifest.contains("\"footAnchor\""));
		assertTrue(manifest.contains("\"muzzleAnchor\""));
		assertTrue(manifest.contains("\"name\": \"medical\""));
		assertTrue(manifest.contains("\"name\": \"extract\""));
		assertFalse("operator generator must not name a host sprite",
				generator.contains("rogue.png"));
		assertFalse("original operator generator must not stream a host sprite", generator.contains("createReadStream"));
	}

	@Test
	public void landmarkAtlasHasAllFirstRaidFramesAndNoSourceImageReader() throws Exception {
		assertPng("src/main/assets/environment/bukov/first_raid_landmarks.png", 320, 32);

		String generator = text("../scripts/generate_bukov_landmarks.mjs");
		String notes = text("../docs/bukov/first-raid-landmarks.md");
		assertTrue(generator.contains("const COUNT = 10"));
		assertFalse(generator.contains("readFileSync"));
		assertFalse(generator.contains("createReadStream"));
		assertTrue(notes.contains("帧 0：档案柜"));
		assertTrue(notes.contains("帧 4：泵站机组"));
		assertTrue(notes.contains("帧 5：固定撤离点"));
		assertTrue(notes.contains("帧 6：条件撤离点"));
		assertTrue(notes.contains("帧 7：工业战利品箱"));
		assertTrue(notes.contains("帧 8：混凝土掩体"));
		assertTrue(notes.contains("帧 9：沙袋掩体"));
	}

	@Test
	public void itemAtlasHasDedicatedSeventyTwoFrameContract() throws Exception {
		assertPng("src/main/assets/sprites/bukov/items_interactions.png", 1152, 16);

		String generator = text("../scripts/generate_bukov_item_visuals.mjs");
		String manifest = text(
				"src/main/assets/sprites/bukov/items_interactions_manifest.json");
		assertTrue(generator.contains("const FRAME = 16"));
		assertTrue(generator.contains("FRAME_COUNT !== 72"));
		assertTrue(manifest.contains("\"frameCount\": 72"));
		assertFalse(generator.contains("ItemSpriteSheet"));
		assertFalse(generator.contains("sprites/items.png"));
	}

	@Test
	public void provenanceDeclaresAllRuntimeAssetsAsProjectOriginal() throws Exception {
		String provenance = text("../artwork/licenses/ASSET_PROVENANCE.csv");
		assertTrue(provenance.contains(
				"core/src/main/assets/sprites/bukov_operator.png,project-generated original artwork"));
		assertTrue(provenance.contains(
				"core/src/main/assets/sprites/bukov_operator_lower.png,project-generated original artwork layer"));
		assertTrue(provenance.contains(
				"core/src/main/assets/sprites/bukov_operator_upper.png,project-generated original artwork layer"));
		assertTrue(provenance.contains(
				"core/src/main/assets/environment/bukov/first_raid_landmarks.png,project-generated original artwork"));
		assertTrue(provenance.contains(
				"core/src/main/assets/sprites/bukov/items_interactions.png,project-generated original artwork"));
	}

	private static void assertPng(String path, int width, int height) throws Exception {
		try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
			assertEquals(0x89504E47, in.readInt());
			assertEquals(0x0D0A1A0A, in.readInt());
			assertEquals(13, in.readInt());
			assertEquals(0x49484452, in.readInt());
			assertEquals(width, in.readInt());
			assertEquals(height, in.readInt());
			assertEquals("asset must be 8-bit", 8, in.readUnsignedByte());
			assertEquals("asset must be RGBA", 6, in.readUnsignedByte());
		}
	}

	private static String text(String path) throws Exception {
		return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
	}
}
