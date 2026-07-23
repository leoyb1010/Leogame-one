package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Presentation-only pulse emitted after a realtime action or outcome commits.
 *
 * Consumers must read an event synchronously and must not retain it after the
 * pool drain returns.
 */
public final class CombatPresentationEvent {

	public enum Type {
		PLAYER_FIRE,
		PLAYER_RELOAD,
		PLAYER_HIT,
		PLAYER_MEDICAL_START,
		PLAYER_MEDICAL_END,
		PLAYER_DEATH,
		PLAYER_EXTRACTION,
		EXTRACTION_COMPLETE,
		ENEMY_FIRE,
		ENEMY_MELEE,
		ENEMY_HIT,
		ENEMY_DEATH
	}

	public interface Consumer {
		void accept(CombatPresentationEvent event);
	}

	private Type type;
	private int sourceId;
	private int targetId;
	private int sourceCell;
	private int targetCell;
	private CombatFeedbackType feedbackType;
	private float intensity;

	void set(Type type,
			 int sourceId,
			 int targetId,
			 int sourceCell,
			 int targetCell,
			 CombatFeedbackType feedbackType,
			 float intensity) {
		this.type = type;
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.sourceCell = sourceCell;
		this.targetCell = targetCell;
		this.feedbackType = feedbackType;
		this.intensity = intensity;
	}

	public Type type() {
		return type;
	}

	public int sourceId() {
		return sourceId;
	}

	public int targetId() {
		return targetId;
	}

	public int sourceCell() {
		return sourceCell;
	}

	public int targetCell() {
		return targetCell;
	}

	public CombatFeedbackType feedbackType() {
		return feedbackType;
	}

	public float intensity() {
		return intensity;
	}
}
