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
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.LeoIdentityConfig;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovBranding;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.LeoStyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.Callback;

/** One-time, offline-first identity grant shown on the first title-screen visit. */
public class WndLeoWelcome extends Window {

	public WndLeoWelcome() {
		this(null);
	}

	public WndLeoWelcome(final Callback onAccept) {
		super(0, 0, BukovMode.active()
				? Chrome.get(Chrome.Type.WINDOW)
				: new NinePatch(Assets.Interfaces.LEO_DIALOG_FRAME, 24));

		boolean bukov = BukovMode.active();
		int width = PixelScene.landscape() ? 176 : 132;
		float y = 0;

		if (bukov) {
			RenderedTextBlock brand =
					PixelScene.renderTextBlock(LeoIdentityConfig.gameTitle(), 13);
			brand.hardlight(LeoIdentityConfig.ANTIQUE_GOLD);
			brand.setPos((width - brand.width()) / 2f, y + 2);
			add(brand);
			y = brand.bottom() + 3;

			RenderedTextBlock mode =
					PixelScene.renderTextBlock(LeoIdentityConfig.motto(), 6);
			mode.hardlight(LeoIdentityConfig.EMERALD);
			mode.maxWidth(width - 8);
			mode.align(RenderedTextBlock.CENTER_ALIGN);
			mode.setPos(4, y);
			add(mode);
			y = mode.bottom() + 6;
		} else {
			Image emblem = new Image(Assets.Interfaces.LEO_TITLE_EMBLEM);
			float emblemWidth = PixelScene.landscape() ? 82 : 72;
			emblem.scale.set(emblemWidth / emblem.width());
			emblem.x = (width - emblem.width()) / 2f;
			emblem.y = y;
			add(emblem);
			y = emblem.y + emblem.height() - 5;
		}

		RenderedTextBlock title = PixelScene.renderTextBlock(
				Messages.get(this, BukovBranding.messageKey(bukov, "title")), 11);
		title.hardlight(LeoIdentityConfig.ANTIQUE_GOLD);
		title.setPos((width - title.width()) / 2f, y);
		add(title);
		y = title.bottom() + 5;

		RenderedTextBlock message = PixelScene.renderTextBlock(
				Messages.get(this, BukovBranding.messageKey(bukov, "body")), 6);
		message.maxWidth(width - 8);
		message.setPos(4, y);
		add(message);
		y = message.bottom() + 5;

		RenderedTextBlock privacy = PixelScene.renderTextBlock(
				Messages.get(this, BukovBranding.messageKey(bukov, "privacy")), 6);
		privacy.hardlight(LeoIdentityConfig.EMERALD);
		privacy.maxWidth(width - 8);
		privacy.setPos(4, y);
		add(privacy);
		y = privacy.bottom() + 6;

		String acceptText =
				Messages.get(this, BukovBranding.messageKey(bukov, "accept"));
		StyledButton accept;
		if (bukov) {
			accept = new StyledButton(Chrome.Type.GREY_BUTTON_TR, acceptText, 8) {
				@Override
				protected void onClick() {
					accept(onAccept);
				}
			};
		} else {
			accept = new LeoStyledButton(acceptText, 8) {
				@Override
				protected void onClick() {
					accept(onAccept);
				}
			};
		}
		accept.textColor(LeoIdentityConfig.ANTIQUE_GOLD);
		accept.setRect(4, y, width - 8, 22);
		add(accept);

		resize(width, (int) accept.bottom());
	}

	private void accept(Callback onAccept) {
		SPDSettings.leoIdentityGranted(true);
		hide();
		if (onAccept != null) onAccept.call();
	}

	@Override
	public void onBackPressed() {
		// The first-run privacy promise must be acknowledged explicitly.
	}
}
