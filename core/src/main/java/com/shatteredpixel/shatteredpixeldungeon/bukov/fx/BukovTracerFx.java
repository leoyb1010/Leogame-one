package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * Short-lived, hitscan tracer rendered as a moving head with a restrained tail.
 *
 * Coordinates are world-space pixels. This effect is intentionally independent
 * from the host game's arrow and magic-missile effects.
 */
public final class BukovTracerFx extends Group {

	/**
	 * Firearms are hitscan for gameplay. The visual head only bridges a few
	 * frames so it reads as a round, not a slow projectile or energy bolt.
	 */
	public static final float TRAVEL_SECONDS = 0.10f;
	public static final float DURATION_SECONDS = 0.18f;
	static final float MAX_TRAIL_PIXELS = 16f;
	static final float MAX_TRAIL_FRACTION = 0.25f;
	// Muted amber stays readable without resembling a full-length laser.
	// ColorBlock consumes ARGB, so the alpha byte must be explicit.
	public static final int FRIENDLY_COLOR = 0xFFFFD27A;
	public static final int HOSTILE_COLOR = 0xFFFF725C;
	static final int HEAD_OUTLINE_COLOR = 0xFF151B20;

	private final float duration = DURATION_SECONDS;
	private final TraceGeometry geometry = new TraceGeometry();
	private final TailSegment workingTail = new TailSegment();
	private final LightLine glowLine;
	private final LightLine coreLine;
	private final BulletHead bulletHeadOutline;
	private final BulletHead bulletHead;
	private float age;
	private boolean hasPresentedTravelFrame;

	/**
	 * Creates one dormant view slot. {@link BukovCombatFxViewPool} owns these
	 * slots and reconfigures them without allocating scene nodes per shot.
	 */
	public BukovTracerFx() {
		glowLine = new LightLine(0.12f);
		add(glowLine);
		coreLine = new LightLine(0.8f);
		add(coreLine);
		bulletHeadOutline = new BulletHead(false);
		add(bulletHeadOutline);
		bulletHead = new BulletHead(true);
		add(bulletHead);
		retire();
	}

	public BukovTracerFx(PointF from, PointF to, boolean hostile, float intensity) {
		this();
		reset(
				from == null ? Float.NaN : from.x,
				from == null ? Float.NaN : from.y,
				to == null ? Float.NaN : to.x,
				to == null ? Float.NaN : to.y,
				hostile,
				intensity);
	}

	boolean reset(
			float fromX,
			float fromY,
			float toX,
			float toY,
			boolean hostile,
			float intensity) {
		if (!geometry.configure(fromX, fromY, toX, toY, intensity)) {
			retire();
			return false;
		}
		age = 0f;
		hasPresentedTravelFrame = false;
		int color = hostile ? HOSTILE_COLOR : FRIENDLY_COLOR;
		glowLine.configure(
				geometry.glowThickness(),
				geometry.angleDegrees(),
				color);
		coreLine.configure(
				geometry.coreThickness(),
				geometry.angleDegrees(),
				color);
		tailSegmentAt(geometry, 0f, workingTail);
		glowLine.place(workingTail, 0f);
		coreLine.place(workingTail, 0f);
		bulletHeadOutline.configure(
				geometry.fromX(),
				geometry.fromY(),
				geometry.angleDegrees(),
				HEAD_OUTLINE_COLOR,
				outlineWidthFor(geometry.coreThickness()),
				outlineHeightFor(geometry.coreThickness()));
		bulletHead.configure(
				geometry.fromX(),
				geometry.fromY(),
				geometry.angleDegrees(),
				color,
				headWidthFor(geometry.coreThickness()),
				headHeightFor(geometry.coreThickness()));
		activate();
		return true;
	}

	@Override
	public void update() {
		super.update();
		boolean hadPresentedTravelFrame = hasPresentedTravelFrame;
		age += Game.elapsed;
		float trailAlpha = trailAlphaAt(age);
		if (age >= duration && !hadPresentedTravelFrame) {
			trailAlpha = Math.max(0.45f, trailAlpha);
		}
		tailSegmentAt(geometry, age, workingTail);
		glowLine.place(workingTail, trailAlpha);
		coreLine.place(workingTail, trailAlpha);
		if (bulletHead != null) {
			float progress = travelProgressAt(age, TRAVEL_SECONDS);
			float headX = geometry.fromX()
					+ (geometry.toX() - geometry.fromX()) * progress;
			float headY = geometry.fromY()
					+ (geometry.toY() - geometry.fromY()) * progress;
			float headAlpha = headAlphaAt(age);
			if (age >= duration && !hadPresentedTravelFrame) {
				headAlpha = Math.max(0.55f, headAlpha);
			}
			bulletHeadOutline.moveTo(headX, headY);
			bulletHeadOutline.fade(headAlpha * 0.9f);
			bulletHead.moveTo(headX, headY);
			bulletHead.fade(headAlpha);
			hasPresentedTravelFrame |= progress > 0f;
		}
		if (shouldExpireAfterUpdate(age, duration, hadPresentedTravelFrame)) {
			retire();
		}
	}

	public static TraceGeometry plan(PointF from, PointF to, float intensity) {
		TraceGeometry result = new TraceGeometry();
		if (from != null && to != null) {
			result.configure(from.x, from.y, to.x, to.y, intensity);
		}
		return result;
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

	static float trailAlphaAt(float age) {
		if (!finite(age) || age < 0f || age >= DURATION_SECONDS) {
			return 0f;
		}
		if (age <= TRAVEL_SECONDS) {
			return 1f;
		}
		return clamp(
				1f - (age - TRAVEL_SECONDS) / (DURATION_SECONDS - TRAVEL_SECONDS),
				0f,
				1f);
	}

	static float headAlphaAt(float age) {
		if (!finite(age) || age < 0f || age >= DURATION_SECONDS) {
			return 0f;
		}
		if (age <= TRAVEL_SECONDS) {
			return 1f;
		}
		// The projectile arrives instantly for gameplay, but the endpoint
		// remains bright long enough for the eye to connect shot and impact.
		float residual = trailAlphaAt(age);
		return clamp(residual * residual, 0f, 1f);
	}

	static float tailLengthFor(float traceLength) {
		if (!finite(traceLength) || traceLength <= 0f) {
			return 0f;
		}
		return Math.min(
				MAX_TRAIL_PIXELS,
				traceLength * MAX_TRAIL_FRACTION);
	}

	static TailSegment tailSegmentAt(TraceGeometry geometry, float age) {
		TailSegment result = new TailSegment();
		tailSegmentAt(geometry, age, result);
		return result;
	}

	private static void tailSegmentAt(
			TraceGeometry geometry, float age, TailSegment out) {
		if (geometry == null || !geometry.visible() || !finite(age) || age < 0f) {
			out.set(false, 0f, 0f, 0f, 0f, 0f, 0f);
			return;
		}
		float endProgress = travelProgressAt(age, TRAVEL_SECONDS);
		float tailFraction = tailLengthFor(geometry.length()) / geometry.length();
		float startProgress = Math.max(0f, endProgress - tailFraction);
		float startX = geometry.fromX()
				+ (geometry.toX() - geometry.fromX()) * startProgress;
		float startY = geometry.fromY()
				+ (geometry.toY() - geometry.fromY()) * startProgress;
		float endX = geometry.fromX()
				+ (geometry.toX() - geometry.fromX()) * endProgress;
		float endY = geometry.fromY()
				+ (geometry.toY() - geometry.fromY()) * endProgress;
		float segmentLength = geometry.length() * (endProgress - startProgress);
		out.set(
				segmentLength > 0.01f,
				startX,
				startY,
				endX,
				endY,
				segmentLength,
				geometry.angleDegrees());
	}

	private void activate() {
		revive();
		active = true;
		visible = true;
	}

	private void retire() {
		alive = false;
		exists = false;
		active = false;
		visible = false;
	}

	static float headWidthFor(float coreThickness) {
		return Math.max(4f, coreThickness * 4f);
	}

	static float headHeightFor(float coreThickness) {
		return Math.max(1.5f, coreThickness * 1.7f);
	}

	static float outlineWidthFor(float coreThickness) {
		return Math.max(5.4f, coreThickness * 5.2f);
	}

	static float outlineHeightFor(float coreThickness) {
		return Math.max(2.3f, coreThickness * 2.6f);
	}

	/**
	 * A tracer may be created after its parent group's update has already
	 * drained combat events. If the next frame hitches past the whole tracer
	 * duration, keep the endpoint for that draw instead of erasing a head that
	 * has only ever been visible at the muzzle.
	 */
	static boolean shouldExpireAfterUpdate(
			float age,
			float duration,
			boolean hadPresentedTravelFrame) {
		if (!finite(age) || !finite(duration) || duration <= 0f) {
			return true;
		}
		return age >= duration && hadPresentedTravelFrame;
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

		private float thickness;
		private final float alphaWeight;

		private LightLine(float alpha) {
			super(1f, 1f, 0xFFFFFFFF);
			origin.set(0f, 0.5f);
			alphaWeight = alpha;
			visible = false;
		}

		private void configure(float thickness, float angle, int color) {
			this.thickness = thickness;
			this.angle = angle;
			color(color & 0xFFFFFF);
			alpha(0f);
			visible = false;
		}

		private void place(TailSegment segment, float alpha) {
			if (segment == null || !segment.visible() || alpha <= 0f) {
				visible = false;
				return;
			}
			visible = true;
			x = segment.startX();
			y = segment.startY() - 0.5f;
			angle = segment.angleDegrees();
			size(segment.length(), thickness);
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

		private BulletHead(boolean additive) {
			super(1f, 1f, 0xFFFFFFFF);
			// ColorBlock's source texture is one pixel; the scale holds the
			// authored size, so 0.5/0.5 is its actual transform origin.
			origin.set(0.5f, 0.5f);
			this.additive = additive;
		}

		private final boolean additive;

		private void configure(
				float x,
				float y,
				float angle,
				int color,
				float width,
				float height) {
			size(width, height);
			color(color & 0xFFFFFF);
			this.angle = angle;
			moveTo(x, y);
			fade(1f);
			visible = true;
		}

		private void moveTo(float centerX, float centerY) {
			x = centerX - origin.x;
			y = centerY - origin.y;
		}

		private void fade(float alpha) {
			alpha(clamp(alpha, 0f, 1f));
		}

		@Override
		public void draw() {
			if (additive) {
				Blending.setLightMode();
			}
			super.draw();
			if (additive) {
				Blending.setNormalMode();
			}
		}
	}

	static final class TailSegment {

		private boolean visible;
		private float startX;
		private float startY;
		private float endX;
		private float endY;
		private float length;
		private float angleDegrees;

		private TailSegment() {
		}

		private void set(
				boolean visible,
				float startX,
				float startY,
				float endX,
				float endY,
				float length,
				float angleDegrees) {
			this.visible = visible;
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
			this.length = length;
			this.angleDegrees = angleDegrees;
		}

		private static TailSegment hidden() {
			return new TailSegment();
		}

		boolean visible() {
			return visible;
		}

		float startX() {
			return startX;
		}

		float startY() {
			return startY;
		}

		float endX() {
			return endX;
		}

		float endY() {
			return endY;
		}

		float length() {
			return length;
		}

		float angleDegrees() {
			return angleDegrees;
		}
	}

	public static final class TraceGeometry {

		private boolean visible;
		private float fromX;
		private float fromY;
		private float toX;
		private float toY;
		private float length;
		private float angleDegrees;
		private float coreThickness;
		private float glowThickness;

		private TraceGeometry() {
		}

		private boolean configure(
				float fromX,
				float fromY,
				float toX,
				float toY,
				float intensity) {
			visible = false;
			if (!finite(fromX) || !finite(fromY)
					|| !finite(toX) || !finite(toY)) {
				return false;
			}
			float dx = toX - fromX;
			float dy = toY - fromY;
			float length = (float)Math.sqrt(dx * dx + dy * dy);
			if (length <= 0.01f) {
				return false;
			}
			float strength = clamp(intensity, 0.35f, 1.6f);
			visible = true;
			this.fromX = fromX;
			this.fromY = fromY;
			this.toX = toX;
			this.toY = toY;
			this.length = length;
			angleDegrees = (float)Math.toDegrees(Math.atan2(dy, dx));
			coreThickness = 0.55f + strength * 0.20f;
			glowThickness = 1.10f + strength * 0.50f;
			return true;
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
