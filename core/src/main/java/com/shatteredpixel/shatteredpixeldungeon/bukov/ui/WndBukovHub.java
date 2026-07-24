package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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
import com.watabou.utils.DeviceCompat;

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
	private static final float MIN_MOBILE_CONTROL_HEIGHT = 22f;
	private static final int GAP = 2;
	private static final int MARGIN = 4;
	private static final int INVENTORY_TOP_P = 124;
	private static final int INVENTORY_TOP_L = 97;
	private static final int INVENTORY_TOP_L_COMPACT = 66;
	private static final int FOOTER_RESERVED = 60;
	private static final int FOOTER_RESERVED_COMPACT = 48;

	private final BukovHubController controller;
	private final Callback deploy;
	private final Callback closed;
	private final BukovHubViewModel viewModel;
	private final BukovHubViewModel.InventoryFilter inventoryFilter;
	private final BukovHubViewModel.InventorySort inventorySort;
	private final String inventoryQuery;
	private final List<BukovHubViewModel.ItemRow> inventoryItems;
	private final BukovHubFocusModel focus;
	private final BukovUiTokens tokens;
	private final List<LoadoutRow> itemRows = new ArrayList<>();
	private final List<TacticalButton> actionButtons = new ArrayList<>();
	private ScrollPane itemScroll;
	private ModeSelectButton modeButton;
	private FilterCycleButton filterButton;
	private SortCycleButton sortButton;
	private InventorySearchButton searchButton;
	private float actionButtonHeight = BUTTON_HEIGHT;
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();

	public WndBukovHub(BukovHubController controller, Callback deploy) {
		this(controller, deploy, () -> {
		});
	}

	public WndBukovHub(
			BukovHubController controller,
			Callback deploy,
			Callback closed) {
		this(
				controller,
				deploy,
				closed,
				0,
				BukovHubViewModel.InventoryFilter.ALL,
				BukovHubViewModel.InventorySort.STASH_ORDER,
				"");
	}

	private WndBukovHub(
			BukovHubController controller,
			Callback deploy,
			Callback closed,
			int restoredFocus,
			BukovHubViewModel.InventoryFilter inventoryFilter,
			BukovHubViewModel.InventorySort inventorySort,
			String inventoryQuery) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		if (controller == null) {
			throw new IllegalArgumentException("controller is required");
		}
		if (deploy == null
				|| closed == null
				|| inventoryFilter == null
				|| inventorySort == null
				|| inventoryQuery == null) {
			throw new IllegalArgumentException(
					"deploy callback and inventory view state are required");
		}
		this.controller = controller;
		this.deploy = deploy;
		this.closed = closed;
		this.inventoryFilter = inventoryFilter;
		this.inventorySort = inventorySort;
		this.inventoryQuery = inventoryQuery;
		viewModel = controller.viewModel();
		inventoryItems = viewModel.inventoryItems(
				inventoryFilter,
				inventorySort,
				inventoryQuery);
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
		boolean landscape = PixelScene.landscape();
		boolean compactLandscape = landscape && windowHeight < 160;
		actionButtonHeight = DeviceCompat.isDesktop()
				? (compactLandscape ? 15f : BUTTON_HEIGHT)
				: mobileControlHeight(
						compactLandscape ? 15f : BUTTON_HEIGHT);
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
				BukovMessages.get(viewModel.activeRaid
						? "bukov.economy.hub.eyebrow_active"
						: "bukov.economy.hub.eyebrow_loadout"),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		eyebrow.setPos(windowWidth - MARGIN - eyebrow.width(), y);
		// The bilingual eyebrow and 11px Chinese title cannot share a 127px
		// portrait header without drawing through one another.
		eyebrow.visible = landscape;
		add(eyebrow);

		RenderedTextBlock title = text(
				BukovMessages.get(viewModel.activeRaid
						? "bukov.economy.hub.title_active"
						: "bukov.economy.hub.title_loadout"),
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		title.setPos(MARGIN, y);
		add(title);
		y += 16;

		if (!compactLandscape) {
			StatusStrip status = new StatusStrip(
					windowWidth - MARGIN * 2);
			status.setRect(
					MARGIN,
					y,
					windowWidth - MARGIN * 2,
					21);
			add(status);
			y += 23;
		}

		if (!landscape) {
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
		modeButton.setRect(
				MARGIN,
				y,
				utilityWidth,
				actionButtonHeight);
		add(modeButton);
		addAction(
				viewModel.activeRaid
						? BukovMessages.get(
								"bukov.economy.hub.vendor_locked")
						: BukovMessages.get(
								"bukov.economy.hub.vendor",
								viewModel.currency),
				BukovHubFocusModel.ACTION_VENDOR,
				MARGIN + utilityWidth + GAP,
				y,
				utilityWidth,
				viewModel.activeRaid
						? "text.disabled"
						: "accent.valuable");
		y += actionButtonHeight + GAP;

		if (!compactLandscape) {
			RenderedTextBlock modeSummary = text(
					compact(
							viewModel.raidModeSummary,
							landscape ? 42 : 27),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			modeSummary.setRect(
					MARGIN,
					y,
					windowWidth - MARGIN * 2,
					7);
			add(modeSummary);
			y += 8;
		}

		float inventoryUtilityHeight = DeviceCompat.isDesktop()
				? 9f : mobileControlHeight(15f);
		float inventoryUtilityWidth =
				(windowWidth - MARGIN * 2 - GAP * 2f) / 3f;
		filterButton = new FilterCycleButton();
		filterButton.setRect(
				MARGIN,
				y,
				inventoryUtilityWidth,
				inventoryUtilityHeight);
		add(filterButton);
		sortButton = new SortCycleButton();
		sortButton.setRect(
				filterButton.right() + GAP,
				y,
				inventoryUtilityWidth,
				inventoryUtilityHeight);
		add(sortButton);
		searchButton = new InventorySearchButton();
		searchButton.setRect(
				sortButton.right() + GAP,
				y,
				inventoryUtilityWidth,
				inventoryUtilityHeight);
		add(searchButton);
		y += inventoryUtilityHeight + 1f;

		float footerReserved = compactLandscape
				? GAP
						+ actionButtonHeight
						+ GAP
						+ actionButtonHeight
				: FOOTER_RESERVED
						- (BUTTON_HEIGHT - actionButtonHeight) * 2f;
		float listHeight = inventoryViewportHeight(
				windowHeight,
				y,
				footerReserved);
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

		if (!compactLandscape) {
			RenderedTextBlock settlement = text(
					settlementText(),
					BukovVisualContract.FONT_CAPTION,
					settlementColor());
			settlement.setRect(
					MARGIN,
					y,
					windowWidth - MARGIN * 2,
					9);
			add(settlement);
			y += 10;
		}

		float half = (windowWidth - MARGIN * 2 - GAP) / 2f;
		addAction(
				viewModel.activeRaid
						? BukovMessages.get(
								"bukov.economy.hub.end_raid")
						: viewModel.canRepeatLoadout
								? BukovMessages.get(
										"bukov.economy.hub.repeat")
								: BukovMessages.get(
										"bukov.economy.hub.recommend"),
				BukovHubFocusModel.ACTION_REPEAT,
				MARGIN,
				y,
				half,
				viewModel.activeRaid ? "accent.danger" : "accent.interact");
		addAction(
				viewModel.activeRaid
						? BukovMessages.get(
								"bukov.economy.hub.loadout_locked")
						: viewModel.canRepeatLoadout
								? BukovMessages.get(
										"bukov.economy.hub.smart_loadout")
								: BukovMessages.get(
										"bukov.economy.hub.clear_loadout"),
				BukovHubFocusModel.ACTION_CLEAR,
				MARGIN + half + GAP,
				y,
				half,
				"panel.border");
		y += actionButtonHeight + GAP;

		addAction(
				viewModel.activeRaid
						? BukovMessages.get(
								"bukov.economy.hub.resume")
						: BukovMessages.get(!viewModel.canDeploy
								? "bukov.economy.hub.repair_deploy"
								: "bukov.economy.hub.confirm_deploy"),
				BukovHubFocusModel.ACTION_DEPLOY,
				MARGIN,
				y,
				half,
				!viewModel.canDeploy ? "accent.danger" : "accent.extract");
		addAction(
				BukovMessages.get("bukov.economy.hub.back"),
				BukovHubFocusModel.ACTION_BACK,
				MARGIN + half + GAP,
				y,
				half,
				"panel.border");
	}

	static int inventoryViewportHeight(
			int windowHeight,
			boolean landscape) {
		if (landscape && windowHeight < 160) {
			return (int)inventoryViewportHeight(
					windowHeight,
					INVENTORY_TOP_L_COMPACT,
					FOOTER_RESERVED_COMPACT);
		}
		return (int)inventoryViewportHeight(
				windowHeight,
				landscape ? INVENTORY_TOP_L : INVENTORY_TOP_P,
				FOOTER_RESERVED);
	}

	static float inventoryViewportHeight(
			float windowHeight,
			float inventoryTop,
			float footerReserved) {
		return Math.max(
				1f,
				windowHeight - inventoryTop - footerReserved);
	}

	static float mobileControlHeight(float requestedHeight) {
		return Math.max(MIN_MOBILE_CONTROL_HEIGHT, requestedHeight);
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

	private RenderedTextBlock text(
			String value, String typography, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				value, tokens.scaledTypographyPx(typography));
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

	static String shortActionLabel(String value) {
		String normalized = value == null ? "" : value.trim();
		int separator = normalized.indexOf('·');
		if (separator > 0) {
			normalized = normalized.substring(0, separator).trim();
		}
		int asciiColon = normalized.indexOf(':');
		int fullWidthColon = normalized.indexOf('：');
		int colon = asciiColon < 0
				? fullWidthColon
				: fullWidthColon < 0
						? asciiColon
						: Math.min(asciiColon, fullWidthColon);
		if (colon > 0) {
			normalized = normalized.substring(0, colon).trim();
		}
		int whitespace = normalized.indexOf(' ');
		if (whitespace > 0) {
			return normalized.substring(0, whitespace);
		}
		int codePoints = normalized.codePointCount(0, normalized.length());
		if (codePoints <= 8) {
			return normalized;
		}
		return normalized.substring(
				0,
				normalized.offsetByCodePoints(0, 7)) + "…";
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

	private BukovTouchIcon hubIcon(BukovTouchIcon.Glyph glyph) {
		return new BukovTouchIcon(
				glyph,
				tokens.color("text.primary"),
				tokens.color("accent.interact"),
				tokens.color("text.disabled"));
	}

	private void layoutIconLabel(
			BukovTouchIcon icon,
			RenderedTextBlock label,
			float left,
			float top,
			float regionWidth,
			float regionHeight) {
		float iconSize = Math.max(
				7f,
				Math.min(12f, regionHeight - 3f));
		float iconLeft = left + 4f;
		icon.setRect(
				iconLeft,
				top + (regionHeight - iconSize) * 0.5f,
				iconSize,
				iconSize);
		float textLeft = iconLeft + iconSize + 3f;
		float textWidth = Math.max(
				1f,
				left + regionWidth - 4f - textLeft);
		label.maxWidth(Math.max(1, (int) textWidth));
		center(label, textLeft, top, textWidth, regionHeight);
	}

	private BukovTouchIcon.Glyph actionGlyph(int action) {
		switch (action) {
			case BukovHubFocusModel.ACTION_VENDOR:
				return BukovTouchIcon.Glyph.VENDOR;
			case BukovHubFocusModel.ACTION_REPEAT:
				return viewModel.activeRaid
						? BukovTouchIcon.Glyph.DROP
						: BukovTouchIcon.Glyph.RECOMMEND;
			case BukovHubFocusModel.ACTION_CLEAR:
				return BukovTouchIcon.Glyph.DROP;
			case BukovHubFocusModel.ACTION_DEPLOY:
				return BukovTouchIcon.Glyph.DEPLOY;
			case BukovHubFocusModel.ACTION_BACK:
				return BukovTouchIcon.Glyph.BACK;
			default:
				return BukovTouchIcon.Glyph.INTERACT;
		}
	}

	private String settlementText() {
		if (viewModel.activeRaid) {
			return BukovMessages.get(
					"bukov.economy.hub.status_active",
					viewModel.activeRaidSummary());
		}
		if (!viewModel.canDeploy) {
			return BukovMessages.get(
					"bukov.economy.hub.status_blocked",
					viewModel.deploymentBlockReason);
		}
		if (viewModel.latestSettlement == null) {
			return viewModel.deploymentReadinessHeadline();
		}
		boolean success = viewModel.latestSettlement.outcome == RaidOutcome.SUCCESS;
		return BukovMessages.get(
				viewModel.latestSettlement.missionCompleted
						? success
								? "bukov.economy.hub.status_last_success_mission"
								: "bukov.economy.hub.status_last_failed_mission"
						: success
								? "bukov.economy.hub.status_last_success"
								: "bukov.economy.hub.status_last_failed",
				viewModel.latestSettlement.value,
				viewModel.latestSettlement.kills);
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
		button.setRect(
				x,
				y,
				buttonWidth,
				actionButtonHeight);
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
			showError(
					BukovMessages.get("bukov.economy.hub.loadout_save_failed"),
					error);
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
										closed,
										restoredFocus,
										inventoryFilter,
										inventorySort,
										inventoryQuery))));
	}

	private void cycleInventoryFilter() {
		BukovHubViewModel.InventoryFilter next =
				inventoryFilter.next();
		int nextFilterFocus =
				viewModel.inventoryItems(
						next,
						inventorySort,
						inventoryQuery).size() + 1;
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovHub(
						controller,
						deploy,
						closed,
						nextFilterFocus,
						next,
						inventorySort,
						inventoryQuery));
	}

	private void cycleInventorySort() {
		BukovHubViewModel.InventorySort next = inventorySort.next();
		int nextSortFocus =
				viewModel.inventoryItems(
						inventoryFilter,
						next,
						inventoryQuery).size() + 2;
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovHub(
						controller,
						deploy,
						closed,
						nextSortFocus,
						inventoryFilter,
						next,
						inventoryQuery));
	}

	private void openInventorySearch() {
		// Pointer and controller entry both return to the semantic search
		// control. Its absolute index must be recomputed because applying a
		// query can change the number of visible inventory rows.
		focus.focus(inventoryItems.size() + 3);
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovInventorySearch(
						inventoryQuery,
						query -> {
							int searchFocus =
									searchFocusFor(query);
							ShatteredPixelDungeon.scene().addToFront(
									new WndBukovHub(
											controller,
											deploy,
											closed,
											searchFocus,
											inventoryFilter,
											inventorySort,
											query));
						}));
	}

	private int searchFocusFor(String query) {
		return viewModel.inventoryItems(
				inventoryFilter,
				inventorySort,
				query).size() + 3;
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
					} else {
						controller.prepareAndConfirmDeployment();
						hide();
						deploy.call();
					}
					break;
				default:
					hide();
					closed.call();
					break;
			}
		} catch (IOException | RuntimeException error) {
			showError(
					BukovMessages.get("bukov.economy.hub.hideout_save_failed"),
					error);
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
		return true;
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
										closed,
										restoredFocus,
										inventoryFilter,
										inventorySort,
										inventoryQuery))));
	}

	private void confirmDeployment() {
		try {
			controller.confirmDeployment();
			hide();
			deploy.call();
		} catch (IOException | RuntimeException error) {
			showError(
					BukovMessages.get("bukov.economy.hub.deploy_failed"),
					error);
		}
	}

	private void reopen() {
		int restoredFocus = focus.index();
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovHub(
						controller,
						deploy,
						closed,
						restoredFocus,
						inventoryFilter,
						inventorySort,
						inventoryQuery));
	}

	private void showError(String title, Throwable error) {
		ShatteredPixelDungeon.reportException(error);
		String detail = error.getMessage() == null
				? error.getClass().getSimpleName()
				: error.getMessage();
		ShatteredPixelDungeon.scene().addToFront(
				new WndMessage(BukovMessages.get(
						"bukov.economy.common.error_detail",
						title,
						detail)));
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
		} else if (focus.sortFocused()) {
			cycleInventorySort();
		} else if (focus.searchFocused()) {
			openInventorySearch();
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
		if (sortButton != null) {
			sortButton.setFocused(focus.sortFocused());
		}
		if (searchButton != null) {
			searchButton.setFocused(focus.searchFocused());
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
			cash = text(BukovMessages.get(
							"bukov.economy.hub.cash",
							viewModel.currency),
					BukovVisualContract.FONT_BODY,
					tokens.color("accent.valuable"));
			add(cash);
			stash = text(BukovMessages.get(
							"bukov.economy.hub.stash_value",
							viewModel.stashValue),
					BukovVisualContract.FONT_BODY,
					tokens.color("text.secondary"));
			add(stash);
			risk = text(
					BukovMessages.get(viewModel.activeRaid
									? "bukov.economy.hub.carried_value"
									: "bukov.economy.hub.risk_value",
							viewModel.riskValue),
					BukovVisualContract.FONT_BODY,
					viewModel.riskValue > 0
							? tokens.color("accent.valuable")
							: tokens.color("text.secondary"));
			add(risk);
			weight = text(BukovMessages.get(
							"bukov.economy.hub.weight",
							viewModel.loadoutSummary()),
					BukovVisualContract.FONT_BODY,
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
					!BukovMessages.get("bukov.economy.hub.slot_empty")
							.equals(viewModel.slotSummary(slot));
			addToBack(stateSurface);
			edge = new ColorBlock(1, 1,
					BukovMessages.get("bukov.economy.hub.slot_empty")
							.equals(viewModel.slotSummary(slot))
							? tokens.color("panel.border")
							: tokens.color("accent.interact"));
			add(edge);
			code = text(BukovMessages.get(
							"bukov.economy.hub.slot_heading",
							slot.code,
							slot.label),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(code);
			value = text(viewModel.slotSummary(slot),
					BukovVisualContract.FONT_BODY,
					BukovMessages.get("bukov.economy.hub.slot_empty")
							.equals(viewModel.slotSummary(slot))
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
								? BukovMessages.get(
										"bukov.economy.hub.empty_stash")
								: !inventoryQuery.isEmpty()
								? BukovMessages.get(
										"bukov.economy.hub.empty_search",
										compact(inventoryQuery, 12))
								: BukovMessages.get(
										"bukov.economy.hub.empty_filter",
										inventoryFilter.label),
						BukovVisualContract.FONT_BODY,
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
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;
		private boolean focused;
		private boolean pointerPressed;

		private FilterCycleButton() {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.interact", 28));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, tokens.color("panel.border"));
			add(edge);
			icon = hubIcon(BukovTouchIcon.Glyph.FILTER);
			add(icon);
			label = text(
					shortActionLabel(BukovMessages.get(
							"bukov.economy.hub.filter",
							inventoryFilter.label,
							inventoryItems.size())),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			cycleInventoryFilter();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			refreshState();
		}

		@Override
		protected void onPointerDown() {
			pointerPressed = true;
			refreshState();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			refreshState();
		}

		private void refreshState() {
			edge.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.interact" : "panel.border"));
			surface.alpha(pointerPressed
					? 0.58f : focused ? 0.34f : 0.15f);
			label.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.interact" : "text.secondary"));
			icon.visualState(pointerPressed, false);
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y + height - 1;
			edge.size(width, pointerPressed ? 2f : 1f);
			layoutIconLabel(icon, label, x, y, width, height);
		}
	}

	private final class SortCycleButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;
		private boolean focused;
		private boolean pointerPressed;

		private SortCycleButton() {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.valuable", 28));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, tokens.color("panel.border"));
			add(edge);
			icon = hubIcon(BukovTouchIcon.Glyph.SORT);
			add(icon);
			label = text(
					shortActionLabel(BukovMessages.get(
							"bukov.economy.hub.sort",
							inventorySort.label)),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			cycleInventorySort();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			refreshState();
		}

		@Override
		protected void onPointerDown() {
			pointerPressed = true;
			refreshState();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			refreshState();
		}

		private void refreshState() {
			edge.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.valuable" : "panel.border"));
			surface.alpha(pointerPressed
					? 0.58f : focused ? 0.34f : 0.15f);
			label.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.valuable" : "text.secondary"));
			icon.visualState(pointerPressed, false);
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y + height - 1;
			edge.size(width, pointerPressed ? 2f : 1f);
			layoutIconLabel(icon, label, x, y, width, height);
		}
	}

	private final class InventorySearchButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;
		private boolean focused;
		private boolean pointerPressed;

		private InventorySearchButton() {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("accent.interact", 28));
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, tokens.color("panel.border"));
			add(edge);
			icon = hubIcon(BukovTouchIcon.Glyph.SEARCH);
			add(icon);
			label = text(
					shortActionLabel(inventoryQuery.isEmpty()
							? BukovMessages.get(
									"bukov.economy.hub.search")
							: BukovMessages.get(
									"bukov.economy.hub.search_query",
									compact(inventoryQuery, 5))),
					BukovVisualContract.FONT_CAPTION,
					tokens.color(
							inventoryQuery.isEmpty()
									? "text.secondary"
									: "accent.interact"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void onClick() {
			openInventorySearch();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			refreshState();
		}

		@Override
		protected void onPointerDown() {
			pointerPressed = true;
			refreshState();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			refreshState();
		}

		private void refreshState() {
			edge.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.interact" : "panel.border"));
			surface.alpha(pointerPressed
					? 0.58f : focused ? 0.34f : 0.15f);
			label.hardlight(tokens.color(
					pointerPressed || focused
							? "accent.interact"
							: inventoryQuery.isEmpty()
							? "text.secondary"
							: "accent.interact"));
			icon.visualState(pointerPressed, false);
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y + height - 1;
			edge.size(width, pointerPressed ? 2f : 1f);
			layoutIconLabel(icon, label, x, y, width, height);
		}
	}

	private final class ModeSelectButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;
		private boolean focused;
		private boolean pointerPressed;

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
			icon = hubIcon(BukovTouchIcon.Glyph.MODE);
			add(icon);
			label = text(
					shortActionLabel(BukovMessages.get(
							viewModel.canEditLoadout
									? "bukov.economy.hub.mode_select"
									: "bukov.economy.hub.mode_locked",
							viewModel.raidModeName)),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.primary"));
			add(label);
		}

		@Override
		protected void onClick() {
			openModeSelection();
		}

		private void setFocused(boolean focused) {
			this.focused = focused;
			refreshState();
		}

		@Override
		protected void onPointerDown() {
			pointerPressed = true;
			refreshState();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			refreshState();
		}

		private void refreshState() {
			focusEdge.visible = focused || pointerPressed;
			surface.alpha(pointerPressed ? 0.58f : 1f);
			label.hardlight(focused || pointerPressed
					? tokens.color("accent.interact")
					: tokens.color("text.primary"));
			icon.visualState(pointerPressed, false);
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
			layoutIconLabel(icon, label, x, y, width, height);
		}
	}

	private final class LoadoutRow extends Button {

		private final BukovHubViewModel.ItemRow item;
		private final NinePatch background;
		private final NinePatch focusSurface;
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
			background = BukovUiAssets.rarityFrame(
					rarityFrame(item.rarity),
					tokens.color(item.rarity.colorToken));
			addToBack(background);
			focusSurface = BukovUiAssets.surface(
					BukovUiAssets.Surface.ROW_FOCUSED,
					tokens.color("accent.interact"));
			focusSurface.visible = false;
			addToBack(focusSurface);
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
					BukovVisualContract.FONT_CAPTION,
					tokens.color(item.rarity.colorToken));
			add(category);
			name = text(
					compact(item.label, 15) + " ×" + item.quantity,
					BukovVisualContract.FONT_BODY,
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
					BukovVisualContract.FONT_CAPTION,
					!item.deployable
							? tokens.color("text.disabled")
							: item.selected
							? tokens.color("accent.valuable")
							: tokens.color("text.disabled"));
			metrics.align(RenderedTextBlock.RIGHT_ALIGN);
			add(metrics);
		}

		private void setFocused(boolean focused) {
			background.visible = !focused;
			focusSurface.visible = focused;
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
			focusSurface.x = x;
			focusSurface.y = y;
			focusSurface.size(width, height);
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

		private BukovUiAssets.RarityFrame rarityFrame(
				BukovHubViewModel.ItemRarity rarity) {
			switch (rarity) {
				case UNCOMMON:
					return BukovUiAssets.RarityFrame.UNCOMMON;
				case RARE:
					return BukovUiAssets.RarityFrame.RARE;
				case LEGENDARY:
					return BukovUiAssets.RarityFrame.LEGENDARY;
				case COMMON:
				default:
					return BukovUiAssets.RarityFrame.COMMON;
			}
		}
	}

	private class TacticalButton extends Button {

		private final int action;
		private final boolean enabled;
		private final NinePatch surface;
		private final NinePatch pressed;
		private final NinePatch focusSurface;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;

		private TacticalButton(
				String value,
				int action,
				int accent,
				boolean enabled) {
			this.action = action;
			this.enabled = enabled;
			surface = BukovUiAssets.surface(
					enabled
							? BukovUiAssets.Surface.BUTTON
							: BukovUiAssets.Surface.BUTTON_DISABLED,
					tokens.color(enabled
							? "panel.surface" : "panel.deep"));
			if (action == BukovHubFocusModel.ACTION_DEPLOY && enabled) {
				surface.hardlight(tokens.color("accent.extract"));
				surface.alpha(0.20f);
			}
			addToBack(surface);
			pressed = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_PRESSED,
					tokens.color("panel.deep"));
			pressed.visible = false;
			addToBack(pressed);
			focusSurface = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_FOCUSED,
					tokens.color("accent.interact"));
			focusSurface.visible = false;
			addToBack(focusSurface);
			edge = new ColorBlock(1, 1,
					enabled ? accent : tokens.color("text.disabled"));
			add(edge);
			focusEdge = new ColorBlock(1, 1,
					tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			icon = hubIcon(actionGlyph(action));
			icon.visualState(false, !enabled);
			add(icon);
			label = text(
					shortActionLabel(value),
					BukovVisualContract.FONT_CAPTION,
					enabled
							? tokens.color("text.primary")
							: tokens.color("text.disabled"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		private void setFocused(boolean focused) {
			boolean showFocus = focused && enabled;
			surface.visible = !showFocus;
			focusSurface.visible = showFocus;
			focusEdge.visible = focused;
			label.hardlight(!enabled
					? tokens.color("text.disabled")
					: focused
					? tokens.color("accent.interact")
					: tokens.color("text.primary"));
			icon.visualState(false, !enabled);
		}

		@Override
		protected void onClick() {
			if (enabled) {
				activateAction(action);
			}
		}

		@Override
		protected void onPointerDown() {
			if (!enabled) return;
			surface.visible = false;
			focusSurface.visible = false;
			pressed.visible = true;
			icon.visualState(true, false);
		}

		@Override
		protected void onPointerUp() {
			if (!enabled) return;
			pressed.visible = false;
			icon.visualState(false, false);
			boolean focused = focus.actionIndex() == action;
			surface.visible = !focused;
			focusSurface.visible = focused;
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			pressed.x = x;
			pressed.y = y;
			pressed.size(width, height);
			focusSurface.x = x;
			focusSurface.y = y;
			focusSurface.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 2;
			focusEdge.size(width, 2);
			layoutIconLabel(icon, label, x, y, width, height);
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
					BukovMessages.get(
							"bukov.economy.hub.confirm_eyebrow"),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			eyebrow.align(RenderedTextBlock.CENTER_ALIGN);
			center(eyebrow, 5, 5, confirmWidth - 10, 8);
			add(eyebrow);

			RenderedTextBlock heading = text(
					BukovMessages.get(
							"bukov.economy.hub.confirm_heading"),
					BukovVisualContract.FONT_BODY,
					tokens.color("accent.extract"));
			heading.align(RenderedTextBlock.CENTER_ALIGN);
			center(heading, 5, 15, confirmWidth - 10, 14);
			add(heading);

			RenderedTextBlock warning = text(
					BukovMessages.get(
							"bukov.economy.hub.confirm_warning",
							viewModel.riskValue,
							viewModel.loadoutSummary()),
					BukovVisualContract.FONT_BODY,
					tokens.color("text.primary"));
			warning.maxWidth(confirmWidth - 14);
			warning.align(RenderedTextBlock.CENTER_ALIGN);
			center(warning, 7, 32, confirmWidth - 14, 20);
			add(warning);

			ConfirmButton cancel = new ConfirmButton(
					BukovMessages.get("bukov.economy.hub.confirm_cancel"),
					false,
					tokens.color("panel.border"));
			cancel.setRect(
					5,
					59,
					61,
					DeviceCompat.isDesktop()
							? 19f : mobileControlHeight(19f));
			add(cancel);
			ConfirmButton accept = new ConfirmButton(
					BukovMessages.get("bukov.economy.hub.confirm_accept"),
					true,
					tokens.color("accent.extract"));
			accept.setRect(
					72,
					59,
					61,
					DeviceCompat.isDesktop()
							? 19f : mobileControlHeight(19f));
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
			private final BukovTouchIcon icon;
			private final RenderedTextBlock label;
			private final int accent;
			private boolean pointerPressed;

			private ConfirmButton(String value, boolean accepts, int accent) {
				this.accepts = accepts;
				this.accent = accent;
				surface = new ColorBlock(1, 1,
						tokens.color("panel.surface"));
				addToBack(surface);
				edge = new ColorBlock(1, 1, accent);
				add(edge);
				icon = hubIcon(accepts
						? BukovTouchIcon.Glyph.DEPLOY
						: BukovTouchIcon.Glyph.BACK);
				add(icon);
				label = text(
						shortActionLabel(value),
						BukovVisualContract.FONT_CAPTION,
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
			protected void onPointerDown() {
				pointerPressed = true;
				refreshPressedState();
			}

			@Override
			protected void onPointerUp() {
				pointerPressed = false;
				refreshPressedState();
			}

			private void refreshPressedState() {
				surface.alpha(pointerPressed ? 0.56f : 1f);
				edge.hardlight(pointerPressed
						? tokens.color("accent.interact") : accent);
				label.hardlight(pointerPressed
						? tokens.color("accent.interact")
						: accepts
								? tokens.color("text.primary")
								: tokens.color("text.secondary"));
				icon.visualState(pointerPressed, false);
				layout();
			}

			@Override
			protected void layout() {
				super.layout();
				surface.x = x;
				surface.y = y;
				surface.size(width, height);
				edge.x = x;
				edge.y = y;
				edge.size(pointerPressed ? 3f : 2f, height);
				layoutIconLabel(icon, label, x, y, width, height);
			}
		}
	}
}
