package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RealtimeBodyTest {

	@Test
	public void interpolatesAndRoundTripsThroughBundle() {
		RealtimeBody body = new RealtimeBody(22, 10, 0.3f);
		body.beginStep();
		body.x += 1f;
		body.y += 2f;
		body.velocityX = 3f;
		body.active = false;

		PointF midpoint = body.interpolated(0.5f);
		assertEquals(3f, midpoint.x, 0.0001f);
		assertEquals(3.5f, midpoint.y, 0.0001f);

		Bundle bundle = new Bundle();
		body.storeInBundle(bundle);
		RealtimeBody restored = new RealtimeBody();
		restored.restoreFromBundle(bundle);

		assertEquals(body.x, restored.x, 0f);
		assertEquals(body.y, restored.y, 0f);
		assertEquals(3f, restored.velocityX, 0f);
		assertEquals(0.3f, restored.radius, 0f);
		assertFalse(restored.active);
	}
}
