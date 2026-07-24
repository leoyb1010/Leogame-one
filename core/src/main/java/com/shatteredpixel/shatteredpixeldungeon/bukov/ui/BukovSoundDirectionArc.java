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
 * One fixed segment of the player-centred sound ring.
 *
 * <p>Both short (footstep) and long (gunshot/cue) meshes are allocated once.
 * Selecting a sound type only switches buffers and therefore remains
 * allocation-free at high refresh rates.</p>
 */
final class BukovSoundDirectionArc extends Visual {

	private static final int SEGMENTS = 5;
	private static final float INNER_RADIUS = 0.76f;
	private static final float SHORT_HALF_SWEEP_RADIANS =
			(float)Math.toRadians(11f);
	private static final float LONG_HALF_SWEEP_RADIANS =
			(float)Math.toRadians(21f);

	private final SmartTexture texture =
			TextureCache.createSolid(0xFFFFFFFF);
	private final FloatBuffer shortVertices = vertexBuffer();
	private final FloatBuffer longVertices = vertexBuffer();
	private final ShortBuffer indices;
	private boolean longArc;

	BukovSoundDirectionArc(int color) {
		super(0f, 0f, 0f, 0f);
		hardlight(color);
		indices = ByteBuffer.allocateDirect(
				SEGMENTS * 6 * Short.SIZE / 8)
				.order(ByteOrder.nativeOrder())
				.asShortBuffer();
		buildVertices(shortVertices, SHORT_HALF_SWEEP_RADIANS);
		buildVertices(longVertices, LONG_HALF_SWEEP_RADIANS);
		buildIndices();
		visible = false;
	}

	void direction(BukovRaidHudState.Direction direction) {
		angle = BukovHitDirectionArc.angleFor(direction);
	}

	void longArc(boolean longArc) {
		this.longArc = longArc;
	}

	void fit(float centerX, float centerY, float radius) {
		x = centerX;
		y = centerY;
		float safeRadius = Math.max(1f, radius);
		scale.set(safeRadius, safeRadius);
	}

	@Override
	public void draw() {
		super.draw();
		NoosaScript script = NoosaScript.get();
		texture.bind();
		script.uModel.valueM4(matrix);
		script.lighting(rm, gm, bm, am, ra, ga, ba, aa);
		script.camera(camera());
		script.drawElements(
				longArc ? longVertices : shortVertices,
				indices,
				SEGMENTS * 6);
	}

	private static FloatBuffer vertexBuffer() {
		return ByteBuffer.allocateDirect(
				SEGMENTS * 4 * 4 * Float.SIZE / 8)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
	}

	private void buildVertices(FloatBuffer vertices, float halfSweep) {
		float start = -(float)Math.PI * 0.5f - halfSweep;
		float step = halfSweep * 2f / SEGMENTS;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			float first = start + step * segment;
			float second = first + step;
			putVertex(vertices, (float)Math.cos(first), (float)Math.sin(first));
			putVertex(
					vertices,
					(float)Math.cos(first) * INNER_RADIUS,
					(float)Math.sin(first) * INNER_RADIUS);
			putVertex(
					vertices,
					(float)Math.cos(second) * INNER_RADIUS,
					(float)Math.sin(second) * INNER_RADIUS);
			putVertex(vertices, (float)Math.cos(second), (float)Math.sin(second));
		}
		((Buffer)vertices).position(0);
	}

	private void buildIndices() {
		for (int segment = 0; segment < SEGMENTS; segment++) {
			short base = (short)(segment * 4);
			indices.put(base);
			indices.put((short)(base + 1));
			indices.put((short)(base + 2));
			indices.put(base);
			indices.put((short)(base + 2));
			indices.put((short)(base + 3));
		}
		((Buffer)indices).position(0);
	}

	private static void putVertex(FloatBuffer vertices, float x, float y) {
		vertices.put(x);
		vertices.put(y);
		vertices.put(0.5f);
		vertices.put(0.5f);
	}
}
