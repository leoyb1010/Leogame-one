package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.KeyEvent;
import com.watabou.input.ControllerHandler;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Raid-only backpack. It intentionally does not use WndBag, ItemButton or any
 * inherited dungeon inventory chrome.
 */
public final class WndBukovBackpack extends Window {

	public static final class ActionFeedback {
		public final boolean changed;
		public final boolean closeWindow;
		public final String message;

		private ActionFeedback(
				boolean changed,
				boolean closeWindow,
				String message) {
			this.changed = changed;
			this.closeWindow = closeWindow;
			this.message = message == null ? "" : message;
		}

		public static ActionFeedback changed(String message) {
			return new ActionFeedback(true, false, message);
		}

		public static ActionFeedback startedUse(String message) {
			return new ActionFeedback(true, true, message);
		}

		public static ActionFeedback rejected(String message) {
			return new ActionFeedback(false, false, message);
		}
	}

	/**
	 * Narrow adapter implemented by the live raid. setBackpackOpen(true) must
	 * pause fixed-step simulation and block real-time input; false resumes it.
	 */
	public interface Controller {
		BukovBackpackViewModel snapshot();
		ActionFeedback drop(String itemUid);
		ActionFeedback useMedical(String itemUid);
		ActionFeedback equipFirearm(String itemUid);
		void setBackpackOpen(boolean open);
	}

	private static final int WIDTH_P = 154;
	private static final int HEIGHT_P = 218;
	private static final int WIDTH_L = 226;
	private static final int HEIGHT_L = 170;
	private static final int MARGIN = 4;
	private static final int ROW_HEIGHT = 28;
	private static final int BUTTON_HEIGHT = 18;
	private static final int GAP = 2;
	private static final int HEADER_HEIGHT = 33;
	private static final int FOOTER_HEIGHT = 75;
	private static final int VIEWPORT_MARGIN = 4;
	private static final int MIN_LIST_HEIGHT_L = 40;
	private static final int MIN_LIST_HEIGHT_P = 72;

	private final Controller controller;
	private final BukovUiTokens tokens;
	private final List<ItemRowButton> rowButtons = new ArrayList<>();
	private final TacticalButton[] actionButtons =
			new TacticalButton[Action.values().length];
	private BukovBackpackViewModel viewModel;
	private final BukovFocusModel focus;
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private String selectedUid;
	private boolean openSignal;
	private ScrollPane itemScroll;
	private RenderedTextBlock totals;
	private RenderedTextBlock detail;
	private RenderedTextBlock feedback;
	private TacticalButton dropButton;
	private TacticalButton useButton;
	private TacticalButton equipButton;

	public WndBukovBackpack(Controller controller) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		if (controller == null || controller.snapshot() == null) {
			throw new IllegalArgumentException(
					"controller and backpack snapshot are required");
		}
		this.controller = controller;
		viewModel = controller.snapshot();
		tokens = BukovUiTokens.loadDefault();
		if (!viewModel.items.isEmpty()) {
			selectedUid = viewModel.items.get(0).itemUid;
		}
		focus = new BukovFocusModel(
				viewModel.items.size() + Action.values().length,
				viewModel.items.isEmpty()
						? Action.CLOSE.ordinal()
						: 0);

		boolean landscape = PixelScene.landscape();
		int windowWidth = BukovWindowLayout.safeWidth(
				landscape ? WIDTH_L : WIDTH_P);
		int windowHeight = BukovWindowLayout.safeHeight(
				landscape ? HEIGHT_L : HEIGHT_P);
		resize(windowWidth, windowHeight);
		build(windowWidth, windowHeight);
		controller.setBackpackOpen(true);
		openSignal = true;
	}

	private void build(int windowWidth, int windowHeight) {
		ColorBlock header = new ColorBlock(
				windowWidth,
				HEADER_HEIGHT - 1,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = HEADER_HEIGHT - 2;
		add(headerRule);
		RenderedTextBlock title = text(
				"行动背包",
				11,
				tokens.color("accent.valuable"));
		title.setPos(MARGIN, 3);
		add(title);

		RenderedTextBlock code = text(
				"RAID INVENTORY",
				6,
				tokens.color("text.secondary"));
		code.setPos(windowWidth - MARGIN - code.width(), 5);
		add(code);

		ColorBlock totalsSurface = new ColorBlock(
				windowWidth - MARGIN * 2,
				11,
				tokens.colorWithAlpha("ink.background", 190));
		totalsSurface.x = MARGIN;
		totalsSurface.y = 18;
		add(totalsSurface);
		totals = text("", 7, tokens.color("accent.valuable"));
		totals.setRect(MARGIN + 3, 20, windowWidth - MARGIN * 2 - 6, 8);
		add(totals);
		updateTotals();

		float listHeight = inventoryViewportHeight(
				windowHeight,
				PixelScene.landscape());
		createList(windowWidth, listHeight);

		float footerY = HEADER_HEIGHT + listHeight + GAP;
		ColorBlock detailSurface = new ColorBlock(
				windowWidth - MARGIN * 2,
				31,
				tokens.colorWithAlpha("panel.surface", 210));
		detailSurface.x = MARGIN;
		detailSurface.y = footerY;
		add(detailSurface);
		ColorBlock detailEdge = new ColorBlock(
				2,
				31,
				tokens.color("accent.interact"));
		detailEdge.x = MARGIN;
		detailEdge.y = footerY;
		add(detailEdge);
		detail = text("", 7, tokens.color("text.secondary"));
		detail.setRect(MARGIN + 5, footerY + 3,
				windowWidth - MARGIN * 2 - 9, 18);
		add(detail);

		feedback = text("", 6, tokens.color("accent.interact"));
		feedback.setRect(MARGIN + 5, footerY + 21,
				windowWidth - MARGIN * 2 - 9, 8);
		add(feedback);

		float actionY = footerY + 32;
		float third = (windowWidth - MARGIN * 2 - GAP * 2) / 3f;
		dropButton = new TacticalButton("丢弃", Action.DROP);
		dropButton.setRect(MARGIN, actionY, third, BUTTON_HEIGHT);
		add(dropButton);
		actionButtons[Action.DROP.ordinal()] = dropButton;
		useButton = new TacticalButton("使用医疗", Action.USE);
		useButton.setRect(
				MARGIN + third + GAP,
				actionY,
				third,
				BUTTON_HEIGHT);
		add(useButton);
		actionButtons[Action.USE.ordinal()] = useButton;
		equipButton = new TacticalButton("装备", Action.EQUIP);
		equipButton.setRect(
				MARGIN + (third + GAP) * 2,
				actionY,
				third,
				BUTTON_HEIGHT);
		add(equipButton);
		actionButtons[Action.EQUIP.ordinal()] = equipButton;

		TacticalButton close = new TacticalButton("关闭背包 · 返回行动", Action.CLOSE);
		close.setRect(
				MARGIN,
				actionY + BUTTON_HEIGHT + GAP,
				windowWidth - MARGIN * 2,
				BUTTON_HEIGHT);
		add(close);
		actionButtons[Action.CLOSE.ordinal()] = close;
		updateSelection();
		updateFocus();
	}

	private void createList(float windowWidth, float listHeight) {
		InventoryList list = new InventoryList(windowWidth - MARGIN * 2);
		itemScroll = new ScrollPane(list);
		add(itemScroll);
		itemScroll.setRect(
				MARGIN,
				HEADER_HEIGHT,
				windowWidth - MARGIN * 2,
				listHeight);
	}

	private void refreshAfterAction() {
		int previousItemCount = viewModel.items.size();
		int previousIndex = focus.index();
		int previousAction = previousIndex >= previousItemCount
				? previousIndex - previousItemCount
				: -1;
		viewModel = controller.snapshot();
		if (viewModel.find(selectedUid) == null) {
			selectedUid = viewModel.items.isEmpty()
					? null
					: viewModel.items.get(0).itemUid;
		}
		float scrollX = itemScroll.left();
		float scrollY = itemScroll.top();
		float scrollWidth = itemScroll.width();
		float scrollHeight = itemScroll.height();
		remove(itemScroll);
		itemScroll.destroy();
		rowButtons.clear();
		InventoryList list = new InventoryList(scrollWidth);
		itemScroll = new ScrollPane(list);
		add(itemScroll);
		itemScroll.setRect(scrollX, scrollY, scrollWidth, scrollHeight);
		focus.setCount(viewModel.items.size() + Action.values().length);
		if (previousAction >= 0) {
			focus.focus(viewModel.items.size() + Math.min(
					previousAction,
					Action.values().length - 1));
		} else if (!viewModel.items.isEmpty()) {
			focus.focus(Math.min(previousIndex, viewModel.items.size() - 1));
		} else {
			focus.focus(viewModel.items.size() + Action.CLOSE.ordinal());
		}
		updateTotals();
		updateSelection();
		updateFocus();
	}

	private void updateTotals() {
		totals.text("负重 " + viewModel.weightSummary()
				+ "    携带价值 " + viewModel.totalValue);
	}

	private void updateSelection() {
		BukovBackpackViewModel.ItemRow selected =
				viewModel.find(selectedUid);
		for (ItemRowButton row : rowButtons) {
			row.setSelected(
					selected != null
							&& row.item.itemUid.equals(selected.itemUid));
		}
		if (selected == null) {
			detail.text("背包为空 · 搜索容器与地面物资补给本次行动");
			dropButton.setEnabled(false);
			useButton.setEnabled(false);
			equipButton.setEnabled(false);
			return;
		}
		detail.text(selected.stateSummary());
		dropButton.setEnabled(selected.canDrop);
		useButton.setEnabled(selected.canUse);
		equipButton.setEnabled(selected.canEquip);
	}

	private void runAction(Action action) {
		if (action == Action.CLOSE) {
			hide();
			return;
		}
		BukovBackpackViewModel.ItemRow selected =
				viewModel.find(selectedUid);
		if (selected == null) {
			feedback.text("没有选中物品");
			return;
		}
		ActionFeedback result;
		switch (action) {
			case DROP:
				if (!selected.canDrop) {
					result = ActionFeedback.rejected("任务档案不可丢弃");
				} else {
					result = controller.drop(selected.itemUid);
				}
				break;
			case USE:
				result = selected.canUse
						? controller.useMedical(selected.itemUid)
						: ActionFeedback.rejected("该物品不能直接使用");
				break;
			default:
				result = selected.canEquip
						? controller.equipFirearm(selected.itemUid)
						: ActionFeedback.rejected(
								selected.equipped
										? "该武器已经装备"
										: "请选择另一把武器");
				break;
		}
		if (result == null) {
			result = ActionFeedback.rejected("操作未完成");
		}
		feedback.text(result.message);
		if (result.closeWindow) {
			hide();
		} else if (result.changed) {
			refreshAfterAction();
		}
	}

	private RenderedTextBlock text(String value, int size, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(value, size);
		result.maxWidth(width - MARGIN * 2);
		result.hardlight(color);
		return result;
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
		if (BukovNavigation.inventory(event)
				|| BukovNavigation.back(event)) {
			hide();
			return true;
		} else if (BukovNavigation.previous(event)) {
			focus.move(-1, focusMask());
			updateFocus();
		} else if (BukovNavigation.next(event)) {
			focus.move(1, focusMask());
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
			focus.move(delta, focusMask());
			updateFocus();
		}
	}

	private void activateFocused() {
		int itemCount = viewModel.items.size();
		if (focus.index() < itemCount) {
			selectedUid = viewModel.items.get(focus.index()).itemUid;
			feedback.text("");
			updateSelection();
			updateFocus();
			return;
		}
		Action action = Action.values()[focus.index() - itemCount];
		TacticalButton button = actionButtons[action.ordinal()];
		if (button != null && button.enabled) {
			runAction(action);
		}
	}

	private boolean[] focusMask() {
		int itemCount = viewModel.items.size();
		boolean[] enabled = new boolean[
				itemCount + Action.values().length];
		for (int i = 0; i < itemCount; i++) {
			enabled[i] = true;
		}
		for (Action action : Action.values()) {
			TacticalButton button = actionButtons[action.ordinal()];
			enabled[itemCount + action.ordinal()] =
					button != null && button.enabled;
		}
		return enabled;
	}

	private void updateFocus() {
		int itemCount = viewModel.items.size();
		for (int i = 0; i < rowButtons.size(); i++) {
			rowButtons.get(i).setFocused(focus.index() == i);
		}
		for (Action action : Action.values()) {
			TacticalButton button = actionButtons[action.ordinal()];
			if (button != null) {
				button.setFocused(
						focus.index() == itemCount + action.ordinal());
			}
		}
		if (focus.index() < itemCount && itemScroll != null) {
			float rowY = focus.index() * ROW_HEIGHT;
			itemScroll.scrollTo(0, Math.max(0, rowY - ROW_HEIGHT));
		}
	}

	@Override
	public void destroy() {
		if (openSignal) {
			openSignal = false;
			controller.setBackpackOpen(false);
		}
		super.destroy();
	}

	static int inventoryViewportHeight(int windowHeight, boolean landscape) {
		int available = windowHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
		return Math.max(1, available);
	}

	static boolean fitsWindow(int windowHeight, boolean landscape) {
		return HEADER_HEIGHT
				+ inventoryViewportHeight(windowHeight, landscape)
				+ FOOTER_HEIGHT <= windowHeight;
	}

	static int windowWidthFor(int viewportWidth, boolean landscape) {
		int desired = landscape ? WIDTH_L : WIDTH_P;
		return Math.max(
				1,
				Math.min(desired, viewportWidth - VIEWPORT_MARGIN * 2));
	}

	static int windowHeightFor(int viewportHeight, boolean landscape) {
		int desired = landscape ? HEIGHT_L : HEIGHT_P;
		return Math.max(
				1,
				Math.min(desired, viewportHeight - VIEWPORT_MARGIN * 2));
	}

	static boolean fitsViewport(
			int viewportWidth,
			int viewportHeight,
			boolean landscape) {
		int windowWidth = windowWidthFor(viewportWidth, landscape);
		int windowHeight = windowHeightFor(viewportHeight, landscape);
		return windowWidth + VIEWPORT_MARGIN * 2 <= viewportWidth
				&& windowHeight + VIEWPORT_MARGIN * 2 <= viewportHeight
				&& fitsWindow(windowHeight, landscape);
	}

	private enum Action {
		DROP,
		USE,
		EQUIP,
		CLOSE
	}

	private final class InventoryList extends Component {

		private InventoryList(float listWidth) {
			int rows = Math.max(1, viewModel.items.size());
			setSize(listWidth, rows * ROW_HEIGHT);
			ColorBlock surface = new ColorBlock(
					listWidth,
					height(),
					tokens.color("panel.surface"));
			addToBack(surface);
			if (viewModel.items.isEmpty()) {
				RenderedTextBlock empty = text(
						"背包为空 · 靠近物资后交互拾取",
						7,
						tokens.color("text.disabled"));
				empty.setRect(4, 8, listWidth - 8, 12);
				add(empty);
				return;
			}
			for (int i = 0; i < viewModel.items.size(); i++) {
				ItemRowButton row = new ItemRowButton(
						viewModel.items.get(i),
						i);
				row.setRect(
						0,
						i * ROW_HEIGHT,
						listWidth,
						ROW_HEIGHT - 1);
				rowButtons.add(row);
				add(row);
			}
		}
	}

	private final class ItemRowButton extends Button {

		private final BukovBackpackViewModel.ItemRow item;
		private final int itemIndex;
		private final ColorBlock background;
		private final ColorBlock stateSurface;
		private final ColorBlock edge;
		private final ColorBlock divider;
		private final RenderedTextBlock category;
		private final BukovItemSprite icon;
		private final RenderedTextBlock title;
		private final RenderedTextBlock metrics;
		private boolean selected;
		private boolean focused;

		private ItemRowButton(
				BukovBackpackViewModel.ItemRow item,
				int itemIndex) {
			this.item = item;
			this.itemIndex = itemIndex;
			background = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.surface", 210));
			addToBack(background);
			stateSurface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.extract", 30));
			stateSurface.visible = false;
			addToBack(stateSurface);
			edge = new ColorBlock(1, 1,
					tokens.color("text.disabled"));
			add(edge);
			divider = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.border", 145));
			add(divider);
			category = text(
					item.category.code,
					6,
					tokens.color("text.secondary"));
			add(category);
			icon = new BukovItemSprite();
			icon.view(BukovItemSprite.frameForDefinition(item.definitionId));
			add(icon);
			title = text(
					item.title(),
					7,
					item.equipped
							? tokens.color("accent.extract")
							: tokens.color("text.primary"));
			add(title);
			metrics = text(
					item.rowEconomySummary(),
					6,
					tokens.color("text.disabled"));
			add(metrics);
		}

		private void setSelected(boolean selected) {
			this.selected = selected;
			updateState();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			updateState();
		}

		private void updateState() {
			stateSurface.visible = focused || selected;
			stateSurface.hardlight(focused
					? tokens.color("accent.interact")
					: tokens.color("accent.extract"));
			edge.hardlight(focused
					? tokens.color("accent.interact")
					: selected
					? tokens.color("accent.extract")
					: tokens.color("text.disabled"));
			title.hardlight(focused
					? tokens.color("accent.interact")
					: item.equipped
					? tokens.color("accent.extract")
					: tokens.color("text.primary"));
		}

		@Override
		protected void onClick() {
			focus.focus(itemIndex);
			selectedUid = item.itemUid;
			feedback.text("");
			updateSelection();
			updateFocus();
		}

		@Override
		protected void layout() {
			super.layout();
			background.x = x;
			background.y = y;
			background.size(width, height);
			stateSurface.x = x;
			stateSurface.y = y;
			stateSurface.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2, height);
			divider.x = x + 4;
			divider.y = y + height - 1;
			divider.size(width - 8, 1);
			category.setPos(x + 5, y + 4);
			icon.x = x + 18;
			icon.y = y + 2;
			title.maxWidth(Math.max(1, (int) width - 43));
			title.setPos(x + 37, y + 3);
			metrics.maxWidth(Math.max(1, (int) width - 10));
			metrics.setPos(x + 5, y + 14);
		}
	}

	private final class TacticalButton extends Button {

		private final Action action;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final RenderedTextBlock label;
		private final ColorBlock focusEdge;
		private boolean enabled = true;

		private TacticalButton(String value, Action action) {
			this.action = action;
			surface = new ColorBlock(1, 1,
					tokens.colorWithAlpha(
							action == Action.CLOSE
									? "accent.extract"
									: "panel.surface",
							action == Action.CLOSE ? 36 : 255));
			addToBack(surface);
			edge = new ColorBlock(1, 1,
					action == Action.CLOSE
							? tokens.color("accent.extract")
							: tokens.color("accent.interact"));
			add(edge);
			focusEdge = new ColorBlock(
					1,
					1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(value, 7, tokens.color("text.primary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		private void setEnabled(boolean enabled) {
			this.enabled = enabled;
			label.hardlight(enabled
					? tokens.color("text.primary")
					: tokens.color("text.disabled"));
			edge.hardlight(enabled
					? action == Action.CLOSE
					? tokens.color("accent.extract")
					: tokens.color("accent.interact")
					: tokens.color("panel.border"));
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
				focus.focus(viewModel.items.size() + action.ordinal());
				updateFocus();
				runAction(action);
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
			label.setRect(
					x + 3,
					y + (height - 9) / 2f,
					width - 6,
					9);
		}
	}
}
