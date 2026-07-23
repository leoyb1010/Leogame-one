/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovExplorationFogSourceGateTest {

	@Test
	public void realtimeFogRemembersVisibilityAndUsesDedicatedPalette()
			throws Exception {
		String gameScene = read(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String fog = read(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/tiles/FogOfWar.java");

		assertTrue(gameScene.contains("rememberBukovVisibility();"));
		assertTrue(gameScene.contains(
				"Dungeon.level.visited[cell] = true;"));
		assertTrue(gameScene.contains(
				"new FogOfWar("));
		assertTrue(fog.contains("BUKOV_FOG_COLORS"));
		assertTrue(fog.contains(
				"bukovPalette ? BUKOV_FOG_COLORS : FOG_COLORS"));
	}

	private static String read(String relative) throws Exception {
		Path path = Paths.get(relative);
		if (!Files.exists(path)) {
			path = Paths.get("..").resolve(relative).normalize();
		}
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
	}
}
