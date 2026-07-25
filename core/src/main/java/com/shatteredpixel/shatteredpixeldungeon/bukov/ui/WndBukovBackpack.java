package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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
import com.watabou.utils.DeviceCompat;

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
	private static final int BUTTON_HEIGHT = 22;
	private static final int GAP = 2;
	private static final float ACTION_ICON_SIZE = 10f;
	private static final float ACTION_ICON_LABEL_GAP = 2f;
	private static final int MIN_HEADER_HEIGHT = 33;
	private static final int FOOTER_HEIGHT = 83;
	private static final int VIEWPORT_MARGIN = 4;
	private static final int MIN_LIST_HEIGHT_L = 40;
	private static final int MIN_LIST_HEIGHT_P = 72;
	private static final int HEADER_TOP = 3;
	private static final int HEADER_LABEL_GAP = 1;
	private static final int HEADER_TOTALS_GAP = 3;
	private static final int HEADER_BOTTOM = 4;
	private static final int HEADER_INLINE_GAP = 5;
	private static final int DETAIL_PADDING_X = 5;
	private static final int DETAIL_PADDING_Y = 3;
	private static final int DETAIL_MIN_HEIGHT_L = 32;
	private static final int DETAIL_MIN_HEIGHT_P = 38;
	private static final int DETAIL_MAX_HEIGHT_L = 46;
	private static final int DETAIL_MAX_HEIGHT_P = 58;
	private static final int ACTION_STACK_HEIGHT =
			BUTTON_HEIGHT * 2 + GAP;
	private static final int FOOTER_FIXED_HEIGHT =
			GAP + GAP + ACTION_STACK_HEIGHT + MARGIN;

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
	private ScrollPane detailScroll;
	private DetailContent detailContent;
	private RenderedTextBlock totals;
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
		RenderedTextBlock title = text(
				BukovMessages.get("bukov.raid.backpack.title"),
				BukovVisualContract.FONT_BODY,
				tokens.color("accent.valuable"));
		RenderedTextBlock code = text(
				BukovMessages.get(pausedHintKey(
						DeviceCompat.isDesktop())),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		totals = text(
				"", BukovVisualContract.FONT_BODY,
				tokens.color("accent.valuable"));
		totals.maxWidth(Math.max(1, windowWidth - MARGIN * 2 - 6));
		updateTotals();

		int innerWidth = Math.max(1, windowWidth - MARGIN * 2);
		boolean inlineHeader = headerFitsInline(
				windowWidth,
				title.width(),
				code.width());
		fitSingleLine(code, code.text(), innerWidth);
		int titleY = HEADER_TOP;
		int codeY = inlineHeader
				? HEADER_TOP + 2
				: HEADER_TOP + (int)Math.ceil(title.height())
						+ HEADER_LABEL_GAP;
		int labelsBottom = (int)Math.ceil(Math.max(
				titleY + title.height(),
				codeY + code.height()));
		int totalsY = labelsBottom + HEADER_TOTALS_GAP;
		LayoutMetrics layout = layoutFor(
				windowWidth,
				windowHeight,
				PixelScene.landscape(),
				totalsY,
				(int)Math.ceil(totals.height()),
				estimatedDetailContentHeight(windowWidth));

		ColorBlock header = new ColorBlock(
				windowWidth,
				layout.headerHeight - 1,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = layout.headerHeight - 2;
		add(headerRule);
		title.setPos(MARGIN, titleY);
		add(title);
		code.setPos(
				inlineHeader
						? windowWidth - MARGIN - code.width()
						: MARGIN,
				codeY);
		add(code);

		ColorBlock totalsSurface = new ColorBlock(
				windowWidth - MARGIN * 2,
				Math.max(1, layout.headerHeight - totalsY - HEADER_BOTTOM + 1),
				tokens.colorWithAlpha("ink.background", 190));
		totalsSurface.x = MARGIN;
		totalsSurface.y = totalsY - 1;
		add(totalsSurface);
		totals.setPos(MARGIN + 3, totalsY);
		add(totals);

		createList(windowWidth, layout.headerHeight, layout.listHeight);

		float footerY = layout.headerHeight + layout.listHeight + GAP;
		ColorBlock detailSurface = new ColorBlock(
				windowWidth - MARGIN * 2,
				layout.detailHeight,
				tokens.colorWithAlpha("panel.surface", 210));
		detailSurface.x = MARGIN;
		detailSurface.y = footerY;
		add(detailSurface);
		ColorBlock detailEdge = new ColorBlock(
				2,
				layout.detailHeight,
				tokens.color("accent.interact"));
		detailEdge.x = MARGIN;
		detailEdge.y = footerY;
		add(detailEdge);
		int detailWidth = Math.max(
				1,
				windowWidth - MARGIN * 2 - DETAIL_PADDING_X - 4);
		detailContent = new DetailContent(detailWidth);
		detailContent.setMessages(
				selectedDetail(),
				"");
		detailScroll = new ScrollPane(detailContent);
		add(detailScroll);
		detailScroll.setRect(
				MARGIN + DETAIL_PADDING_X,
				footerY + DETAIL_PADDING_Y,
				detailWidth,
				Math.max(1,
						layout.detailHeight - DETAIL_PADDING_Y * 2));
		detailContent.setMinimumHeight(detailScroll.height());

		float actionY = footerY + layout.detailHeight + GAP;
		float third = (windowWidth - MARGIN * 2 - GAP * 2) / 3f;
		dropButton = new TacticalButton(
				BukovMessages.get("bukov.raid.backpack.drop"),
				Action.DROP);
		dropButton.setRect(MARGIN, actionY, third, BUTTON_HEIGHT);
		add(dropButton);
		actionButtons[Action.DROP.ordinal()] = dropButton;
		useButton = new TacticalButton(
				BukovMessages.get("bukov.raid.backpack.use_medical"),
				Action.USE);
		useButton.setRect(
				MARGIN + third + GAP,
				actionY,
				third,
				BUTTON_HEIGHT);
		add(useButton);
		actionButtons[Action.USE.ordinal()] = useButton;
		equipButton = new TacticalButton(
				BukovMessages.get("bukov.raid.backpack.equip"),
				Action.EQUIP);
		equipButton.setRect(
				MARGIN + (third + GAP) * 2,
				actionY,
				third,
				BUTTON_HEIGHT);
		add(equipButton);
		actionButtons[Action.EQUIP.ordinal()] = equipButton;

		TacticalButton close = new TacticalButton(
				BukovMessages.get("bukov.raid.backpack.close"),
				Action.CLOSE);
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

	private void createList(
			float windowWidth,
			float listY,
			float listHeight) {
		InventoryList list = new InventoryList(windowWidth - MARGIN * 2);
		itemScroll = new ScrollPane(list);
		add(itemScroll);
		itemScroll.setRect(
				MARGIN,
				listY,
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
		totals.text(viewModel.totalsSummary());
	}

	private String selectedDetail() {
		BukovBackpackViewModel.ItemRow selected =
				viewModel.find(selectedUid);
		return selected == null
				? BukovMessages.get("bukov.raid.backpack.empty_detail")
				: selected.stateSummary();
	}

	private int estimatedDetailContentHeight(int windowWidth) {
		int contentWidth = Math.max(
				1,
				windowWidth - MARGIN * 2 - DETAIL_PADDING_X - 4);
		RenderedTextBlock measurement = text(
				selectedDetail(),
				BukovVisualContract.FONT_BODY,
				tokens.color("text.secondary"));
		measurement.maxWidth(contentWidth);
		int height = Math.max(1, (int)Math.ceil(measurement.height()));
		measurement.destroy();
		return height;
	}

	private void setDetail(String value) {
		if (detailContent == null) {
			return;
		}
		detailContent.setDetail(value);
		if (detailScroll != null) {
			detailScroll.scrollTo(0, 0);
		}
	}

	private void setFeedback(String value) {
		if (detailContent == null) {
			return;
		}
		detailContent.setFeedback(value);
		if (detailScroll != null) {
			detailScroll.scrollTo(
					0,
					Math.max(0,
							detailContent.height()
									- detailScroll.height()));
		}
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
			setDetail(BukovMessages.get(
					"bukov.raid.backpack.empty_detail"));
			dropButton.setEnabled(false);
			useButton.setEnabled(false);
			equipButton.setEnabled(false);
			return;
		}
		setDetail(selected.stateSummary());
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
			setFeedback(BukovMessages.get(
					"bukov.raid.backpack.no_selection"));
			return;
		}
		ActionFeedback result;
		switch (action) {
			case DROP:
				if (!selected.canDrop) {
					result = ActionFeedback.rejected(BukovMessages.get(
							"bukov.raid.backpack.mission_no_drop"));
				} else {
					result = controller.drop(selected.itemUid);
				}
				break;
			case USE:
				result = selected.canUse
						? controller.useMedical(selected.itemUid)
						: ActionFeedback.rejected(BukovMessages.get(
								"bukov.raid.backpack.cannot_use"));
				break;
			default:
				result = selected.canEquip
						? controller.equipFirearm(selected.itemUid)
						: ActionFeedback.rejected(
								selected.equipped
										? BukovMessages.get(
												"bukov.raid.backpack.already_equipped")
										: BukovMessages.get(
												"bukov.raid.backpack.select_another_weapon"));
				break;
		}
		if (result == null) {
			result = ActionFeedback.rejected(BukovMessages.get(
					"bukov.raid.backpack.action_incomplete"));
		}
		setFeedback(result.message);
		if (result.closeWindow) {
			hide();
		} else if (result.changed) {
			refreshAfterAction();
		}
	}

	private RenderedTextBlock text(
			String value, String typography, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				value, tokens.scaledTypographyPx(typography));
		result.maxWidth(width - MARGIN * 2);
		result.hardlight(color);
		return result;
	}

	/**
	 * RenderedTextBlock wraps by language-aware tokens but does not clip to the
	 * component rectangle. Rows and buttons are fixed-height interaction
	 * targets, so fit their copy to one measured line instead of allowing the
	 * glyphs to paint over the following control.
	 */
	private static void fitSingleLine(
			RenderedTextBlock block,
			String value,
			int maximumWidth) {
		int width = Math.max(1, maximumWidth);
		String normalized = value == null ? "" : value.trim();
		block.maxWidth(width);
		if (normalized.isEmpty()) {
			block.text(" ");
			block.visible = false;
			return;
		}
		block.visible = true;
		block.text(normalized);
		if (block.nLines <= 1 && block.width() <= width) {
			return;
		}
		int codePoints = normalized.codePointCount(0, normalized.length());
		int low = 1;
		int high = Math.max(1, codePoints - 1);
		String best = "…";
		while (low <= high) {
			int keep = (low + high) >>> 1;
			int end = normalized.offsetByCodePoints(0, keep);
			String candidate = normalized.substring(0, end).trim() + "…";
			block.text(candidate);
			if (block.nLines <= 1 && block.width() <= width) {
				best = candidate;
				low = keep + 1;
			} else {
				high = keep - 1;
			}
		}
		block.text(best);
		if (block.nLines > 1 || block.width() > width) {
			block.text(" ");
			block.visible = false;
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
			setFeedback("");
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

	static final class LayoutMetrics {
		final int headerHeight;
		final int listHeight;
		final int detailHeight;
		final int footerHeight;

		private LayoutMetrics(
				int headerHeight,
				int listHeight,
				int detailHeight,
				int footerHeight) {
			this.headerHeight = headerHeight;
			this.listHeight = listHeight;
			this.detailHeight = detailHeight;
			this.footerHeight = footerHeight;
		}
	}

	static boolean headerFitsInline(
			int windowWidth,
			float titleWidth,
			float codeWidth) {
		if (windowWidth <= 0 || titleWidth < 0f || codeWidth < 0f) {
			throw new IllegalArgumentException(
					"header dimensions must be non-negative");
		}
		int innerWidth = Math.max(1, windowWidth - MARGIN * 2);
		return titleWidth + codeWidth + HEADER_INLINE_GAP <= innerWidth;
	}

	static String pausedHintKey(boolean desktop) {
		return desktop
				? "bukov.raid.backpack.paused_hint"
				: "bukov.raid.backpack.paused_hint_touch";
	}

	/**
	 * Pure layout policy used after the actual localized text has been
	 * measured. The action stack is reserved first so the close control cannot
	 * fall below the safe viewport; detail copy gets a bounded scroll card and
	 * the inventory receives the remaining height.
	 */
	static LayoutMetrics layoutFor(
			int windowWidth,
			int windowHeight,
			boolean landscape,
			int totalsY,
			int totalsTextHeight,
			int detailContentHeight) {
		if (windowWidth <= 0 || windowHeight <= 0
				|| totalsY < 0 || totalsTextHeight < 0
				|| detailContentHeight < 0) {
			throw new IllegalArgumentException(
					"backpack layout dimensions must be non-negative");
		}
		int desiredHeader = Math.max(
				MIN_HEADER_HEIGHT,
				totalsY + Math.max(1, totalsTextHeight)
						+ HEADER_BOTTOM);
		int absoluteFooter = FOOTER_FIXED_HEIGHT + 1;
		int headerHeight = Math.min(
				desiredHeader,
				Math.max(1, windowHeight - absoluteFooter - 1));
		int minimumList = landscape
				? MIN_LIST_HEIGHT_L : MIN_LIST_HEIGHT_P;
		int availableDetail = windowHeight
				- headerHeight
				- minimumList
				- FOOTER_FIXED_HEIGHT;
		int minimumDetail = landscape
				? DETAIL_MIN_HEIGHT_L : DETAIL_MIN_HEIGHT_P;
		int maximumDetail = landscape
				? DETAIL_MAX_HEIGHT_L : DETAIL_MAX_HEIGHT_P;
		int desiredDetail = Math.max(
				minimumDetail,
				detailContentHeight + DETAIL_PADDING_Y * 2);
		int detailHeight = Math.max(
				1,
				Math.min(
						desiredDetail,
						Math.min(
								maximumDetail,
								Math.max(1, availableDetail))));
		int footerHeight = FOOTER_FIXED_HEIGHT + detailHeight;
		int listHeight = Math.max(
				1,
				windowHeight - headerHeight - footerHeight);
		return new LayoutMetrics(
				headerHeight,
				listHeight,
				detailHeight,
				footerHeight);
	}

	static int inventoryViewportHeight(int windowHeight, boolean landscape) {
		int available = windowHeight - MIN_HEADER_HEIGHT - FOOTER_HEIGHT;
		return Math.max(1, available);
	}

	static boolean fitsWindow(int windowHeight, boolean landscape) {
		return MIN_HEADER_HEIGHT
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

	private static BukovTouchIcon.Glyph actionGlyph(Action action) {
		switch (action) {
			case DROP:
				return BukovTouchIcon.Glyph.DROP;
			case USE:
				return BukovTouchIcon.Glyph.MEDICAL;
			case EQUIP:
				return BukovTouchIcon.Glyph.INTERACT;
			case CLOSE:
			default:
				return BukovTouchIcon.Glyph.BACK;
		}
	}

	private static String shortActionLabel(String value) {
		String normalized = value == null ? "" : value.trim();
		int separator = normalized.indexOf('·');
		if (separator > 0) {
			normalized = normalized.substring(0, separator).trim();
		}
		int whitespace = normalized.indexOf(' ');
		if (whitespace > 0) {
			normalized = normalized.substring(0, whitespace);
		}
		int codePoints = normalized.codePointCount(0, normalized.length());
		int maximum = normalized.matches("\\p{ASCII}*") ? 7 : 4;
		if (codePoints <= maximum) {
			return normalized;
		}
		return normalized.substring(
				0,
				normalized.offsetByCodePoints(0, maximum - 1))
				+ "…";
	}

	private final class DetailContent extends Component {

		private final int contentWidth;
		private final RenderedTextBlock detail;
		private final RenderedTextBlock feedback;
		private float minimumHeight;

		private DetailContent(int contentWidth) {
			this.contentWidth = Math.max(1, contentWidth);
			detail = text(
					" ",
					BukovVisualContract.FONT_BODY,
					tokens.color("text.secondary"));
			detail.maxWidth(this.contentWidth);
			add(detail);
			feedback = text(
					" ",
					BukovVisualContract.FONT_CAPTION,
					tokens.color("accent.interact"));
			feedback.maxWidth(this.contentWidth);
			feedback.visible = false;
			add(feedback);
			reflow();
		}

		private void setMinimumHeight(float minimumHeight) {
			this.minimumHeight = Math.max(1f, minimumHeight);
			reflow();
		}

		private void setMessages(
				String detailValue,
				String feedbackValue) {
			setDetailText(detailValue);
			setFeedbackText(feedbackValue);
			reflow();
		}

		private void setDetail(String value) {
			setDetailText(value);
			reflow();
		}

		private void setFeedback(String value) {
			setFeedbackText(value);
			reflow();
		}

		private void setDetailText(String value) {
			String normalized = value == null ? "" : value.trim();
			detail.visible = !normalized.isEmpty();
			detail.text(normalized.isEmpty() ? " " : normalized);
			detail.maxWidth(contentWidth);
		}

		private void setFeedbackText(String value) {
			String normalized = value == null ? "" : value.trim();
			feedback.visible = !normalized.isEmpty();
			feedback.text(normalized.isEmpty() ? " " : normalized);
			feedback.maxWidth(contentWidth);
		}

		private void reflow() {
			detail.setPos(0, 0);
			float cursor = detail.visible ? detail.height() : 0f;
			if (feedback.visible) {
				if (cursor > 0f) {
					cursor += GAP;
				}
				feedback.setPos(0, cursor);
				cursor += feedback.height();
			}
			setSize(contentWidth, Math.max(minimumHeight, Math.max(1f, cursor)));
		}
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
						BukovMessages.get(
								"bukov.raid.backpack.empty_list"),
					BukovVisualContract.FONT_BODY,
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
					item.category.label,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(category);
			icon = new BukovItemSprite();
			icon.view(BukovItemSprite.frameForDefinition(item.definitionId));
			add(icon);
			title = text(
					item.title(),
					BukovVisualContract.FONT_BODY,
					item.equipped
							? tokens.color("accent.extract")
							: tokens.color("text.primary"));
			add(title);
			metrics = text(
					item.rowEconomySummary(),
					BukovVisualContract.FONT_CAPTION,
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
			setFeedback("");
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
			fitSingleLine(
					title,
					item.title(),
					Math.max(1, (int)width - 43));
			title.setPos(x + 37, y + 3);
			fitSingleLine(
					metrics,
					item.rowEconomySummary(),
					Math.max(1, (int)width - 10));
			metrics.setPos(x + 5, y + 14);
		}
	}

	private final class TacticalButton extends Button {

		private final Action action;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;
		private final ColorBlock focusEdge;
		private final String labelValue;
		private boolean enabled = true;
		private boolean focused;
		private boolean pointerPressed;

		private TacticalButton(String value, Action action) {
			this.action = action;
			labelValue = shortActionLabel(value);
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
			icon = new BukovTouchIcon(
					actionGlyph(action),
					tokens.color("text.primary"),
					tokens.color("accent.interact"),
					tokens.color("text.disabled"));
			add(icon);
			label = text(
					labelValue, BukovVisualContract.FONT_CAPTION,
					tokens.color("text.primary"));
			add(label);
		}

		private void setEnabled(boolean enabled) {
			this.enabled = enabled;
			if (!enabled) {
				pointerPressed = false;
			}
			label.hardlight(enabled
					? tokens.color("text.primary")
					: tokens.color("text.disabled"));
			edge.hardlight(enabled
					? action == Action.CLOSE
					? tokens.color("accent.extract")
						: tokens.color("accent.interact")
						: tokens.color("panel.border"));
			updateIconState();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			focusEdge.visible = focused;
			label.hardlight(!enabled
					? tokens.color("text.disabled")
					: focused
						? tokens.color("accent.interact")
						: tokens.color("text.primary"));
			updateIconState();
		}

		private void updateIconState() {
			icon.visualState(
					enabled && pointerPressed,
					!enabled);
		}

		@Override
		protected void onPointerDown() {
			if (!enabled) {
				return;
			}
			pointerPressed = true;
			updateIconState();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			updateIconState();
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
			int labelWidth = Math.max(
					1,
					(int)(width - ACTION_ICON_SIZE
							- ACTION_ICON_LABEL_GAP - 8f));
			fitSingleLine(
					label,
					labelValue,
					labelWidth);
			float contentWidth = ACTION_ICON_SIZE
					+ ACTION_ICON_LABEL_GAP + label.width();
			float contentX = x + (width - contentWidth) / 2f;
			icon.setRect(
					contentX,
					y + (height - ACTION_ICON_SIZE) / 2f,
					ACTION_ICON_SIZE,
					ACTION_ICON_SIZE);
			label.setPos(
					contentX + ACTION_ICON_SIZE
							+ ACTION_ICON_LABEL_GAP,
					y + (height - label.height()) / 2f);
		}
	}
}
