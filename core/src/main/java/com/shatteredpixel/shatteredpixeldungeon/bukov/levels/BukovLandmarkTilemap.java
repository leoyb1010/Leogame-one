package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/** Original 32px Bukov landmark art placed on the host's 16px world grid. */
public final class BukovLandmarkTilemap extends CustomTilemap {

	public enum Kind {
		ARCHIVE_CABINET(0, 1),
		MAINTENANCE_GATE(1, 3),
		PUMP_STATION(4, 1),
		BASE_EXTRACTION(5, 1),
		CONDITIONAL_EXTRACTION(6, 1),
		INDUSTRIAL_CACHE(7, 1),
		CONCRETE_COVER(8, 1),
		SANDBAG_COVER(9, 1);

		final int firstFrame;
		final int frameCount;

		Kind(int firstFrame, int frameCount) {
			this.firstFrame = firstFrame;
			this.frameCount = frameCount;
		}
	}

	private static final String KIND = "bukov_landmark_kind";
	private static final String VISUAL_ASSET_ID =
			"bukov_landmark_visual_asset_id";
	private static final int TEXTURE_WIDTH = 320;
	private static final int CELLS_PER_FRAME = 2;
	private static final String DEFAULT_VISUAL_ASSET_ID = "fog_depot";

	private Kind kind = Kind.ARCHIVE_CABINET;
	private String visualAssetId = DEFAULT_VISUAL_ASSET_ID;

	public BukovLandmarkTilemap() {
		configure(Kind.ARCHIVE_CABINET);
	}

	public BukovLandmarkTilemap(Kind kind) {
		configure(kind, DEFAULT_VISUAL_ASSET_ID);
	}

	public BukovLandmarkTilemap(
			Kind kind, String visualAssetId) {
		configure(kind, visualAssetId);
	}

	public Kind kind() {
		return kind;
	}

	public String visualAssetId() {
		return visualAssetId;
	}

	private void configure(Kind kind) {
		configure(kind, DEFAULT_VISUAL_ASSET_ID);
	}

	private void configure(Kind kind, String visualAssetId) {
		if (kind == null) {
			throw new IllegalArgumentException("kind is required");
		}
		if (visualAssetId == null
				|| !visualAssetId.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException(
					"visualAssetId is required");
		}
		this.kind = kind;
		this.visualAssetId = visualAssetId;
		texture = "environment/bukov/landmarks_"
				+ visualAssetId + ".png";
		tileW = kind.frameCount * CELLS_PER_FRAME;
		tileH = CELLS_PER_FRAME;
	}

	@Override
	public Tilemap create() {
		Tilemap result = super.create();
		result.map(
				mapSimpleImage(
						kind.firstFrame * CELLS_PER_FRAME,
						0,
						TEXTURE_WIDTH),
				tileW);
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KIND, kind);
		bundle.put(VISUAL_ASSET_ID, visualAssetId);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		configure(
				bundle.getEnum(KIND, Kind.class),
				bundle.contains(VISUAL_ASSET_ID)
						? bundle.getString(VISUAL_ASSET_ID)
						: DEFAULT_VISUAL_ASSET_ID);
	}
}
