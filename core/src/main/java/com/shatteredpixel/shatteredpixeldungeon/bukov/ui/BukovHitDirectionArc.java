package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Thin 60-degree annular sector used at the viewport edge for incoming damage.
 * One fixed mesh is scaled into the current viewport ellipse, so it allocates
 * nothing per frame and remains stable at 120/144 Hz.
 */
final class BukovHitDirectionArc extends Visual {

	private static final int SEGMENTS = 6;
	private static final float INNER_RADIUS = 0.92f;
	private static final float HALF_SWEEP_RADIANS =
			(float)Math.toRadians(30f);

	private final SmartTexture texture;
	private final FloatBuffer vertices;
	private final ShortBuffer indices;

	BukovHitDirectionArc(int color) {
		this(color, BukovUiTokens.loadDefault());
	}

	BukovHitDirectionArc(int color, BukovUiTokens tokens) {
		super(0f, 0f, 0f, 0f);
		if (tokens == null) {
			throw new IllegalArgumentException("tokens are required");
		}
		texture = TextureCache.createSolid(
				tokens.colorWithAlpha("combat.fx.solid", 255));
		hardlight(color);
		vertices = ByteBuffer.allocateDirect(
				SEGMENTS * 4 * 4 * Float.SIZE / 8)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		indices = ByteBuffer.allocateDirect(
				SEGMENTS * 6 * Short.SIZE / 8)
				.order(ByteOrder.nativeOrder())
				.asShortBuffer();
		buildMesh();
		visible = false;
	}

	void direction(BukovRaidHudState.Direction direction) {
		angle = angleFor(direction);
	}

	void fit(float centerX, float centerY, float radiusX, float radiusY) {
		x = centerX;
		y = centerY;
		scale.set(
				Math.max(1f, radiusX),
				Math.max(1f, radiusY));
	}

	static float angleFor(BukovRaidHudState.Direction direction) {
		if (direction == null) return 0f;
		switch (direction) {
			case NE: return 45f;
			case E: return 90f;
			case SE: return 135f;
			case S: return 180f;
			case SW: return 225f;
			case W: return 270f;
			case NW: return 315f;
			case N:
			default:
				return 0f;
		}
	}

	@Override
	public void draw() {
		super.draw();
		NoosaScript script = NoosaScript.get();
		texture.bind();
		script.uModel.valueM4(matrix);
		script.lighting(rm, gm, bm, am, ra, ga, ba, aa);
		script.camera(camera());
		script.drawElements(vertices, indices, SEGMENTS * 6);
	}

	private void buildMesh() {
		float start = -(float)Math.PI * 0.5f - HALF_SWEEP_RADIANS;
		float step = HALF_SWEEP_RADIANS * 2f / SEGMENTS;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			float first = start + step * segment;
			float second = first + step;
			putVertex((float)Math.cos(first), (float)Math.sin(first));
			putVertex(
					(float)Math.cos(first) * INNER_RADIUS,
					(float)Math.sin(first) * INNER_RADIUS);
			putVertex(
					(float)Math.cos(second) * INNER_RADIUS,
					(float)Math.sin(second) * INNER_RADIUS);
			putVertex((float)Math.cos(second), (float)Math.sin(second));

			short base = (short)(segment * 4);
			indices.put(base);
			indices.put((short)(base + 1));
			indices.put((short)(base + 2));
			indices.put(base);
			indices.put((short)(base + 2));
			indices.put((short)(base + 3));
		}
		((Buffer)vertices).position(0);
		((Buffer)indices).position(0);
	}

	private void putVertex(float x, float y) {
		vertices.put(x);
		vertices.put(y);
		vertices.put(0.5f);
		vertices.put(0.5f);
	}
}
