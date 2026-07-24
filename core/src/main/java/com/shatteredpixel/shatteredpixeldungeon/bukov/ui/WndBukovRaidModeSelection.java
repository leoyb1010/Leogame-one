package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.ControllerHandler;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Explicit five-card raid-mode selection. It never deploys the player. */
public final class WndBukovRaidModeSelection extends Window {

	private static final int WIDTH_P = 150;
	private static final int HEIGHT_P = 226;
	private static final int WIDTH_L = 220;
	private static final int HEIGHT_L = 180;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int CARD_HEIGHT = 31;
	private static final int BUTTON_HEIGHT = 19;
	private static final int HEADER_HEIGHT = 34;
	private static final int FOOTER_HEIGHT = 25;

	private final BukovHubController controller;
	private final Callback close;
	private final BukovRaidModeSelectionViewModel viewModel;
	private final BukovRaidModeFocusModel focus;
	private final BukovUiTokens tokens;
	private final List<ModeCardButton> cardButtons = new ArrayList<>();
	private final List<ActionButton> actionButtons = new ArrayList<>();
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private ScrollPane cardScroll;
	private boolean committing;
	private boolean closing;

	public WndBukovRaidModeSelection(
			BukovHubController controller,
			Callback close) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		if (controller == null || close == null) {
			throw new IllegalArgumentException(
					"controller and close callback are required");
		}
		this.controller = controller;
		this.close = close;
		viewModel = BukovRaidModeSelectionViewModel.from(
				controller.selectedRaidMode(),
				controller.hasActiveRaid());
		focus = new BukovRaidModeFocusModel(
				viewModel.currentMode,
				viewModel.locked);
		tokens = BukovUiTokens.loadDefault();

		int windowWidth = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);
		int windowHeight = BukovWindowLayout.safeHeight(
				PixelScene.landscape() ? HEIGHT_L : HEIGHT_P);
		resize(windowWidth, windowHeight);
		build(windowWidth, windowHeight);
		updateFocus();
	}

	private void build(int windowWidth, int windowHeight) {
		ColorBlock header = new ColorBlock(
				windowWidth,
				HEADER_HEIGHT,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock rule = new ColorBlock(
				windowWidth,
				1,
				tokens.color(
						viewModel.locked
								? "accent.danger"
								: "accent.interact"));
		rule.y = HEADER_HEIGHT - 1;
		add(rule);

		RenderedTextBlock eyebrow = text(
				"OPERATION PROFILE / FIVE MODES",
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN, 3);
		add(eyebrow);
		RenderedTextBlock title = text(
				"选择行动模式",
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		title.setPos(MARGIN, 13);
		add(title);
		RenderedTextBlock state = text(
				viewModel.stateMessage,
				BukovVisualContract.FONT_CAPTION,
				tokens.color(
						viewModel.locked
								? "accent.danger"
								: "text.secondary"));
		state.setPos(MARGIN, 26);
		add(state);

		int listHeight = Math.max(
				CARD_HEIGHT * 2,
				windowHeight - HEADER_HEIGHT - FOOTER_HEIGHT);
		ModeList list = new ModeList(windowWidth - MARGIN * 2);
		cardScroll = new ScrollPane(list);
		add(cardScroll);
		cardScroll.setRect(
				MARGIN,
				HEADER_HEIGHT,
				windowWidth - MARGIN * 2,
				listHeight);

		float half = (windowWidth - MARGIN * 2 - GAP) / 2f;
		float buttonY = windowHeight - BUTTON_HEIGHT - 3;
		addAction(
				BukovRaidModeFocusModel.ACTION_APPLY,
				MARGIN,
				buttonY,
				half,
				"accent.extract");
		addAction(
				BukovRaidModeFocusModel.ACTION_BACK,
				MARGIN + half + GAP,
				buttonY,
				half,
				"panel.border");
	}

	private RenderedTextBlock text(
			String value, String typography, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				value, tokens.typographyPx(typography));
		result.maxWidth(Math.max(1, (int) width - MARGIN * 2));
		result.hardlight(color);
		return result;
	}

	private static String compact(String value, int maxCharacters) {
		if (value.length() <= maxCharacters) {
			return value;
		}
		return value.substring(0, Math.max(1, maxCharacters - 3)) + "...";
	}

	private static void center(
			RenderedTextBlock block,
			float x,
			float y,
			float regionWidth,
			float regionHeight) {
		block.setPos(
				x + (regionWidth - block.width()) / 2f,
				y + (regionHeight - block.height()) / 2f);
	}

	private void addAction(
			int action,
			float x,
			float y,
			float actionWidth,
			String accentToken) {
		ActionButton button = new ActionButton(
				action,
				tokens.color(accentToken));
		button.setRect(x, y, actionWidth, BUTTON_HEIGHT);
		actionButtons.add(button);
		add(button);
	}

	private void selectMode(int modeIndex) {
		if (committing || closing) {
			return;
		}
		focus.selectMode(modeIndex);
		updateFocus();
	}

	private void activateFocused() {
		if (focus.modeFocused()) {
			selectMode(focus.modeIndex());
		} else {
			activateAction(focus.actionIndex());
		}
	}

	private void activateAction(int action) {
		if (committing || closing) {
			return;
		}
		if (action == BukovRaidModeFocusModel.ACTION_APPLY) {
			applySelection();
		} else {
			closeToHub();
		}
	}

	private void applySelection() {
		if (!focus.applyEnabled()) {
			return;
		}
		committing = true;
		try {
			controller.selectRaidMode(focus.draftMode());
			closeToHubAfterCommit();
		} catch (IOException | RuntimeException error) {
			committing = false;
			showError("模式保存失败", error);
		}
	}

	private void closeToHubAfterCommit() {
		committing = false;
		closeToHub();
	}

	private void closeToHub() {
		if (closing || committing) {
			return;
		}
		closing = true;
		hide();
		close.call();
	}

	private void showError(String title, Throwable error) {
		ShatteredPixelDungeon.reportException(error);
		String detail = error.getMessage() == null
				? error.getClass().getSimpleName()
				: error.getMessage();
		ShatteredPixelDungeon.scene().addToFront(
				new WndMessage(title + "：\n" + detail));
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (!event.pressed || closing || committing) {
			return true;
		}
		if (BukovNavigation.back(event)) {
			closeToHub();
		} else if (BukovNavigation.previous(event)) {
			focus.move(-1);
			updateFocus();
		} else if (BukovNavigation.next(event)) {
			focus.move(1);
			updateFocus();
		} else if (BukovNavigation.confirm(event)) {
			activateFocused();
		}
		return true;
	}

	@Override
	public void update() {
		super.update();
		if (closing || committing) {
			return;
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

	@Override
	public void onBackPressed() {
		closeToHub();
	}

	private void updateFocus() {
		for (int index = 0; index < cardButtons.size(); index++) {
			cardButtons.get(index).setState(
					index == viewModel.currentMode.ordinal(),
					index == focus.draftMode().ordinal(),
					focus.modeFocused() && index == focus.modeIndex());
		}
		for (int index = 0; index < actionButtons.size(); index++) {
			actionButtons.get(index).setState(
					index == focus.actionIndex());
		}
		if (focus.modeFocused() && cardScroll != null) {
			float cardY = focus.modeIndex() * CARD_HEIGHT;
			cardScroll.scrollTo(
					0,
					Math.max(0, cardY - CARD_HEIGHT));
		}
	}

	private final class ModeList extends Component {

		private ModeList(float listWidth) {
			setSize(
					listWidth,
					viewModel.cards.size() * CARD_HEIGHT);
			ColorBlock surface = new ColorBlock(
					listWidth,
					height(),
					tokens.color("panel.deep"));
			addToBack(surface);
			for (int index = 0; index < viewModel.cards.size(); index++) {
				ModeCardButton card = new ModeCardButton(index);
				card.setRect(
						0,
						index * CARD_HEIGHT,
						listWidth,
						CARD_HEIGHT - 1);
				cardButtons.add(card);
				add(card);
			}
		}
	}

	private final class ModeCardButton extends Button {

		private final int modeIndex;
		private final BukovRaidModeSelectionViewModel.ModeCard card;
		private final ColorBlock surface;
		private final ColorBlock stateEdge;
		private final ColorBlock focusEdge;
		private final ColorBlock divider;
		private final RenderedTextBlock heading;
		private final RenderedTextBlock economy;
		private final RenderedTextBlock timing;
		private final RenderedTextBlock reward;

		private ModeCardButton(int modeIndex) {
			this.modeIndex = modeIndex;
			card = viewModel.cards.get(modeIndex);
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.surface", 225));
			addToBack(surface);
			stateEdge = new ColorBlock(
					1,
					1,
					tokens.color("panel.border"));
			add(stateEdge);
			focusEdge = new ColorBlock(
					1,
					1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			divider = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.border", 145));
			add(divider);
			heading = text(
					card.code + "  " + card.name,
					BukovVisualContract.FONT_BODY,
					tokens.color("text.primary"));
			add(heading);
			economy = text(
					card.equipmentSource + " · " + card.deathLoss,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(economy);
			timing = text(
					card.durationAndExtraction,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(timing);
			reward = text(
					card.rewardAndBoss,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("accent.valuable"));
			add(reward);
		}

		@Override
		protected void onClick() {
			selectMode(modeIndex);
		}

		private void setState(
				boolean current,
				boolean draft,
				boolean focused) {
			focusEdge.visible = focused;
			stateEdge.hardlight(tokens.color(
					current
							? "accent.extract"
							: draft
							? "accent.interact"
							: "panel.border"));
			surface.hardlight(tokens.color(
					draft ? "accent.interact" : "panel.surface"));
			surface.alpha(draft ? 0.27f : current ? 0.20f : 0.13f);
			String badge = current
					? viewModel.locked ? "  [当前/锁定]" : "  [当前]"
					: draft ? "  [待应用]" : "";
			heading.text(card.code + "  " + card.name + badge);
			heading.hardlight(tokens.color(
					focused
							? "accent.interact"
							: current
							? "accent.extract"
							: "text.primary"));
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			stateEdge.x = x;
			stateEdge.y = y;
			stateEdge.size(2, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 2;
			focusEdge.size(width, 2);
			divider.x = x + 4;
			divider.y = y + height - 1;
			divider.size(width - 8, 1);

			int maximum = PixelScene.landscape() ? 62 : 38;
			heading.maxWidth(Math.max(1, (int) width - 8));
			economy.text(compact(
					card.equipmentSource + " · " + card.deathLoss,
					maximum));
			timing.text(compact(card.durationAndExtraction, maximum));
			reward.text(compact(card.rewardAndBoss, maximum));
			economy.maxWidth(Math.max(1, (int) width - 8));
			timing.maxWidth(Math.max(1, (int) width - 8));
			reward.maxWidth(Math.max(1, (int) width - 8));
			heading.setPos(x + 5, y + 2);
			economy.setPos(x + 5, y + 9);
			timing.setPos(x + 5, y + 16);
			reward.setPos(x + 5, y + 23);
		}
	}

	private final class ActionButton extends Button {

		private final int action;
		private final int accent;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock label;

		private ActionButton(int action, int accent) {
			this.action = action;
			this.accent = accent;
			surface = new ColorBlock(
					1,
					1,
					tokens.color("panel.surface"));
			addToBack(surface);
			edge = new ColorBlock(1, 1, accent);
			add(edge);
			focusEdge = new ColorBlock(
					1,
					1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(
					"", BukovVisualContract.FONT_BODY,
					tokens.color("text.primary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			activateAction(action);
		}

		private void setState(boolean focused) {
			boolean enabled = action != BukovRaidModeFocusModel.ACTION_APPLY
					|| focus.applyEnabled();
			focusEdge.visible = focused;
			edge.hardlight(enabled
					? accent
					: tokens.color("text.disabled"));
			label.text(actionLabel(action));
			label.hardlight(tokens.color(
					!enabled
							? "text.disabled"
							: focused
							? "accent.interact"
							: "text.primary"));
			surface.alpha(focused ? 0.32f : enabled ? 0.18f : 0.08f);
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
			focusEdge.x = x;
			focusEdge.y = y + height - 2;
			focusEdge.size(width, 2);
			label.maxWidth(Math.max(1, (int) width - 6));
			center(label, x + 3, y, width - 6, height);
		}
	}

	private String actionLabel(int action) {
		if (action == BukovRaidModeFocusModel.ACTION_BACK) {
			return "返回整备";
		}
		if (viewModel.locked) {
			return "行动中锁定";
		}
		if (!focus.hasPendingSelection()) {
			return "当前模式已应用";
		}
		return "应用 " + focus.draftMode().displayName;
	}
}
