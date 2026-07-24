package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.ControllerHandler;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;

/**
 * One-shot, optional first-run tuning for the three controls that most affect
 * immediate readability and feel. Every selection is persisted immediately;
 * Back/Escape always dismisses the window.
 */
public final class WndBukovFirstRunCalibration extends Window {

	private static final int WIDTH_P = 172;
	private static final int WIDTH_L = 224;
	private static final int HEIGHT = 138;
	private static final int MARGIN = 5;
	private static final int ROW_HEIGHT = 20;
	private static final int GAP = 3;

	private final BukovUiTokens tokens;
	private final CalibrationButton[] buttons =
			new CalibrationButton[Calibration.values().length];
	private final BukovFocusModel focus =
			new BukovFocusModel(Calibration.values().length, 0);
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();

	public WndBukovFirstRunCalibration() {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		tokens = BukovUiTokens.loadDefault();
		int width = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);
		int height = BukovWindowLayout.safeHeight(HEIGHT);
		resize(width, height);
		build(width, height);
		updateFocus();
	}

	private void build(int width, int height) {
		ColorBlock header = new ColorBlock(
				width,
				29,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				width,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = 28;
		add(headerRule);

		RenderedTextBlock eyebrow = PixelScene.renderTextBlock(
				entryMessage("calibration.eyebrow"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		eyebrow.hardlight(tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN + 1, 3);
		add(eyebrow);

		RenderedTextBlock title = PixelScene.renderTextBlock(
				entryMessage("calibration.title"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_BODY));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(MARGIN + 1, 12);
		add(title);

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				entryMessage("calibration.hint"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		hint.hardlight(tokens.color("text.secondary"));
		hint.setPos(MARGIN + 1, 33);
		add(hint);

		float top = 45;
		for (Calibration calibration : Calibration.values()) {
			CalibrationButton button = new CalibrationButton(calibration);
			buttons[calibration.ordinal()] = button;
			float buttonTop = calibration == Calibration.DONE
					? height - ROW_HEIGHT - MARGIN
					: top + calibration.ordinal() * (ROW_HEIGHT + GAP);
			button.setRect(
					MARGIN,
					buttonTop,
					width - MARGIN * 2,
					ROW_HEIGHT);
			add(button);
		}
	}

	@Override
	public void onBackPressed() {
		hide();
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
			buttons[i].setFocused(focus.index() == i);
		}
	}

	private enum Calibration {
		UI_SCALE,
		VIBRATION,
		AIM_ASSIST,
		DONE
	}

	private final class CalibrationButton extends Button {

		private final Calibration calibration;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusRule;
		private final RenderedTextBlock label;
		private final RenderedTextBlock value;

		private CalibrationButton(Calibration calibration) {
			this.calibration = calibration;
			boolean done = calibration == Calibration.DONE;
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha(
							done ? "accent.extract" : "panel.surface",
							done ? 42 : 224));
			addToBack(surface);
			edge = new ColorBlock(
					1,
					1,
					tokens.color(done
							? "accent.extract" : "accent.interact"));
			add(edge);
			focusRule = new ColorBlock(
					1, 1, tokens.color("accent.valuable"));
			focusRule.visible = false;
			add(focusRule);
			label = PixelScene.renderTextBlock(
					tokens.scaledTypographyPx(
							BukovVisualContract.FONT_BODY));
			label.hardlight(tokens.color("text.primary"));
			add(label);
			value = PixelScene.renderTextBlock(
					tokens.scaledTypographyPx(
							BukovVisualContract.FONT_BODY));
			value.align(RenderedTextBlock.RIGHT_ALIGN);
			value.hardlight(tokens.color(
					done ? "accent.extract" : "accent.interact"));
			add(value);
			refresh();
		}

		@Override
		protected void onClick() {
			focus.focus(calibration.ordinal());
			switch (calibration) {
				case UI_SCALE:
					SPDSettings.bukovUiScale(
							(SPDSettings.bukovUiScale() + 1) % 3);
					break;
				case VIBRATION:
					SPDSettings.bukovControllerVibration(
							(SPDSettings.bukovControllerVibration() + 1) % 3);
					break;
				case AIM_ASSIST:
					SPDSettings.bukovAimAssist(
							(SPDSettings.bukovAimAssist() + 1) % 3);
					break;
				case DONE:
					hide();
					return;
				default:
					throw new IllegalStateException(
							"Unsupported first-run calibration");
			}
			refresh();
			updateFocus();
		}

		private void refresh() {
			switch (calibration) {
				case UI_SCALE:
					setCopy(
							entryMessage("calibration.ui_scale"),
							WndBukovSettings.percentLevel(
									SPDSettings.bukovUiScale()));
					break;
				case VIBRATION:
					setCopy(
							entryMessage("calibration.vibration"),
							WndBukovSettings.threeLevel(
									SPDSettings.bukovControllerVibration()));
					break;
				case AIM_ASSIST:
					setCopy(
							entryMessage("calibration.aim_assist"),
							WndBukovSettings.aimAssistLabel(
									SPDSettings.bukovAimAssist()));
					break;
				case DONE:
					setCopy(
							entryMessage("calibration.done"),
							entryMessage("calibration.enter"));
					break;
				default:
					throw new IllegalStateException(
							"Unsupported first-run calibration");
			}
		}

		private void setCopy(String left, String right) {
			label.text(left);
			value.text(right);
		}

		private void setFocused(boolean focused) {
			focusRule.visible = focused;
			label.hardlight(focused
					? tokens.color("accent.valuable")
					: tokens.color("text.primary"));
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2, height);
			focusRule.x = x;
			focusRule.y = y + height - 1;
			focusRule.size(width, 1);
			label.setRect(
					x + 6,
					y + (height - 9) / 2f,
					width * 0.48f,
					9);
			value.setRect(
					x + width * 0.47f,
					y + (height - 9) / 2f,
					width * 0.49f - 4,
					9);
		}
	}

	private static String entryMessage(String key, Object... args) {
		return BukovMessages.get("bukov.entry." + key, args);
	}
}
