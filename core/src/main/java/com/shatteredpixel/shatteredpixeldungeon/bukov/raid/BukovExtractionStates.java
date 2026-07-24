/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;

import java.util.ArrayList;
import java.util.List;

/** Converts authored map extraction definitions into live raid state. */
public final class BukovExtractionStates {

	public static List<ExtractionState> fromLayout(BukovRaidLayout layout) {
		if (layout == null || layout.extractions.isEmpty()) {
			throw new IllegalArgumentException(
					"generated raid layout with extractions is required");
		}
		List<ExtractionState> result = new ArrayList<>();
		for (ExtractionDefinition definition : layout.extractions) {
			if (definition == null) {
				throw new IllegalArgumentException(
						"extraction definition is required");
			}
			definition.validate();
			ExtractionState.Type type;
			switch (definition.type) {
				case CONDITIONAL:
					type = ExtractionState.Type.CONDITIONAL;
					break;
				case TEMPORARY:
					type = ExtractionState.Type.TEMPORARY;
					break;
				case BASELINE:
				default:
					type = ExtractionState.Type.BASIC;
					break;
			}
			result.add(new ExtractionState(
					definition.id,
					type,
					definition.interactionSeconds,
					definition.availableFromSeconds,
					definition.availableUntilSeconds));
		}
		return result;
	}

	private BukovExtractionStates() {
	}
}
