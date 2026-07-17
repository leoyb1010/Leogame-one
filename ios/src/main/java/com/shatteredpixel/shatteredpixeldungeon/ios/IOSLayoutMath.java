/*
 * Leo's Dungeon Siege iOS layout math.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.ios;

import com.watabou.utils.RectF;

/** Pure conversion kept outside UIKit so safe-area behavior is regression-testable. */
public final class IOSLayoutMath {

	private IOSLayoutMath() {
	}

	public static RectF scaledInsets(float left, float top, float right, float bottom, float scale) {
		return new RectF(left * scale, top * scale, right * scale, bottom * scale);
	}
}
