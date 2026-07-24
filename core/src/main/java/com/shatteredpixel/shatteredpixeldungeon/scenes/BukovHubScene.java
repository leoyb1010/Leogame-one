package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovUiSoundPlayer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovUiSoundRouter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovFocusModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovFocusRepeater;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubViewModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovIconLabelButton;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovNavigation;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchIcon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiAssets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiScale;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovVisualContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovHub;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovRaidModeSelection;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovVendor;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.ControllerHandler;
import com.watabou.input.GameAction;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.RectF;

import java.io.IOException;

/**
 * Full-screen Bukov hideout.
 *
 * This is a real product scene rather than a title-screen modal: deployment
 * status, contract choice, training, stash and locked active-raid state all
 * live behind one persistent scene boundary.
 */
public final class BukovHubScene extends PixelScene {

	private static final float MARGIN = BukovVisualContract.OUTER_MARGIN;
	private static final float GAP = BukovVisualContract.GAP;

	private BukovUiTokens tokens;
	private BukovHubController controller;
	private BukovHubViewModel state;
	private int uiScaleLevel;
	private float uiMargin;
	private float uiGap;

	@Override
	public void create() {
		super.create();
		Music.INSTANCE.end();
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		uiCamera.visible = false;
		tokens = BukovUiTokens.loadDefault();
		uiScaleLevel = SPDSettings.bukovUiScale();
		uiMargin = BukovUiScale.value(MARGIN, uiScaleLevel);
		uiGap = BukovUiScale.value(GAP, uiScaleLevel);

		try {
			controller = new BukovHubController(
					BukovSaveServices.platformDefault());
			state = controller.viewModel();
			build();
		} catch (Exception error) {
			ShatteredPixelDungeon.reportException(error);
			buildFailure(error);
		}
		fadeIn();
	}

	@Override
	public void update() {
		super.update();
		BukovUiSoundRouter.update(Game.elapsed);
	}

	private void build() {
		final int screenWidth = Camera.main.width;
		final int screenHeight = Camera.main.height;
		final boolean wide = landscape();
		final RectF insets = getCommonInsets();
		final float safeWidth =
				screenWidth - insets.left - insets.right;
		final float usableWidth = BukovVisualContract.contentWidth(
				safeWidth, wide, uiScaleLevel);
		final float left = BukovVisualContract.centeredLeft(
				insets.left, safeWidth, usableWidth);
		final float top = insets.top + uiMargin;
		final float usableHeight =
				screenHeight - insets.top - insets.bottom - uiMargin * 2;

		addBackdrop(screenWidth, screenHeight, wide);

		RenderedTextBlock eyebrow = label(
				wide
						? entryMessage("hub.eyebrow_wide")
						: entryMessage("hub.eyebrow_compact"),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		eyebrow.maxWidth(Math.max(1, (int)usableWidth));
		eyebrow.setPos(left, top);
		add(eyebrow);

		RenderedTextBlock title = label(
				entryMessage("hub.title"),
				BukovVisualContract.FONT_TITLE,
				tokens.color("accent.valuable"));
		title.setPos(left, eyebrow.bottom() + 2f);
		add(title);

		String statusText = state.activeRaid
				? entryMessage("hub.status_active")
				: isEnglish()
						? entryMessage("hub.status_ready")
						: state.careerSummary;
		RenderedTextBlock status = label(
				statusText,
				BukovVisualContract.FONT_CAPTION,
				tokens.color(state.activeRaid
						? "accent.extract" : "text.secondary"));
		status.maxWidth(Math.max(1, (int)usableWidth));
		status.setPos(left, title.bottom() + 2f);
		add(status);

		float contentTop = status.bottom() + 5f;
		float footerButtonHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop(),
				uiScaleLevel);
		float footerHeight = footerButtonHeight * 2f + 3f;
		float availableContentHeight = Math.max(
				58f,
				top + usableHeight - contentTop - footerHeight - uiGap);
		float contentHeight = wide && DeviceCompat.isDesktop()
				? Math.min(145f, availableContentHeight)
				: availableContentHeight;
		float leftWidth = wide
				? Math.max(74f, usableWidth * 0.32f)
				: usableWidth;
		float rightLeft = wide ? left + leftWidth + uiGap : left;
		float rightWidth = wide
				? usableWidth - leftWidth - uiGap
				: usableWidth;
		float leftHeight = wide
				? contentHeight
				: Math.min(38f, contentHeight * 0.30f);
		float rightTop = wide
				? contentTop
				: contentTop + leftHeight + uiGap;
		float rightHeight = wide
				? contentHeight
				: contentHeight - leftHeight - uiGap;

		buildStatusPanel(left, contentTop, leftWidth, leftHeight);
		if (state.activeRaid) {
			buildActiveRaidPanel(
					rightLeft, rightTop, rightWidth, rightHeight);
		} else {
			buildDeploymentPanel(
					rightLeft, rightTop, rightWidth, rightHeight);
		}
		buildFooter(
				left,
				wide
						? contentTop + contentHeight + uiGap
						: top + usableHeight - footerHeight,
				usableWidth,
				footerHeight,
				wide);

		if (DeviceCompat.isDesktop()) {
			ExitButton exit = new ExitButton();
			exit.setPos(
					screenWidth - insets.right - exit.width(),
					insets.top);
			add(exit);
		}
	}

	private void addBackdrop(int screenWidth, int screenHeight, boolean wide) {
		Image background = new Image(wide
				? Assets.Splashes.Bukov.TITLE_INDUSTRIAL_LANDSCAPE_V2
				: Assets.Splashes.Bukov.TITLE_INDUSTRIAL_PORTRAIT_V2);
		background.texture.filter(SmartTexture.LINEAR, SmartTexture.LINEAR);
		float cover = Math.max(
				screenWidth / background.width(),
				screenHeight / background.height());
		background.scale.set(cover);
		background.x = (screenWidth - background.width()) / 2f;
		background.y = (screenHeight - background.height()) / 2f;
		add(background);

		ColorBlock veil = new ColorBlock(
				screenWidth,
				screenHeight,
				tokens.colorWithAlpha("ink.background", wide ? 208 : 224));
		add(veil);
	}

	private void buildStatusPanel(
			float x,
			float y,
			float width,
			float height) {
		addPanel(x, y, width, height, entryMessage("hub.panel_stash"));
		float textLeft = x + 6f;
		float textTop = y + 18f;

		if (!landscape()) {
			float column = (width - 12f) / 3f;
			addMetric(
					entryMessage("hub.metric_cash"),
					String.valueOf(state.currency),
					textLeft,
					textTop,
					"accent.valuable");
			addMetric(
					entryMessage("hub.metric_stash_value"),
					String.valueOf(state.stashValue),
					textLeft + column,
					textTop,
					"text.primary");
			addMetric(
					entryMessage(state.activeRaid
							? "hub.metric_raid_carry"
							: "hub.metric_risk"),
					String.valueOf(state.riskValue),
					textLeft + column * 2f,
					textTop,
					state.activeRaid
							? "accent.extract" : "text.secondary");
			return;
		}

		addMetric(
				entryMessage("hub.metric_cash"),
				String.valueOf(state.currency),
				textLeft,
				textTop,
				"accent.valuable");
		addMetric(
				entryMessage("hub.metric_stash_value"),
				String.valueOf(state.stashValue),
				textLeft,
				textTop + 15f,
				"text.primary");
		addMetric(
				entryMessage(state.activeRaid
						? "hub.metric_raid_carry"
						: "hub.metric_risk"),
				String.valueOf(state.riskValue),
				textLeft,
				textTop + 30f,
				state.activeRaid ? "accent.extract" : "text.secondary");

		if (height > 70f) {
			if (state.activeRaid) {
				return;
			}
			RenderedTextBlock contract = label(
					isEnglish()
							? entryMessage("hub.contract_active_generic")
							: entryMessage(
									"hub.contract_active",
									state.activeContract,
									state.activeContractObjective),
						BukovVisualContract.FONT_CAPTION,
					tokens.color("accent.extract"));
			contract.maxWidth(Math.max(1, (int) width - 12));
			contract.setPos(textLeft, textTop + 47f);
			if (contract.bottom() <= y + height - 6f) {
				add(contract);
			}
		}
	}

	private void buildDeploymentPanel(
			float x,
			float y,
			float width,
			float height) {
		addPanel(
				x, y, width, height,
				entryMessage("hub.panel_deployment"));
		boolean wide = landscape();
		float actionHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop(),
				uiScaleLevel);
		float innerX = x + 5f;
		float innerY = y + 18f;
		float innerWidth = width - 10f;
		float cardHeight = wide
				? Math.max(32f,
						Math.min(46f, height - actionHeight - 41f))
				: Math.max(29f,
						Math.min(38f, height - actionHeight - 47f));

		BukovRaidMode selectedMode = controller.selectedRaidMode();
		boolean training = selectedMode.trainingGround();
		ModeCard modeCard = new ModeCard(
				entryMessage(
						"hub.current_mode",
						localizedModeName(selectedMode)),
				entryMessage(
						"hub.mode_detail",
						localizedModeSummary(selectedMode)),
				true,
				training) {
			@Override
			protected void activate() {
				openRaidModeSelection();
			}
		};
		modeCard.setRect(
				innerX,
				innerY,
				innerWidth,
				cardHeight);
		add(modeCard);

		RenderedTextBlock readiness = label(
				state.deploymentReadinessHeadline(),
				BukovVisualContract.FONT_CAPTION,
				tokens.color(state.canDeploy
						? "accent.extract"
						: "accent.danger"));
		readiness.maxWidth(Math.max(1, (int) innerWidth));
		readiness.setPos(innerX, modeCard.bottom() + uiGap);
		add(readiness);

		float actionsY = Math.min(
				readiness.bottom() + uiGap,
				y + height - actionHeight - 6f);
		float third = (innerWidth - uiGap * 2f) / 3f;
		addButton(
				state.canDeploy
						? entryMessage(training
								? "hub.button_enter_training"
								: "hub.button_confirm")
						: entryMessage("hub.button_prepare"),
				ButtonGlyph.DEPLOY,
				innerX,
				actionsY,
				third,
				actionHeight,
				state.canDeploy ? "accent.extract" : "accent.danger",
				true,
				SPDAction.TAG_ATTACK,
				new Callback() {
					@Override
					public void call() {
						BukovHubViewModel currentState =
								controller.viewModel();
						if (currentState.canDeploy) {
							deploy();
							return;
						}
						try {
							controller.prepareAndConfirmDeployment();
							enterDeploymentScene();
						} catch (IOException | RuntimeException error) {
							showError(
									entryMessage("hub.error_prepare"),
									error);
						}
					}
				});
		addButton(
				entryMessage(training
						? "hub.button_training_area"
						: "hub.button_switch_area"),
				ButtonGlyph.AREA,
				innerX + third + uiGap,
				actionsY,
				third,
				actionHeight,
				"accent.interact",
				!training,
				SPDAction.TAG_RESUME,
				new Callback() {
					@Override
					public void call() {
						try {
							controller.cycleSelectedMap();
							reload();
						} catch (IOException | RuntimeException error) {
							showError(
									entryMessage("hub.error_area"),
									error);
						}
					}
				});
		addButton(
				entryMessage(training
						? "hub.button_training_loadout"
						: "hub.button_manage_loadout"),
				ButtonGlyph.LOADOUT,
				innerX + (third + uiGap) * 2f,
				actionsY,
				third,
				actionHeight,
				"accent.interact",
				!training,
				SPDAction.TAG_LOOT,
				new Callback() {
					@Override
					public void call() {
						openLoadout();
					}
				});

		if (!state.canDeploy
				&& actionsY + actionHeight + 18f < y + height) {
			RenderedTextBlock blocked = label(
					entryMessage(
							"hub.deployment_check",
							isEnglish()
									? entryMessage(
											"hub.deployment_blocked_generic")
									: state.deploymentBlockReason),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("accent.danger"));
			blocked.maxWidth(Math.max(1, (int) innerWidth));
			blocked.setPos(
					innerX,
					actionsY + actionHeight + uiGap);
			add(blocked);
		}
	}

	private void buildActiveRaidPanel(
			float x,
			float y,
			float width,
			float height) {
		addPanel(
				x, y, width, height,
				entryMessage("hub.panel_active"));
		float innerX = x + 6f;
		float innerY = y + 20f;
		float actionHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop(),
				uiScaleLevel);
		RenderedTextBlock mode = label(
				localizedModeName(controller.selectedRaidMode()),
				BukovVisualContract.FONT_SECTION,
				tokens.color("accent.extract"));
		mode.setPos(innerX, innerY);
		add(mode);

		int elapsed = Math.max(0, (int)state.activeElapsedSeconds);
		RenderedTextBlock summary = label(
				entryMessage(
						"hub.active_summary_compact",
						elapsed / 60,
						elapsed % 60),
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		summary.maxWidth(Math.max(1, (int) width - 12));
		summary.setPos(innerX, mode.bottom() + 5f);
		add(summary);

		float actionY = y + height - actionHeight - 6f;
		float half = (width - 12f - uiGap) / 2f;
		addButton(
				entryMessage("hub.button_continue"),
				ButtonGlyph.CONTINUE,
				innerX,
				actionY,
				half,
				actionHeight,
				"accent.extract",
				true,
				SPDAction.TAG_ATTACK,
				new Callback() {
					@Override
					public void call() {
						deploy();
					}
				});
		addButton(
				entryMessage("hub.button_abandon"),
				ButtonGlyph.ABANDON,
				innerX + half + uiGap,
				actionY,
				half,
				actionHeight,
				"accent.danger",
				true,
				SPDAction.WAIT,
				new Callback() {
					@Override
					public void call() {
						confirmAbandon();
					}
				});
	}

	private void buildFooter(
			float x,
			float y,
			float width,
			float height,
			boolean wide) {
		float gap = 3f;
		float buttonWidth = (width - gap * 2f) / 3f;
		float buttonHeight = (height - gap) / 2f;
		addButton(
				entryMessage("hub.button_contracts"),
				ButtonGlyph.CONTRACTS,
				x,
				y,
				buttonWidth,
				buttonHeight,
				"accent.extract",
				true,
				SPDAction.TAG_LOOT,
				new Callback() {
					@Override
					public void call() {
						openServices(
								WndBukovServices.Tab.CONTRACTS);
					}
				});
		addButton(
				entryMessage("hub.button_insurance"),
				ButtonGlyph.INSURANCE,
				x + buttonWidth + gap,
				y,
				buttonWidth,
				buttonHeight,
				"accent.valuable",
				true,
				SPDAction.TAG_RESUME,
				new Callback() {
					@Override
					public void call() {
						openServices(
								WndBukovServices.Tab.INSURANCE);
					}
				});
		addButton(
				entryMessage("hub.button_firearms"),
				ButtonGlyph.FIREARMS,
				x + (buttonWidth + gap) * 2f,
				y,
				buttonWidth,
				buttonHeight,
				"accent.interact",
				true,
				SPDAction.JOURNAL,
				new Callback() {
					@Override
					public void call() {
						openServices(
								WndBukovServices.Tab.FIREARMS);
					}
				});
		addButton(
				entryMessage(state.activeRaid
						? "hub.button_trade_locked"
						: "hub.button_vendor"),
				ButtonGlyph.VENDOR,
				x,
				y + buttonHeight + gap,
				buttonWidth,
				buttonHeight,
				state.activeRaid ? "panel.border" : "accent.valuable",
				!state.activeRaid,
				SPDAction.TAG_LOOT,
				new Callback() {
					@Override
					public void call() {
						openVendor();
					}
				});
		addButton(
				entryMessage("hub.button_settings"),
				ButtonGlyph.SETTINGS,
				x + buttonWidth + gap,
				y + buttonHeight + gap,
				buttonWidth,
				buttonHeight,
				"panel.border",
				true,
				SPDAction.TAG_RESUME,
				new Callback() {
					@Override
					public void call() {
						addToFront(new WndBukovSettings());
					}
				});
		addButton(
				entryMessage("hub.button_title"),
				ButtonGlyph.TITLE,
				x + (buttonWidth + gap) * 2f,
				y + buttonHeight + gap,
				buttonWidth,
				buttonHeight,
				"panel.border",
				true,
				SPDAction.WAIT,
				new Callback() {
					@Override
					public void call() {
						BukovMode.leave();
						ShatteredPixelDungeon.switchScene(TitleScene.class);
					}
				});
	}

	private void addMetric(
			String name,
			String value,
			float x,
			float y,
			String valueToken) {
		RenderedTextBlock nameBlock = label(
				name,
				BukovVisualContract.FONT_CAPTION,
				tokens.color("text.secondary"));
		nameBlock.setPos(x, y);
		add(nameBlock);
		RenderedTextBlock valueBlock = label(
				value,
				BukovVisualContract.FONT_SECTION,
				tokens.color(valueToken));
		valueBlock.setPos(x, y + 5f);
		add(valueBlock);
	}

	private void addPanel(
			float x,
			float y,
			float width,
			float height,
			String title) {
		ColorBlock surface = new ColorBlock(
				width,
				height,
				tokens.colorWithAlpha("panel.surface", 238));
		surface.x = x;
		surface.y = y;
		add(surface);
		ColorBlock edge = new ColorBlock(
				2f,
				height,
				tokens.color("accent.interact"));
		edge.x = x;
		edge.y = y;
		add(edge);
		ColorBlock rule = new ColorBlock(
				width,
				1f,
				tokens.color("panel.border"));
		rule.x = x;
		rule.y = y + 15f;
		add(rule);
		RenderedTextBlock heading = label(
				title,
				BukovVisualContract.FONT_BODY,
				tokens.color("text.primary"));
		heading.setPos(x + 6f, y + 4f);
		add(heading);
	}

	private void addButton(
			String text,
			ButtonGlyph glyph,
			float x,
			float y,
			float width,
			float height,
			String accentToken,
			boolean enabled,
			GameAction action,
			Callback callback) {
		TacticalButton button = new TacticalButton(
				text,
				glyph,
				tokens.color(accentToken),
				enabled,
				action,
				callback);
		button.setRect(x, y, width, height);
		add(button);
	}

	private RenderedTextBlock label(
			String value, String typography, int color) {
		RenderedTextBlock result = renderTextBlock(
				value, tokens.scaledTypographyPx(typography));
		result.hardlight(color);
		return result;
	}

	private static String entryMessage(String key, Object... args) {
		return BukovMessages.get("bukov.entry." + key, args);
	}

	private static boolean isEnglish() {
		return Messages.lang() == Languages.ENGLISH;
	}

	private static String localizedModeName(BukovRaidMode mode) {
		return entryMessage("hub.mode." + modeKey(mode) + ".name");
	}

	private static String localizedModeSummary(BukovRaidMode mode) {
		return entryMessage("hub.mode." + modeKey(mode) + ".summary");
	}

	private static String modeKey(BukovRaidMode mode) {
		switch (mode) {
			case EXPEDITION:
				return "expedition";
			case QUICK_SWEEP:
				return "quick_sweep";
			case SCAVENGER:
				return "scavenger";
			case BOSS_CONTRACT:
				return "boss_contract";
			case TRAINING_GROUND:
				return "training_ground";
			default:
				throw new IllegalArgumentException(
						"Unsupported Bukov raid mode: " + mode);
		}
	}

	private static String safeErrorDetail(Throwable error) {
		String detail = error.getMessage() == null
				? error.getClass().getSimpleName()
				: error.getMessage();
		return isEnglish() && containsCjk(detail)
				? entryMessage("hub.error_generic")
				: detail;
	}

	private static boolean containsCjk(String value) {
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character >= '\u2E80' && character <= '\u9FFF'
					|| character >= '\uF900'
							&& character <= '\uFAFF') {
				return true;
			}
		}
		return false;
	}

	private void openLoadout() {
		addToFront(new WndBukovHub(
				controller,
				new Callback() {
					@Override
					public void call() {
						enterDeploymentScene();
					}
				},
				new Callback() {
					@Override
					public void call() {
						reload();
					}
				}));
	}

	private void openRaidModeSelection() {
		addToFront(new WndBukovRaidModeSelection(
				controller,
				new Callback() {
					@Override
					public void call() {
						reload();
					}
				}));
	}

	private void openVendor() {
		addToFront(new WndBukovVendor(controller, new Callback() {
			@Override
			public void call() {
				reload();
			}
		}));
	}

	private void openServices(WndBukovServices.Tab tab) {
		addToFront(new WndBukovServices(
				controller,
				new Callback() {
					@Override
					public void call() {
						reload();
					}
				},
				tab));
	}

	private void deploy() {
		try {
			controller.confirmDeployment();
			enterDeploymentScene();
		} catch (IOException | RuntimeException error) {
			showError(entryMessage("hub.error_deploy"), error);
		}
	}

	private void enterDeploymentScene() {
		BukovMode.enter();
		BukovMode.prepareRaidMode(controller.selectedRaidMode());
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		ShatteredPixelDungeon.switchScene(BukovDeploymentScene.class);
	}

	private void confirmAbandon() {
		addToFront(new AbandonConfirmWindow());
	}

	private final class AbandonConfirmWindow extends Window {

		private static final int CANCEL = 0;
		private static final int CONFIRM = 1;
		private static final int BUTTON_HEIGHT = 22;
		private static final int MARGIN = 6;
		private static final int BUTTON_GAP = 4;

		private final BukovFocusModel focus =
				new BukovFocusModel(2, CANCEL);
		private final BukovFocusRepeater focusRepeater =
				new BukovFocusRepeater();
		private final ConfirmButton[] buttons = new ConfirmButton[2];
		private boolean submitting;

		private AbandonConfirmWindow() {
			super(
					0,
					0,
					new NinePatch(
							TextureCache.createSolid(
									BukovHubScene.this.tokens.colorWithAlpha(
											"ink.background", 255)),
							0));
			RectF insets = BukovHubScene.this.getCommonInsets();
			int availableWidth = Math.max(
					1,
					(int)Math.floor(
							Camera.main.width
									- insets.left - insets.right - 8f));
			int availableHeight = Math.max(
					1,
					(int)Math.floor(
							Camera.main.height
									- insets.top - insets.bottom - 8f));
			int windowWidth = Math.min(
					DeviceCompat.isDesktop() ? 176 : 160,
					availableWidth);
			int windowHeight = Math.min(108, availableHeight);
			resize(windowWidth, windowHeight);

			ColorBlock header = new ColorBlock(
					windowWidth,
					18,
					tokens.colorWithAlpha("panel.surface", 255));
			add(header);
			ColorBlock headerEdge = new ColorBlock(
					windowWidth,
					1,
					tokens.color("accent.danger"));
			headerEdge.y = 17;
			add(headerEdge);

			RenderedTextBlock title = label(
					entryMessage("hub.abandon_title"),
					BukovVisualContract.FONT_BODY,
					tokens.color("accent.danger"));
			title.setPos(MARGIN, 5);
			title.maxWidth(Math.max(1, windowWidth - MARGIN * 2));
			add(title);

			RenderedTextBlock body = label(
					entryMessage("hub.abandon_body"),
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			body.maxWidth(Math.max(1, windowWidth - MARGIN * 2));
			body.setPos(MARGIN, 23);
			add(body);

			float buttonWidth =
					(windowWidth - MARGIN * 2f - BUTTON_GAP) / 2f;
			float buttonY = windowHeight - BUTTON_HEIGHT - MARGIN;
			buttons[CANCEL] = new ConfirmButton(
					BukovTouchIcon.Glyph.BACK,
					entryMessage("hub.cancel"),
					false);
			buttons[CANCEL].setRect(
					MARGIN,
					buttonY,
					buttonWidth,
					BUTTON_HEIGHT);
			add(buttons[CANCEL]);
			buttons[CONFIRM] = new ConfirmButton(
					BukovTouchIcon.Glyph.DROP,
					entryMessage("hub.confirm_abandon"),
					true);
			buttons[CONFIRM].setRect(
					MARGIN + buttonWidth + BUTTON_GAP,
					buttonY,
					buttonWidth,
					BUTTON_HEIGHT);
			add(buttons[CONFIRM]);
			updateFocus();
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
		public boolean onSignal(KeyEvent event) {
			if (!event.pressed) {
				return true;
			}
			if (BukovNavigation.back(event)) {
				cancel();
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
		public void onBackPressed() {
			cancel();
		}

		private void updateFocus() {
			for (int index = 0; index < buttons.length; index++) {
				if (buttons[index] != null) {
					buttons[index].setFocused(focus.index() == index);
				}
			}
		}

		private void cancel() {
			BukovUiSoundRouter.play(BukovUiSoundPlayer.Cue.CANCEL);
			hide();
		}

		private void submit() {
			if (submitting) {
				return;
			}
			submitting = true;
			BukovUiSoundRouter.play(BukovUiSoundPlayer.Cue.CONFIRM);
			hide();
			try {
				controller.abandonActiveRaid();
				Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);
				reload();
			} catch (IOException | RuntimeException error) {
				showError(
						entryMessage("hub.error_abandon"),
						error);
			}
		}

		private final class ConfirmButton
				extends BukovIconLabelButton {

			private final boolean confirms;
			private final ColorBlock accent;

			private ConfirmButton(
					BukovTouchIcon.Glyph glyph,
					String text,
					boolean confirms) {
				super(glyph, text, true);
				this.confirms = confirms;
				accent = new ColorBlock(
						1,
						1,
						tokens.color(confirms
								? "accent.danger"
								: "panel.border"));
				add(accent);
			}

			@Override
			protected void onClick() {
				focus.focus(confirms ? CONFIRM : CANCEL);
				updateFocus();
				if (confirms) {
					submit();
				} else {
					cancel();
				}
			}

			@Override
			protected void layout() {
				super.layout();
				if (accent == null) {
					return;
				}
				accent.x = x;
				accent.y = y;
				accent.size(2f, height);
			}
		}
	}

	private void reload() {
		ShatteredPixelDungeon.switchNoFade(BukovHubScene.class);
	}

	private void showError(String title, Throwable error) {
		ShatteredPixelDungeon.reportException(error);
		BukovUiSoundRouter.play(BukovUiSoundPlayer.Cue.ERROR);
		addToFront(new WndMessage(entryMessage(
				"hub.error_format",
				title,
				safeErrorDetail(error))));
	}

	private void buildFailure(Throwable error) {
		BukovUiSoundRouter.play(BukovUiSoundPlayer.Cue.ERROR);
		BukovUiTokens tokens = BukovUiTokens.loadDefault();
		ColorBlock background = new ColorBlock(
				Camera.main.width,
				Camera.main.height,
				tokens.colorWithAlpha("ink.failure", 255));
		add(background);
		addToFront(new WndMessage(entryMessage(
				"hub.error_load",
				safeErrorDetail(error))) {
			@Override
			public void onBackPressed() {
				super.onBackPressed();
				BukovMode.leave();
				ShatteredPixelDungeon.switchScene(TitleScene.class);
			}
		});
	}

	private abstract class ModeCard extends Button {

		private final NinePatch surface;
		private final NinePatch pressed;
		private final ColorBlock edge;
		private final ColorBlock selection;
		private final RenderedTextBlock title;
		private final RenderedTextBlock detail;

		private ModeCard(
				String titleText,
				String detailText,
				boolean selected,
				boolean training) {
			surface = BukovUiAssets.surface(
					selected
							? BukovUiAssets.Surface.BUTTON_FOCUSED
							: BukovUiAssets.Surface.PANEL_RAISED,
					tokens.color(selected
							? "accent.extract" : "panel.surface"));
			addToBack(surface);
			pressed = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_PRESSED,
					tokens.color("panel.border"));
			pressed.visible = false;
			addToBack(pressed);
			edge = new ColorBlock(
					1f,
					1f,
					tokens.color(selected
							? "accent.extract"
							: training
							? "accent.interact"
							: "panel.border"));
			add(edge);
			selection = new ColorBlock(
					1f,
					1f,
					tokens.color(selected
							? "accent.extract" : "text.disabled"));
			add(selection);
			title = label(
					titleText + (selected
							? "  ·  " + entryMessage("hub.selected")
							: ""),
					BukovVisualContract.FONT_BODY,
					tokens.color(selected
							? "accent.extract" : "text.primary"));
			add(title);
			detail = label(
					detailText,
					BukovVisualContract.FONT_CAPTION,
					tokens.color("text.secondary"));
			add(detail);
		}

		protected abstract void activate();

		@Override
		protected void onClick() {
			BukovUiSoundRouter.play(
					BukovUiSoundPlayer.Cue.CONFIRM);
			activate();
		}

		@Override
		protected void onPointerDown() {
			surface.visible = false;
			pressed.visible = true;
		}

		@Override
		protected void onPointerUp() {
			surface.visible = true;
			pressed.visible = false;
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
			edge.x = x;
			edge.y = y;
			edge.size(2f, height);
			selection.x = x;
			selection.y = y;
			selection.size(width, 1f);
			title.maxWidth(Math.max(1, (int) width - 10));
			title.setPos(x + 6f, y + 6f);
			detail.maxWidth(Math.max(1, (int) width - 10));
			detail.setPos(x + 6f, title.bottom() + 4f);
		}
	}

	private final class TacticalButton extends Button {

		private final NinePatch surface;
		private final NinePatch pressed;
		private final ColorBlock edge;
		private final ColorBlock lowerRule;
		private final RenderedTextBlock text;
		private final Image icon;
		private final boolean enabled;
		private final GameAction action;
		private final Callback callback;

		private TacticalButton(
				String value,
				ButtonGlyph glyph,
				int accent,
				boolean enabled,
				GameAction action,
				Callback callback) {
			this.enabled = enabled;
			this.action = action;
			this.callback = callback;
			surface = BukovUiAssets.surface(
					enabled
							? BukovUiAssets.Surface.BUTTON
							: BukovUiAssets.Surface.BUTTON_DISABLED,
					enabled ? accent : tokens.color("panel.deep"));
			surface.alpha(enabled ? 0.82f : 0.72f);
			addToBack(surface);
			pressed = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_PRESSED,
					tokens.color("panel.border"));
			pressed.visible = false;
			addToBack(pressed);
			edge = new ColorBlock(
					1f,
					1f,
					enabled ? accent : tokens.color("text.disabled"));
			add(edge);
			lowerRule = new ColorBlock(
					1f,
					1f,
					enabled ? accent : tokens.color("text.disabled"));
			lowerRule.alpha(enabled ? 0.65f : 0.25f);
			add(lowerRule);
			text = label(
					value,
					BukovVisualContract.FONT_CAPTION,
					tokens.color(enabled
							? "text.primary" : "text.disabled"));
			text.align(RenderedTextBlock.CENTER_ALIGN);
			add(text);
			icon = glyph.image(
					enabled ? accent : tokens.color("text.disabled"));
			icon.hardlight(
					enabled ? accent : tokens.color("text.disabled"));
			icon.alpha(enabled ? 1f : 0.48f);
			add(icon);
		}

		@Override
		protected void onClick() {
			if (enabled && callback != null) {
				BukovUiSoundRouter.play(
						BukovUiSoundPlayer.Cue.CONFIRM);
				callback.call();
			} else {
				BukovUiSoundRouter.play(
						BukovUiSoundPlayer.Cue.ERROR);
			}
		}

		@Override
		protected void onPointerDown() {
			if (!enabled) {
				return;
			}
			surface.visible = false;
			pressed.visible = true;
			layout();
		}

		@Override
		protected void onPointerUp() {
			surface.visible = true;
			pressed.visible = false;
			layout();
		}

		@Override
		public GameAction keyAction() {
			return enabled ? action : null;
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
			edge.x = x;
			edge.y = y;
			edge.size(3f, height);
			lowerRule.x = x;
			lowerRule.y = y + height - 1f;
			lowerRule.size(width, 1f);
			float iconSide = Math.max(
					8f, Math.min(14f, height - 8f));
			float iconScale = iconSide / BukovUiAssets.TILE_SIZE;
			icon.scale.set(iconScale);
			float textWidth = Math.max(
					1f, width - iconSide - 13f);
			text.maxWidth(Math.max(1, (int) textWidth));
			float groupWidth = iconSide + 4f + text.width();
			float groupLeft = x + (width - groupWidth) / 2f;
			icon.x = PixelScene.align(groupLeft);
			icon.y = PixelScene.align(
					y + (height - iconSide) / 2f
							+ (pressed.visible ? 1f : 0f));
			text.setPos(
					icon.x + iconSide + 4f,
					y + (height - text.height()) / 2f);
		}
	}

	private enum ButtonGlyph {
		DEPLOY,
		AREA,
		LOADOUT,
		CONTINUE,
		ABANDON,
		CONTRACTS,
		INSURANCE,
		FIREARMS,
		VENDOR,
		SETTINGS,
		TITLE;

		private Image image(int fallbackColor) {
			switch (this) {
				case DEPLOY:
				case CONTINUE:
					return BukovUiAssets.icon(
							BukovUiAssets.StatusIcon.ACTION,
							fallbackColor);
				case AREA:
					return BukovUiAssets.hud(
							BukovUiAssets.HudElement.OBJECTIVE,
							fallbackColor);
				case LOADOUT:
				case VENDOR:
					return BukovUiAssets.icon(
							BukovUiAssets.StatusIcon.LOOT,
							fallbackColor);
				case ABANDON:
					return BukovUiAssets.icon(
							BukovUiAssets.StatusIcon.DANGER,
							fallbackColor);
				case CONTRACTS:
					return BukovUiAssets.hud(
							BukovUiAssets.HudElement.OBJECTIVE,
							fallbackColor);
				case INSURANCE:
					return BukovUiAssets.hud(
							BukovUiAssets.HudElement.ARMOR,
							fallbackColor);
				case FIREARMS:
					return BukovUiAssets.touchGlyph(
							BukovUiAssets.TouchGlyph.AIM_FIRE,
							fallbackColor);
				case SETTINGS:
					return BukovUiAssets.touchGlyph(
							BukovUiAssets.TouchGlyph.PAUSE,
							fallbackColor);
				case TITLE:
					return BukovUiAssets.icon(
							BukovUiAssets.StatusIcon.EXTRACT,
							fallbackColor);
				default:
					throw new IllegalStateException(
							"Unsupported button glyph: " + this);
			}
		}
	}
}
