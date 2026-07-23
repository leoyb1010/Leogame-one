package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Reused three-layer gunshot playback values. Audio backends may apply
 * lowPassHz when supported; the current Sample adapter still applies gain,
 * stereo direction and pitch without allocating.
 */
public final class GunshotAudioPlan {

	private float mechanicalLeft;
	private float mechanicalRight;
	private float mechanicalPitch;
	private float bodyLeft;
	private float bodyRight;
	private float bodyPitch;
	private float tailLeft;
	private float tailRight;
	private float tailPitch;
	private float lowPassHz;
	private boolean audible;

	void set(
			float mechanicalLeft,
			float mechanicalRight,
			float mechanicalPitch,
			float bodyLeft,
			float bodyRight,
			float bodyPitch,
			float tailLeft,
			float tailRight,
			float tailPitch,
			float lowPassHz,
			boolean audible) {
		this.mechanicalLeft = mechanicalLeft;
		this.mechanicalRight = mechanicalRight;
		this.mechanicalPitch = mechanicalPitch;
		this.bodyLeft = bodyLeft;
		this.bodyRight = bodyRight;
		this.bodyPitch = bodyPitch;
		this.tailLeft = tailLeft;
		this.tailRight = tailRight;
		this.tailPitch = tailPitch;
		this.lowPassHz = lowPassHz;
		this.audible = audible;
	}

	public float mechanicalLeft() {
		return mechanicalLeft;
	}

	public float mechanicalRight() {
		return mechanicalRight;
	}

	public float mechanicalPitch() {
		return mechanicalPitch;
	}

	public float bodyLeft() {
		return bodyLeft;
	}

	public float bodyRight() {
		return bodyRight;
	}

	public float bodyPitch() {
		return bodyPitch;
	}

	public float tailLeft() {
		return tailLeft;
	}

	public float tailRight() {
		return tailRight;
	}

	public float tailPitch() {
		return tailPitch;
	}

	public float lowPassHz() {
		return lowPassHz;
	}

	public boolean audible() {
		return audible;
	}
}
