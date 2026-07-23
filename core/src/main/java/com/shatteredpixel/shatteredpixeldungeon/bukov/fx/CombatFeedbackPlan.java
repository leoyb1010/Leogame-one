package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Presentation-only output. Multiple requests merge by maximum envelope;
 * hitstop never stacks.
 */
public final class CombatFeedbackPlan {

	private boolean visual;
	private boolean audio;
	private float visualIntensity;
	private float shakeAmplitudePx;
	private int shakeDurationMs;
	private float vibrationAmplitude;
	private int vibrationDurationMs;
	private int hitstopMs;

	public void clear() {
		visual = false;
		audio = false;
		visualIntensity = 0f;
		shakeAmplitudePx = 0f;
		shakeDurationMs = 0;
		vibrationAmplitude = 0f;
		vibrationDurationMs = 0;
		hitstopMs = 0;
	}

	void merge(boolean visual,
			   boolean audio,
			   float visualIntensity,
			   float shakeAmplitudePx,
			   int shakeDurationMs,
			   float vibrationAmplitude,
			   int vibrationDurationMs,
			   int hitstopMs) {
		this.visual |= visual;
		this.audio |= audio;
		this.visualIntensity = Math.max(
				this.visualIntensity,
				visualIntensity
		);
		this.shakeAmplitudePx = Math.max(
				this.shakeAmplitudePx,
				shakeAmplitudePx
		);
		this.shakeDurationMs = Math.max(
				this.shakeDurationMs,
				shakeDurationMs
		);
		this.vibrationAmplitude = Math.max(
				this.vibrationAmplitude,
				vibrationAmplitude
		);
		this.vibrationDurationMs = Math.max(
				this.vibrationDurationMs,
				vibrationDurationMs
		);
		this.hitstopMs = Math.max(this.hitstopMs, hitstopMs);
	}

	public boolean visual() {
		return visual;
	}

	public boolean audio() {
		return audio;
	}

	public float visualIntensity() {
		return visualIntensity;
	}

	public float shakeAmplitudePx() {
		return shakeAmplitudePx;
	}

	public int shakeDurationMs() {
		return shakeDurationMs;
	}

	public float vibrationAmplitude() {
		return vibrationAmplitude;
	}

	public int vibrationDurationMs() {
		return vibrationDurationMs;
	}

	public int hitstopMs() {
		return hitstopMs;
	}
}
