/*
 * Leo's Dungeon Siege personal-edition welcome ritual.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.LeoIdentityConfig;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.LeoStyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;

/** One-time, offline-first identity grant shown on the first title-screen visit. */
public class WndLeoWelcome extends Window {

	public WndLeoWelcome() {
		super(0, 0, new NinePatch(Assets.Interfaces.LEO_DIALOG_FRAME, 24));

		int width = PixelScene.landscape() ? 176 : 132;
		float y = 0;

		Image emblem = new Image(Assets.Interfaces.LEO_TITLE_EMBLEM);
		float emblemWidth = PixelScene.landscape() ? 82 : 72;
		emblem.scale.set(emblemWidth / emblem.width());
		emblem.x = (width - emblem.width()) / 2f;
		emblem.y = y;
		add(emblem);
		y = emblem.y + emblem.height() - 5;

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 11);
		title.hardlight(LeoIdentityConfig.ANTIQUE_GOLD);
		title.setPos((width - title.width()) / 2f, y);
		add(title);
		y = title.bottom() + 5;

		RenderedTextBlock message = PixelScene.renderTextBlock(Messages.get(this, "body"), 6);
		message.maxWidth(width - 8);
		message.setPos(4, y);
		add(message);
		y = message.bottom() + 5;

		RenderedTextBlock privacy = PixelScene.renderTextBlock(Messages.get(this, "privacy"), 6);
		privacy.hardlight(LeoIdentityConfig.EMERALD);
		privacy.maxWidth(width - 8);
		privacy.setPos(4, y);
		add(privacy);
		y = privacy.bottom() + 6;

		LeoStyledButton accept = new LeoStyledButton(Messages.get(this, "accept"), 8) {
			@Override
			protected void onClick() {
				SPDSettings.leoIdentityGranted(true);
				hide();
			}
		};
		accept.textColor(LeoIdentityConfig.ANTIQUE_GOLD);
		accept.setRect(4, y, width - 8, 22);
		add(accept);

		resize(width, (int) accept.bottom());
	}

	@Override
	public void onBackPressed() {
		// The first-run privacy promise must be acknowledged explicitly.
	}
}
