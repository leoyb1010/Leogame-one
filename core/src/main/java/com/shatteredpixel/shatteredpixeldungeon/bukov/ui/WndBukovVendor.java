package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovEconomyService;
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
import java.util.Locale;

/** Playable offline buy/sell surface for the hideout vendor. */
public final class WndBukovVendor extends Window {

	private enum Tab {
		BUY,
		SELL
	}

	private static final int WIDTH_P = 150;
	private static final int HEIGHT_P = 218;
	private static final int WIDTH_L = 220;
	private static final int HEIGHT_L = 174;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int ROW_HEIGHT = 19;
	private static final int BUTTON_HEIGHT = 18;
	private static long transactionSequence;

	private final BukovHubController controller;
	private final Callback close;
	private final BukovVendorViewModel viewModel;
	private final BukovUiTokens tokens;
	private final Tab tab;
	private final BukovVendorFocusModel focus;
	private final List<StockRow> rows = new ArrayList<>();
	private final List<ActionButton> actionButtons = new ArrayList<>();
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private final String notice;
	private ScrollPane stockScroll;
	private boolean submitting;
	private boolean closing;
	private String pendingTransactionId;

	public WndBukovVendor(BukovHubController controller, Callback close) {
		this(controller, close, Tab.BUY, 0, null);
	}

	private WndBukovVendor(
			BukovHubController controller,
			Callback close,
			Tab tab,
			int selectedItem,
			String notice) {
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
		this.tab = tab;
		this.notice = notice;
		viewModel = controller.vendorViewModel();
		tokens = BukovUiTokens.loadDefault();
		focus = new BukovVendorFocusModel(rowCount(), selectedItem);

		int windowWidth = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);
		int windowHeight = BukovWindowLayout.safeHeight(
				PixelScene.landscape() ? HEIGHT_L : HEIGHT_P);
		resize(windowWidth, windowHeight);
		build(windowWidth, windowHeight);
		updateFocus();
	}

	private int rowCount() {
		return tab == Tab.BUY
				? viewModel.offers.size()
				: viewModel.stash.size();
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

	private void build(int windowWidth, int windowHeight) {
		ColorBlock header = new ColorBlock(
				windowWidth,
				38,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = 37;
		add(headerRule);

		RenderedTextBlock eyebrow = text(
				"TRADING POST / " + tab.name(),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN, 3);
		add(eyebrow);

		RenderedTextBlock cash = text(
				"现金余额  " + viewModel.currency,
				BukovVisualContract.FONT_BODY,
				tokens.color("accent.valuable"));
		cash.setPos(windowWidth - MARGIN - cash.width(), 3);
		add(cash);

		RenderedTextBlock title = text(
				tab == Tab.BUY ? "购买补给" : "出售仓库物资",
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		title.setPos(MARGIN, 14);
		add(title);

		RenderedTextBlock hint = text(
				viewModel.tradingLocked
						? "行动进行中，交易已锁定"
						: tab == Tab.BUY
						? "选择物资，再确认购买"
						: "仅可出售未配装的仓库物资",
				BukovVisualContract.FONT_BODY,
				viewModel.tradingLocked
						? tokens.color("accent.danger")
						: tokens.color("text.secondary"));
		hint.setPos(MARGIN, 28);
		add(hint);

		int listTop = 40;
		int footer = 34;
		int listHeight = completeRowViewportHeight(
				windowHeight,
				listTop,
				footer);
		StockList list = new StockList(windowWidth - MARGIN * 2);
		stockScroll = new ScrollPane(list);
		// ScrollPane layout requires the parent camera.
		add(stockScroll);
		stockScroll.setRect(
				MARGIN,
				listTop,
				windowWidth - MARGIN * 2,
				listHeight);

		RenderedTextBlock feedback = text(
				notice == null ? selectionSummary() : notice,
				BukovVisualContract.FONT_CAPTION,
				notice == null
						? tokens.color("text.secondary")
						: tokens.color("accent.extract"));
		feedback.setRect(
				MARGIN,
				listTop + listHeight + 2,
				windowWidth - MARGIN * 2,
				9);
		add(feedback);

		float third = (windowWidth - MARGIN * 2 - GAP * 2) / 3f;
		float buttonY = windowHeight - BUTTON_HEIGHT - MARGIN;
		addAction(
				tab == Tab.BUY ? "查看出售" : "查看购买",
				BukovVendorFocusModel.ACTION_TAB,
				MARGIN,
				buttonY,
				third,
				true,
				"accent.interact");
		addAction(
				tradeLabel(),
				BukovVendorFocusModel.ACTION_TRADE,
				MARGIN + third + GAP,
				buttonY,
				third,
				tradeEnabled(),
				tab == Tab.BUY ? "accent.valuable" : "accent.extract");
		addAction(
				"返回基地",
				BukovVendorFocusModel.ACTION_BACK,
				MARGIN + (third + GAP) * 2,
				buttonY,
				third,
				true,
				"panel.border");
	}

	static int completeRowViewportHeight(
			int windowHeight,
			int listTop,
			int footer) {
		int available = Math.max(
				ROW_HEIGHT * 2,
				windowHeight - listTop - footer);
		return Math.max(
				ROW_HEIGHT * 2,
				available / ROW_HEIGHT * ROW_HEIGHT);
	}

	private String selectionSummary() {
		if (rowCount() == 0 || focus.selectedItem() < 0) {
			return tab == Tab.BUY
					? "商店暂无库存"
					: "没有可浏览的未配装物资";
		}
		if (tab == Tab.BUY) {
			BukovVendorViewModel.BuyRow row =
					viewModel.offers.get(focus.selectedItem());
			return row.label + " · 单价 " + row.price
					+ (row.affordable ? "" : " · 资金不足");
		}
		BukovVendorViewModel.SellRow row =
				viewModel.stash.get(focus.selectedItem());
		return row.sellable
				? row.label + " · 回收 " + row.price
				: row.label + " · " + row.blockReason;
	}

	private String tradeLabel() {
		if (viewModel.tradingLocked) {
			return "交易锁定";
		}
		if (rowCount() == 0 || focus.selectedItem() < 0) {
			return tab == Tab.BUY ? "无库存" : "无物资";
		}
		if (tab == Tab.BUY) {
			return viewModel.offers.get(focus.selectedItem()).affordable
					? "确认购买"
					: "资金不足";
		}
		return viewModel.stash.get(focus.selectedItem()).sellable
				? "确认出售"
				: "不可出售";
	}

	private boolean tradeEnabled() {
		if (viewModel.tradingLocked
				|| rowCount() == 0
				|| focus.selectedItem() < 0) {
			return false;
		}
		return tab == Tab.BUY
				? viewModel.offers.get(focus.selectedItem()).affordable
				: viewModel.stash.get(focus.selectedItem()).sellable;
	}

	private void addAction(
			String label,
			int action,
			float x,
			float y,
			float buttonWidth,
			boolean enabled,
			String accentToken) {
		ActionButton button = new ActionButton(
				label,
				action,
				enabled,
				tokens.color(accentToken));
		button.setRect(x, y, buttonWidth, BUTTON_HEIGHT);
		actionButtons.add(button);
		add(button);
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
		return value.substring(0, maxCharacters - 3) + "...";
	}

	private void select(int item) {
		focus.selectItem(item);
		reopen(tab, item, null);
	}

	private void activateAction(int action) {
		switch (action) {
			case BukovVendorFocusModel.ACTION_TAB:
				reopen(tab == Tab.BUY ? Tab.SELL : Tab.BUY, 0, null);
				break;
			case BukovVendorFocusModel.ACTION_TRADE:
				if (tradeEnabled()) {
					trade();
				}
				break;
			default:
				closeToHub();
				break;
		}
	}

	private void trade() {
		if (submitting || !tradeEnabled()) {
			return;
		}
		submitting = true;
		if (pendingTransactionId == null) {
			pendingTransactionId = nextTransactionId(
					tab == Tab.BUY ? "buy" : "sell");
		}
		try {
			BukovEconomyService.Receipt receipt;
			String result;
			if (tab == Tab.BUY) {
				BukovVendorViewModel.BuyRow row =
						viewModel.offers.get(focus.selectedItem());
				receipt = controller.buy(
						pendingTransactionId,
						row.offerId);
				result = receipt.alreadyCommitted
						? "购买已确认 · 余额 " + receipt.balanceAfter
						: "购买完成 · -" + (-receipt.currencyDelta)
								+ " · 余额 " + receipt.balanceAfter;
			} else {
				BukovVendorViewModel.SellRow row =
						viewModel.stash.get(focus.selectedItem());
				receipt = controller.sell(
						pendingTransactionId,
						row.itemUid);
				result = (receipt.alreadyCommitted
						? "出售已确认 · +" : "出售完成 · +")
						+ receipt.currencyDelta
						+ " · 余额 " + receipt.balanceAfter;
			}
			pendingTransactionId = null;
			reopen(tab, focus.selectedItem(), result);
		} catch (IOException | RuntimeException error) {
			submitting = false;
			showError("交易失败", error);
		}
	}

	private static synchronized String nextTransactionId(String operation) {
		transactionSequence++;
		return String.format(
				Locale.ROOT,
				"ui-%s-%d-%d",
				operation,
				System.currentTimeMillis(),
				transactionSequence);
	}

	private void reopen(Tab nextTab, int selectedItem, String result) {
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovVendor(
						controller,
						close,
						nextTab,
						selectedItem,
						result));
	}

	private void closeToHub() {
		if (closing) {
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
		if (!event.pressed) {
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
			if (focus.itemFocused()) {
				select(focus.itemIndex());
			} else {
				activateAction(focus.actionIndex());
			}
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
		closeToHub();
	}

	private void updateFocus() {
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setState(
					i == focus.selectedItem(),
					focus.itemFocused() && i == focus.itemIndex());
		}
		for (int i = 0; i < actionButtons.size(); i++) {
			actionButtons.get(i).setFocused(
					!focus.itemFocused() && i == focus.actionIndex());
		}
		if (focus.itemFocused() && stockScroll != null) {
			float rowY = focus.itemIndex() * ROW_HEIGHT;
			stockScroll.scrollTo(0, Math.max(0, rowY - ROW_HEIGHT));
		}
	}

	private final class StockList extends Component {

		private StockList(float listWidth) {
			int count = Math.max(1, rowCount());
			setSize(listWidth, count * ROW_HEIGHT);
			ColorBlock surface = new ColorBlock(
					listWidth,
					height(),
					tokens.color("panel.surface"));
			addToBack(surface);
			if (rowCount() == 0) {
					RenderedTextBlock empty = text(
						tab == Tab.BUY
								? "库存正在整理"
								: "未配装仓库为空",
							BukovVisualContract.FONT_BODY,
						tokens.color("text.disabled"));
				empty.setRect(4, 5, listWidth - 8, ROW_HEIGHT - 4);
				add(empty);
				return;
			}
			for (int i = 0; i < rowCount(); i++) {
				StockRow row = new StockRow(i);
				row.setRect(
						0,
						i * ROW_HEIGHT,
						listWidth,
						ROW_HEIGHT - 1);
				rows.add(row);
				add(row);
			}
		}
	}

	private final class StockRow extends Button {

		private final int itemIndex;
		private final ColorBlock background;
		private final ColorBlock selection;
		private final ColorBlock focusEdge;
		private final ColorBlock divider;
		private final RenderedTextBlock name;
		private final RenderedTextBlock metrics;
		private final RenderedTextBlock price;
		private final boolean tradeable;

		private StockRow(int itemIndex) {
			this.itemIndex = itemIndex;
			background = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.surface", 220));
			addToBack(background);
			selection = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.valuable", 42));
			add(selection);
			focusEdge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			divider = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.border", 145));
			add(divider);

			String rowName;
			int quantity;
			float weight;
			long value;
			String priceText;
			if (tab == Tab.BUY) {
				BukovVendorViewModel.BuyRow row =
						viewModel.offers.get(itemIndex);
				rowName = row.label;
				quantity = row.quantity;
				weight = row.weight;
				value = row.itemValue;
				priceText = "购买 " + row.price;
				tradeable = row.affordable;
			} else {
				BukovVendorViewModel.SellRow row =
						viewModel.stash.get(itemIndex);
				rowName = row.label;
				quantity = row.quantity;
				weight = row.weight;
				value = row.itemValue;
				priceText = row.sellable ? "回收 " + row.price : "不可出售";
				tradeable = row.sellable;
			}
			name = text(
					compact(rowName, PixelScene.landscape() ? 18 : 12)
							+ " ×" + quantity,
					BukovVisualContract.FONT_BODY,
					tradeable
							? tokens.color("text.primary")
							: tokens.color("text.disabled"));
			add(name);
			metrics = text(
					BukovHubViewModel.formatWeight(weight)
							+ "kg · 价值" + value,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(metrics);
			price = text(
					priceText,
					BukovVisualContract.FONT_BODY,
					tradeable
							? tokens.color("accent.valuable")
							: tokens.color("text.disabled"));
			price.align(RenderedTextBlock.RIGHT_ALIGN);
			add(price);
		}

		private void setState(boolean selected, boolean focused) {
			selection.visible = selected;
			focusEdge.visible = focused;
			name.hardlight(focused
					? tokens.color("accent.interact")
					: tradeable
					? tokens.color("text.primary")
					: tokens.color("text.disabled"));
		}

		@Override
		protected void onClick() {
			select(itemIndex);
		}

		@Override
		protected void layout() {
			super.layout();
			background.x = x;
			background.y = y;
			background.size(width, height);
			selection.x = x;
			selection.y = y;
			selection.size(width, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 1;
			focusEdge.size(width, 2);
			divider.x = x + 4;
			divider.y = y + height - 1;
			divider.size(width - 8, 1);
			price.setPos(x + width - price.width() - 3, y + 5);
			float copyWidth = Math.max(
					1,
					width - price.width() - 11);
			name.maxWidth((int) copyWidth);
			metrics.maxWidth((int) copyWidth);
			name.setPos(x + 5, y + 2);
			metrics.setPos(x + 5, y + 10);
		}
	}

	private final class ActionButton extends Button {

		private final int action;
		private final boolean enabled;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final RenderedTextBlock label;

		private ActionButton(
				String value,
				int action,
				boolean enabled,
				int accent) {
			this.action = action;
			this.enabled = enabled;
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha(
							action == BukovVendorFocusModel.ACTION_TRADE
									? tab == Tab.BUY
											? "accent.valuable"
											: "accent.extract"
									: "panel.surface",
							action == BukovVendorFocusModel.ACTION_TRADE
									&& enabled ? 40 : 255));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1,
					enabled ? accent : tokens.color("text.disabled"));
			add(edge);
			focusEdge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			label = text(
					value,
					BukovVisualContract.FONT_BODY,
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
			label.setPos(
					x + (width - label.width()) / 2f,
					y + (height - label.height()) / 2f);
		}
	}
}
