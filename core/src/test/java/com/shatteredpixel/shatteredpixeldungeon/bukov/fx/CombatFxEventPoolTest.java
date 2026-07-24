package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CombatFxEventPoolTest {

	@Test
	public void drainsCompleteGunshotPacketInEmissionOrder() {
		CombatFxEventPool pool = new CombatFxEventPool(5);
		List<CombatFxEvent.Type> types = new ArrayList<>();
		List<Integer> sequences = new ArrayList<>();

		pool.muzzle(7, 10, false, 1f, 2f, 1f, 0f, 1f);
		pool.shell(7, 10, false, 1f, 2f, 0f, -1f, 0.9f);
		pool.tracer(7, 10, false, 1f, 2f, 5f, 2f, 0.8f);
		pool.impact(7, 10, false, 5f, 2f, 0.6f);

		assertEquals(4, pool.drain(event -> {
			types.add(event.type());
			sequences.add(event.sequence());
			assertEquals(7, event.sourceId());
			assertFalse(event.hostile());
		}));
		assertEquals(CombatFxEvent.Type.MUZZLE_FLASH, types.get(0));
		assertEquals(CombatFxEvent.Type.SHELL, types.get(1));
		assertEquals(CombatFxEvent.Type.TRACER, types.get(2));
		assertEquals(CombatFxEvent.Type.IMPACT, types.get(3));
		assertEquals(Integer.valueOf(10), sequences.get(3));
		assertEquals(0, pool.size());
	}

	@Test
	public void drainsAllSevenAuthoredTypesWithoutAllocatingNewEvents() {
		CombatFxEventPool pool = new CombatFxEventPool(7);
		List<CombatFxEvent.Type> types = new ArrayList<>();

		pool.muzzle(3, 9, false, 1f, 2f, 1f, 0f, 1f);
		pool.shell(3, 9, false, 1f, 2f, 0f, 1f, 1f);
		pool.tracer(3, 9, false, 1f, 2f, 4f, 2f, 1f);
		pool.impact(3, 9, false, 4f, 2f, 1f);
		pool.bloodMist(3, 9, false, 4f, 2f, 1f, 0f, 1f);
		pool.bulletMark(3, 9, false, 4f, 2f, 1f, 0f, 1f);
		pool.explosion(3, 9, false, 4f, 2f, 1f);

		assertEquals(7, pool.drain(event -> types.add(event.type())));
		assertEquals(CombatFxEvent.Type.MUZZLE_FLASH, types.get(0));
		assertEquals(CombatFxEvent.Type.SHELL, types.get(1));
		assertEquals(CombatFxEvent.Type.TRACER, types.get(2));
		assertEquals(CombatFxEvent.Type.IMPACT, types.get(3));
		assertEquals(CombatFxEvent.Type.BLOOD_MIST, types.get(4));
		assertEquals(CombatFxEvent.Type.BULLET_MARK, types.get(5));
		assertEquals(CombatFxEvent.Type.EXPLOSION, types.get(6));
	}

	@Test
	public void overflowDropsOldestCosmeticEvent() {
		CombatFxEventPool pool = new CombatFxEventPool(2);
		List<Integer> sequences = new ArrayList<>();

		pool.impact(1, 1, true, 1f, 1f, 1f);
		pool.impact(1, 2, true, 2f, 2f, 1f);
		pool.impact(1, 3, true, 3f, 3f, 1f);
		pool.drain(event -> sequences.add(event.sequence()));

		assertEquals(1L, pool.dropped());
		assertEquals(Integer.valueOf(2), sequences.get(0));
		assertEquals(Integer.valueOf(3), sequences.get(1));
	}

	@Test
	public void slotsAreReusedAfterDrain() {
		CombatFxEventPool pool = new CombatFxEventPool(1);
		CombatFxEvent[] first = new CombatFxEvent[1];
		CombatFxEvent[] second = new CombatFxEvent[1];

		pool.impact(1, 1, false, 1f, 1f, 1f);
		pool.drain(event -> first[0] = event);
		pool.impact(1, 2, true, 2f, 2f, 0.5f);
		pool.drain(event -> second[0] = event);

		assertSame(first[0], second[0]);
		assertTrue(second[0].hostile());
		assertEquals(0.5f, second[0].intensity(), 0f);
	}
}
