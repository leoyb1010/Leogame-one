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
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovHub;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovSettings;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.SmartTexture;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.RectF;

/**
 * Bukov product title: industrial port artwork, clear offline hierarchy, and
 * no fantasy campaign surfaces on the player-visible path.
 */
public class TitleScene extends PixelScene {

	private BukovUiTokens tokens;
	private RenderedTextBlock eyebrow;
	private RenderedTextBlock title;
	private RenderedTextBlock englishTitle;
	private RenderedTextBlock status;
	private ColorBlock identityPanel;
	private ColorBlock menuPanel;
	private TacticalTitleButton btnContinue;
	private TacticalTitleButton btnBukov;
	private TacticalTitleButton btnSettings;
	private TacticalTitleButton btnAbout;
	private BitmapText version;
	private ExitButton btnExit;

	@Override
	public void create() {
		super.create();
		Music.INSTANCE.end();
		uiCamera.visible = false;
		tokens = BukovUiTokens.loadDefault();

		final int screenWidth = Camera.main.width;
		final int screenHeight = Camera.main.height;
		final RectF insets = getCommonInsets();
		final boolean wide = landscape();

		Image background = new Image(wide
				? Assets.Splashes.Bukov.TITLE_INDUSTRIAL_LANDSCAPE_V2
				: Assets.Splashes.Bukov.TITLE_INDUSTRIAL_PORTRAIT_V2);
		background.texture.filter(SmartTexture.LINEAR, SmartTexture.LINEAR);
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
				0xFF02090C);
		atmosphere.alpha(wide ? 0.18f : 0.28f);
		add(atmosphere);

		float usableLeft = insets.left;
		float usableTop = insets.top;
		float usableWidth =
				screenWidth - insets.left - insets.right;
		float usableHeight =
				screenHeight - insets.top - insets.bottom;

		float panelWidth = Math.min(
				wide ? 108f : 116f,
				usableWidth - 12f);
		final boolean activeRaid = hasActiveRaid();
		float identityHeight = 45f;
		float menuHeight = activeRaid ? 76f : 54f;
		float identityLeft = wide
				? usableLeft + 9f
				: usableLeft + (usableWidth - panelWidth) / 2f;
		float menuLeft = wide
				? usableLeft + usableWidth - panelWidth - 9f
				: identityLeft;
		float identityTop = wide
				? usableTop + 9f
				: usableTop + 15f;
		float menuTop = wide
				? usableTop + usableHeight - menuHeight - 10f
				: identityTop + identityHeight + 10f;

		identityPanel = panel(
				identityLeft,
				identityTop,
				panelWidth,
				identityHeight,
				tokens.colorWithAlpha("ink.background", 218),
				tokens.color("accent.interact"));

		eyebrow = label(
				"OFFLINE EXTRACTION / 单机搜打撤",
				wide ? 6 : 5,
				tokens.color("text.secondary"));
		eyebrow.setPos(identityLeft + 7f, identityTop + 5f);
		add(eyebrow);

		title = label(
				"逃离布科夫",
				wide ? 16 : 14,
				tokens.color("accent.valuable"));
		title.setPos(identityLeft + 7f, eyebrow.bottom() + 3f);
		add(title);

		englishTitle = label(
				"ESCAPE FROM BUKOV",
				wide ? 7 : 6,
				tokens.color("text.primary"));
		englishTitle.setPos(
				identityLeft + 7f,
				identityTop + identityHeight
						- englishTitle.height() - 4f);
		add(englishTitle);

		menuPanel = panel(
				menuLeft,
				menuTop,
				panelWidth,
				menuHeight,
				tokens.colorWithAlpha("ink.background", 238),
				tokens.color("panel.border"));

		status = label(
				activeRaid
						? "ACTIVE RAID  /  CHECKPOINT READY"
						: "HIDEOUT  /  LOADOUT READY",
				5,
				activeRaid
						? tokens.color("accent.extract")
						: tokens.color("text.secondary"));
		status.setPos(menuLeft + 7f, menuTop + 5f);
		add(status);

		float buttonLeft = menuLeft + 6f;
		float buttonWidth = panelWidth - 12f;
		float buttonTop = status.bottom() + 4f;
		if (activeRaid) {
			btnContinue = new TacticalTitleButton(
					"继续行动  /  CONTINUE",
					tokens.color("accent.extract"),
					SPDAction.TAG_ATTACK) {
				@Override
				protected void activate() {
					deployRaid();
				}
			};
			btnContinue.setRect(
					buttonLeft, buttonTop, buttonWidth, 18f);
			add(btnContinue);
			buttonTop = btnContinue.bottom() + 3f;
		}

		btnBukov = new TacticalTitleButton(
				activeRaid
						? "进入基地  /  HIDEOUT"
						: "进入基地  /  START",
				tokens.color("accent.interact"),
				activeRaid ? SPDAction.TAG_LOOT : SPDAction.TAG_ATTACK) {
			@Override
			protected void activate() {
				openBukovMode();
			}
		};
		btnBukov.setRect(buttonLeft, buttonTop, buttonWidth, 18f);
		add(btnBukov);

		float secondaryTop = btnBukov.bottom() + 3f;
		float secondaryWidth = (buttonWidth - 3f) / 2f;
		btnSettings = new TacticalTitleButton(
				"设置",
				tokens.color("panel.border"),
				SPDAction.TAG_RESUME) {
			@Override
			protected void activate() {
				ShatteredPixelDungeon.scene().addToFront(
						new WndBukovSettings());
			}
		};
		btnSettings.setRect(
				buttonLeft, secondaryTop, secondaryWidth, 16f);
		add(btnSettings);

		btnAbout = new TacticalTitleButton(
				"关于",
				tokens.color("panel.border"),
				SPDAction.JOURNAL) {
			@Override
			protected void activate() {
				ShatteredPixelDungeon.switchScene(AboutScene.class);
			}
		};
		btnAbout.setRect(
				btnSettings.right() + 3f,
				secondaryTop,
				secondaryWidth,
				16f);
		add(btnAbout);

		version = new BitmapText("v" + Game.version, pixelFont);
		version.measure();
		version.hardlight(tokens.color("text.disabled"));
		version.x = usableLeft + usableWidth - version.width() - 4f;
		version.y = usableTop + usableHeight - version.height() - 3f;
		add(version);

		if (DeviceCompat.isDesktop()) {
			btnExit = new ExitButton();
			btnExit.setPos(
					screenWidth - insets.right - btnExit.width(),
					insets.top);
			add(btnExit);
		}

		fadeIn();
		if (BukovMode.consumeLaunchHubRequest()
				|| BukovMode.consumeHubRequest()) {
			openBukovMode();
		}
	}

	private ColorBlock panel(
			float x,
			float y,
			float width,
			float height,
			int fill,
			int edgeColor) {
		ColorBlock result = new ColorBlock(width, height, fill);
		result.x = x;
		result.y = y;
		add(result);
		ColorBlock edge = new ColorBlock(2f, height, edgeColor);
		edge.x = x;
		edge.y = y;
		add(edge);
		ColorBlock topRule = new ColorBlock(width, 1f, edgeColor);
		topRule.x = x;
		topRule.y = y;
		topRule.alpha(0.75f);
		add(topRule);
		return result;
	}

	private RenderedTextBlock label(String value, int size, int color) {
		RenderedTextBlock block = renderTextBlock(value, size);
		block.hardlight(color);
		return block;
	}

	private void deployRaid() {
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		ShatteredPixelDungeon.switchScene(BukovDeploymentScene.class);
	}

	private static boolean hasActiveRaid() {
		if (!GamesInProgress.gameExists(BukovMode.SAVE_SLOT)) {
			return false;
		}
		try {
			return BukovSaveServices.platformDefault()
					.loadRaidCheckpoint() != null;
		} catch (Exception error) {
			ShatteredPixelDungeon.reportException(error);
			return false;
		}
	}

	private void openBukovMode() {
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		try {
			BukovHubController hub =
					new BukovHubController(
							BukovSaveServices.platformDefault());
			addToFront(new WndBukovHub(hub, new Callback() {
				@Override
				public void call() {
					deployRaid();
				}
			}));
		} catch (Exception error) {
			ShatteredPixelDungeon.reportException(error);
			addToFront(new WndMessage(
					"基地读取失败：\n" + error.getMessage()));
		}
	}

	/**
	 * Retained only for host-save tooling and upstream compatibility. No
	 * player-visible title control calls this method.
	 */
	@SuppressWarnings("unused")
	private void openClassicMode() {
		if (GamesInProgress.checkAll().isEmpty()) {
			GamesInProgress.selectedClass = null;
			GamesInProgress.curSlot = 1;
			ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
		} else {
			ShatteredPixelDungeon.switchNoFade(StartScene.class);
		}
	}

	private abstract class TacticalTitleButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock pressed;
		private final ColorBlock edge;
		private final ColorBlock lowerRule;
		private final RenderedTextBlock text;
		private final GameAction keyAction;

		private TacticalTitleButton(
				String value,
				int accent,
				GameAction keyAction) {
			this.keyAction = keyAction;
			surface = new ColorBlock(
					1f, 1f, accent);
			surface.alpha(0.18f);
			addToBack(surface);
			pressed = new ColorBlock(
					1f, 1f, tokens.color("panel.border"));
			pressed.visible = false;
			addToBack(pressed);
			edge = new ColorBlock(1f, 1f, accent);
			add(edge);
			lowerRule = new ColorBlock(1f, 1f, accent);
			lowerRule.alpha(0.65f);
			add(lowerRule);
			text = label(value, 7, tokens.color("text.primary"));
			text.align(RenderedTextBlock.CENTER_ALIGN);
			add(text);
		}

		protected abstract void activate();

		@Override
		protected void onClick() {
			activate();
		}

		@Override
		protected void onPointerDown() {
			surface.visible = false;
			pressed.visible = true;
			Sample.INSTANCE.play(Assets.Sounds.CLICK);
		}

		@Override
		protected void onPointerUp() {
			surface.visible = true;
			pressed.visible = false;
		}

		@Override
		public GameAction keyAction() {
			return keyAction;
		}

		@Override
		protected void layout() {
			super.layout();
			fit(surface);
			fit(pressed);
			edge.x = x;
			edge.y = y;
			edge.size(3f, height);
			lowerRule.x = x;
			lowerRule.y = y + height - 1f;
			lowerRule.size(width, 1f);
			text.maxWidth(Math.max(1, (int)width - 8));
			text.setPos(
					x + (width - text.width()) / 2f,
					y + (height - text.height()) / 2f);
		}

		private void fit(ColorBlock block) {
			block.x = x;
			block.y = y;
			block.size(width, height);
		}
	}
}
