package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
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

/**
 * Complete, controller-navigable settings surface for Bukov mode.
 *
 * Accessibility controls deliberately occupy the first four rows instead of a
 * secondary page. Every click writes through SPDSettings immediately.
 */
public final class WndBukovSettings extends Window {

	private static final int PORTRAIT_WIDTH = 172;
	private static final int PORTRAIT_HEIGHT = 214;
	private static final int LANDSCAPE_WIDTH = 230;
	private static final int LANDSCAPE_HEIGHT = 166;
	private static final int MARGIN = 4;
	private static final int HEADER_HEIGHT = 32;
	private static final int FOOTER_HEIGHT = 26;
	private static final int ROW_HEIGHT = 20;
	private static final int GAP = 2;

	private static final int[] DEAD_ZONE_INNER = {10, 16, 22};
	private static final int[] DEAD_ZONE_OUTER = {100, 96, 92};
	private static final int[] TRIGGER_PRESS = {55, 65, 75};
	private static final int[] TRIGGER_RELEASE = {35, 45, 55};

	private final Runnable closeListener;
	private final BukovUiTokens tokens;
	private final SettingButton[] buttons =
			new SettingButton[Setting.values().length];
	private final BukovFocusModel focus =
			new BukovFocusModel(Setting.values().length, 0);
	private final BukovFocusRepeater focusRepeater =
			new BukovFocusRepeater();
	private ScrollPane scroll;
	private boolean closeReported;

	public WndBukovSettings() {
		this(null);
	}

	public WndBukovSettings(Runnable closeListener) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)), 0));
		this.closeListener = closeListener;
		tokens = BukovUiTokens.loadDefault();
		int windowWidth = BukovWindowLayout.safeWidth(
				PixelScene.landscape()
						? LANDSCAPE_WIDTH : PORTRAIT_WIDTH);
		int windowHeight = BukovWindowLayout.safeHeight(
				PixelScene.landscape()
						? LANDSCAPE_HEIGHT : PORTRAIT_HEIGHT);
		resize(windowWidth, windowHeight);
		build(windowWidth, windowHeight);
		updateFocus();
	}

	private void build(int windowWidth, int windowHeight) {
		ColorBlock header = new ColorBlock(
				windowWidth,
				HEADER_HEIGHT - 2,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = HEADER_HEIGHT - 3;
		add(headerRule);

		RenderedTextBlock eyebrow = PixelScene.renderTextBlock(
				"SYSTEM / ACCESSIBILITY / AUDIO",
				tokens.typographyPx(
						BukovVisualContract.FONT_CAPTION));
		eyebrow.hardlight(tokens.color("text.secondary"));
		eyebrow.setPos(MARGIN + 2, 4);
		add(eyebrow);

		RenderedTextBlock title = PixelScene.renderTextBlock(
				"行动体验设置",
				tokens.typographyPx(
						BukovVisualContract.FONT_BODY));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(MARGIN + 2, 14);
		add(title);
		RenderedTextBlock saved = PixelScene.renderTextBlock(
				"即时生效 · 本地保存",
				tokens.typographyPx(
						BukovVisualContract.FONT_CAPTION));
		saved.hardlight(tokens.color("accent.extract"));
		saved.setPos(windowWidth - MARGIN - saved.width() - 2, 17);
		add(saved);

		SettingsList list = new SettingsList(windowWidth - MARGIN * 2);
		scroll = new ScrollPane(list);
		add(scroll);
		scroll.setRect(
				MARGIN,
				HEADER_HEIGHT,
				windowWidth - MARGIN * 2,
				windowHeight - HEADER_HEIGHT - FOOTER_HEIGHT);

		SettingButton close = new SettingButton(Setting.CLOSE);
		buttons[Setting.CLOSE.ordinal()] = close;
		close.setRect(
				MARGIN,
				windowHeight - FOOTER_HEIGHT + 3,
				windowWidth - MARGIN * 2,
				ROW_HEIGHT + 1);
		add(close);
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
				landscape ? LANDSCAPE_HEIGHT : PORTRAIT_HEIGHT);
	}

	@Override
	public void hide() {
		super.hide();
		if (!closeReported && closeListener != null) {
			closeReported = true;
			closeListener.run();
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
		if (BukovNavigation.back(event)) {
			hide();
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

	private void updateFocus() {
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				buttons[i].setFocused(focus.index() == i);
			}
		}
		if (focus.index() < Setting.CLOSE.ordinal() && scroll != null) {
			float rowY = focus.index() * (ROW_HEIGHT + GAP);
			scroll.scrollTo(0, Math.max(0, rowY - ROW_HEIGHT));
		}
	}

	private enum Setting {
		REDUCE_MOTION,
		REDUCE_FLASHES,
		COLORBLIND,
		SOUND_VISUALIZATION,
		MASTER,
		MUSIC,
		SFX,
		AMBIENCE,
		PERFORMANCE,
		UI_SCALE,
		SHAKE,
		VIBRATION,
		DAMAGE_NUMBERS,
		AIM_ASSIST,
		LEFT_DEAD_ZONE,
		RIGHT_DEAD_ZONE,
		AIM_CURVE,
		TRIGGER,
		BRIGHTNESS,
		LEGAL,
		CLOSE
	}

	private final class SettingsList extends Component {

		private SettingsList(float listWidth) {
			int count = Setting.CLOSE.ordinal();
			setSize(listWidth, count * (ROW_HEIGHT + GAP) - GAP);
			for (int i = 0; i < count; i++) {
				Setting setting = Setting.values()[i];
				SettingButton button = new SettingButton(setting);
				button.setRect(
						0,
						i * (ROW_HEIGHT + GAP),
						listWidth,
						ROW_HEIGHT);
				buttons[i] = button;
				add(button);
			}
		}
	}

	private final class SettingButton extends Button {

		private final Setting setting;
		private final ColorBlock background;
		private final ColorBlock edge;
		private final ColorBlock focusEdge;
		private final ColorBlock valueSurface;
		private final RenderedTextBlock label;
		private final RenderedTextBlock value;

		private SettingButton(Setting setting) {
			this.setting = setting;
			background = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha("panel.surface", 235));
			addToBack(background);
			edge = new ColorBlock(
					1,
					1,
					setting == Setting.CLOSE
							? tokens.color("accent.valuable")
							: tokens.color("accent.interact"));
			add(edge);
			focusEdge = new ColorBlock(
					1, 1, tokens.color("accent.interact"));
			focusEdge.visible = false;
			add(focusEdge);
			valueSurface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha(
							setting == Setting.CLOSE
									? "accent.extract"
									: "ink.background",
							setting == Setting.CLOSE ? 44 : 190));
			add(valueSurface);
			label = PixelScene.renderTextBlock(
					tokens.typographyPx(
							BukovVisualContract.FONT_BODY));
			label.hardlight(tokens.color("text.primary"));
			add(label);
			value = PixelScene.renderTextBlock(
					tokens.typographyPx(
							BukovVisualContract.FONT_BODY));
			value.hardlight(tokens.color(
					setting == Setting.CLOSE
							? "accent.extract"
							: "accent.interact"));
			value.align(RenderedTextBlock.RIGHT_ALIGN);
			add(value);
			refreshLabel();
		}

		@Override
		protected void onClick() {
			focus.focus(setting.ordinal());
			switch (setting) {
				case REDUCE_MOTION:
					SPDSettings.bukovReduceMotion(
							!SPDSettings.bukovReduceMotion());
					break;
				case REDUCE_FLASHES:
					SPDSettings.bukovReduceFlashes(
							!SPDSettings.bukovReduceFlashes());
					break;
				case COLORBLIND:
					SPDSettings.bukovColorblindAssist(
							!SPDSettings.bukovColorblindAssist());
					break;
				case SOUND_VISUALIZATION:
					SPDSettings.bukovSoundVisualization(
							!SPDSettings.bukovSoundVisualization());
					break;
				case MASTER:
					SPDSettings.bukovMasterVolume(nextVolume(
							SPDSettings.bukovMasterVolume()));
					break;
				case MUSIC:
					SPDSettings.bukovMusicVolume(nextVolume(
							SPDSettings.bukovMusicVolume()));
					break;
				case SFX:
					SPDSettings.bukovSfxVolume(nextVolume(
							SPDSettings.bukovSfxVolume()));
					break;
				case AMBIENCE:
					SPDSettings.bukovAmbienceVolume(nextVolume(
							SPDSettings.bukovAmbienceVolume()));
					break;
				case PERFORMANCE:
					SPDSettings.bukovPerformanceProfile(
							(SPDSettings.bukovPerformanceProfile() + 1) % 3);
					break;
				case UI_SCALE:
					SPDSettings.bukovUiScale(
							(SPDSettings.bukovUiScale() + 1) % 3);
					break;
				case SHAKE:
					SPDSettings.screenShake(
							nextThreeLevel(SPDSettings.screenShake(), 4));
					break;
				case VIBRATION:
					SPDSettings.bukovControllerVibration(
							(SPDSettings.bukovControllerVibration() + 1) % 3);
					break;
				case DAMAGE_NUMBERS:
					SPDSettings.bukovDamageNumbers(
							(SPDSettings.bukovDamageNumbers() + 1) % 3);
					break;
				case AIM_ASSIST:
					SPDSettings.bukovAimAssist(
							(SPDSettings.bukovAimAssist() + 1) % 3);
					break;
				case LEFT_DEAD_ZONE:
					setDeadZone(true, nextDeadZoneProfile(
							SPDSettings.bukovLeftInnerDeadZone()));
					break;
				case RIGHT_DEAD_ZONE:
					setDeadZone(false, nextDeadZoneProfile(
							SPDSettings.bukovRightInnerDeadZone()));
					break;
				case AIM_CURVE:
					SPDSettings.bukovAimCurve(
							1 - SPDSettings.bukovAimCurve());
					break;
				case TRIGGER:
					int profile = nextTriggerProfile(
							SPDSettings.bukovTriggerPress());
					SPDSettings.bukovTriggerThresholds(
							TRIGGER_PRESS[profile],
							TRIGGER_RELEASE[profile]);
					break;
				case BRIGHTNESS:
					int value = SPDSettings.brightness() + 1;
					SPDSettings.brightness(value > 1 ? -1 : value);
					break;
				case LEGAL:
					showLegalNotice();
					return;
				case CLOSE:
					hide();
					return;
				default:
					throw new IllegalStateException(
							"Unsupported Bukov setting");
			}
			refreshLabel();
			updateFocus();
		}

		private void showLegalNotice() {
			ShatteredPixelDungeon.scene().addToFront(new WndMessage(
					"开源许可 / LEGAL\n\n"
						+ "本程序依据 GNU GPLv3 或更高版本提供，"
						+ "不附带任何担保。你可以依照该许可证复制、"
						+ "修改与再发布。\n\n"
						+ "技术来源、作者版权与第三方素材署名仅作为"
						+ "法律信息保留，不代表当前游戏的产品身份。\n\n"
						+ "完整本地文本：\n"
						+ "legal/LICENSE.txt\n"
						+ "legal/THIRD_PARTY_NOTICES.txt\n\n"
						+ "对应源码：\n"
						+ "https://github.com/leoyb1010/Leogame-one"));
		}

		private void setDeadZone(boolean left, int profile) {
			if (left) {
				SPDSettings.bukovLeftInnerDeadZone(
						DEAD_ZONE_INNER[profile]);
				SPDSettings.bukovLeftOuterDeadZone(
						DEAD_ZONE_OUTER[profile]);
			} else {
				SPDSettings.bukovRightInnerDeadZone(
						DEAD_ZONE_INNER[profile]);
				SPDSettings.bukovRightOuterDeadZone(
						DEAD_ZONE_OUTER[profile]);
			}
		}

		private void setFocused(boolean focused) {
			focusEdge.visible = focused;
			label.hardlight(focused
					? tokens.color("accent.interact")
					: tokens.color("text.primary"));
			value.hardlight(focused
					? tokens.color("text.primary")
					: setting == Setting.CLOSE
					? tokens.color("accent.extract")
					: tokens.color("accent.interact"));
		}

		private void refreshLabel() {
			switch (setting) {
				case REDUCE_MOTION:
					setCopy("减少动效",
							enabled(SPDSettings.bukovReduceMotion()));
					break;
				case REDUCE_FLASHES:
					setCopy("降低闪光",
							enabled(SPDSettings.bukovReduceFlashes()));
					break;
				case COLORBLIND:
					setCopy("色盲辅助",
							enabled(SPDSettings.bukovColorblindAssist()));
					break;
				case SOUND_VISUALIZATION:
					setCopy("关键声音视觉化",
							enabled(SPDSettings.bukovSoundVisualization()));
					break;
				case MASTER:
					setCopy("主音量",
							volumeLabel(SPDSettings.bukovMasterVolume()));
					break;
				case MUSIC:
					setCopy("音乐音量",
							volumeLabel(SPDSettings.bukovMusicVolume()));
					break;
				case SFX:
					setCopy("音效音量",
							volumeLabel(SPDSettings.bukovSfxVolume()));
					break;
				case AMBIENCE:
					setCopy("环境声音量",
							volumeLabel(SPDSettings.bukovAmbienceVolume()));
					break;
				case PERFORMANCE:
					setCopy("性能档",
							performanceLabel(
									SPDSettings.bukovPerformanceProfile()));
					break;
				case UI_SCALE:
					setCopy("UI 缩放",
							percentLevel(SPDSettings.bukovUiScale()));
					break;
				case SHAKE:
					setCopy("屏幕震动",
							scaleLabel(SPDSettings.screenShake(), 4));
					break;
				case VIBRATION:
					setCopy("手柄震动",
							threeLevel(
									SPDSettings.bukovControllerVibration()));
					break;
				case DAMAGE_NUMBERS:
					setCopy("伤害数字",
							damageNumbersLabel(
									SPDSettings.bukovDamageNumbers()));
					break;
				case AIM_ASSIST:
					setCopy("辅助瞄准",
							aimAssistLabel(SPDSettings.bukovAimAssist()));
					break;
				case LEFT_DEAD_ZONE:
					setCopy("左摇杆死区",
							deadZoneLabel(
									SPDSettings.bukovLeftInnerDeadZone(),
									SPDSettings.bukovLeftOuterDeadZone()));
					break;
				case RIGHT_DEAD_ZONE:
					setCopy("右摇杆死区",
							deadZoneLabel(
									SPDSettings.bukovRightInnerDeadZone(),
									SPDSettings.bukovRightOuterDeadZone()));
					break;
				case AIM_CURVE:
					setCopy("瞄准曲线",
							SPDSettings.bukovAimCurve() == 0
									? "线性" : "经典 S");
					break;
				case TRIGGER:
					setCopy("扳机阈值",
							SPDSettings.bukovTriggerPress()
							+ "% / "
							+ SPDSettings.bukovTriggerRelease()
							+ "%");
					break;
				case BRIGHTNESS:
					setCopy("地图亮度",
							brightnessLabel(SPDSettings.brightness()));
					break;
				case LEGAL:
					setCopy("开源许可 / Legal", "查看");
					break;
				case CLOSE:
					setCopy("完成并返回", "BACK");
					break;
				default:
					throw new IllegalStateException(
							"Unsupported Bukov setting");
			}
		}

		private void setCopy(String labelText, String valueText) {
			label.text(labelText);
			value.text(valueText);
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
			focusEdge.x = x;
			focusEdge.y = y + height - 2;
			focusEdge.size(width, 2);
			float valueWidth = setting == Setting.CLOSE
					? 48f
					: Math.min(78f, width * 0.46f);
			valueSurface.x = x + width - valueWidth - 3f;
			valueSurface.y = y + 3f;
			valueSurface.size(valueWidth, height - 6f);
			label.setRect(
					x + 6f,
					y + (height - 9f) / 2f,
					width - valueWidth - 12f,
					9f);
			value.setRect(
					x + width - valueWidth,
					y + (height - 9f) / 2f,
					valueWidth - 7f,
					9f);
		}
	}

	static String enabled(boolean value) {
		return value ? "开启" : "关闭";
	}

	static String scaleLabel(int value, int maximum) {
		if (value <= 0) return "关闭";
		if (value >= maximum) return "强";
		return value == 1 ? "低" : "标准";
	}

	static String brightnessLabel(int value) {
		if (value < 0) return "柔暗";
		if (value > 0) return "明亮";
		return "标准";
	}

	static int nextThreeLevel(int value, int maximum) {
		if (value <= 0) return maximum / 2;
		if (value < maximum) return maximum;
		return 0;
	}

	static int nextVolume(int value) {
		return value >= 10 ? 0 : Math.min(10, value + 2);
	}

	static String volumeLabel(int value) {
		return value <= 0 ? "静音" : value * 10 + "%";
	}

	static String performanceLabel(int profile) {
		switch (profile) {
			case 0:
				return "高画质";
			case 1:
				return "平衡";
			default:
				return "高帧";
		}
	}

	static String threeLevel(int level) {
		if (level <= 0) return "关闭";
		return level == 1 ? "50%" : "100%";
	}

	static String percentLevel(int level) {
		return (100 + level * 25) + "%";
	}

	static String damageNumbersLabel(int level) {
		if (level <= 0) return "关闭";
		return level == 1 ? "仅大伤害" : "全部";
	}

	static String aimAssistLabel(int level) {
		if (level <= 0) return "关闭";
		return level == 1 ? "轻 15%" : "标准 30%";
	}

	static String deadZoneLabel(int inner, int outer) {
		return inner + "% / " + outer + "%";
	}

	static int nextDeadZoneProfile(int currentInner) {
		for (int i = 0; i < DEAD_ZONE_INNER.length; i++) {
			if (currentInner <= DEAD_ZONE_INNER[i]) {
				return (i + 1) % DEAD_ZONE_INNER.length;
			}
		}
		return 0;
	}

	static int nextTriggerProfile(int currentPress) {
		for (int i = 0; i < TRIGGER_PRESS.length; i++) {
			if (currentPress <= TRIGGER_PRESS[i]) {
				return (i + 1) % TRIGGER_PRESS.length;
			}
		}
		return 0;
	}
}
