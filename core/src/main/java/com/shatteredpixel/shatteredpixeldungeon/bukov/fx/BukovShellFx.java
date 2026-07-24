package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * Tiny ejected casing rendered in world space.
 *
 * This is deliberately presentation-only: the casing follows a deterministic
 * parabola and never participates in collision or the 120 Hz simulation.
 */
public final class BukovShellFx extends Group {

	public static final float DURATION_SECONDS = 0.30f;
	private static final int FRIENDLY_COLOR = 0xD6B85F;
	private static final int HOSTILE_COLOR = 0xA87348;

	private final ShellTrajectory trajectory;
	private final ColorBlock casing;
	private float age;

	public BukovShellFx(
			PointF origin,
			PointF ejectionDirection,
			boolean hostile,
			float intensity) {
		trajectory = plan(origin, ejectionDirection, intensity);
		if (!trajectory.visible()) {
			casing = null;
			visible = false;
			kill();
			return;
		}
		casing = new Casing(hostile ? HOSTILE_COLOR : FRIENDLY_COLOR);
		add(casing);
		place(0f);
	}

	@Override
	public void update() {
		super.update();
		age += Game.elapsed;
		float progress = progressAt(age);
		place(progress);
		if (casing != null) {
			casing.alpha(BukovTracerFx.alphaAt(age, DURATION_SECONDS));
		}
		if (BukovTracerFx.expiredAt(age, DURATION_SECONDS)) {
			killAndErase();
		}
	}

	private void place(float progress) {
		if (casing == null) return;
		casing.x = trajectory.xAt(progress) - 1.5f;
		casing.y = trajectory.yAt(progress) - 1f;
		casing.angle = trajectory.angleAt(progress);
	}

	public static ShellTrajectory plan(
			PointF origin,
			PointF ejectionDirection,
			float intensity) {
		if (origin == null
				|| ejectionDirection == null
				|| !finite(origin.x)
				|| !finite(origin.y)
				|| !finite(ejectionDirection.x)
				|| !finite(ejectionDirection.y)) {
			return ShellTrajectory.hidden();
		}
		float length = (float)Math.sqrt(
				ejectionDirection.x * ejectionDirection.x
						+ ejectionDirection.y * ejectionDirection.y);
		if (length <= 0.01f) {
			return ShellTrajectory.hidden();
		}
		float strength = Math.max(0.45f, Math.min(1.6f, intensity));
		float directionX = ejectionDirection.x / length;
		float directionY = ejectionDirection.y / length;
		return new ShellTrajectory(
				true,
				origin.x,
				origin.y,
				directionX * (3.2f + strength * 1.4f),
				directionY * (3.2f + strength * 1.4f),
				2.2f + strength);
	}

	public static float progressAt(float age) {
		if (!finite(age)) return 0f;
		return Math.max(0f, Math.min(1f, age / DURATION_SECONDS));
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	private static final class Casing extends ColorBlock {

		private Casing(int color) {
			super(3f, 2f, color);
			origin.set(1.5f, 1f);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	public static final class ShellTrajectory {

		private final boolean visible;
		private final float originX;
		private final float originY;
		private final float velocityX;
		private final float velocityY;
		private final float arcHeight;

		private ShellTrajectory(
				boolean visible,
				float originX,
				float originY,
				float velocityX,
				float velocityY,
				float arcHeight) {
			this.visible = visible;
			this.originX = originX;
			this.originY = originY;
			this.velocityX = velocityX;
			this.velocityY = velocityY;
			this.arcHeight = arcHeight;
		}

		private static ShellTrajectory hidden() {
			return new ShellTrajectory(false, 0f, 0f, 0f, 0f, 0f);
		}

		public boolean visible() {
			return visible;
		}

		public float xAt(float progress) {
			float safe = Math.max(0f, Math.min(1f, progress));
			return originX + velocityX * safe;
		}

		public float yAt(float progress) {
			float safe = Math.max(0f, Math.min(1f, progress));
			return originY
					+ velocityY * safe
					- 4f * arcHeight * safe * (1f - safe);
		}

		public float angleAt(float progress) {
			float safe = Math.max(0f, Math.min(1f, progress));
			return 45f + safe * 540f;
		}
	}
}
