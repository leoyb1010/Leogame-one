package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.KeyEvent;
import com.watabou.input.ControllerHandler;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;

/**
 * Raid-only pause surface. It deliberately exposes none of the inherited
 * dungeon journal, hero sheet, backpack, restart or class-selection actions.
 */
public final class WndBukovPause extends Window {

	public interface SaveAndReturn {
		void run();
	}

	private static final int WIDTH_P = 166;
	private static final int WIDTH_L = 190;
	private static final int GAP = 3;
	private static final int MARGIN = 5;
	private static final int CONTINUE = 0;
	private static final int SETTINGS = 1;
	private static final int SAVE_AND_RETURN = 2;

	private final SaveAndReturn saveAndReturn;
	private final BukovUiTokens tokens;
	private final BukovFocusModel focus =
			new BukovFocusModel(3, CONTINUE);
	private final ActionButton[] buttons = new ActionButton[3];
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private final int buttonHeight;
	private int y;

	public WndBukovPause(SaveAndReturn saveAndReturn) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		this.saveAndReturn = saveAndReturn;
		tokens = BukovUiTokens.loadDefault();
		buttonHeight = Math.round(
				BukovVisualContract.controlHeight(
						true, SPDSettings.bukovUiScale()));
		int windowWidth = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);

		ColorBlock header = new ColorBlock(
				windowWidth,
				28,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerEdge = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerEdge.y = 27;
		add(headerEdge);

		RenderedTextBlock eyebrow = PixelScene.renderTextBlock(
				BukovMessages.get("bukov.raid.pause.eyebrow"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		eyebrow.hardlight(tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN, 4);
		add(eyebrow);

		RenderedTextBlock title = PixelScene.renderTextBlock(
				BukovMessages.get("bukov.raid.pause.title"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_BODY));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(MARGIN, 13);
		add(title);
		y = 33;

		ColorBlock statusSurface = new ColorBlock(
				windowWidth - MARGIN * 2,
				23,
				tokens.colorWithAlpha("panel.surface", 180));
		statusSurface.x = MARGIN;
		statusSurface.y = y;
		add(statusSurface);
		RenderedTextBlock status = PixelScene.renderTextBlock(
				BukovMessages.get(
						"bukov.raid.pause.checkpoint_status"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		status.hardlight(tokens.color("text.secondary"));
		status.setRect(MARGIN + 5, y + 4, windowWidth - MARGIN * 2 - 10, 16);
		add(status);
		y += 28;

		addButton(new ActionButton(
				BukovTouchIcon.Glyph.MOVEMENT,
				BukovMessages.get("bukov.raid.pause.resume_label"),
				BukovMessages.get("bukov.raid.pause.resume_code"),
				CONTINUE), windowWidth);
		addButton(new ActionButton(
				BukovTouchIcon.Glyph.PAUSE,
				BukovMessages.get("bukov.raid.pause.settings_label"),
				BukovMessages.get("bukov.raid.pause.settings_code"),
				SETTINGS), windowWidth);
		addButton(new ActionButton(
				BukovTouchIcon.Glyph.BACKPACK,
				BukovMessages.get("bukov.raid.pause.leave_label"),
				BukovMessages.get("bukov.raid.pause.leave_code"),
				SAVE_AND_RETURN),
				windowWidth);

		y += 2;
		resize(windowWidth, y);
	}

	private void addButton(ActionButton button, int windowWidth) {
		add(button);
		button.setRect(MARGIN, y, windowWidth - MARGIN * 2, buttonHeight);
		buttons[button.action] = button;
		y += buttonHeight + GAP;
		updateFocus();
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (!event.pressed) {
			return true;
		}
		if (BukovNavigation.back(event)) {
			hide();
		} else if (BukovNavigation.previous(event)) {
			focus.move(-1);
			updateFocus();
		} else if (BukovNavigation.next(event)) {
			focus.move(1);
			updateFocus();
		} else if (BukovNavigation.confirm(event)) {
			buttons[focus.index()].onClick();
		}
		return true;
	}

	@Override
	public void update() {
		super.update();
		int delta = focusRepeater.update(
				ControllerHandler.leftStickPosition.x,
				ControllerHandler.leftStickPosition.y,
				Game.elapsed);
		if (delta != 0) {
			focus.move(delta);
			updateFocus();
		}
	}

	private void updateFocus() {
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				buttons[i].setFocused(focus.index() == i);
			}
		}
	}

	private final class ActionButton extends BukovIconLabelButton {

		private final int action;
		private final RenderedTextBlock code;

		private ActionButton(
				BukovTouchIcon.Glyph glyph,
				String label,
				String codeLabel,
				int action) {
			super(glyph, label, true);
			this.action = action;
			contentRightInset(54f);
			code = PixelScene.renderTextBlock(
					codeLabel,
					tokens.scaledTypographyPx(
							BukovVisualContract.FONT_CAPTION));
			code.hardlight(tokens.color("text.secondary"));
			code.align(RenderedTextBlock.RIGHT_ALIGN);
			add(code);
		}

		@Override
		protected void onClick() {
			focus.focus(action);
			updateFocus();
			switch (action) {
				case CONTINUE:
					hide();
					break;
				case SETTINGS:
					focus.pushChild();
					GameScene.show(new WndBukovSettings(new Runnable() {
						@Override
						public void run() {
							focus.popChild();
							updateFocus();
						}
					}));
					break;
				default:
					if (WndBukovPause.this.saveAndReturn != null) {
						WndBukovPause.this.saveAndReturn.run();
					}
					break;
			}
		}

		@Override
		public void setFocused(boolean focused) {
			super.setFocused(focused);
			code.hardlight(focused
					? tokens.color("accent.interact")
					: tokens.color("text.secondary"));
		}

		@Override
		protected void layout() {
			super.layout();
			if (code == null) {
				return;
			}
			code.setRect(
					x + width - 53,
					y + (height - 7) / 2f,
					47,
					7);
		}
	}
}
