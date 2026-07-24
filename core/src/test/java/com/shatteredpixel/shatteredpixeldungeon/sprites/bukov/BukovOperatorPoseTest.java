package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovOperatorPoseTest {

	@Test
	public void movingEastWhileAimingNorthKeepsIndependentFacings() {
		BukovOperatorPose pose = new BukovOperatorPose();

		pose.update(1f, 0f, 0f, -1f);

		assertEquals(BukovFacing8.E, pose.locomotionFacing());
		assertEquals(BukovFacing8.N, pose.upperBodyFacing());
		assertTrue(pose.aimActive());
	}

	@Test
	public void locomotionDrivesBothLayersWhenAimIsReleased() {
		BukovOperatorPose pose = new BukovOperatorPose();
		pose.update(-1f, 0f, 0f, -1f);

		pose.update(1f, 1f, 0f, 0f);

		assertEquals(BukovFacing8.SE, pose.locomotionFacing());
		assertEquals(BukovFacing8.SE, pose.upperBodyFacing());
		assertFalse(pose.aimActive());
	}

	@Test
	public void stationaryFramesKeepLastStableDirections() {
		BukovOperatorPose pose = new BukovOperatorPose();
		pose.update(-1f, 1f, 1f, -1f);

		pose.update(0f, 0f, 0f, 0f);

		assertEquals(BukovFacing8.SW, pose.locomotionFacing());
		assertEquals(BukovFacing8.NE, pose.upperBodyFacing());
		assertFalse(pose.aimActive());
	}

	@Test
	public void explicitTargetOnlyTurnsUpperBody() {
		BukovOperatorPose pose = new BukovOperatorPose();
		pose.update(0f, -1f, 0f, 0f);

		pose.faceUpperBody(-1f, 0f);

		assertEquals(BukovFacing8.N, pose.locomotionFacing());
		assertEquals(BukovFacing8.W, pose.upperBodyFacing());
	}
}
