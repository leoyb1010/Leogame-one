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
		BUTTON_PRESSED(3),
		BUTTON_FOCUSED(4),
		BUTTON_DISABLED(5),
		ROW_FOCUSED(6);

		private final int column;

		Surface(int column) {
			this.column = column;
		}
	}

	public enum RarityFrame {
		COMMON(0),
		UNCOMMON(1),
		RARE(2),
		LEGENDARY(3);

		private final int column;

		RarityFrame(int column) {
			this.column = column;
		}
	}

	public enum HudElement {
		HEALTH(0),
		ARMOR(1),
		AMMO(2),
		INTERACT(3),
		OBJECTIVE(4),
		TIMER(5),
		SOUND(6),
		HIT(7);

		private final int column;

		HudElement(int column) {
			this.column = column;
		}
	}

	public enum TouchGlyph {
		MOVEMENT(8, 2),
		AIM_FIRE(9, 2),
		INTERACT(10, 2),
		RELOAD(11, 2),
		MEDICAL(12, 2),
		DROP(13, 2),
		BACKPACK(14, 2),
		PAUSE(15, 2),
		MODE(0, 4),
		VENDOR(1, 4),
		FILTER(2, 4),
		SORT(3, 4),
		SEARCH(4, 4),
		RECOMMEND(5, 4),
		DEPLOY(6, 4),
		BACK(7, 4),
		SETTINGS(8, 4),
		DOCUMENT(9, 4),
		RESUME(10, 4);

		private final int column;
		private final int row;

		TouchGlyph(int column, int row) {
			this.column = column;
			this.row = row;
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

	public enum Stamp {
		EXTRACTED(8),
		LOST(11);

		private final int column;

		Stamp(int column) {
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
		return image(
				icon.column * TILE_SIZE,
				TILE_SIZE * 3,
				TILE_SIZE,
				TILE_SIZE,
				fallbackColor);
	}

	public static NinePatch rarityFrame(
			RarityFrame rarity, int fallbackColor) {
		if (rarity == null) {
			throw new IllegalArgumentException("rarity is required");
		}
		NinePatch result;
		if (!atlasAvailable()) {
			result = new NinePatch(
					TextureCache.createSolid(fallbackColor), 0);
		} else {
			result = new NinePatch(
					Assets.Interfaces.BUKOV_UI,
					rarity.column * TILE_SIZE,
					TILE_SIZE,
					TILE_SIZE,
					TILE_SIZE,
					NINE_PATCH_MARGIN);
		}
		result.texture.filter(
				SmartTexture.NEAREST, SmartTexture.NEAREST);
		return result;
	}

	public static Image hud(
			HudElement element, int fallbackColor) {
		if (element == null) {
			throw new IllegalArgumentException("HUD element is required");
		}
		return image(
				element.column * TILE_SIZE,
				TILE_SIZE * 2,
				TILE_SIZE,
				TILE_SIZE,
				fallbackColor);
	}

	public static Image touchGlyph(
			TouchGlyph glyph, int fallbackColor) {
		if (glyph == null) {
			throw new IllegalArgumentException("touch glyph is required");
		}
		return image(
				glyph.column * TILE_SIZE,
				TILE_SIZE * glyph.row,
				TILE_SIZE,
				TILE_SIZE,
				fallbackColor);
	}

	static int touchGlyphColumn(TouchGlyph glyph) {
		if (glyph == null) {
			throw new IllegalArgumentException("touch glyph is required");
		}
		return glyph.column;
	}

	static int touchGlyphRow(TouchGlyph glyph) {
		if (glyph == null) {
			throw new IllegalArgumentException("touch glyph is required");
		}
		return glyph.row;
	}

	public static Image touchDisabledStrike(int fallbackColor) {
		return image(
				TILE_SIZE * 14,
				TILE_SIZE * 3,
				TILE_SIZE,
				TILE_SIZE,
				fallbackColor);
	}

	public static Image stamp(Stamp stamp, int fallbackColor) {
		if (stamp == null) {
			throw new IllegalArgumentException("stamp is required");
		}
		return image(
				stamp.column * TILE_SIZE,
				TILE_SIZE * 3,
				TILE_SIZE * 3,
				TILE_SIZE,
				fallbackColor);
	}

	private static Image image(
			int x,
			int y,
			int width,
			int height,
			int fallbackColor) {
		Image result;
		if (!atlasAvailable()) {
			result = new Image(
					TextureCache.createSolid(fallbackColor),
					0,
					0,
					width,
					height);
		} else {
			result = new Image(
					Assets.Interfaces.BUKOV_UI,
					x,
					y,
					width,
					height);
		}
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
