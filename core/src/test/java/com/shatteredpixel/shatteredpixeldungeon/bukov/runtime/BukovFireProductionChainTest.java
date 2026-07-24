package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.FireControl;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FireMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinitionTest;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchState;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Production-components integration gate for the player-facing ballistic
 * chain. It deliberately starts at the input merger used by RealtimeInput,
 * not at a hand-authored FireControl flag.
 */
public class BukovFireProductionChainTest {

	@Test
	public void mouseTouchAndControllerReachMagazineTracerAndDamage()
			throws Exception {
		FirearmDefinition definition =
				FirearmDefinitionTest.validDefinition();
		definition.fireMode = FireMode.SEMI;
		definition.baseSpreadDeg = 0f;
		definition.movingSpreadDeg = 0f;
		definition.tracerIntensity = 0.8f;
		definition.effectiveRangeTiles = 10f;

		// Mouse shot reaches the target before the wall.
		assertDamagingShot(
				definition,
				RealtimeInput.resolveFireHeld(
						true, false, false, false),
				RealtimeInput.resolveFirePressed(
						true, false, true, false),
				6);

		// The actual mobile aim/fire stick leaves its dead zone, then enters
		// the same production merge and edge detector.
		BukovTouchState touch = new BukovTouchState();
		assertTrue(touch.beginStick(
				BukovTouchState.Stick.AIM_FIRE,
				17,
				100f,
				100f,
				50f,
				145f,
				100f));
		boolean touchHeld = RealtimeInput.resolveFireHeld(
				false, false, touch.fireHeld(), false);
		assertTrue(touchHeld);
		assertTouchHeldForFixedStepThenReleased(
				definition,
				touch,
				touchHeld);

		// Controller R2 is represented by controllerFireHeld/Pressed in the
		// same merger. The test does not require physical hardware in CI.
		boolean controllerHeld = RealtimeInput.resolveFireHeld(
				false, true, false, false);
		assertDamagingShot(
				definition,
				controllerHeld,
				RealtimeInput.resolveFirePressed(
						false, true, controllerHeld, false),
				6);
	}

	@Test
	public void touchShotStopsAtWallAndCannotDamageTargetBehindIt() {
		FirearmDefinition definition =
				FirearmDefinitionTest.validDefinition();
		definition.fireMode = FireMode.SEMI;
		definition.tracerIntensity = 0.8f;
		definition.effectiveRangeTiles = 10f;
		Firearm firearm = firearm(2);
		ShotSink sink = new ShotSink(definition, 3);
		BukovTouchState touch = new BukovTouchState();
		assertTrue(touch.beginStick(
				BukovTouchState.Stick.AIM_FIRE,
				31,
				100f,
				100f,
				50f,
				150f,
				100f));
		boolean held = RealtimeInput.resolveFireHeld(
				false, false, touch.fireHeld(), false);

		new FireControl().update(
				0f,
				held,
				RealtimeInput.resolveFirePressed(
						false, false, held, false),
				false,
				firearm,
				definition,
				sink);

		assertEquals(1, firearm.magazineAmmo());
		assertEquals(100, sink.targetHp);
		assertEquals(3f, sink.tracerToX, 0.0001f);
		assertTrue(sink.nonZeroTracer);
	}

	private static void assertDamagingShot(
			FirearmDefinition definition,
			boolean held,
			boolean pressed,
			int wallX) {
		Firearm firearm = firearm(2);
		ShotSink sink = new ShotSink(definition, wallX);

		new FireControl().update(
				0f,
				held,
				pressed,
				false,
				firearm,
				definition,
				sink);

		assertEquals(1, firearm.magazineAmmo());
		assertTrue(sink.targetHp < 100);
		assertTrue(sink.nonZeroTracer);
	}

	private static void assertTouchHeldForFixedStepThenReleased(
			FirearmDefinition definition,
			BukovTouchState touch,
			boolean initialHeld) {
		Firearm firearm = firearm(2);
		ShotSink sink = new ShotSink(definition, 6);
		FireControl control = new FireControl();
		FixedStepClock clock = new FixedStepClock(120f, 0.10f, 8);
		final boolean[] previousHeld = {false};

		// The live raid also runs at 120 Hz. Keep the real aim/fire state held
		// across one complete simulation step, including its rising edge.
		assertEquals(1f / 120f, clock.stepSeconds(), 0f);
		clock.advance(clock.stepSeconds(), dt -> {
			boolean held = RealtimeInput.resolveFireHeld(
					false, false, touch.fireHeld(), false);
			control.update(
					dt,
					held,
					RealtimeInput.resolveFirePressed(
							false,
							false,
							held,
							previousHeld[0]),
					false,
					firearm,
					definition,
					sink);
			previousHeld[0] = held;
		});
		assertTrue(initialHeld);
		assertEquals(1, firearm.magazineAmmo());
		assertTrue(sink.targetHp < 100);
		assertTrue(sink.nonZeroTracer);

		// Releasing the pointer clears aim/fire immediately. A later fixed step
		// must neither stick the trigger nor consume another round.
		touch.endPointer(17);
		clock.advance(clock.stepSeconds(), dt -> {
			boolean held = RealtimeInput.resolveFireHeld(
					false, false, touch.fireHeld(), false);
			control.update(
					dt,
					held,
					RealtimeInput.resolveFirePressed(
							false,
							false,
							held,
							previousHeld[0]),
					false,
					firearm,
					definition,
					sink);
			previousHeld[0] = held;
		});
		assertEquals(1, firearm.magazineAmmo());
	}

	private static Firearm firearm(int magazine) {
		return new Firearm().configure(
				"test",
				"production-chain",
				magazine,
				"test_standard");
	}

	private static final class ShotSink implements FireControl.Sink {
		private final FirearmDefinition definition;
		private final int wallX;
		private final RealtimeBody target = body(4.5f, 2.5f);
		private final HitscanResolver.Hit hit =
				new HitscanResolver.Hit();
		private final CombatFxEventPool fx =
				new CombatFxEventPool(4);
		int targetHp = 100;
		boolean nonZeroTracer;
		float tracerToX;

		ShotSink(FirearmDefinition definition, int wallX) {
			this.definition = definition;
			this.wallX = wallX;
		}

		@Override
		public void fire(
				Firearm firearm,
				FirearmDefinition ignored) {
			HitscanResolver.cast(
					1.5f,
					2.5f,
					1f,
					0f,
					definition.effectiveRangeTiles * 2f,
					map(wallX),
					(minX, minY, maxX, maxY) ->
							Collections.singletonList(target),
					null,
					hit);
			fx.tracer(
					1,
					1,
					false,
					1.5f,
					2.5f,
					hit.x,
					hit.y,
					definition.tracerIntensity);
			fx.drain(new CombatFxEvent.Consumer() {
				@Override
				public void accept(CombatFxEvent event) {
					if (event.type() != CombatFxEvent.Type.TRACER) {
						return;
					}
					tracerToX = event.toX();
					float dx = event.toX() - event.fromX();
					float dy = event.toY() - event.fromY();
					nonZeroTracer = event.intensity() > 0f
							&& dx * dx + dy * dy > 0.0001f;
				}
			});
			if (hit.body == target) {
				float damage = RealtimeDamage.resolve(
						definition.damage,
						1f,
						hit.distance,
						definition.effectiveRangeTiles,
						definition.penetration,
						RealtimeDamage.HitZone.CORE,
						null);
				targetHp -= Math.max(1, Math.round(damage));
			}
		}

		@Override
		public FireControl.AmmoSelection requestAmmo(
				String caliber,
				String preferredDefinitionId,
				int maximum,
				boolean allowAlternative) {
			return FireControl.AmmoSelection.none();
		}

		@Override
		public void dryFire() {
		}

		@Override
		public void reloadStarted(float seconds) {
		}

		@Override
		public void reloadAudioCues(
				FirearmDefinition firearm,
				int cueMask) {
		}

		@Override
		public void reloadFinished() {
		}
	}

	private static RealtimeBody body(float x, float y) {
		RealtimeBody body = new RealtimeBody();
		body.x = x;
		body.y = y;
		body.previousX = x;
		body.previousY = y;
		body.radius = 0.25f;
		return body;
	}

	private static CollisionMap map(final int wallX) {
		return new CollisionMap() {
			@Override
			public int width() {
				return 12;
			}

			@Override
			public int height() {
				return 8;
			}

			@Override
			public boolean blocked(int x, int y) {
				return x <= 0
						|| y <= 0
						|| x >= 11
						|| y >= 7
						|| x == wallX;
			}
		};
	}
}
