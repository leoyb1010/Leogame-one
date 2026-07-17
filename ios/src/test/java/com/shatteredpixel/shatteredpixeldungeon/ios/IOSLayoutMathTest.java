package com.shatteredpixel.shatteredpixeldungeon.ios;

import com.watabou.utils.RectF;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IOSLayoutMathTest {

	@Test
	public void preservesEveryUIKitSafeAreaEdgeAtBackBufferScale() {
		RectF insets = IOSLayoutMath.scaledInsets(6f, 59f, 7f, 34f, 3f);
		assertEquals(18f, insets.left, 0.0001f);
		assertEquals(177f, insets.top, 0.0001f);
		assertEquals(21f, insets.right, 0.0001f);
		assertEquals(102f, insets.bottom, 0.0001f);
	}
}
