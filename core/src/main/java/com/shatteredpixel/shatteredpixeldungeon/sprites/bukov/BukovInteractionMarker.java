package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/**
 * World-space marker for Bukov-only interaction anchors.
 *
 * Markers share the item atlas but have a separate API so extraction and
 * objective anchors cannot accidentally fall back to the host BEACON image.
 */
public final class BukovInteractionMarker extends Image {

	public enum Kind {
		FIXED_EXTRACTION(BukovItemSprite.Frame.FIXED_EXTRACTION),
		CONDITIONAL_EXTRACTION(BukovItemSprite.Frame.CONDITIONAL_EXTRACTION),
		PUMP_STATION(BukovItemSprite.Frame.PUMP_STATION),
		BOSS_DECOY(BukovItemSprite.Frame.CONDITIONAL_EXTRACTION),
		BOSS_SYNCHRONIZED_TRACE(BukovItemSprite.Frame.FIXED_EXTRACTION);

		private final BukovItemSprite.Frame frame;

		Kind(BukovItemSprite.Frame frame) {
			this.frame = frame;
		}

		public BukovItemSprite.Frame frame() {
			return frame;
		}
	}

	private static final TextureFilm FILM = new TextureFilm(
			BukovItemSprite.FRAME_SIZE * BukovItemSprite.FRAME_COUNT,
			BukovItemSprite.FRAME_SIZE,
			BukovItemSprite.FRAME_SIZE,
			BukovItemSprite.FRAME_SIZE);

	private Kind kind;
	private int cell = -1;
	private float pulseTime;

	public BukovInteractionMarker(Kind kind) {
		super(BukovItemSprite.ATLAS);
		kind(kind);
	}

	public BukovInteractionMarker kind(Kind kind) {
		if (kind == null) {
			throw new IllegalArgumentException("kind is required");
		}
		this.kind = kind;
		frame(FILM.get(kind.frame().index()));
		return this;
	}

	public Kind kind() {
		return kind;
	}

	public BukovInteractionMarker placeAtCell(int cell) {
		this.cell = cell;
		place();
		return this;
	}

	public int cell() {
		return cell;
	}

	private void place() {
		if (cell < 0 || Dungeon.level == null) {
			return;
		}
		PointF point = new PointF(
				PixelScene.align(Camera.main,
						((cell % Dungeon.level.width()) + 0.5f) * DungeonTilemap.SIZE - width() * 0.5f),
				PixelScene.align(Camera.main,
						((cell / Dungeon.level.width()) + 0.5f) * DungeonTilemap.SIZE - height() * 0.5f));
		point(point);
	}

	@Override
	public void update() {
		super.update();
		place();
		pulseTime += Game.elapsed;
		// The synchronized phase-two trace is a stable, slower signal. Hollow
		// decoys flicker rapidly, giving a deterministic visual clue without
		// requiring colour perception.
		float rate = kind == Kind.BOSS_SYNCHRONIZED_TRACE ? 1.8f
				: kind == Kind.BOSS_DECOY ? 7f : 4f;
		float floor = kind == Kind.BOSS_SYNCHRONIZED_TRACE ? 0.94f : 0.86f;
		float pulse = floor
				+ (1f - floor) * (float)Math.sin(pulseTime * rate);
		alpha(pulse);
	}
}
