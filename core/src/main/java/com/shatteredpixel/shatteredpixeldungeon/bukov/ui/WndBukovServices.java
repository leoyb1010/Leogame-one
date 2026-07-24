package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentSlot;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovInsuranceService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractService;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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

/** Compact, controller-ready contract, insurance and firearm workshop window. */
public final class WndBukovServices extends Window {

	public enum Tab {
		CONTRACTS,
		INSURANCE,
		FIREARMS;

		Tab next() {
			Tab[] tabs = values();
			return tabs[(ordinal() + 1) % tabs.length];
		}
	}

	private static final int WIDTH_P = 154;
	private static final int HEIGHT_P = 220;
	private static final int WIDTH_L = 226;
	private static final int HEIGHT_L = 176;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int ROW_HEIGHT = 31;
	private static final int BUTTON_HEIGHT = 18;

	private final BukovHubController controller;
	private final Callback close;
	private final Tab tab;
	private final FirearmAttachmentSlot slot;
	private final BukovServicesViewModel model;
	private final BukovServicesFocusModel focus;
	private final BukovUiTokens tokens;
	private final List<ServiceRow> rows = new ArrayList<>();
	private final List<ActionButton> actions = new ArrayList<>();
	private final BukovFocusRepeater repeater = new BukovFocusRepeater();
	private final String notice;
	private ScrollPane scroll;
	private boolean closing;

	public WndBukovServices(
			BukovHubController controller,
			Callback close,
			Tab tab) {
		this(
				controller,
				close,
				tab,
				0,
				FirearmAttachmentSlot.OPTIC,
				null);
	}

	private WndBukovServices(
			BukovHubController controller,
			Callback close,
			Tab tab,
			int selectedRow,
			FirearmAttachmentSlot slot,
			String notice) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		if (controller == null || close == null || tab == null || slot == null) {
			throw new IllegalArgumentException(
					"controller, close, tab and slot are required");
		}
		this.controller = controller;
		this.close = close;
		this.tab = tab;
		this.slot = slot;
		this.notice = notice;
		model = controller.servicesViewModel();
		focus = new BukovServicesFocusModel(rowCount(), selectedRow);
		tokens = BukovUiTokens.loadDefault();
		int windowWidth = BukovWindowLayout.safeWidth(
				PixelScene.landscape() ? WIDTH_L : WIDTH_P);
		int windowHeight = BukovWindowLayout.safeHeight(
				PixelScene.landscape() ? HEIGHT_L : HEIGHT_P);
		resize(windowWidth, windowHeight);
		build(windowWidth, windowHeight);
		updateFocus();
	}

	private int rowCount() {
		switch (tab) {
			case CONTRACTS: return model.contracts.size();
			case INSURANCE: return model.insuranceItems.size();
			default: return model.firearms.size();
		}
	}

	private void build(int windowWidth, int windowHeight) {
		ColorBlock header = new ColorBlock(
				windowWidth,
				40,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock line = new ColorBlock(
				windowWidth, 1, tokens.color(accentToken()));
		line.y = 39;
		add(line);

		RenderedTextBlock eyebrow = text(
				BukovMessages.get(
						"bukov.economy.services.eyebrow_"
								+ tab.name().toLowerCase(
										java.util.Locale.ROOT)),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN, 3);
		add(eyebrow);
		RenderedTextBlock cash = text(
				BukovMessages.get(
						"bukov.economy.services.balance",
						model.currency),
				BukovVisualContract.FONT_BODY,
				tokens.color("accent.valuable"));
		cash.setPos(windowWidth - MARGIN - cash.width(), 3);
		add(cash);
		RenderedTextBlock title = text(
				title(),
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		title.setPos(MARGIN, 14);
		add(title);
		RenderedTextBlock subtitle = text(
				subtitle(),
				BukovVisualContract.FONT_CAPTION,
				tokens.color(model.locked
						? "accent.danger" : "text.secondary"));
		subtitle.setRect(MARGIN, 28, windowWidth - MARGIN * 2, 8);
		add(subtitle);

		int listTop = 42;
		int footer = 36;
		int listHeight = Math.max(
				ROW_HEIGHT * 2,
				windowHeight - listTop - footer);
		ServiceList list = new ServiceList(windowWidth - MARGIN * 2);
		scroll = new ScrollPane(list);
		add(scroll);
		scroll.setRect(
				MARGIN,
				listTop,
				windowWidth - MARGIN * 2,
				listHeight);

		RenderedTextBlock feedback = text(
				notice == null ? selectionSummary() : notice,
				BukovVisualContract.FONT_CAPTION,
				tokens.color(notice == null
						? "text.secondary" : "accent.extract"));
		feedback.setRect(
				MARGIN,
				listTop + listHeight + 1,
				windowWidth - MARGIN * 2,
				8);
		add(feedback);

		float fourth = (windowWidth - MARGIN * 2 - GAP * 3) / 4f;
		float y = windowHeight - BUTTON_HEIGHT - MARGIN;
		addAction(
				nextTabLabel(),
				BukovTouchIcon.Glyph.MODE,
				BukovServicesFocusModel.ACTION_TAB,
				MARGIN, y, fourth, true, "accent.interact");
		addAction(
				primaryLabel(),
				primaryGlyph(),
				BukovServicesFocusModel.ACTION_PRIMARY,
				MARGIN + fourth + GAP,
				y, fourth, primaryEnabled(), accentToken());
		addAction(
				secondaryLabel(),
				secondaryGlyph(),
				BukovServicesFocusModel.ACTION_SECONDARY,
				MARGIN + (fourth + GAP) * 2f,
				y, fourth, secondaryEnabled(), "accent.valuable");
		addAction(
				BukovMessages.get("bukov.economy.services.back"),
				BukovTouchIcon.Glyph.BACK,
				BukovServicesFocusModel.ACTION_BACK,
				MARGIN + (fourth + GAP) * 3f,
				y, fourth, true, "panel.border");
	}

	private String title() {
		if (tab == Tab.CONTRACTS) {
			return BukovMessages.get("bukov.economy.services.title_contracts");
		}
		if (tab == Tab.INSURANCE) {
			return BukovMessages.get("bukov.economy.services.title_insurance");
		}
		return BukovMessages.get("bukov.economy.services.title_firearms");
	}

	private String subtitle() {
		if (model.locked) {
			return BukovMessages.get("bukov.economy.services.state_locked");
		}
		if (tab == Tab.CONTRACTS) {
			return BukovMessages.get(
					"bukov.economy.services.subtitle_contracts");
		}
		if (tab == Tab.INSURANCE) {
			return BukovMessages.get(
					"bukov.economy.services.subtitle_insurance",
					model.pendingInsuranceReturns,
					model.dueInsuranceReturns);
		}
		return BukovMessages.get(
				"bukov.economy.services.subtitle_firearms",
				slotName(slot));
	}

	private String accentToken() {
		if (tab == Tab.CONTRACTS) return "accent.extract";
		if (tab == Tab.INSURANCE) return "accent.valuable";
		return "accent.interact";
	}

	private String nextTabLabel() {
		if (tab == Tab.CONTRACTS) {
			return BukovMessages.get("bukov.economy.services.next_insurance");
		}
		if (tab == Tab.INSURANCE) {
			return BukovMessages.get("bukov.economy.services.next_firearms");
		}
		return BukovMessages.get("bukov.economy.services.next_contracts");
	}

	private String primaryLabel() {
		if (tab == Tab.CONTRACTS) {
			if (!hasSelection()) {
				return BukovMessages.get(
						"bukov.economy.services.action_no_contract");
			}
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(focus.selectedRow());
			if (row.claimed) {
				return BukovMessages.get(
						"bukov.economy.services.action_claimed");
			}
			return row.ready
					? BukovMessages.get(
							"bukov.economy.services.action_claim",
							row.reward)
					: BukovMessages.get(
							"bukov.economy.services.action_incomplete");
		}
		if (tab == Tab.INSURANCE) {
			if (!hasSelection()) {
				return BukovMessages.get(
						"bukov.economy.services.action_no_loadout");
			}
			return model.insuranceItems.get(focus.selectedRow()).insured
					? BukovMessages.get(
							"bukov.economy.services.action_uninsure")
					: BukovMessages.get(
							"bukov.economy.services.action_insure");
		}
		if (!hasSelection()) {
			return BukovMessages.get(
					"bukov.economy.services.action_no_firearm");
		}
		return installedInSelectedSlot()
				? BukovMessages.get("bukov.economy.services.action_remove")
				: BukovMessages.get("bukov.economy.services.action_install");
	}

	private String secondaryLabel() {
		if (tab == Tab.CONTRACTS) {
			return BukovMessages.get("bukov.economy.services.target_progress");
		}
		if (tab == Tab.INSURANCE) {
			return model.dueInsuranceReturns > 0
					? BukovMessages.get(
							"bukov.economy.services.claim_returns",
							model.dueInsuranceReturns)
					: BukovMessages.get(
							"bukov.economy.services.no_returns");
		}
		return slotName(slot);
	}

	private BukovTouchIcon.Glyph primaryGlyph() {
		if (tab == Tab.CONTRACTS) {
			return BukovTouchIcon.Glyph.INTERACT;
		}
		if (tab == Tab.INSURANCE) {
			return BukovTouchIcon.Glyph.BACKPACK;
		}
		return BukovTouchIcon.Glyph.AIM_FIRE;
	}

	private BukovTouchIcon.Glyph secondaryGlyph() {
		if (tab == Tab.CONTRACTS) {
			return BukovTouchIcon.Glyph.SEARCH;
		}
		if (tab == Tab.INSURANCE) {
			return BukovTouchIcon.Glyph.BACKPACK;
		}
		return BukovTouchIcon.Glyph.RELOAD;
	}

	private boolean primaryEnabled() {
		if (model.locked || !hasSelection()) return false;
		if (tab == Tab.CONTRACTS) {
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(focus.selectedRow());
			return row.ready && !row.claimed;
		}
		return true;
	}

	private boolean secondaryEnabled() {
		if (model.locked) return false;
		if (tab == Tab.INSURANCE) return model.dueInsuranceReturns > 0;
		return tab == Tab.FIREARMS && hasSelection();
	}

	private boolean hasSelection() {
		return focus.selectedRow() >= 0 && focus.selectedRow() < rowCount();
	}

	private boolean installedInSelectedSlot() {
		BukovServicesViewModel.FirearmRow row =
				model.firearms.get(focus.selectedRow());
		String none = BukovMessages.get("bukov.economy.services.none");
		if (slot == FirearmAttachmentSlot.OPTIC) return !none.equals(row.optic);
		if (slot == FirearmAttachmentSlot.MAGAZINE) return !none.equals(row.magazine);
		return !none.equals(row.muzzle);
	}

	private String selectionSummary() {
		if (!hasSelection()) {
			if (tab == Tab.INSURANCE) {
				return BukovMessages.get(
						"bukov.economy.services.empty_insurance");
			}
			if (tab == Tab.FIREARMS) {
				return BukovMessages.get(
						"bukov.economy.services.empty_firearms");
			}
			return BukovMessages.get(
					"bukov.economy.services.empty_contracts");
		}
		if (tab == Tab.CONTRACTS) {
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(focus.selectedRow());
			return BukovMessages.get(
					"bukov.economy.services.contract_summary",
					row.objective,
					row.reward);
		}
		if (tab == Tab.INSURANCE) {
			BukovServicesViewModel.InsuranceRow row =
					model.insuranceItems.get(focus.selectedRow());
			return BukovMessages.get(
					row.insured
							? "bukov.economy.services.insurance_summary_insured"
							: "bukov.economy.services.insurance_summary",
					row.label,
					row.value);
		}
		return model.firearms.get(focus.selectedRow()).deltaLabel();
	}

	private void addAction(
			String label,
			BukovTouchIcon.Glyph glyph,
			int action,
			float x,
			float y,
			float buttonWidth,
			boolean enabled,
			String token) {
		ActionButton button = new ActionButton(
				label,
				glyph,
				action,
				enabled,
				tokens.color(token));
		button.setRect(x, y, buttonWidth, BUTTON_HEIGHT);
		actions.add(button);
		add(button);
	}

	private void activateAction(int action) {
		try {
			switch (action) {
				case BukovServicesFocusModel.ACTION_TAB:
					reopen(tab.next(), 0, slot, null);
					return;
				case BukovServicesFocusModel.ACTION_PRIMARY:
					if (primaryEnabled()) primary();
					return;
				case BukovServicesFocusModel.ACTION_SECONDARY:
					if (secondaryEnabled()) secondary();
					return;
				default:
					closeToHub();
			}
		} catch (IOException | RuntimeException error) {
			showError(
					BukovMessages.get("bukov.economy.services.operation_failed"),
					error);
		}
	}

	private void primary() throws IOException {
		int selected = focus.selectedRow();
		if (tab == Tab.CONTRACTS) {
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(selected);
			BukovLongTermContractService.ClaimResult result =
					controller.claimContract(row.contractId);
			reopen(tab, selected, slot,
					result.status
							== BukovLongTermContractService.ClaimStatus.CLAIMED
							? BukovMessages.get(
									"bukov.economy.services.claim_done",
									result.currencyGranted)
							: BukovMessages.get(
									result.status
											== BukovLongTermContractService
													.ClaimStatus.NOT_READY
											? "bukov.economy.services.claim_not_ready"
											: "bukov.economy.services.claim_already"));
		} else if (tab == Tab.INSURANCE) {
			BukovServicesViewModel.InsuranceRow row =
					model.insuranceItems.get(selected);
			boolean insured = controller.toggleInsurance(row.itemUid);
			reopen(tab, selected, slot,
					BukovMessages.get(insured
							? "bukov.economy.services.insure_done"
							: "bukov.economy.services.uninsure_done"));
		} else {
			BukovServicesViewModel.FirearmRow row =
					model.firearms.get(selected);
			boolean installed =
					controller.toggleAttachment(row.itemUid, slot);
			reopen(tab, selected, slot,
					BukovMessages.get(
							installed
									? "bukov.economy.services.install_done"
									: "bukov.economy.services.remove_done",
							slotName(slot)));
		}
	}

	private void secondary() throws IOException {
		if (tab == Tab.INSURANCE) {
			BukovInsuranceService.ClaimResult result =
					controller.claimInsuranceReturns();
			reopen(tab, focus.selectedRow(), slot,
					BukovMessages.get(
							"bukov.economy.services.returns_done",
							result.returnedItemUids.size(),
							result.returnedValue));
		} else if (tab == Tab.FIREARMS) {
			reopen(
					tab,
					focus.selectedRow(),
					nextSlot(slot),
					null);
		}
	}

	private void select(int row) {
		focus.selectRow(row);
		reopen(tab, row, slot, null);
	}

	private void reopen(
			Tab next,
			int selected,
			FirearmAttachmentSlot nextSlot,
			String nextNotice) {
		hide();
		ShatteredPixelDungeon.scene().addToFront(
				new WndBukovServices(
						controller,
						close,
						next,
						selected,
						nextSlot,
						nextNotice));
	}

	private void closeToHub() {
		if (closing) return;
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
				new WndMessage(BukovMessages.get(
						"bukov.economy.common.error_detail",
						title,
						detail)));
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (!event.pressed) return true;
		if (BukovNavigation.back(event)) {
			closeToHub();
		} else if (BukovNavigation.previous(event)) {
			focus.move(-1);
			updateFocus();
		} else if (BukovNavigation.next(event)) {
			focus.move(1);
			updateFocus();
		} else if (BukovNavigation.confirm(event)) {
			if (focus.rowFocused()) {
				select(focus.rowIndex());
			} else {
				activateAction(focus.actionIndex());
			}
		}
		return true;
	}

	@Override
	public void update() {
		super.update();
		int delta = repeater.update(
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
		for (int index = 0; index < rows.size(); index++) {
			rows.get(index).setState(
					index == focus.selectedRow(),
					focus.rowFocused() && index == focus.rowIndex());
		}
		for (int index = 0; index < actions.size(); index++) {
			actions.get(index).setFocused(
					!focus.rowFocused()
							&& index == focus.actionIndex());
		}
		if (focus.rowFocused() && scroll != null) {
			scroll.scrollTo(
					0,
					Math.max(0, focus.rowIndex() * ROW_HEIGHT - ROW_HEIGHT));
		}
	}

	private RenderedTextBlock text(
			String value, String typography, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				value, tokens.scaledTypographyPx(typography));
		result.maxWidth(Math.max(1, (int) width - MARGIN * 2));
		result.hardlight(color);
		return result;
	}

	private final class ServiceList extends Component {
		private ServiceList(float listWidth) {
			int count = Math.max(1, rowCount());
			setSize(listWidth, count * ROW_HEIGHT);
			ColorBlock background = new ColorBlock(
					listWidth, height(), tokens.color("panel.surface"));
			addToBack(background);
			if (rowCount() == 0) {
				RenderedTextBlock empty = text(
						selectionSummary(),
						BukovVisualContract.FONT_BODY,
						tokens.color("text.disabled"));
				empty.setRect(4, 8, listWidth - 8, ROW_HEIGHT);
				add(empty);
				return;
			}
			for (int index = 0; index < rowCount(); index++) {
				ServiceRow row = new ServiceRow(index);
				row.setRect(
						0,
						index * ROW_HEIGHT,
						listWidth,
						ROW_HEIGHT - 1);
				rows.add(row);
				add(row);
			}
		}
	}

	private final class ServiceRow extends Button {
		private final int rowIndex;
		private final ColorBlock background;
		private final ColorBlock edge;
		private final RenderedTextBlock name;
		private final RenderedTextBlock detail;

		private ServiceRow(int rowIndex) {
			this.rowIndex = rowIndex;
			background = new ColorBlock(
					1, 1, tokens.colorWithAlpha("panel.surface", 220));
			addToBack(background);
			edge = new ColorBlock(
					1, 1, tokens.color("panel.border"));
			add(edge);
			name = text(
					rowName(rowIndex), BukovVisualContract.FONT_BODY,
					tokens.color("text.primary"));
			add(name);
			detail = text(
					rowDetail(rowIndex),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(detail);
		}

		private void setState(boolean selected, boolean focused) {
			background.alpha(selected ? 1f : 0.62f);
			edge.color(tokens.color(
					focused ? "accent.interact"
							: selected ? accentToken() : "panel.border"));
		}

		@Override
		protected void onClick() {
			select(rowIndex);
		}

		@Override
		protected void layout() {
			super.layout();
			background.x = x;
			background.y = y;
			background.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2f, height);
			name.maxWidth(Math.max(1, (int) width - 8));
			name.setPos(x + 5, y + 4);
			detail.maxWidth(Math.max(1, (int) width - 8));
			detail.setPos(x + 5, name.bottom() + 3);
		}
	}

	private String rowName(int index) {
		if (tab == Tab.CONTRACTS) {
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(index);
			return BukovMessages.get(
					"bukov.economy.services.contract_row",
					row.title,
					row.progressLabel());
		}
		if (tab == Tab.INSURANCE) {
			BukovServicesViewModel.InsuranceRow row =
					model.insuranceItems.get(index);
			return row.insured
					? BukovMessages.get(
							"bukov.economy.services.insured_row",
							row.label)
					: row.label;
		}
		return model.firearms.get(index).label;
	}

	private String rowDetail(int index) {
		if (tab == Tab.CONTRACTS) {
			BukovServicesViewModel.ContractRow row =
					model.contracts.get(index);
			return BukovMessages.get(
					"bukov.economy.services.contract_summary",
					row.objective,
					row.reward);
		}
		if (tab == Tab.INSURANCE) {
			return BukovMessages.get(
					"bukov.economy.services.risk_value",
					model.insuranceItems.get(index).value);
		}
		return model.firearms.get(index).slotsLabel()
				+ "\n" + model.firearms.get(index).deltaLabel();
	}

	private final class ActionButton extends Button {
		private final int action;
		private final boolean enabled;
		private final ColorBlock surface;
		private final ColorBlock edge;
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;

		private ActionButton(
				String value,
				BukovTouchIcon.Glyph glyph,
				int action,
				boolean enabled,
				int accent) {
			this.action = action;
			this.enabled = enabled;
			surface = new ColorBlock(1, 1, accent);
			surface.alpha(enabled ? 0.18f : 0.05f);
			addToBack(surface);
			edge = new ColorBlock(
					1, 1, enabled ? accent : tokens.color("text.disabled"));
			add(edge);
			icon = new BukovTouchIcon(
					glyph,
					tokens.color("text.primary"),
					tokens.color("accent.interact"),
					tokens.color("text.disabled"));
			icon.visualState(false, !enabled);
			add(icon);
			label = text(
					value,
					BukovVisualContract.FONT_CAPTION,
					tokens.color(enabled
							? "text.primary" : "text.disabled"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		private void setFocused(boolean focused) {
			edge.alpha(focused ? 1f : 0.55f);
			surface.alpha(focused ? 0.34f : enabled ? 0.18f : 0.05f);
			icon.visualState(false, !enabled);
		}

		@Override
		protected void onClick() {
			if (enabled) activateAction(action);
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
			float iconSize = Math.max(
					7f,
					Math.min(9f, height - 7f));
			float iconLeft = x + 3f;
			icon.setRect(
					iconLeft,
					y + (height - iconSize) / 2f,
					iconSize,
					iconSize);
			float textLeft = iconLeft + iconSize + 1f;
			float textWidth = Math.max(
					1f,
					x + width - 2f - textLeft);
			label.maxWidth(Math.max(1, (int)textWidth));
			label.setPos(
					textLeft + (textWidth - label.width()) / 2f,
					y + (height - label.height()) / 2f);
		}
	}

	private static FirearmAttachmentSlot nextSlot(
			FirearmAttachmentSlot slot) {
		FirearmAttachmentSlot[] slots = FirearmAttachmentSlot.values();
		return slots[(slot.ordinal() + 1) % slots.length];
	}

	private static String slotName(FirearmAttachmentSlot slot) {
		if (slot == FirearmAttachmentSlot.OPTIC) {
			return BukovMessages.get("bukov.economy.services.slot_optic");
		}
		if (slot == FirearmAttachmentSlot.MAGAZINE) {
			return BukovMessages.get("bukov.economy.services.slot_magazine");
		}
		return BukovMessages.get("bukov.economy.services.slot_muzzle");
	}
}
