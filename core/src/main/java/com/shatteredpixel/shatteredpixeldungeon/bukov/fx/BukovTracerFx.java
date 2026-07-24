package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * Short-lived, hitscan tracer rendered as a real muzzle-to-impact line.
 *
 * Coordinates are world-space pixels. This effect is intentionally independent
 * from the host game's arrow and magic-missile effects.
 */
public final class BukovTracerFx extends Group {

	/**
	 * Long enough to survive several frames at both 60 and 120 fps. The old
	 * 80 ms trace was technically rendered but was easy to miss during normal
	 * mouse/touch input, which made consumed ammunition look like a failed
	 * shot.
	 */
	public static final float DURATION_SECONDS = 0.16f;
	public static final int FRIENDLY_COLOR = 0xB9F6FF;
	public static final int HOSTILE_COLOR = 0xFF765E;

	private final float duration;
	private final TraceGeometry geometry;
	private final BulletHead bulletHead;
	private float age;

	public BukovTracerFx(PointF from, PointF to, boolean hostile, float intensity) {
		geometry = plan(from, to, intensity);
		duration = DURATION_SECONDS;
		if (!geometry.visible()) {
			bulletHead = null;
			visible = false;
			kill();
			return;
		}

		int color = hostile ? HOSTILE_COLOR : FRIENDLY_COLOR;
		add(new LightLine(
				geometry.fromX(),
				geometry.fromY(),
				geometry.length(),
				geometry.glowThickness(),
				geometry.angleDegrees(),
				color,
				0.24f));
		add(new LightLine(
				geometry.fromX(),
				geometry.fromY(),
				geometry.length(),
				geometry.coreThickness(),
				geometry.angleDegrees(),
				color,
				1f));
		bulletHead = new BulletHead(
				geometry.fromX(),
				geometry.fromY(),
				geometry.angleDegrees(),
				color,
				geometry.coreThickness());
		add(bulletHead);
	}

	@Override
	public void update() {
		super.update();
		age += Game.elapsed;
		float alpha = alphaAt(age, duration);
		for (int index = 0; index < length; index++) {
			com.watabou.noosa.Gizmo child = members.get(index);
			if (child instanceof LightLine) {
				((LightLine) child).fade(alpha);
			}
		}
		if (bulletHead != null) {
			float progress = travelProgressAt(age, duration);
			bulletHead.moveTo(
					geometry.fromX()
							+ (geometry.toX() - geometry.fromX()) * progress,
					geometry.fromY()
							+ (geometry.toY() - geometry.fromY()) * progress);
			bulletHead.fade(alpha);
		}
		if (expiredAt(age, duration)) {
			killAndErase();
		}
	}

	public static TraceGeometry plan(PointF from, PointF to, float intensity) {
		if (from == null || to == null
				|| !finite(from.x) || !finite(from.y)
				|| !finite(to.x) || !finite(to.y)) {
			return TraceGeometry.hidden();
		}
		float dx = to.x - from.x;
		float dy = to.y - from.y;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length <= 0.01f) {
			return TraceGeometry.hidden();
		}
		float strength = clamp(intensity, 0.35f, 1.6f);
		return new TraceGeometry(
				true,
				from.x,
				from.y,
				to.x,
				to.y,
				length,
				(float) Math.toDegrees(Math.atan2(dy, dx)),
				0.65f + strength * 0.35f,
				1.7f + strength * 0.8f);
	}

	public static float alphaAt(float age, float duration) {
		if (!finite(age) || !finite(duration) || duration <= 0f || age >= duration) {
			return 0f;
		}
		return clamp(1f - age / duration, 0f, 1f);
	}

	public static boolean expiredAt(float age, float duration) {
		return !finite(age) || !finite(duration) || duration <= 0f || age >= duration;
	}

	public static float travelProgressAt(float age, float duration) {
		if (!finite(age) || !finite(duration) || duration <= 0f) {
			return 0f;
		}
		return clamp(age / duration, 0f, 1f);
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	private static float clamp(float value, float minimum, float maximum) {
		if (!finite(value)) {
			return minimum;
		}
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static final class LightLine extends ColorBlock {

		private final float alphaWeight;

		private LightLine(float x,
						  float y,
						  float length,
						  float thickness,
						  float angle,
						  int color,
						  float alpha) {
			super(length, thickness, color);
			origin.set(0f, 0.5f);
			this.x = x;
			this.y = y - 0.5f;
			this.angle = angle;
			alphaWeight = alpha;
			fade(1f);
		}

		private void fade(float alpha) {
			alpha(alpha * alphaWeight);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static final class BulletHead extends ColorBlock {

		private BulletHead(
				float x,
				float y,
				float angle,
				int color,
				float coreThickness) {
			super(
					Math.max(3.4f, coreThickness * 3.6f),
					Math.max(1.8f, coreThickness * 1.8f),
					color);
			// ColorBlock's source texture is one pixel; the scale holds the
			// authored size, so 0.5/0.5 is its actual transform origin.
			origin.set(0.5f, 0.5f);
			this.x = x - origin.x;
			this.y = y - origin.y;
			this.angle = angle;
		}

		private void moveTo(float centerX, float centerY) {
			x = centerX - origin.x;
			y = centerY - origin.y;
		}

		private void fade(float alpha) {
			alpha(Math.max(0.35f, alpha));
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	public static final class TraceGeometry {

		private final boolean visible;
		private final float fromX;
		private final float fromY;
		private final float toX;
		private final float toY;
		private final float length;
		private final float angleDegrees;
		private final float coreThickness;
		private final float glowThickness;

		private TraceGeometry(boolean visible,
							  float fromX,
							  float fromY,
							  float toX,
							  float toY,
							  float length,
							  float angleDegrees,
							  float coreThickness,
							  float glowThickness) {
			this.visible = visible;
			this.fromX = fromX;
			this.fromY = fromY;
			this.toX = toX;
			this.toY = toY;
			this.length = length;
			this.angleDegrees = angleDegrees;
			this.coreThickness = coreThickness;
			this.glowThickness = glowThickness;
		}

		private static TraceGeometry hidden() {
			return new TraceGeometry(false, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
		}

		public boolean visible() {
			return visible;
		}

		public float fromX() {
			return fromX;
		}

		public float fromY() {
			return fromY;
		}

		public float toX() {
			return toX;
		}

		public float toY() {
			return toY;
		}

		public float length() {
			return length;
		}

		public float angleDegrees() {
			return angleDegrees;
		}

		public float coreThickness() {
			return coreThickness;
		}

		public float glowThickness() {
			return glowThickness;
		}
	}
}
