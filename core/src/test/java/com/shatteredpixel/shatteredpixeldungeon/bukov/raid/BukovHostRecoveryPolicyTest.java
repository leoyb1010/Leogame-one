package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHostRecoveryPolicyTest {

	@After
	public void clearHandoff() {
		BukovDeploymentHandoff.clear();
	}

	@Test
	public void neitherDocumentCreatesNewHost() {
		assertEquals(
				BukovHostRecoveryPolicy.Action.CREATE_NEW_HOST,
				BukovHostRecoveryPolicy.decide(false, false));
	}

	@Test
	public void checkpointAndHostResumeTogether() {
		assertEquals(
				BukovHostRecoveryPolicy.Action.RESUME_MATCHED_HOST,
				BukovHostRecoveryPolicy.decide(true, true));
	}

	@Test
	public void checkpointWithoutHostSettlesInterruptedRaid() {
		assertEquals(
				BukovHostRecoveryPolicy.Action
						.SETTLE_INTERRUPTED_CHECKPOINT,
				BukovHostRecoveryPolicy.decide(true, false));
	}

	@Test
	public void hostWithoutCheckpointMustBeArchived() {
		assertEquals(
				BukovHostRecoveryPolicy.Action.ARCHIVE_ORPHAN_HOST,
				BukovHostRecoveryPolicy.decide(false, true));
	}

	@Test
	public void freshHostHandoffIsSeedBoundAndOneShot() {
		BukovDeploymentHandoff.authorizeFreshHost(77L);
		assertFalse(BukovDeploymentHandoff.consumeFreshHost(78L));
		assertFalse(BukovDeploymentHandoff.consumeFreshHost(77L));

		BukovDeploymentHandoff.authorizeFreshHost(77L);
		assertTrue(BukovDeploymentHandoff.consumeFreshHost(77L));
		assertFalse(BukovDeploymentHandoff.consumeFreshHost(77L));
	}
}
