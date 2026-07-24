/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovUiSoundPlayer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovUiSoundRouter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchIcon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiAssets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovVisualContract;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHardNotification;
import com.watabou.gltextures.SmartTexture;
import com.watabou.input.ControllerHandler;
import com.watabou.input.GameAction;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.FileUtils;
import com.watabou.utils.RectF;

/**
 * One-shot Bukov briefing for a new local profile.
 *
 * <p>Version bookkeeping is intentionally silent. Returning players go
 * straight to the product title instead of seeing the host game's update,
 * class, ranking, or developer-promotion surfaces.</p>
 */
public class WelcomeScene extends PixelScene {

	private static boolean triedCleaningTemp = false;

	private BukovUiTokens tokens;

	@Override
	public void create() {
		super.create();
		Music.INSTANCE.end();

		final int previousVersion = SPDSettings.version();

		if (!triedCleaningTemp && FileUtils.cleanTempFiles()) {
			add(new WndHardNotification(
					Icons.get(Icons.WARNING),
					entryMessage("welcome.error_title"),
					entryMessage("welcome.save_warning"),
					entryMessage("welcome.continue"),
					5) {
				@Override
				public void hide() {
					super.hide();
					triedCleaningTemp = true;
					ShatteredPixelDungeon.resetScene();
				}
			});
			return;
		}

		if (previousVersion != 0 && !SPDSettings.intro()) {
			SPDSettings.version(ShatteredPixelDungeon.versionCode);
			ShatteredPixelDungeon.switchNoFade(TitleScene.class);
			return;
		}

		uiCamera.visible = false;
		tokens = BukovUiTokens.loadDefault();

		final int screenWidth = Camera.main.width;
		final int screenHeight = Camera.main.height;
		final boolean wide = landscape();
		final RectF insets = getCommonInsets();
		final float safeWidth =
				screenWidth - insets.left - insets.right;
		final float safeHeight =
				screenHeight - insets.top - insets.bottom;

		addBukovBackdrop(screenWidth, screenHeight, wide);

		float panelWidth = Math.min(
				BukovVisualContract.panelWidth(safeWidth, wide),
				safeWidth - BukovVisualContract.OUTER_MARGIN * 2f);
		float panelLeft =
				insets.left + (safeWidth - panelWidth) / 2f;
		float contentWidth = panelWidth - 14f;

		RenderedTextBlock eyebrow = label(
				entryMessage("welcome.eyebrow"),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		RenderedTextBlock title = label(
				entryMessage("welcome.title"),
				BukovVisualContract.FONT_TITLE,
				tokens.color("accent.valuable"));
		RenderedTextBlock englishTitle = label(
				entryMessage("welcome.english_title"),
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		RenderedTextBlock briefing = label(
				entryMessage("welcome.briefing"),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("accent.extract"));
		RenderedTextBlock message = renderTextBlock(
				entryMessage("welcome.intro"),
				tokens.typographyPx(
						BukovVisualContract.FONT_BODY));
		message.maxWidth(Math.max(1, (int)contentWidth));
		message.hardlight(tokens.color("text.secondary"));

		float buttonHeight = BukovVisualContract.controlHeight(
				!com.watabou.utils.DeviceCompat.isDesktop());
		float panelHeight =
				7f + eyebrow.height()
				+ 3f + title.height()
				+ 2f + englishTitle.height()
				+ 7f + briefing.height()
				+ 4f + message.height()
				+ 8f + buttonHeight + 7f;
		float panelTop = insets.top + Math.max(
				BukovVisualContract.OUTER_MARGIN,
				(safeHeight - panelHeight) / 2f);
		if (panelTop + panelHeight
				> screenHeight - insets.bottom
						- BukovVisualContract.OUTER_MARGIN) {
			panelTop = insets.top + BukovVisualContract.OUTER_MARGIN;
		}

		NinePatch panel = BukovUiAssets.surface(
				BukovUiAssets.Surface.PANEL,
				tokens.colorWithAlpha("ink.background", 238));
		panel.x = panelLeft;
		panel.y = panelTop;
		panel.size(panelWidth, panelHeight);
		add(panel);

		ColorBlock edge = new ColorBlock(
				2f,
				panelHeight,
				tokens.color("accent.interact"));
		edge.x = panelLeft;
		edge.y = panelTop;
		add(edge);

		ColorBlock topRule = new ColorBlock(
				panelWidth,
				1f,
				tokens.color("panel.border"));
		topRule.x = panelLeft;
		topRule.y = panelTop;
		add(topRule);

		float contentLeft = panelLeft + 7f;
		float cursor = panelTop + 7f;
		eyebrow.setPos(contentLeft, cursor);
		add(eyebrow);
		cursor = eyebrow.bottom() + 3f;
		title.setPos(contentLeft, cursor);
		add(title);
		cursor = title.bottom() + 2f;
		englishTitle.setPos(contentLeft, cursor);
		add(englishTitle);
		cursor = englishTitle.bottom() + 7f;
		briefing.setPos(contentLeft, cursor);
		add(briefing);
		cursor = briefing.bottom() + 4f;
		message.setPos(contentLeft, cursor);
		add(message);
		cursor = message.bottom() + 8f;

		WelcomeActionButton enter = new WelcomeActionButton(
				entryMessage("welcome.enter"),
				BukovTouchIcon.Glyph.DEPLOY,
				previousVersion == 0);
		enter.setRect(
				contentLeft,
				cursor,
				contentWidth,
				buttonHeight);
		add(enter);

		if (SPDSettings.intro()
				&& ControllerHandler.isControllerConnected()) {
			addToFront(new WndHardNotification(
					Icons.CONTROLLER.get(),
					entryMessage("welcome.controller_title"),
					entryMessage("welcome.controller_body"),
					entryMessage("welcome.controller_okay"),
					0) {
				@Override
				public void onBackPressed() {
					// The first controller hint must be acknowledged.
				}
			});
		}
	}

	private void addBukovBackdrop(
			int screenWidth, int screenHeight, boolean wide) {
		Image background = new Image(wide
				? Assets.Splashes.Bukov.TITLE_INDUSTRIAL_LANDSCAPE_V2
				: Assets.Splashes.Bukov.TITLE_INDUSTRIAL_PORTRAIT_V2);
		background.texture.filter(
				SmartTexture.LINEAR,
				SmartTexture.LINEAR);
		float cover = Math.max(
				screenWidth / background.width(),
				screenHeight / background.height());
		background.scale.set(cover);
		background.x = (screenWidth - background.width()) / 2f;
		background.y = (screenHeight - background.height()) / 2f;
		add(background);

		ColorBlock atmosphere = new ColorBlock(
				screenWidth,
				screenHeight,
				tokens.colorWithAlpha("ink.shadow", 255));
		atmosphere.alpha(wide ? 0.24f : 0.34f);
		add(atmosphere);
	}

	private RenderedTextBlock label(
			String value, String typography, int color) {
		RenderedTextBlock block = renderTextBlock(
				value, tokens.typographyPx(typography));
		block.hardlight(color);
		return block;
	}

	private static String entryMessage(String key, Object... args) {
		return BukovMessages.get("bukov.entry." + key, args);
	}

	private void enterBukov(boolean brandNewProfile) {
		if (brandNewProfile) {
			SPDSettings.scheduleBukovFirstRunCalibration();
		}
		SPDSettings.version(ShatteredPixelDungeon.versionCode);
		SPDSettings.intro(false);
		ShatteredPixelDungeon.switchScene(TitleScene.class);
	}

	private final class WelcomeActionButton extends Button {

		private final NinePatch surface;
		private final NinePatch pressed;
		private final ColorBlock edge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock text;
		private final boolean brandNewProfile;

		private WelcomeActionButton(
				String value,
				BukovTouchIcon.Glyph glyph,
				boolean brandNewProfile) {
			this.brandNewProfile = brandNewProfile;
			surface = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON,
					tokens.colorWithAlpha("panel.surface", 238));
			surface.alpha(0.86f);
			addToBack(surface);
			pressed = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_PRESSED,
					tokens.colorWithAlpha("panel.deep", 255));
			pressed.visible = false;
			addToBack(pressed);
			edge = new ColorBlock(
					1f,
					1f,
					tokens.color("accent.interact"));
			add(edge);
			icon = new BukovTouchIcon(
					glyph,
					tokens.color("text.primary"),
					tokens.color("accent.interact"),
					tokens.color("text.disabled"));
			add(icon);
			text = label(
					value,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.primary"));
			text.align(RenderedTextBlock.CENTER_ALIGN);
			add(text);
		}

		@Override
		protected void onClick() {
			BukovUiSoundRouter.play(
					BukovUiSoundPlayer.Cue.CONFIRM);
			enterBukov(brandNewProfile);
		}

		@Override
		protected void onPointerDown() {
			surface.visible = false;
			pressed.visible = true;
			icon.visualState(true, false);
			layout();
		}

		@Override
		protected void onPointerUp() {
			surface.visible = true;
			pressed.visible = false;
			icon.visualState(false, false);
			layout();
		}

		@Override
		public GameAction keyAction() {
			return SPDAction.TAG_ATTACK;
		}

		@Override
		protected void layout() {
			super.layout();
			fit(surface);
			fit(pressed);
			edge.x = x;
			edge.y = y;
			edge.size(3f, height);
			float iconSize = Math.max(10f, Math.min(15f, height - 7f));
			float textWidth = Math.max(1f, width - iconSize - 15f);
			text.maxWidth(Math.max(1, (int)textWidth));
			float contentWidth = iconSize + 4f + text.width();
			float contentLeft = x + (width - contentWidth) * 0.5f;
			icon.setRect(
					contentLeft,
					y + (height - iconSize) * 0.5f,
					iconSize,
					iconSize);
			text.setPos(
					contentLeft + iconSize + 4f,
					y + (height - text.height()) / 2f
							+ (pressed.visible ? 1f : 0f));
		}

		private void fit(NinePatch block) {
			block.x = x;
			block.y = y;
			block.size(width, height);
		}
	}
}
