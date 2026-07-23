package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
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

	private static final int WIDTH = 148;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 3;
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
	private int y;

	public WndBukovPause(SaveAndReturn saveAndReturn) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		this.saveAndReturn = saveAndReturn;
		tokens = BukovUiTokens.loadDefault();

		RenderedTextBlock title = PixelScene.renderTextBlock("行动暂停", 11);
		title.hardlight(tokens.color("accent.valuable"));
		title.setRect(0, 2, WIDTH, 14);
		title.align(RenderedTextBlock.CENTER_ALIGN);
		add(title);
		y = 20;

		RenderedTextBlock status = PixelScene.renderTextBlock(
				"行动进度会在返回藏身处前写入本地存档。", 7);
		status.hardlight(tokens.color("text.secondary"));
		status.setRect(5, y, WIDTH - 10, 22);
		status.align(RenderedTextBlock.CENTER_ALIGN);
		add(status);
		y += 25;

		addButton(new ActionButton("继续行动", CONTINUE));
		addButton(new ActionButton("设置", SETTINGS));
		addButton(new ActionButton("保存并返回藏身处", SAVE_AND_RETURN));

		resize(WIDTH, y);
	}

	private void addButton(ActionButton button) {
		add(button);
		button.setRect(4, y, WIDTH - 8, BUTTON_HEIGHT);
		buttons[button.action] = button;
		y += BUTTON_HEIGHT + GAP;
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

	private final class ActionButton extends Button {

		private final int action;
		private final ColorBlock background;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock text;

		private ActionButton(String label, int action) {
			this.action = action;
			background = new ColorBlock(
					1, 1, tokens.color("panel.surface"));
			addToBack(background);
			edge = new ColorBlock(1, 1,
					action == SAVE_AND_RETURN
							? tokens.color("accent.danger")
							: tokens.color("accent.extract"));
			add(edge);
			focusEdge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			text = PixelScene.renderTextBlock(label, 8);
			text.hardlight(action == SAVE_AND_RETURN
					? tokens.color("accent.danger")
					: tokens.color("text.primary"));
			text.align(RenderedTextBlock.CENTER_ALIGN);
			add(text);
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

		private void setFocused(boolean focused) {
			focusEdge.visible = focused;
			text.hardlight(action == SAVE_AND_RETURN
					? tokens.color("accent.danger")
					: focused
					? tokens.color("accent.interact")
					: tokens.color("text.primary"));
		}

		@Override
		protected void layout() {
			super.layout();
			background.x = x;
			background.y = y;
			background.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2, height);
			focusEdge.x = x;
			focusEdge.y = y;
			focusEdge.size(width, 1);
			text.setRect(x + 4, y + (height - 10) / 2f, width - 8, 10);
		}
	}
}
