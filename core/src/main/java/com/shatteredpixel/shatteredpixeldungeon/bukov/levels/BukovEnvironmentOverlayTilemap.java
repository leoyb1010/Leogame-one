package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * Static two-cell ambience decal selected by the active raid theme.
 *
 * It owns no terrain and therefore cannot change collision, routes or mission
 * anchors. One map places at most three instances, avoiding an animation loop
 * or per-frame particle allocation.
 */
public final class BukovEnvironmentOverlayTilemap extends CustomTilemap {

	public enum Variant {
		AMBIENT(0),
		ACCENT(1);

		final int frame;

		Variant(int frame) {
			this.frame = frame;
		}
	}

	private static final String VARIANT =
			"bukov_environment_overlay_variant";
	private static final String VISUAL_ASSET_ID =
			"bukov_environment_overlay_visual_asset_id";
	private static final int TEXTURE_WIDTH = 64;
	private static final int CELLS_PER_FRAME = 2;
	private static final String DEFAULT_VISUAL_ASSET_ID = "fog_depot";

	private Variant variant = Variant.AMBIENT;
	private String visualAssetId = DEFAULT_VISUAL_ASSET_ID;

	public BukovEnvironmentOverlayTilemap() {
		configure(Variant.AMBIENT, DEFAULT_VISUAL_ASSET_ID);
	}

	public BukovEnvironmentOverlayTilemap(
			Variant variant, String visualAssetId) {
		configure(variant, visualAssetId);
	}

	public Variant variant() {
		return variant;
	}

	public String visualAssetId() {
		return visualAssetId;
	}

	private void configure(
			Variant variant, String visualAssetId) {
		if (variant == null) {
			throw new IllegalArgumentException("variant is required");
		}
		if (visualAssetId == null
				|| !visualAssetId.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException(
					"visualAssetId is required");
		}
		this.variant = variant;
		this.visualAssetId = visualAssetId;
		texture = "environment/bukov/overlays_"
				+ visualAssetId + ".png";
		tileW = CELLS_PER_FRAME;
		tileH = CELLS_PER_FRAME;
	}

	@Override
	public Tilemap create() {
		Tilemap result = super.create();
		result.map(
				mapSimpleImage(
						variant.frame * CELLS_PER_FRAME,
						0,
						TEXTURE_WIDTH),
				tileW);
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(VARIANT, variant);
		bundle.put(VISUAL_ASSET_ID, visualAssetId);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		configure(
				bundle.getEnum(VARIANT, Variant.class),
				bundle.contains(VISUAL_ASSET_ID)
						? bundle.getString(VISUAL_ASSET_ID)
						: DEFAULT_VISUAL_ASSET_ID);
	}
}
