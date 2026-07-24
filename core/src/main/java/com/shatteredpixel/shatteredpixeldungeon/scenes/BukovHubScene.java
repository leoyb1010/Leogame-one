package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubViewModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovVisualContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovHub;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovVendor;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.gltextures.SmartTexture;
import com.watabou.input.GameAction;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
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

	@Override
	public void create() {
		super.create();
		Music.INSTANCE.end();
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		uiCamera.visible = false;
		tokens = BukovUiTokens.loadDefault();

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

	private void build() {
		final int screenWidth = Camera.main.width;
		final int screenHeight = Camera.main.height;
		final boolean wide = landscape();
		final RectF insets = getCommonInsets();
		final float safeWidth =
				screenWidth - insets.left - insets.right;
		final float usableWidth = BukovVisualContract.contentWidth(
				safeWidth, wide);
		final float left = BukovVisualContract.centeredLeft(
				insets.left, safeWidth, usableWidth);
		final float top = insets.top + MARGIN;
		final float usableHeight =
				screenHeight - insets.top - insets.bottom - MARGIN * 2;

		addBackdrop(screenWidth, screenHeight, wide);

		RenderedTextBlock eyebrow = label(
				"ESCAPE FROM BUKOV  /  OFFLINE OPERATIONS",
				6,
				tokens.color("text.secondary"));
		eyebrow.setPos(left, top);
		add(eyebrow);

		RenderedTextBlock title = label(
				"布科夫藏身处",
				wide ? 15 : 13,
				tokens.color("accent.valuable"));
		title.setPos(left, eyebrow.bottom() + 2f);
		add(title);

		String statusText = state.activeRaid
				? "ACTIVE RAID  ·  行动检查点已锁定"
				: "READY  ·  " + state.careerSummary;
		RenderedTextBlock status = label(
				statusText,
				6,
				tokens.color(state.activeRaid
						? "accent.extract" : "text.secondary"));
		status.setPos(left, title.bottom() + 2f);
		add(status);

		float contentTop = status.bottom() + 5f;
		float footerButtonHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop());
		float footerHeight = footerButtonHeight * 2f + 3f;
		float availableContentHeight = Math.max(
				58f,
				top + usableHeight - contentTop - footerHeight);
		float contentHeight = wide
				? Math.min(145f, availableContentHeight)
				: availableContentHeight;
		float leftWidth = wide
				? Math.max(74f, usableWidth * 0.32f)
				: usableWidth;
		float rightLeft = wide ? left + leftWidth + GAP : left;
		float rightWidth = wide
				? usableWidth - leftWidth - GAP
				: usableWidth;
		float leftHeight = wide
				? contentHeight
				: Math.min(38f, contentHeight * 0.30f);
		float rightTop = wide
				? contentTop
				: contentTop + leftHeight + GAP;
		float rightHeight = wide
				? contentHeight
				: contentHeight - leftHeight - GAP;

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
						? contentTop + contentHeight + GAP
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
		addPanel(x, y, width, height, "仓库状态 / STASH");
		float textLeft = x + 6f;
		float textTop = y + 18f;

		if (!landscape()) {
			float column = (width - 12f) / 3f;
			addMetric(
					"现金",
					String.valueOf(state.currency),
					textLeft,
					textTop,
					"accent.valuable");
			addMetric(
					"仓库价值",
					String.valueOf(state.stashValue),
					textLeft + column,
					textTop,
					"text.primary");
			addMetric(
					state.activeRaid ? "行动携带" : "本次风险",
					String.valueOf(state.riskValue),
					textLeft + column * 2f,
					textTop,
					state.activeRaid
							? "accent.extract" : "text.secondary");
			return;
		}

		addMetric(
				"现金",
				String.valueOf(state.currency),
				textLeft,
				textTop,
				"accent.valuable");
		addMetric(
				"仓库价值",
				String.valueOf(state.stashValue),
				textLeft,
				textTop + 15f,
				"text.primary");
		addMetric(
				state.activeRaid ? "行动携带" : "本次风险",
				String.valueOf(state.riskValue),
				textLeft,
				textTop + 30f,
				state.activeRaid ? "accent.extract" : "text.secondary");

		if (height > 70f) {
			RenderedTextBlock contract = label(
					"当前合同  " + state.activeContract
							+ "\n" + state.activeContractObjective,
					6,
					tokens.color("accent.extract"));
			contract.maxWidth(Math.max(1, (int) width - 12));
			contract.setPos(textLeft, textTop + 47f);
			add(contract);

			RenderedTextBlock loadout = label(
					state.activeRaid
							? "配装与交易在行动结束前锁定"
							: state.deploymentReadinessHeadline()
									+ "\n负重 " + state.loadoutSummary()
									+ "\n已选 " + state.selectedCount + " 件物资",
					6,
					tokens.color(state.activeRaid
							? "text.disabled"
							: state.canDeploy
									? "accent.extract"
									: "accent.danger"));
			loadout.maxWidth(Math.max(1, (int) width - 12));
			loadout.setPos(textLeft, y + height - loadout.height() - 7f);
			add(loadout);
		}
	}

	private void buildDeploymentPanel(
			float x,
			float y,
			float width,
			float height) {
		addPanel(x, y, width, height, "行动选择 / DEPLOYMENT");
		boolean wide = landscape();
		float actionHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop());
		float innerX = x + 5f;
		float innerY = y + 18f;
		float innerWidth = width - 10f;
		float cardGap = 4f;
		float cardWidth = wide
				? (innerWidth - cardGap) / 2f
				: innerWidth;
		float cardHeight = wide
				? Math.max(27f,
						Math.min(46f, height - actionHeight - 25f))
				: Math.max(23f,
						Math.min(32f,
								(height - actionHeight - 31f) / 2f));

		boolean training = controller.selectedRaidMode().trainingGround();
		BukovRaidMode formal = training
				? BukovRaidMode.EXPEDITION
				: controller.selectedRaidMode();
		ModeCard formalCard = new ModeCard(
				"正式行动  /  " + formal.displayName,
				state.selectedMapName + " · " + formal.summary,
				!training,
				false) {
			@Override
			protected void activate() {
				try {
					controller.cycleFormalRaidMode();
					reload();
				} catch (IOException | RuntimeException error) {
					showError("行动选择失败", error);
				}
			}
		};
		formalCard.setRect(
				innerX,
				innerY,
				cardWidth,
				cardHeight);
		add(formalCard);

		float trainingX = wide ? innerX + cardWidth + cardGap : innerX;
		float trainingY = wide ? innerY : formalCard.bottom() + cardGap;
		ModeCard trainingCard = new ModeCard(
				"演练场  /  TRAINING",
				"3-5分钟 · 免费制式装备 · 无损失 · 随时进入",
				training,
				true) {
			@Override
			protected void activate() {
				try {
					controller.selectTrainingGround();
					reload();
				} catch (IOException | RuntimeException error) {
					showError("演练场选择失败", error);
				}
			}
		};
		trainingCard.setRect(
				trainingX,
				trainingY,
				cardWidth,
				cardHeight);
		add(trainingCard);

		float actionsY = Math.max(
				formalCard.bottom(),
				trainingCard.bottom()) + 5f;
		float third = (innerWidth - GAP * 2f) / 3f;
		addButton(
				state.canDeploy
						? (training ? "进入演练场" : "确认出击")
						: "配装不完整",
				innerX,
				actionsY,
				third,
				actionHeight,
				state.canDeploy ? "accent.extract" : "accent.danger",
				state.canDeploy,
				SPDAction.TAG_ATTACK,
				new Callback() {
					@Override
					public void call() {
						deploy();
					}
				});
		addButton(
				training ? "固定训练区" : "切换区域",
				innerX + third + GAP,
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
							showError("区域选择失败", error);
						}
					}
				});
		addButton(
				training ? "训练装备" : "管理配装",
				innerX + (third + GAP) * 2f,
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

		if (!state.canDeploy && actionsY + 27f < y + height) {
			RenderedTextBlock blocked = label(
					"出击检查  /  " + state.deploymentBlockReason,
					6,
					tokens.color("accent.danger"));
			blocked.maxWidth(Math.max(1, (int) innerWidth));
			blocked.setPos(innerX, actionsY + 22f);
			add(blocked);
		}
	}

	private void buildActiveRaidPanel(
			float x,
			float y,
			float width,
			float height) {
		addPanel(x, y, width, height, "行动检查点 / ACTIVE RAID");
		float innerX = x + 6f;
		float innerY = y + 20f;
		float actionHeight = BukovVisualContract.controlHeight(
				!DeviceCompat.isDesktop());
		RenderedTextBlock mode = label(
				controller.selectedRaidMode().displayName,
				12,
				tokens.color("accent.extract"));
		mode.setPos(innerX, innerY);
		add(mode);

		RenderedTextBlock summary = label(
				state.activeRaidSummary()
						+ "\n\n已锁定：行动模式、仓库交易、出战配装"
						+ "\n可用：继续行动，或确认放弃并结算损失",
				6,
				tokens.color("text.secondary"));
		summary.maxWidth(Math.max(1, (int) width - 12));
		summary.setPos(innerX, mode.bottom() + 5f);
		add(summary);

		float actionY = y + height - actionHeight - 6f;
		float half = (width - 12f - GAP) / 2f;
		addButton(
				"继续行动",
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
				"放弃行动",
				innerX + half + GAP,
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
				"合同",
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
				"保险",
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
				"改枪",
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
				state.activeRaid ? "交易锁定" : "补给商店",
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
				"设置",
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
				"返回标题",
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
				6,
				tokens.color("text.secondary"));
		nameBlock.setPos(x, y);
		add(nameBlock);
		RenderedTextBlock valueBlock = label(
				value,
				9,
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
				7,
				tokens.color("text.primary"));
		heading.setPos(x + 6f, y + 4f);
		add(heading);
	}

	private void addButton(
			String text,
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
				tokens.color(accentToken),
				enabled,
				action,
				callback);
		button.setRect(x, y, width, height);
		add(button);
	}

	private RenderedTextBlock label(String value, int size, int color) {
		RenderedTextBlock result = renderTextBlock(value, size);
		result.hardlight(color);
		return result;
	}

	private void openLoadout() {
		addToFront(new WndBukovHub(controller, new Callback() {
			@Override
			public void call() {
				deploy();
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
			BukovMode.enter();
			BukovMode.prepareRaidMode(controller.selectedRaidMode());
			GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
			Dungeon.hero = null;
			Dungeon.daily = Dungeon.dailyReplay = false;
			ShatteredPixelDungeon.switchScene(BukovDeploymentScene.class);
		} catch (IOException | RuntimeException error) {
			showError("出击确认失败", error);
		}
	}

	private void confirmAbandon() {
		addToFront(new WndOptions(
				"放弃本次行动？",
				"当前行动会按未撤离结算，正式行动中携带的物资将按规则损失。此操作不可撤销。",
				"取消",
				"确认放弃") {
			@Override
			protected void onSelect(int index) {
				if (index != 1) {
					return;
				}
				try {
					controller.abandonActiveRaid();
					Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);
					reload();
				} catch (IOException | RuntimeException error) {
					showError("行动放弃失败", error);
				}
			}
		});
	}

	private void reload() {
		ShatteredPixelDungeon.switchNoFade(BukovHubScene.class);
	}

	private void showError(String title, Throwable error) {
		ShatteredPixelDungeon.reportException(error);
		String detail = error.getMessage() == null
				? error.getClass().getSimpleName()
				: error.getMessage();
		addToFront(new WndMessage(title + "：\n" + detail));
	}

	private void buildFailure(Throwable error) {
		BukovUiTokens tokens = BukovUiTokens.loadDefault();
		ColorBlock background = new ColorBlock(
				Camera.main.width,
				Camera.main.height,
				tokens.colorWithAlpha("ink.failure", 255));
		add(background);
		String detail = error.getMessage() == null
				? error.getClass().getSimpleName()
				: error.getMessage();
		addToFront(new WndMessage("藏身处读取失败：\n" + detail) {
			@Override
			public void onBackPressed() {
				super.onBackPressed();
				BukovMode.leave();
				ShatteredPixelDungeon.switchScene(TitleScene.class);
			}
		});
	}

	private abstract class ModeCard extends Button {

		private final ColorBlock surface;
		private final ColorBlock pressed;
		private final ColorBlock edge;
		private final ColorBlock selection;
		private final RenderedTextBlock title;
		private final RenderedTextBlock detail;

		private ModeCard(
				String titleText,
				String detailText,
				boolean selected,
				boolean training) {
			surface = new ColorBlock(
					1f,
					1f,
					tokens.colorWithAlpha(
							selected ? "accent.extract" : "panel.surface",
							selected ? 38 : 244));
			addToBack(surface);
			pressed = new ColorBlock(
					1f, 1f, tokens.color("panel.border"));
			pressed.alpha(0.55f);
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
					titleText + (selected ? "  ·  已选择" : ""),
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
			activate();
		}

		@Override
		protected void onPointerDown() {
			surface.visible = false;
			pressed.visible = true;
			Sample.INSTANCE.play(Assets.Sounds.CLICK);
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

		private final ColorBlock surface;
		private final ColorBlock pressed;
		private final ColorBlock edge;
		private final ColorBlock lowerRule;
		private final RenderedTextBlock text;
		private final boolean enabled;
		private final GameAction action;
		private final Callback callback;

		private TacticalButton(
				String value,
				int accent,
				boolean enabled,
				GameAction action,
				Callback callback) {
			this.enabled = enabled;
			this.action = action;
			this.callback = callback;
			surface = new ColorBlock(1f, 1f, accent);
			surface.alpha(enabled ? 0.18f : 0.06f);
			addToBack(surface);
			pressed = new ColorBlock(
					1f, 1f, tokens.color("panel.border"));
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
					BukovVisualContract.FONT_BODY,
					tokens.color(enabled
							? "text.primary" : "text.disabled"));
			text.align(RenderedTextBlock.CENTER_ALIGN);
			add(text);
		}

		@Override
		protected void onClick() {
			if (enabled && callback != null) {
				callback.call();
			}
		}

		@Override
		protected void onPointerDown() {
			if (!enabled) {
				return;
			}
			surface.visible = false;
			pressed.visible = true;
			Sample.INSTANCE.play(Assets.Sounds.CLICK);
		}

		@Override
		protected void onPointerUp() {
			surface.visible = true;
			pressed.visible = false;
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
			text.maxWidth(Math.max(1, (int) width - 8));
			text.setPos(
					x + (width - text.width()) / 2f,
					y + (height - text.height()) / 2f);
		}
	}
}
