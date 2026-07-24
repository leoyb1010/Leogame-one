package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyEvent;
import com.watabou.input.ControllerHandler;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated search-extract settlement. No ranking, dungeon victory or
 * GAME OVER surfaces are reachable from this window.
 */
public final class WndBukovSettlement extends Window {

	public interface ReturnToHideout {
		void run();
	}

	public interface RepeatLastLoadout {
		void run() throws Exception;
	}

	private static final int WIDTH_P = 150;
	private static final int HEIGHT_P = 202;
	private static final int WIDTH_L = 216;
	private static final int HEIGHT_L = 158;
	private static final int ROW_HEIGHT = 17;
	private static final int BUTTON_HEIGHT = 20;

	private final BukovSettlementViewModel viewModel;
	private final ReturnToHideout returnToHideout;
	private final RepeatLastLoadout repeatLastLoadout;
	private final BukovUiTokens tokens;
	private final BukovFocusModel focus;
	private final ActionButton[] actionButtons;
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private final BukovSettlementRevealModel reveal;
	private RenderedTextBlock animatedTotals;
	private RenderedTextBlock outcomeStamp;
	private Manifest manifest;
	private boolean returning;

	public WndBukovSettlement(
			RaidResult result,
			float elapsedSeconds,
			int kills,
			ReturnToHideout returnToHideout) {
		this(
				BukovSettlementViewModel.from(
						result,
						elapsedSeconds,
						kills),
				returnToHideout,
				null);
	}

	public WndBukovSettlement(
			RaidResult result,
			float elapsedSeconds,
			int kills,
			ReturnToHideout returnToHideout,
			RepeatLastLoadout repeatLastLoadout) {
		this(
				BukovSettlementViewModel.from(
						result,
						elapsedSeconds,
						kills),
				returnToHideout,
				repeatLastLoadout);
	}

	public WndBukovSettlement(
			BukovSettlementViewModel viewModel,
			ReturnToHideout returnToHideout) {
		this(viewModel, returnToHideout, null);
	}

	public WndBukovSettlement(
			BukovSettlementViewModel viewModel,
			ReturnToHideout returnToHideout,
			RepeatLastLoadout repeatLastLoadout) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(0xFF101514), 0));
		if (viewModel == null) {
			throw new IllegalArgumentException("viewModel is required");
		}
		if (returnToHideout == null) {
			throw new IllegalArgumentException("returnToHideout is required");
		}
		this.viewModel = viewModel;
		this.returnToHideout = returnToHideout;
		this.repeatLastLoadout = repeatLastLoadout;
		int actionCount = repeatLastLoadout == null ? 1 : 2;
		focus = new BukovFocusModel(actionCount, 0);
		actionButtons = new ActionButton[actionCount];
		tokens = BukovUiTokens.loadDefault();
		reveal = new BukovSettlementRevealModel(
				viewModel.items.size(),
				viewModel.value);

		int width = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);
		int height = BukovWindowLayout.safeHeight(
				PixelScene.landscape() ? HEIGHT_L : HEIGHT_P);
		resize(width, height);
		build(width, height);
	}

	private void build(int width, int height) {
		boolean success = viewModel.outcome == RaidOutcome.SUCCESS;
		int accent = tokens.color(
				success ? "accent.extract" : "accent.danger");
		float y = 3;

		RenderedTextBlock eyebrow = text(
				"行动结算 · 确认可跳过",
				7,
				tokens.color("text.secondary"));
		eyebrow.setRect(5, y, width - 10, 9);
		eyebrow.align(RenderedTextBlock.CENTER_ALIGN);
		add(eyebrow);
		y += 11;

		RenderedTextBlock headline = text(viewModel.headline, 15, accent);
		headline.setRect(5, y, width - 10, 18);
		headline.align(RenderedTextBlock.CENTER_ALIGN);
		add(headline);
		outcomeStamp = text(
				success ? "[ 撤离确认 ]" : "[ 行动损失 ]",
				7,
				accent);
		outcomeStamp.setRect(5, y + 11, width - 10, 8);
		outcomeStamp.align(RenderedTextBlock.RIGHT_ALIGN);
		add(outcomeStamp);
		y += 20;

		ColorBlock divider = new ColorBlock(width - 10, 1, accent);
		divider.x = 5;
		divider.y = y;
		add(divider);
		y += 4;

		animatedTotals = text(
				viewModel.totals(0L),
				8,
				tokens.color("text.primary"));
		animatedTotals.setRect(5, y, width - 10, 10);
		animatedTotals.align(RenderedTextBlock.CENTER_ALIGN);
		add(animatedTotals);
		y += 11;

		RenderedTextBlock stats = text(
				viewModel.stats(),
				7,
				tokens.color("text.secondary"));
		stats.setRect(5, y, width - 10, 9);
		stats.align(RenderedTextBlock.CENTER_ALIGN);
		add(stats);
		y += 10;

		RenderedTextBlock mission = text(
				viewModel.mission(),
				7,
				viewModel.missionCompleted
						? tokens.color("accent.extract")
						: tokens.color("text.secondary"));
		mission.setRect(5, y, width - 10, 9);
		mission.align(RenderedTextBlock.CENTER_ALIGN);
		add(mission);
		y += 11;

		RenderedTextBlock listTitle = text(
				viewModel.manifestTitle,
				8,
				accent);
		listTitle.setRect(5, y, width - 10, 10);
		add(listTitle);
		y += 11;

		float listHeight = height - y - BUTTON_HEIGHT - 8;
		manifest = new Manifest(width - 10);
		ScrollPane scroll = new ScrollPane(manifest);
		add(scroll);
		scroll.setRect(5, y, width - 10, listHeight);

		float footerY = height - BUTTON_HEIGHT - 3;
		if (repeatLastLoadout == null) {
			ActionButton button = new ActionButton(
					"确认并返回藏身处",
					accent,
					false);
			button.setRect(5, footerY, width - 10, BUTTON_HEIGHT);
			add(button);
			actionButtons[0] = button;
		} else {
			float half = (width - 12) / 2f;
			ActionButton repeat = new ActionButton(
					"沿用配装",
					tokens.color("accent.interact"),
					true);
			repeat.setRect(5, footerY, half, BUTTON_HEIGHT);
			add(repeat);
			actionButtons[0] = repeat;
			ActionButton button = new ActionButton(
					"返回藏身处",
					accent,
					false);
			button.setRect(7 + half, footerY, half, BUTTON_HEIGHT);
			add(button);
			actionButtons[1] = button;
		}
		updateFocus();
		updateReveal();
	}

	private RenderedTextBlock text(String value, int size, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(value, size);
		result.hardlight(color);
		result.maxWidth(width - 10);
		return result;
	}

	@Override
	public void onBackPressed() {
		if (skipReveal()) return;
		returnToHideout();
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (!event.pressed) {
			return true;
		}
		if (!reveal.complete()) {
			if (BukovNavigation.back(event)
					|| BukovNavigation.confirm(event)) {
				skipReveal();
			}
			return true;
		}
		if (BukovNavigation.back(event)) {
			returnToHideout();
		} else if (BukovNavigation.previous(event)) {
			focus.move(-1);
			updateFocus();
		} else if (BukovNavigation.next(event)) {
			focus.move(1);
			updateFocus();
		} else if (BukovNavigation.confirm(event)) {
			actionButtons[focus.index()].onClick();
		}
		return true;
	}

	@Override
	public void update() {
		super.update();
		if (!reveal.complete()) {
			reveal.advance(Game.elapsed);
			updateReveal();
			if (!reveal.complete()) return;
		}
		int delta = focusRepeater.update(
				ControllerHandler.leftStickPosition.x,
				ControllerHandler.leftStickPosition.y,
				Game.elapsed);
		if (delta != 0) {
			focus.move(delta);
			updateFocus();
		}
	}

	private boolean skipReveal() {
		if (reveal.complete()) return false;
		reveal.skip();
		updateReveal();
		return true;
	}

	private void updateReveal() {
		if (animatedTotals != null) {
			animatedTotals.text(
					viewModel.totals(reveal.displayedValue()));
		}
		if (outcomeStamp != null) {
			outcomeStamp.visible = reveal.stampVisible();
		}
		if (manifest != null) {
			manifest.reveal(reveal.visibleRows());
		}
	}

	private void updateFocus() {
		for (int i = 0; i < actionButtons.length; i++) {
			if (actionButtons[i] != null) {
				actionButtons[i].setFocused(focus.index() == i);
			}
		}
	}

	private void returnToHideout() {
		if (returning) {
			return;
		}
		returning = true;
		hide();
		returnToHideout.run();
	}

	private void repeatAndReturn() {
		if (returning) {
			return;
		}
		try {
			repeatLastLoadout.run();
		} catch (Exception error) {
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
					.reportException(error);
			String detail = error.getMessage() == null
					? error.getClass().getSimpleName()
					: error.getMessage();
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
					.scene().addToFront(new WndMessage(
							"沿用配装失败：\n" + detail));
			return;
		}
		returnToHideout();
	}

	private final class Manifest extends Component {

		private final List<RenderedTextBlock> itemRows =
				new ArrayList<>();

		private Manifest(float listWidth) {
			int rows = Math.max(1, viewModel.items.size());
			setSize(listWidth, rows * ROW_HEIGHT);
			ColorBlock surface = new ColorBlock(
					listWidth,
					height(),
					tokens.color("panel.surface"));
			addToBack(surface);
			if (viewModel.items.isEmpty()) {
				addRow(viewModel.emptyManifest(), 0,
						tokens.color("text.disabled"));
			} else {
				for (int i = 0; i < viewModel.items.size(); i++) {
					BukovSettlementViewModel.ItemRow row =
							viewModel.items.get(i);
					addRow(
							row.summary(),
							i,
							row.legacy
									? tokens.color("text.disabled")
									: tokens.color("text.primary"));
				}
			}
		}

		private void addRow(String value, int index, int color) {
			RenderedTextBlock row = text(value, 7, color);
			row.setRect(3, index * ROW_HEIGHT + 3,
					width() - 6, ROW_HEIGHT - 3);
			add(row);
			if (!viewModel.items.isEmpty()) {
				itemRows.add(row);
			}
		}

		private void reveal(int visibleRows) {
			for (int index = 0; index < itemRows.size(); index++) {
				itemRows.get(index).visible = index < visibleRows;
			}
		}
	}

	private final class ActionButton extends Button {

		private final ColorBlock background;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock label;

		private final boolean repeat;

		private ActionButton(String labelText, int accent, boolean repeat) {
			this.repeat = repeat;
			background = new ColorBlock(1, 1,
					tokens.color("panel.surface"));
			addToBack(background);
			edge = new ColorBlock(1, 1, accent);
			add(edge);
			focusEdge = new ColorBlock(
					1,
					1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(labelText, 8,
					tokens.color("text.primary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			if (skipReveal()) return;
			focus.focus(repeat ? 0 : actionButtons.length - 1);
			updateFocus();
			if (repeat) {
				repeatAndReturn();
			} else {
				returnToHideout();
			}
		}

		private void setFocused(boolean focused) {
			focusEdge.visible = focused;
			label.hardlight(focused
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
			label.setRect(x + 4, y + (height - 10) / 2f,
					width - 8, 10);
		}
	}
}
