package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
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
	private static final int TEXTURE_WIDTH = 320;
	private static final int CELLS_PER_FRAME = 2;

	private Kind kind = Kind.ARCHIVE_CABINET;

	public BukovLandmarkTilemap() {
		configure(Kind.ARCHIVE_CABINET);
	}

	public BukovLandmarkTilemap(Kind kind) {
		configure(kind);
	}

	public Kind kind() {
		return kind;
	}

	private void configure(Kind kind) {
		if (kind == null) {
			throw new IllegalArgumentException("kind is required");
		}
		this.kind = kind;
		texture = Assets.Environment.BUKOV_FIRST_RAID_LANDMARKS;
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
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		configure(bundle.getEnum(KIND, Kind.class));
	}
}
