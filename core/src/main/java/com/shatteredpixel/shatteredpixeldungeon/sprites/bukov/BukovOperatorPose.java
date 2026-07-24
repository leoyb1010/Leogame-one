package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

/**
 * Resolves the two independent directions used by the realtime operator.
 *
 * <p>The lower body follows locomotion and keeps its last useful direction
 * while stationary. The upper body follows aim whenever an aim vector exists;
 * otherwise it follows locomotion. Keeping this state outside {@code HeroSprite}
 * makes the input contract deterministic and independently testable.</p>
 */
public final class BukovOperatorPose {

	private BukovFacing8 locomotionFacing = BukovFacing8.S;
	private BukovFacing8 upperBodyFacing = BukovFacing8.S;
	private boolean aimActive;

	public void update(float moveX, float moveY, float aimX, float aimY) {
		boolean moving = moveX != 0f || moveY != 0f;
		if (moving) {
			locomotionFacing = BukovFacing8.resolve(moveX, moveY);
		}

		aimActive = aimX != 0f || aimY != 0f;
		if (aimActive) {
			upperBodyFacing = BukovFacing8.resolve(aimX, aimY);
		} else if (moving) {
			upperBodyFacing = locomotionFacing;
		}
	}

	public void faceUpperBody(float dx, float dy) {
		if (dx != 0f || dy != 0f) {
			upperBodyFacing = BukovFacing8.resolve(dx, dy);
		}
	}

	public BukovFacing8 locomotionFacing() {
		return locomotionFacing;
	}

	public BukovFacing8 upperBodyFacing() {
		return upperBodyFacing;
	}

	public boolean aimActive() {
		return aimActive;
	}
}
