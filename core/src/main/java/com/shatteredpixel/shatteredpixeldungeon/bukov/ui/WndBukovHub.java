package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
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
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Industrial-tactical hideout and deployment surface.
 *
 * It intentionally uses only Bukov panels, typography and focus states: no
 * inherited dungeon checkbox, red-button, hero-selection or backpack chrome.
 */
public final class WndBukovHub extends Window {

	private static final int WIDTH_P = 150;
	private static final int HEIGHT_P = 226;
	private static final int WIDTH_L = 220;
	private static final int HEIGHT_L = 180;
	private static final int ROW_HEIGHT = 16;
	private static final int BUTTON_HEIGHT = 18;
	private static final int GAP = 2;
	private static final int MARGIN = 4;
	private static final int INVENTORY_TOP_P = 107;
	private static final int INVENTORY_TOP_L = 80;
	private static final int FOOTER_RESERVED = 52;

	private final BukovHubController controller;
	private final Callback deploy;
	private final BukovHubViewModel viewModel;
	private final BukovHubViewModel.InventoryFilter inventoryFilter;
	private final List<BukovHubViewModel.ItemRow> inventoryItems;
	private final BukovHubFocusModel focus;
	private final BukovUiTokens tokens;
	private final List<LoadoutRow> itemRows = new ArrayList<>();
	private final List<TacticalButton> actionButtons = new ArrayList<>();
	private ScrollPane itemScroll;
	private ModeSelectButton modeButton;
	private FilterCycleButton filterButton;
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();

	public WndBukovHub(BukovHubController controller, Callback deploy) {
		this(
				controller,
				deploy,
				0,
				BukovHubViewModel.InventoryFilter.ALL);
	}

	private WndBukovHub(
			BukovHubController controller,
			Callback deploy,
			int restoredFocus,
			BukovHubViewModel.InventoryFilter inventoryFilter) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		if (controller == null) {
			throw new IllegalArgumentException("controller is required");
		}
		if (deploy == null || inventoryFilter == null) {
			throw new IllegalArgumentException(
					"deploy callback and inventory filter are required");
		}
		this.controller = controller;
		this.deploy = deploy;
		this.inventoryFilter = inventoryFilter;
		viewModel = controller.viewModel();
		inventoryItems = viewModel.inventoryItems(inventoryFilter);
		focus = new BukovHubFocusModel(inventoryItems.size());
		focus.focus(restoredFocus);
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
		float y = 3;
		ColorBlock header = new ColorBlock(
				windowWidth,
				16,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = 15;
		add(headerRule);

		RenderedTextBlock eyebrow = text(
				viewModel.activeRaid
						? "HIDEOUT / ACTIVE RAID"
						: "HIDEOUT / LOADOUT",
				6,
				tokens.color("text.secondary"));
		eyebrow.setPos(windowWidth - MARGIN - eyebrow.width(), y);
		add(eyebrow);

		RenderedTextBlock title = text(
				viewModel.activeRaid
						? "继续布科夫行动"
						: "布科夫行动整备",
				11,
				tokens.color("text.primary"));
		title.setPos(MARGIN, y);
		add(title);
		y += 16;

		StatusStrip status = new StatusStrip(windowWidth - MARGIN * 2);
		status.setRect(MARGIN, y, windowWidth - MARGIN * 2, 21);
		add(status);
		y += 23;

		if (!PixelScene.landscape()) {
			float slotGap = 2;
			float slotWidth =
					(windowWidth - MARGIN * 2 - slotGap * 2) / 3f;
			BukovHubViewModel.LoadoutSlot[] slots = {
					BukovHubViewModel.LoadoutSlot.PRIMARY,
					BukovHubViewModel.LoadoutSlot.AMMUNITION,
					BukovHubViewModel.LoadoutSlot.MEDICAL
			};
			for (int i = 0; i < slots.length; i++) {
				LoadoutSlotCard card = new LoadoutSlotCard(slots[i]);
				card.setRect(
						MARGIN + i * (slotWidth + slotGap),
						y,
						slotWidth,
						24);
				add(card);
			}
			y += 27;
		}

		float utilityWidth =
				(windowWidth - MARGIN * 2 - GAP) / 2f;
		modeButton = new ModeSelectButton();
		modeButton.setRect(MARGIN, y, utilityWidth, BUTTON_HEIGHT);
		add(modeButton);
		addAction(
				viewModel.activeRaid
						? "交易锁定"
						: "补给商店 · " + viewModel.currency,
				BukovHubFocusModel.ACTION_VENDOR,
				MARGIN + utilityWidth + GAP,
				y,
				utilityWidth,
				viewModel.activeRaid
						? "text.disabled"
						: "accent.valuable");
		y += BUTTON_HEIGHT + GAP;

		RenderedTextBlock modeSummary = text(
				compact(
						viewModel.raidModeSummary,
						PixelScene.landscape() ? 42 : 27),
				6,
				tokens.color("text.secondary"));
		modeSummary.setRect(
				MARGIN,
				y,
				windowWidth - MARGIN * 2,
				7);
		add(modeSummary);
		y += 8;

		float filterWidth = PixelScene.landscape() ? 70f : 58f;
		RenderedTextBlock stashLabel = text(
				viewModel.activeRaid
						? "本次行动携带"
						: "仓库物资 · 点击加入配装",
				7,
				tokens.color("text.secondary"));
		stashLabel.setRect(
				MARGIN,
				y,
				windowWidth - MARGIN * 2 - filterWidth - GAP,
				9);
		add(stashLabel);
		filterButton = new FilterCycleButton();
		filterButton.setRect(
				windowWidth - MARGIN - filterWidth,
				y,
				filterWidth,
				9);
		add(filterButton);
		y += 10;

		float listHeight = inventoryViewportHeight(
				windowHeight,
				PixelScene.landscape());
		InventoryList list = new InventoryList(windowWidth - MARGIN * 2);
		itemScroll = new ScrollPane(list);
		// ScrollPane.layout() needs its parent camera, so attach it before sizing.
		add(itemScroll);
		itemScroll.setRect(
				MARGIN,
				y,
				windowWidth - MARGIN * 2,
				listHeight);
		y += listHeight + GAP;

		RenderedTextBlock settlement = text(
				settlementText(),
				6,
				settlementColor());
		settlement.setRect(MARGIN, y, windowWidth - MARGIN * 2, 9);
		add(settlement);
		y += 10;

		float half = (windowWidth - MARGIN * 2 - GAP) / 2f;
		addAction(
				viewModel.activeRaid
						? "结束本次行动"
						: viewModel.canRepeatLoadout
								? "沿用上次"
								: "推荐配装",
				BukovHubFocusModel.ACTION_REPEAT,
				MARGIN,
				y,
				half,
				viewModel.activeRaid ? "accent.danger" : "accent.interact");
		addAction(
				viewModel.activeRaid
						? "配装已锁定"
						: viewModel.canRepeatLoadout
								? "智能配装"
								: "清空配装",
				BukovHubFocusModel.ACTION_CLEAR,
				MARGIN + half + GAP,
				y,
				half,
				"panel.border");
		y += BUTTON_HEIGHT + GAP;

		addAction(
				viewModel.activeRaid
						? "继续行动"
						: !viewModel.canDeploy ? "配装不完整" : "确认出击",
				BukovHubFocusModel.ACTION_DEPLOY,
				MARGIN,
				y,
				half,
				!viewModel.canDeploy ? "accent.danger" : "accent.extract");
		addAction(
				"返回藏身处",
				BukovHubFocusModel.ACTION_BACK,
				MARGIN + half + GAP,
				y,
				half,
				"panel.border");
	}

	static int inventoryViewportHeight(
			int windowHeight,
			boolean landscape) {
		return Math.max(
				1,
				windowHeight
						- (landscape
								? INVENTORY_TOP_L : INVENTORY_TOP_P)
						- FOOTER_RESERVED);
	}

	static int windowHeightFor(
			int viewportHeight,
			float safeTop,
			float safeBottom,
			boolean landscape) {
		return BukovWindowLayout.fit(
				viewportHeight,
				safeTop,
				safeBottom,
				landscape ? HEIGHT_L : HEIGHT_P);
	}

	static boolean fitsViewport(
			int viewportHeight,
			float safeTop,
			float safeBottom,
			boolean landscape) {
		int windowHeight = windowHeightFor(
				viewportHeight,
				safeTop,
				safeBottom,
				landscape);
		return BukovWindowLayout.fits(
				viewportHeight,
				safeTop,
				safeBottom,
				windowHeight);
	}

	private RenderedTextBlock text(String value, int size, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(value, size);
		result.maxWidth(width - MARGIN * 2);
		result.hardlight(color);
		return result;
	}

	private static String compact(String value, int maxCharacters) {
		if (value.length() <= maxCharacters) {
			return value;
		}
		return value.substring(0, maxCharacters - 3) + "...";
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

	private String settlementText() {
		if (viewModel.activeRaid) {
			return "行动进行中  /  " + viewModel.activeRaidSummary();
		}
		if (!viewModel.canDeploy) {
			return "无法出击  /  " + viewModel.deploymentBlockReason;
		}
		if (viewModel.latestSettlement == null) {
			return viewModel.deploymentReadinessHeadline();
		}
		boolean success = viewModel.latestSettlement.outcome == RaidOutcome.SUCCESS;
		return "配装已就绪 / 可立即出击 · "
				+ (success ? "上次已撤离 +" : "上次未归还 -")
				+ viewModel.latestSettlement.value
				+ " · "
				+ viewModel.latestSettlement.kills
				+ " 击杀"
				+ (viewModel.latestSettlement.missionCompleted
						? " · 任务完成"
						: "");
	}

	private int settlementColor() {
		if (viewModel.activeRaid) {
			return tokens.color("accent.extract");
		}
		if (!viewModel.canDeploy) {
			return tokens.color("accent.danger");
		}
		if (viewModel.latestSettlement == null) {
			return tokens.color("text.disabled");
		}
		return tokens.color(
				viewModel.latestSettlement.outcome == RaidOutcome.SUCCESS
						? "accent.extract"
						: "accent.danger");
	}

	private void addAction(
			String label,
			int action,
			float x,
			float y,
			float buttonWidth,
			String accentToken) {
		TacticalButton button = new TacticalButton(
				label,
				action,
				tokens.color(accentToken),
				actionEnabled(action));
		button.setRect(x, y, buttonWidth, BUTTON_HEIGHT);
		actionButtons.add(button);
		add(button);
	}

	private void toggle(String itemUid) {
		if (!viewModel.canEditLoadout) {
			return;
		}
		try {
			controller.toggleItem(itemUid);
			reopen();
		} catch (IOException | RuntimeException error) {
			showError("配装保存失败", error);
		}
	}

	private void openModeSelection() {
		// Pointer and controller entry share the same deterministic return
		// target instead of restoring whichever row happened to be focused.
		focus.focus(inventoryItems.size());
		int restoredFocus = focus.index();
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovRaidModeSelection(
						controller,
						() -> ShatteredPixelDungeon.scene().addToFront(
								new WndBukovHub(
										controller,
										deploy,
										restoredFocus,
										inventoryFilter))));
	}

	private void cycleInventoryFilter() {
		BukovHubViewModel.InventoryFilter next =
				inventoryFilter.next();
		int nextFilterFocus =
				viewModel.inventoryItems(next).size() + 1;
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovHub(
						controller,
						deploy,
						nextFilterFocus,
						next));
	}

	private void activateAction(int action) {
		if (!actionEnabled(action)) {
			return;
		}
		try {
			switch (action) {
				case BukovHubFocusModel.ACTION_VENDOR:
					openVendor();
					break;
				case BukovHubFocusModel.ACTION_REPEAT:
					if (viewModel.canRepeatLoadout) {
						controller.repeatLastLoadout();
					} else {
						controller.recommendLoadout();
					}
					reopen();
					break;
				case BukovHubFocusModel.ACTION_CLEAR:
					if (viewModel.canRepeatLoadout) {
						controller.recommendLoadout();
					} else {
						controller.clearLoadout();
					}
					reopen();
					break;
				case BukovHubFocusModel.ACTION_DEPLOY:
					if (viewModel.activeRaid) {
						confirmDeployment();
					} else if (viewModel.canDeploy) {
						ShatteredPixelDungeon.scene().addToFront(
								new DeploymentConfirmWindow());
					}
					break;
				default:
					hide();
					break;
			}
		} catch (IOException | RuntimeException error) {
			showError("藏身处保存失败", error);
		}
	}

	private boolean actionEnabled(int action) {
		if (viewModel.activeRaid) {
			// Ending a raid needs a separate destructive confirmation plus host
			// save cleanup. Until that flow exists, expose the state but keep
			// only resume and return available.
			return action == BukovHubFocusModel.ACTION_DEPLOY
					|| action == BukovHubFocusModel.ACTION_BACK;
		}
		return action != BukovHubFocusModel.ACTION_DEPLOY
				|| viewModel.canDeploy;
	}

	private void openVendor() {
		int restoredFocus = focus.index();
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovVendor(
						controller,
						() -> ShatteredPixelDungeon.scene().addToFront(
								new WndBukovHub(
										controller,
										deploy,
										restoredFocus,
										inventoryFilter))));
	}

	private void confirmDeployment() {
		try {
			controller.confirmDeployment();
			hide();
			deploy.call();
		} catch (IOException | RuntimeException error) {
			showError("出击确认失败", error);
		}
	}

	private void reopen() {
		int restoredFocus = focus.index();
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovHub(
						controller,
						deploy,
						restoredFocus,
						inventoryFilter));
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
		if (!event.pressed) {
			return true;
		}
		if (BukovNavigation.back(event)) {
			onBackPressed();
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
		activateAction(BukovHubFocusModel.ACTION_BACK);
	}

	private void activateFocused() {
		if (focus.itemFocused()) {
			BukovHubViewModel.ItemRow item =
					inventoryItems.get(focus.itemIndex());
			if (item.deployable) {
				toggle(item.itemUid);
			}
		} else if (focus.modeFocused()) {
			openModeSelection();
		} else if (focus.filterFocused()) {
			cycleInventoryFilter();
		} else {
			activateAction(focus.actionIndex());
		}
	}

	private void updateFocus() {
		for (int i = 0; i < itemRows.size(); i++) {
			itemRows.get(i).setFocused(
					focus.itemFocused() && i == focus.itemIndex());
		}
		for (int i = 0; i < actionButtons.size(); i++) {
			actionButtons.get(i).setFocused(
					!focus.itemFocused() && i == focus.actionIndex());
		}
		if (modeButton != null) {
			modeButton.setFocused(focus.modeFocused());
		}
		if (filterButton != null) {
			filterButton.setFocused(focus.filterFocused());
		}
		if (focus.itemFocused() && itemScroll != null) {
			float rowY = focus.itemIndex() * ROW_HEIGHT;
			itemScroll.scrollTo(0, Math.max(0, rowY - ROW_HEIGHT));
		}
	}

	private final class StatusStrip extends Component {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock dividerA;
		private final ColorBlock dividerB;
		private final ColorBlock dividerC;
		private final RenderedTextBlock cash;
		private final RenderedTextBlock stash;
		private final RenderedTextBlock risk;
		private final RenderedTextBlock weight;

		private StatusStrip(float stripWidth) {
			surface = new ColorBlock(1, 1, tokens.color("panel.surface"));
			addToBack(surface);
			edge = new ColorBlock(1, 1,
					!viewModel.canDeploy
							? tokens.color("accent.danger")
							: tokens.color("accent.valuable"));
			add(edge);
			dividerA = statusDivider();
			dividerB = statusDivider();
			dividerC = statusDivider();
			cash = text("现金\n" + viewModel.currency, 7,
					tokens.color("accent.valuable"));
			add(cash);
			stash = text("仓库价值\n" + viewModel.stashValue, 7,
					tokens.color("text.secondary"));
			add(stash);
			risk = text(
					(viewModel.activeRaid ? "行动携带\n" : "本次风险\n")
							+ viewModel.riskValue,
					7,
					viewModel.riskValue > 0
							? tokens.color("accent.valuable")
							: tokens.color("text.secondary"));
			add(risk);
			weight = text("负重\n" + viewModel.loadoutSummary(), 7,
					viewModel.overweight
							? tokens.color("accent.danger")
							: tokens.color("text.primary"));
			add(weight);
			setSize(stripWidth, 21);
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
			float column = (width - 5) / 4f;
			layoutDivider(dividerA, x + 5 + column, y + 3);
			layoutDivider(dividerB, x + 5 + column * 2, y + 3);
			layoutDivider(dividerC, x + 5 + column * 3, y + 3);
			cash.maxWidth((int) column);
			stash.maxWidth((int) column);
			risk.maxWidth((int) column);
			weight.maxWidth((int) column);
			cash.setPos(x + 5, y + 3);
			stash.setPos(x + 5 + column, y + 3);
			risk.setPos(x + 5 + column * 2, y + 3);
			weight.setPos(x + 5 + column * 3, y + 3);
		}

		private ColorBlock statusDivider() {
			ColorBlock divider = new ColorBlock(
					1,
					15,
					tokens.colorWithAlpha("panel.border", 150));
			add(divider);
			return divider;
		}

		private void layoutDivider(ColorBlock divider, float dividerX, float dividerY) {
			divider.x = dividerX - 3;
			divider.y = dividerY;
		}
	}

	private final class LoadoutSlotCard extends Component {

		private final BukovHubViewModel.LoadoutSlot slot;
		private final ColorBlock surface;
		private final ColorBlock stateSurface;
		private final ColorBlock edge;
		private final RenderedTextBlock code;
		private final RenderedTextBlock value;

		private LoadoutSlotCard(BukovHubViewModel.LoadoutSlot slot) {
			this.slot = slot;
			surface = new ColorBlock(1, 1, tokens.color("panel.surface"));
			addToBack(surface);
			stateSurface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.interact", 28));
			stateSurface.visible =
					!"未配置".equals(viewModel.slotSummary(slot));
			addToBack(stateSurface);
			edge = new ColorBlock(1, 1,
					"未配置".equals(viewModel.slotSummary(slot))
							? tokens.color("panel.border")
							: tokens.color("accent.interact"));
			add(edge);
			code = text(slot.code + "  " + slot.label, 6,
					tokens.color("text.secondary"));
			add(code);
			value = text(viewModel.slotSummary(slot), 7,
					"未配置".equals(viewModel.slotSummary(slot))
							? tokens.color("text.disabled")
							: tokens.color("text.primary"));
			add(value);
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			stateSurface.x = x;
			stateSurface.y = y;
			stateSurface.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(width, 1);
			code.maxWidth(Math.max(1, (int) width - 6));
			value.maxWidth(Math.max(1, (int) width - 6));
			code.setPos(x + 3, y + 3);
			value.setPos(x + 3, y + 13);
		}
	}

	private final class InventoryList extends Component {

		private InventoryList(float listWidth) {
			int rows = Math.max(1, inventoryItems.size());
			setSize(listWidth, rows * ROW_HEIGHT);
			ColorBlock surface = new ColorBlock(
					listWidth,
					height(),
					tokens.color("panel.surface"));
			addToBack(surface);
			if (inventoryItems.isEmpty()) {
				RenderedTextBlock empty = text(
						viewModel.stashItems.isEmpty()
								? "仓库为空 · 完成撤离可带回物资"
								: inventoryFilter.label + "分类暂无物资",
						7,
						tokens.color("text.disabled"));
				empty.setRect(4, 4, listWidth - 8, ROW_HEIGHT - 4);
				add(empty);
			} else {
				for (int i = 0; i < inventoryItems.size(); i++) {
					LoadoutRow row = new LoadoutRow(
							inventoryItems.get(i));
					row.setRect(0, i * ROW_HEIGHT, listWidth, ROW_HEIGHT - 1);
					itemRows.add(row);
					add(row);
				}
			}
		}
	}

	private final class FilterCycleButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final RenderedTextBlock label;

		private FilterCycleButton() {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.interact", 28));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, tokens.color("panel.border"));
			add(edge);
			label = text(
					"筛选 " + viewModel.inventoryFilterSummary(
							inventoryFilter),
					6,
					tokens.color("text.secondary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			cycleInventoryFilter();
		}

		private void setFocused(boolean focused) {
			edge.hardlight(tokens.color(
					focused ? "accent.interact" : "panel.border"));
			surface.alpha(focused ? 0.34f : 0.15f);
			label.hardlight(tokens.color(
					focused ? "accent.interact" : "text.secondary"));
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y + height - 1;
			edge.size(width, 1);
			label.maxWidth(Math.max(1, (int) width - 2));
			center(label, x + 1, y, width - 2, height);
		}
	}

	private final class ModeSelectButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock label;

		private ModeSelectButton() {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.interact", 34));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			add(edge);
			focusEdge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(
					"模式  " + viewModel.raidModeName
							+ (viewModel.canEditLoadout
									? "  [选择]"
									: "  [查看/锁定]"),
					7,
					tokens.color("text.primary"));
			add(label);
		}

		@Override
		protected void onClick() {
			openModeSelection();
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
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 2;
			focusEdge.size(width, 2);
			label.setPos(x + 5, y + 4);
		}
	}

	private final class LoadoutRow extends Button {

		private final BukovHubViewModel.ItemRow item;
		private final ColorBlock background;
		private final ColorBlock selectedSurface;
		private final ColorBlock selected;
		private final ColorBlock focusEdge;
		private final ColorBlock rarityEdge;
		private final ColorBlock divider;
		private final RenderedTextBlock category;
		private final RenderedTextBlock name;
		private final RenderedTextBlock metrics;

		private LoadoutRow(BukovHubViewModel.ItemRow item) {
			this.item = item;
			background = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.surface", 220));
			addToBack(background);
			selectedSurface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.extract", 30));
			selectedSurface.visible = item.selected;
			addToBack(selectedSurface);
			selected = new ColorBlock(1, 1,
					item.selected
							? tokens.color("accent.extract")
							: tokens.color("text.disabled"));
			add(selected);
			focusEdge = new ColorBlock(1, 1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			rarityEdge = new ColorBlock(
					1,
					1,
					tokens.color(item.rarity.colorToken));
			add(rarityEdge);
			divider = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.border", 135));
			add(divider);
			category = text(
					item.slot.label + "/" + item.rarity.label,
					6,
					tokens.color(item.rarity.colorToken));
			add(category);
			name = text(
					compact(item.label, 15) + " ×" + item.quantity,
					7,
					!item.deployable
							? tokens.color("text.disabled")
							: item.selected
							? tokens.color("text.primary")
							: tokens.color("text.secondary"));
			add(name);
			metrics = text(
					compact(
							item.value + " · " + item.comparisonLabel(),
							PixelScene.landscape() ? 20 : 13),
					6,
					!item.deployable
							? tokens.color("text.disabled")
							: item.selected
							? tokens.color("accent.valuable")
							: tokens.color("text.disabled"));
			metrics.align(RenderedTextBlock.RIGHT_ALIGN);
			add(metrics);
		}

		private void setFocused(boolean focused) {
			focusEdge.visible = focused;
			selectedSurface.visible = item.selected || focused;
			selectedSurface.hardlight(focused
					? tokens.color("accent.interact")
					: tokens.color("accent.extract"));
			name.hardlight(!item.deployable
					? tokens.color("text.disabled")
					: focused
					? tokens.color("accent.interact")
					: item.selected
					? tokens.color("text.primary")
					: tokens.color("text.secondary"));
		}

		@Override
		protected void onClick() {
			if (item.deployable) {
				toggle(item.itemUid);
			}
		}

		@Override
		protected void layout() {
			super.layout();
			background.x = x;
			background.y = y;
			background.size(width, height);
			selectedSurface.x = x;
			selectedSurface.y = y;
			selectedSurface.size(width, height);
			selected.x = x;
			selected.y = y;
			selected.size(2, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 1;
			focusEdge.size(width, 2);
			rarityEdge.x = x;
			rarityEdge.y = y;
			rarityEdge.size(width, 1);
			divider.x = x + 4;
			divider.y = y + height - 1;
			divider.size(width - 8, 1);
			category.setPos(x + 5, y + 4);
			float metricsX = x + width - metrics.width() - 3;
			name.maxWidth(Math.max(
					1,
					(int) (metricsX - (x + 43) - 2)));
			name.setPos(x + 43, y + 3);
			metrics.setPos(metricsX, y + 4);
		}
	}

	private class TacticalButton extends Button {

		private final int action;
		private final boolean enabled;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock label;

		private TacticalButton(
				String value,
				int action,
				int accent,
				boolean enabled) {
			this.action = action;
			this.enabled = enabled;
			surface = new ColorBlock(1, 1, tokens.color("panel.surface"));
			if (action == BukovHubFocusModel.ACTION_DEPLOY && enabled) {
				surface.hardlight(tokens.color("accent.extract"));
				surface.alpha(0.20f);
			}
			addToBack(surface);
			edge = new ColorBlock(1, 1,
					enabled ? accent : tokens.color("text.disabled"));
			add(edge);
			focusEdge = new ColorBlock(1, 1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(value, 8,
					enabled
							? tokens.color("text.primary")
							: tokens.color("text.disabled"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		private void setFocused(boolean focused) {
			focusEdge.visible = focused;
			label.hardlight(!enabled
					? tokens.color("text.disabled")
					: focused
					? tokens.color("accent.interact")
					: tokens.color("text.primary"));
		}

		@Override
		protected void onClick() {
			if (enabled) {
				activateAction(action);
			}
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
			center(label, x + 3, y, width - 6, height);
		}
	}

	/** Dedicated second-step confirmation; it never mutates button copy. */
	private final class DeploymentConfirmWindow extends Window {

		private boolean submitting;

		private DeploymentConfirmWindow() {
			super(0, 0, new NinePatch(
					TextureCache.createSolid(
							BukovUiTokens.loadDefault().colorWithAlpha(
									"panel.deep", 255)), 0));
			int confirmWidth = 138;
			int confirmHeight = 84;
			resize(confirmWidth, confirmHeight);

			RenderedTextBlock eyebrow = text(
					"DEPLOYMENT AUTHORIZATION",
					6,
					tokens.color("text.secondary"));
			eyebrow.align(RenderedTextBlock.CENTER_ALIGN);
			center(eyebrow, 5, 5, confirmWidth - 10, 8);
			add(eyebrow);

			RenderedTextBlock heading = text(
					"确认进入封锁区",
					11,
					tokens.color("accent.extract"));
			heading.align(RenderedTextBlock.CENTER_ALIGN);
			center(heading, 5, 15, confirmWidth - 10, 14);
			add(heading);

			RenderedTextBlock warning = text(
					"带入物资将承担损失风险\n"
							+ "风险价值 " + viewModel.riskValue
							+ "  ·  负重 " + viewModel.loadoutSummary(),
					7,
					tokens.color("text.primary"));
			warning.maxWidth(confirmWidth - 14);
			warning.align(RenderedTextBlock.CENTER_ALIGN);
			center(warning, 7, 32, confirmWidth - 14, 20);
			add(warning);

			ConfirmButton cancel = new ConfirmButton(
					"返回整备",
					false,
					tokens.color("panel.border"));
			cancel.setRect(5, 59, 61, 19);
			add(cancel);
			ConfirmButton accept = new ConfirmButton(
					"进入行动",
					true,
					tokens.color("accent.extract"));
			accept.setRect(72, 59, 61, 19);
			add(accept);
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
			} else if (BukovNavigation.confirm(event)) {
				submit();
			}
			return true;
		}

		private void submit() {
			if (submitting) {
				return;
			}
			submitting = true;
			hide();
			confirmDeployment();
		}

		private final class ConfirmButton extends Button {

			private final boolean accepts;
			private final ColorBlock surface;
			private final ColorBlock edge;
			private final RenderedTextBlock label;

			private ConfirmButton(String value, boolean accepts, int accent) {
				this.accepts = accepts;
				surface = new ColorBlock(1, 1,
						tokens.color("panel.surface"));
				addToBack(surface);
				edge = new ColorBlock(1, 1, accent);
				add(edge);
				label = text(value, 8,
						accepts
								? tokens.color("text.primary")
								: tokens.color("text.secondary"));
				label.align(RenderedTextBlock.CENTER_ALIGN);
				add(label);
			}

			@Override
			protected void onClick() {
				if (accepts) {
					submit();
				} else {
					DeploymentConfirmWindow.this.hide();
				}
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
				center(label, x + 3, y, width - 6, height);
			}
		}
	}
}
