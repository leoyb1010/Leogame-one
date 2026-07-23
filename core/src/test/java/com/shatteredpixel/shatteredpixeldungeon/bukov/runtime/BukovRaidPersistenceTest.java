package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovRaidPersistenceTest {

	@Test
	public void criticalMutationCommitsImmediately() {
		final List<String> writes = new ArrayList<>();
		BukovRaidPersistence persistence =
				new BukovRaidPersistence(() -> writes.add("commit"));

		assertTrue(persistence.criticalStateChanged());
		assertEquals(Arrays.asList("commit"), writes);
		assertFalse(persistence.dirty());
	}

	@Test
	public void failedSecondSurfaceRemainsDirtyUntilWholeCommitRetries() {
		final List<String> writes = new ArrayList<>();
		BukovRaidPersistence persistence =
				new BukovRaidPersistence(new BukovRaidPersistence.Commit() {
					private int attempts;

					@Override
					public void persist() throws IOException {
						writes.add("host-" + attempts);
						if (attempts++ == 0) {
							throw new IOException("checkpoint unavailable");
						}
						writes.add("checkpoint");
					}
				});

		assertFalse(persistence.criticalStateChanged());
		assertTrue(persistence.dirty());
		assertNotNull(persistence.lastFailure());
		assertFalse(persistence.update(0.5f));
		assertTrue(persistence.dirty());
		assertTrue(persistence.update(0.5f));
		assertEquals(
				Arrays.asList("host-0", "host-1", "checkpoint"),
				writes);
		assertFalse(persistence.dirty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void retryRejectsInvalidDelta() {
		BukovRaidPersistence persistence =
				new BukovRaidPersistence(() -> {
					throw new IOException("offline");
				});
		persistence.criticalStateChanged();
		persistence.update(Float.NaN);
	}
}
