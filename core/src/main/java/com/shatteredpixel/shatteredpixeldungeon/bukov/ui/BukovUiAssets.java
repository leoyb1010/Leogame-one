package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;

/**
 * Bukov-owned tactical UI skin shared by desktop and iOS.
 *
 * The atlas is deterministic project-original pixel art. Every factory keeps
 * a named-color fallback so a missing optional skin cannot strand the player
 * on a blank title, loading, or recovery screen.
 */
public final class BukovUiAssets {

	public static final String MANIFEST_PATH =
			"interfaces/bukov_ui_manifest.json";
	public static final int TILE_SIZE = 16;
	private static final int NINE_PATCH_MARGIN = 4;

	public enum Surface {
		PANEL(0),
		PANEL_RAISED(1),
		BUTTON(2),
		BUTTON_PRESSED(3);

		private final int column;

		Surface(int column) {
			this.column = column;
		}
	}

	public enum StatusIcon {
		ACTION(0),
		LOOT(1),
		EXTRACT(2),
		DANGER(3),
		BLEEDING(4),
		FRACTURE(5),
		CONCUSSION(6);

		private final int column;

		StatusIcon(int column) {
			this.column = column;
		}
	}

	private BukovUiAssets() {
	}

	public static NinePatch surface(
			Surface surface, int fallbackColor) {
		if (surface == null) {
			throw new IllegalArgumentException("surface is required");
		}
		NinePatch result;
		if (!atlasAvailable()) {
			result = new NinePatch(
					TextureCache.createSolid(fallbackColor), 0);
		} else {
			result = new NinePatch(
					Assets.Interfaces.BUKOV_UI,
					surface.column * TILE_SIZE,
					0,
					TILE_SIZE,
					TILE_SIZE,
					NINE_PATCH_MARGIN);
		}
		result.texture.filter(
				SmartTexture.NEAREST, SmartTexture.NEAREST);
		return result;
	}

	public static Image icon(
			StatusIcon icon, int fallbackColor) {
		if (icon == null) {
			throw new IllegalArgumentException("icon is required");
		}
		if (!atlasAvailable()) {
			Image fallback =
					new Image(TextureCache.createSolid(fallbackColor));
			fallback.texture.filter(
					SmartTexture.NEAREST, SmartTexture.NEAREST);
			fallback.scale.set(TILE_SIZE);
			return fallback;
		}
		Image result = new Image(
				Assets.Interfaces.BUKOV_UI,
				icon.column * TILE_SIZE,
				TILE_SIZE,
				TILE_SIZE,
				TILE_SIZE);
		result.texture.filter(
				SmartTexture.NEAREST, SmartTexture.NEAREST);
		return result;
	}

	static boolean atlasAvailable() {
		FileHandle atlas = Gdx.files == null
				? new FileHandle(
						"src/main/assets/" + Assets.Interfaces.BUKOV_UI)
				: Gdx.files.internal(Assets.Interfaces.BUKOV_UI);
		try {
			return atlas.exists() && !atlas.isDirectory();
		} catch (RuntimeException unavailable) {
			return false;
		}
	}
}
