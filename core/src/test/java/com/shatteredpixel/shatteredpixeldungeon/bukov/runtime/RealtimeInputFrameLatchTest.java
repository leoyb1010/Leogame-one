package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class RealtimeInputFrameLatchTest {

	@Parameterized.Parameters(name = "{0} render FPS")
	public static Collection<Object[]> renderRates() {
		return Arrays.asList(new Object[][] {
				{30},
				{60},
				{90},
				{120},
				{144}
		});
	}

	private final int renderFps;

	public RealtimeInputFrameLatchTest(int renderFps) {
		this.renderFps = renderFps;
	}

	@Test
	public void oneRenderEdgeIsConsumedByOnlyOneFixedStep() {
		EdgeRecordingWorld world = new EdgeRecordingWorld();
		RealtimeRaidSystem system = new RealtimeRaidSystem(
				world,
				RaidSession.create(renderFps, "edge-" + renderFps));

		float renderDelta = 1f / renderFps;
		for (int frame = 0; frame < renderFps; frame++) {
			system.update(renderDelta);
		}

		assertEquals(1, world.fireEdges);
		assertEquals(1, world.reloadEdges);
		assertEquals(1, world.interactEdges);
		assertEquals(1, world.medicalEdges);
		assertEquals(1, world.medicalSlotEdges);
		assertEquals(1, world.dropEdges);
		assertEquals(1, world.backpackEdges);
		assertTrue(world.fixedSteps >= 119);
		assertEquals(world.fixedSteps, world.heldSteps);
	}

	private static final class EdgeRecordingWorld
			implements RealtimeRaidSystem.World {

		private final InputFrame rendered = new InputFrame();
		private final InputFrame fixed = new InputFrame();
		private final InputEdgeLatch edges = new InputEdgeLatch();
		private int renderFrames;
		private int fixedSteps;
		private int heldSteps;
		private int fireEdges;
		private int reloadEdges;
		private int interactEdges;
		private int medicalEdges;
		private int medicalSlotEdges;
		private int dropEdges;
		private int backpackEdges;

		@Override
		public boolean paused() {
			return false;
		}

		@Override
		public void sampleInput() {
			rendered.clearEdges();
			rendered.fireHeld = true;
			rendered.interactHeld = true;
			if (renderFrames++ == 0) {
				rendered.firePressed = true;
				rendered.reloadPressed = true;
				rendered.interactPressed = true;
				rendered.medicalPressed = true;
				rendered.medicalSlot = 2;
				rendered.dropPressed = true;
				rendered.backpackPressed = true;
			}
			edges.capture(rendered);
		}

		@Override
		public void beginFixedStep() {
			fixedSteps++;
		}

		@Override
		public void pollInput() {
			fixed.fireHeld = rendered.fireHeld;
			fixed.interactHeld = rendered.interactHeld;
			edges.drainTo(fixed);
			if (fixed.fireHeld && fixed.interactHeld) {
				heldSteps++;
			}
			if (fixed.firePressed) fireEdges++;
			if (fixed.reloadPressed) reloadEdges++;
			if (fixed.interactPressed) interactEdges++;
			if (fixed.medicalPressed) medicalEdges++;
			if (fixed.medicalSlot > 0) medicalSlotEdges++;
			if (fixed.dropPressed) dropEdges++;
			if (fixed.backpackPressed) backpackEdges++;
		}

		@Override public void updatePlayer(float dt) {}
		@Override public void emitPlayerActions(float dt) {}
		@Override public void updateSoundField(float dt) {}
		@Override public void updatePerception(float dt) {}
		@Override public void updateBrains(float dt) {}
		@Override public void updateMobs(float dt) {}
		@Override public void updateProjectiles(float dt) {}
		@Override public void resolveDamageAndDeaths(float dt) {}
		@Override public void updateStatuses(float dt) {}
		@Override public void updateLootAndExtraction(float dt) {}
		@Override public ExtractionState.Interaction extractionInteraction() {
			return ExtractionState.Interaction.NONE;
		}
		@Override public void updateCameraAndHud(float dt) {}
		@Override public void endFixedStep() {}
		@Override public void renderInterpolate(float alpha) {}
		@Override public void disposeRealtimeObjects() {}
	}
}
