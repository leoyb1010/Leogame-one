package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RealtimeBodySpatialIndexTest {

	@Test
	public void queryReturnsOnlyIntersectingBodiesInRosterOrder() {
		RealtimeBody first = body(9.8f, 2f, 0.3f);
		RealtimeBody outside = body(14f, 2f, 0.3f);
		RealtimeBody second = body(4.2f, 2f, 0.3f);
		RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(24, 16, 4f);
		index.rebuild(Arrays.asList(first, outside, second));

		assertEquals(
				Arrays.asList(first, second),
				copy(index.candidates(4f, 1.5f, 10f, 2.5f)));
	}

	@Test
	public void movementDeathAndClearKeepMembershipCurrent() {
		RealtimeBody body = body(2f, 2f, 0.3f);
		RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(24, 16, 4f);
		index.rebuild(Collections.singletonList(body));
		assertEquals(1, index.size());
		assertTrue(index.candidates(1f, 1f, 3f, 3f)
				.iterator().hasNext());

		body.x = 18f;
		index.update(body);
		assertFalse(index.candidates(1f, 1f, 3f, 3f)
				.iterator().hasNext());
		assertSame(body, index.candidates(17f, 1f, 19f, 3f)
				.iterator().next());

		body.active = false;
		index.update(body);
		assertEquals(0, index.size());
		assertFalse(index.candidates(17f, 1f, 19f, 3f)
				.iterator().hasNext());

		body.active = true;
		index.rebuild(Collections.singletonList(body));
		index.clear();
		assertEquals(0, index.size());
		assertFalse(index.candidates(17f, 1f, 19f, 3f)
				.iterator().hasNext());
	}

	@Test
	public void rebuildReusesEntriesAndDropsMissingBodies() {
		RealtimeBody kept = body(2f, 2f, 0.3f);
		RealtimeBody removed = body(6f, 2f, 0.3f);
		RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(24, 16, 4f);
		index.rebuild(Arrays.asList(kept, removed));
		index.rebuild(Collections.singletonList(kept));

		assertEquals(1, index.size());
		assertEquals(
				Collections.singletonList(kept),
				copy(index.candidates(0f, 0f, 24f, 16f)));
	}

	@Test
	public void candidateViewIsReusedAcrossShots() {
		RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(24, 16, 4f);
		index.rebuild(Collections.singletonList(body(2f, 2f, 0.3f)));

		Iterable<RealtimeBody> first =
				index.candidates(0f, 0f, 4f, 4f);
		Iterable<RealtimeBody> second =
				index.candidates(0f, 0f, 4f, 4f);

		assertSame(first, second);
	}

	@Test
	public void indexedCastMatchesExhaustiveCastAndTieOrder() {
		RealtimeBody tieWinner = body(6f, 4f, 0.4f);
		RealtimeBody tieLoser = body(6f, 4f, 0.4f);
		RealtimeBody north = body(12f, 10f, 0.45f);
		RealtimeBody south = body(12f, 2f, 0.45f);
		List<RealtimeBody> roster =
				Arrays.asList(tieWinner, tieLoser, north, south);
		RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(24, 16, 4f);
		index.rebuild(roster);

		float[][] rays = {
				{2f, 4f, 1f, 0f},
				{2f, 2f, 1f, 0f},
				{2f, 4f, 1f, 0.75f},
				{18f, 10f, -1f, 0f}
		};
		for (float[] ray : rays) {
			HitscanResolver.Hit exhaustive = new HitscanResolver.Hit();
			HitscanResolver.Hit indexed = new HitscanResolver.Hit();
			HitscanResolver.cast(
					ray[0], ray[1], ray[2], ray[3], 20f,
					openMap(),
					(minX, minY, maxX, maxY) -> roster,
					null,
					exhaustive);
			HitscanResolver.cast(
					ray[0], ray[1], ray[2], ray[3], 20f,
					openMap(),
					index::candidates,
					null,
					indexed);

			assertSame(exhaustive.body, indexed.body);
			assertEquals(exhaustive.distance, indexed.distance, 0.0001f);
			assertEquals(exhaustive.x, indexed.x, 0.0001f);
			assertEquals(exhaustive.y, indexed.y, 0.0001f);
		}
		assertSame(tieWinner, cast(index, 2f, 4f, 1f, 0f).body);
	}

	private static HitscanResolver.Hit cast(
			RealtimeBodySpatialIndex index,
			float originX,
			float originY,
			float directionX,
			float directionY) {
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		HitscanResolver.cast(
				originX, originY, directionX, directionY, 20f,
				openMap(),
				index::candidates,
				null,
				hit);
		return hit;
	}

	private static ArrayList<RealtimeBody> copy(
			Iterable<RealtimeBody> bodies) {
		ArrayList<RealtimeBody> copy = new ArrayList<>();
		for (RealtimeBody body : bodies) {
			copy.add(body);
		}
		return copy;
	}

	private static RealtimeBody body(float x, float y, float radius) {
		RealtimeBody body = new RealtimeBody();
		body.x = x;
		body.y = y;
		body.previousX = x;
		body.previousY = y;
		body.radius = radius;
		return body;
	}

	private static CollisionMap openMap() {
		return new CollisionMap() {
			@Override
			public int width() {
				return 24;
			}

			@Override
			public int height() {
				return 16;
			}

			@Override
			public boolean blocked(int x, int y) {
				return x < 0 || y < 0 || x >= width() || y >= height();
			}
		};
	}
}
