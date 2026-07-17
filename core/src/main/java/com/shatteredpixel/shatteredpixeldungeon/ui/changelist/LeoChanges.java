/*
 * Leo's Dungeon Siege changelog.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.ChangesScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;

/** Product-facing history for Leo's edition; text follows the active language. */
public final class LeoChanges {

	private LeoChanges() {
	}

	public static void addAllChanges(ArrayList<ChangeInfo> changeInfos) {
		ChangeInfo version = new ChangeInfo("v1.0.0", true,
				Messages.get(LeoChanges.class, "summary"));
		version.hardlight(Window.TITLE_COLOR);
		changeInfos.add(version);

		version.addButton(new ChangeButton(Icons.get(Icons.SHPX),
				Messages.get(LeoChanges.class, "identity_title"),
				Messages.get(LeoChanges.class, "identity_body")));
		version.addButton(new ChangeButton(Icons.DISPLAY_LAND.get(),
				Messages.get(LeoChanges.class, "visual_title"),
				Messages.get(LeoChanges.class, "visual_body")));
		version.addButton(new ChangeButton(Icons.get(Icons.PREFS),
				Messages.get(LeoChanges.class, "feel_title"),
				Messages.get(LeoChanges.class, "feel_body")));
		version.addButton(new ChangeButton(Icons.get(Icons.CHANGES),
				Messages.get(ChangesScene.class, "bugfixes"),
				Messages.get(LeoChanges.class, "fixes_body")));
	}
}
