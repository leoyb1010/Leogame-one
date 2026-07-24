package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
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
	private static final BukovUiTokens DEFAULT_TOKENS =
			BukovUiTokens.loadDefault();
	static final int FRIENDLY_COLOR = color(
			DEFAULT_TOKENS, "combat.fx.shell.friendly");
	static final int HOSTILE_COLOR = color(
			DEFAULT_TOKENS, "combat.fx.shell.hostile");

	private final ShellTrajectory trajectory = new ShellTrajectory();
	private final ColorBlock casing;
	private final int friendlyColor;
	private final int hostileColor;
	private float age;

	public BukovShellFx() {
		this(DEFAULT_TOKENS);
	}

	BukovShellFx(BukovUiTokens tokens) {
		if (tokens == null) {
			throw new IllegalArgumentException("tokens are required");
		}
		friendlyColor = color(tokens, "combat.fx.shell.friendly");
		hostileColor = color(tokens, "combat.fx.shell.hostile");
		casing = new Casing(color(tokens, "combat.fx.solid"));
		add(casing);
		retire();
	}

	public BukovShellFx(
			PointF origin,
			PointF ejectionDirection,
			boolean hostile,
			float intensity) {
		this();
		reset(
				origin == null ? Float.NaN : origin.x,
				origin == null ? Float.NaN : origin.y,
				ejectionDirection == null ? Float.NaN : ejectionDirection.x,
				ejectionDirection == null ? Float.NaN : ejectionDirection.y,
				hostile,
				intensity);
	}

	boolean reset(
			float originX,
			float originY,
			float directionX,
			float directionY,
			boolean hostile,
			float intensity) {
		if (!trajectory.configure(
				originX,
				originY,
				directionX,
				directionY,
				intensity)) {
			retire();
			return false;
		}
		casing.color(hostile ? hostileColor : friendlyColor);
		casing.alpha(1f);
		age = 0f;
		place(0f);
		revive();
		active = true;
		visible = true;
		return true;
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
			retire();
		}
	}

	private void retire() {
		alive = false;
		exists = false;
		active = false;
		visible = false;
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
		ShellTrajectory result = new ShellTrajectory();
		if (origin != null && ejectionDirection != null) {
			result.configure(
					origin.x,
					origin.y,
					ejectionDirection.x,
					ejectionDirection.y,
					intensity);
		}
		return result;
	}

	public static float progressAt(float age) {
		if (!finite(age)) return 0f;
		return Math.max(0f, Math.min(1f, age / DURATION_SECONDS));
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	private static final class Casing extends ColorBlock {

		private Casing(int solidColor) {
			super(3f, 2f, solidColor);
			origin.set(1.5f, 1f);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static int color(BukovUiTokens tokens, String name) {
		return tokens.colorWithAlpha(name, 255);
	}

	public static final class ShellTrajectory {

		private boolean visible;
		private float originX;
		private float originY;
		private float velocityX;
		private float velocityY;
		private float arcHeight;

		private ShellTrajectory() {
		}

		private boolean configure(
				float originX,
				float originY,
				float ejectionX,
				float ejectionY,
				float intensity) {
			visible = false;
			if (!finite(originX)
					|| !finite(originY)
					|| !finite(ejectionX)
					|| !finite(ejectionY)) {
				return false;
			}
			float length = (float)Math.sqrt(
					ejectionX * ejectionX + ejectionY * ejectionY);
			if (length <= 0.01f) {
				return false;
			}
			float strength = Math.max(0.45f, Math.min(1.6f, intensity));
			visible = true;
			this.originX = originX;
			this.originY = originY;
			velocityX = ejectionX / length
					* (3.2f + strength * 1.4f);
			velocityY = ejectionY / length
					* (3.2f + strength * 1.4f);
			arcHeight = 2.2f + strength;
			return true;
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
