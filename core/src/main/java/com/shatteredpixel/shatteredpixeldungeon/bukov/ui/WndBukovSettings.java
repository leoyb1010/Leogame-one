package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameLog;
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
	private final int margin;
	private final int headerHeight;
	private final int footerHeight;
	private final int rowHeight;
	private final int gap;
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
		int scaleLevel = SPDSettings.bukovUiScale();
		margin = BukovUiScale.pixels(MARGIN, scaleLevel);
		headerHeight = BukovUiScale.pixels(HEADER_HEIGHT, scaleLevel);
		footerHeight = BukovUiScale.pixels(FOOTER_HEIGHT, scaleLevel);
		rowHeight = BukovUiScale.pixels(ROW_HEIGHT, scaleLevel);
		gap = BukovUiScale.pixels(GAP, scaleLevel);
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
				headerHeight - 2,
				tokens.colorWithAlpha("panel.surface", 255));
		add(header);
		ColorBlock headerRule = new ColorBlock(
				windowWidth,
				1,
				tokens.color("accent.valuable"));
		headerRule.y = headerHeight - 3;
		add(headerRule);

		RenderedTextBlock eyebrow = PixelScene.renderTextBlock(
				entryMessage("settings.eyebrow"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		eyebrow.hardlight(tokens.color("text.secondary"));
		eyebrow.setPos(margin + 2, BukovUiScale.value(
				4f, SPDSettings.bukovUiScale()));
		add(eyebrow);

		RenderedTextBlock title = PixelScene.renderTextBlock(
				entryMessage("settings.title"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_BODY));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(margin + 2, BukovUiScale.value(
				14f, SPDSettings.bukovUiScale()));
		add(title);
		RenderedTextBlock saved = PixelScene.renderTextBlock(
				entryMessage("settings.saved"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		saved.hardlight(tokens.color("accent.extract"));
		saved.setPos(
				windowWidth - margin - saved.width() - 2,
				BukovUiScale.value(
						17f, SPDSettings.bukovUiScale()));
		add(saved);

		SettingsList list = new SettingsList(windowWidth - margin * 2);
		scroll = new ScrollPane(list);
		add(scroll);
		scroll.setRect(
				margin,
				headerHeight,
				windowWidth - margin * 2,
				windowHeight - headerHeight - footerHeight);

		SettingButton close = new SettingButton(Setting.CLOSE);
		buttons[Setting.CLOSE.ordinal()] = close;
		close.setRect(
				margin,
				windowHeight - footerHeight + 3,
				windowWidth - margin * 2,
				rowHeight + 1);
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
			float rowY = focus.index() * (rowHeight + gap);
			scroll.scrollTo(0, Math.max(0, rowY - rowHeight));
		}
	}

	private enum Setting {
		LANGUAGE,
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
			setSize(listWidth, count * (rowHeight + gap) - gap);
			for (int i = 0; i < count; i++) {
				Setting setting = Setting.values()[i];
				SettingButton button = new SettingButton(setting);
				button.setRect(
						0,
						i * (rowHeight + gap),
						listWidth,
						rowHeight);
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
		private final BukovTouchIcon navigationIcon;
		private final RenderedTextBlock label;
		private final RenderedTextBlock value;
		private boolean pointerPressed;

		private SettingButton(Setting setting) {
			this.setting = setting;
			BukovTouchIcon.Glyph navigationGlyph =
					navigationGlyph(setting);
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
			navigationIcon = navigationGlyph == null
					? null
					: new BukovTouchIcon(
							navigationGlyph,
							tokens.color(setting == Setting.CLOSE
									? "accent.extract"
									: "text.primary"),
							tokens.color(setting == Setting.CLOSE
									? "text.primary"
									: "accent.interact"),
							tokens.color("text.disabled"));
			if (navigationIcon != null) {
				add(navigationIcon);
			}
			boolean navigationAction = navigationIcon != null;
			label = PixelScene.renderTextBlock(
					tokens.scaledTypographyPx(
							navigationAction
									? BukovVisualContract.FONT_CAPTION
									: BukovVisualContract.FONT_BODY));
			label.hardlight(tokens.color("text.primary"));
			add(label);
			value = PixelScene.renderTextBlock(
					tokens.scaledTypographyPx(
							navigationAction
									? BukovVisualContract.FONT_CAPTION
									: BukovVisualContract.FONT_BODY));
			value.hardlight(tokens.color(
					setting == Setting.CLOSE
							? "accent.extract"
							: "accent.interact"));
			value.align(RenderedTextBlock.RIGHT_ALIGN);
			add(value);
			refreshLabel();
		}

		private BukovTouchIcon.Glyph navigationGlyph(Setting setting) {
			if (setting == Setting.LEGAL) {
				return BukovTouchIcon.Glyph.SEARCH;
			}
			if (setting == Setting.CLOSE) {
				return BukovTouchIcon.Glyph.BACK;
			}
			return null;
		}

		@Override
		protected void onClick() {
			focus.focus(setting.ordinal());
			switch (setting) {
				case LANGUAGE:
					switchLanguage();
					return;
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

		@Override
		protected void onPointerDown() {
			pointerPressed = true;
			refreshNavigationIcon();
		}

		@Override
		protected void onPointerUp() {
			pointerPressed = false;
			refreshNavigationIcon();
		}

		private void showLegalNotice() {
			ShatteredPixelDungeon.scene().addToFront(new WndMessage(
					entryMessage("settings.legal_notice")));
		}

		private void switchLanguage() {
			final Languages language =
					nextLanguage(SPDSettings.language());
			// Persist before rebuilding so a platform interruption cannot
			// leave the visible language and stored preference out of sync.
			SPDSettings.language(language);
			Messages.setup(language);
			hide();
			ShatteredPixelDungeon.seamlessResetScene(
					new Game.SceneChangeCallback() {
						@Override
						public void beforeCreate() {
							GameLog.wipe();
							Game.platform.resetGenerators();
						}

						@Override
						public void afterCreate() {
							// The rebuilt Bukov scene reads the new bundle.
						}
					});
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
			refreshNavigationIcon();
		}

		private void refreshNavigationIcon() {
			if (navigationIcon != null) {
				navigationIcon.visualState(pointerPressed, false);
			}
		}

		private void refreshLabel() {
			switch (setting) {
				case LANGUAGE:
					setCopy(
							entryMessage("settings.language.label"),
							languageLabel());
					break;
				case REDUCE_MOTION:
					setCopy(entryMessage("settings.reduce_motion"),
							enabled(SPDSettings.bukovReduceMotion()));
					break;
				case REDUCE_FLASHES:
					setCopy(entryMessage("settings.reduce_flashes"),
							enabled(SPDSettings.bukovReduceFlashes()));
					break;
				case COLORBLIND:
					setCopy(entryMessage("settings.colorblind"),
							enabled(SPDSettings.bukovColorblindAssist()));
					break;
				case SOUND_VISUALIZATION:
					setCopy(entryMessage("settings.sound_visualization"),
							enabled(SPDSettings.bukovSoundVisualization()));
					break;
				case MASTER:
					setCopy(entryMessage("settings.master"),
							volumeLabel(SPDSettings.bukovMasterVolume()));
					break;
				case MUSIC:
					setCopy(entryMessage("settings.music"),
							volumeLabel(SPDSettings.bukovMusicVolume()));
					break;
				case SFX:
					setCopy(entryMessage("settings.sfx"),
							volumeLabel(SPDSettings.bukovSfxVolume()));
					break;
				case AMBIENCE:
					setCopy(entryMessage("settings.ambience"),
							volumeLabel(SPDSettings.bukovAmbienceVolume()));
					break;
				case PERFORMANCE:
					setCopy(entryMessage("settings.performance"),
							performanceLabel(
									SPDSettings.bukovPerformanceProfile()));
					break;
				case UI_SCALE:
					setCopy(entryMessage("settings.ui_scale"),
							percentLevel(SPDSettings.bukovUiScale()));
					break;
				case SHAKE:
					setCopy(entryMessage("settings.shake"),
							scaleLabel(SPDSettings.screenShake(), 4));
					break;
				case VIBRATION:
					setCopy(entryMessage("settings.vibration"),
							threeLevel(
									SPDSettings.bukovControllerVibration()));
					break;
				case DAMAGE_NUMBERS:
					setCopy(entryMessage("settings.damage_numbers"),
							damageNumbersLabel(
									SPDSettings.bukovDamageNumbers()));
					break;
				case AIM_ASSIST:
					setCopy(entryMessage("settings.aim_assist"),
							aimAssistLabel(SPDSettings.bukovAimAssist()));
					break;
				case LEFT_DEAD_ZONE:
					setCopy(entryMessage("settings.left_dead_zone"),
							deadZoneLabel(
									SPDSettings.bukovLeftInnerDeadZone(),
									SPDSettings.bukovLeftOuterDeadZone()));
					break;
				case RIGHT_DEAD_ZONE:
					setCopy(entryMessage("settings.right_dead_zone"),
							deadZoneLabel(
									SPDSettings.bukovRightInnerDeadZone(),
									SPDSettings.bukovRightOuterDeadZone()));
					break;
				case AIM_CURVE:
					setCopy(entryMessage("settings.aim_curve"),
							SPDSettings.bukovAimCurve() == 0
									? entryMessage("settings.linear")
									: entryMessage("settings.classic_s"));
					break;
				case TRIGGER:
					setCopy(entryMessage("settings.trigger"),
							SPDSettings.bukovTriggerPress()
							+ "% / "
							+ SPDSettings.bukovTriggerRelease()
							+ "%");
					break;
				case BRIGHTNESS:
					setCopy(entryMessage("settings.brightness"),
							brightnessLabel(SPDSettings.brightness()));
					break;
				case LEGAL:
					setCopy(
							entryMessage("settings.legal"),
							entryMessage("settings.view"));
					break;
				case CLOSE:
					setCopy(
							entryMessage("settings.close"),
							entryMessage("settings.back"));
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
			float scale = BukovUiScale.multiplier(
					SPDSettings.bukovUiScale());
			background.x = x;
			background.y = y;
			background.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(2f * scale, height);
			focusEdge.x = x;
			focusEdge.y = y + height - 2f * scale;
			focusEdge.size(width, 2f * scale);
			float valueWidth = setting == Setting.CLOSE
					? 48f * scale
					: Math.min(78f * scale, width * 0.46f);
			float innerPadding = 3f * scale;
			valueSurface.x = x + width - valueWidth - innerPadding;
			valueSurface.y = y + innerPadding;
			valueSurface.size(
					valueWidth,
					Math.max(1f, height - innerPadding * 2f));
			float textHeight = Math.max(label.height(), value.height());
			float textY = y + Math.max(0f, (height - textHeight) * 0.5f);
			float labelLeft = x + 6f * scale;
			float labelWidth = Math.max(
					1f,
					width - valueWidth - 12f * scale);
			if (navigationIcon != null) {
				float iconSize = Math.max(
						10f * scale,
						Math.min(
								14f * scale,
								height - 6f * scale));
				navigationIcon.setRect(
						labelLeft,
						y + (height - iconSize) * 0.5f,
						iconSize,
						iconSize);
				labelLeft += iconSize + 3f * scale;
				labelWidth = Math.max(
						1f,
						labelWidth - iconSize - 3f * scale);
			}
			label.setRect(
					labelLeft,
					textY,
					labelWidth,
					textHeight);
			value.setRect(
					x + width - valueWidth,
					textY,
					Math.max(1f, valueWidth - 7f * scale),
					textHeight);
		}
	}

	static String enabled(boolean value) {
		return entryMessage(value ? "settings.on" : "settings.off");
	}

	static String scaleLabel(int value, int maximum) {
		if (value <= 0) return entryMessage("settings.off");
		if (value >= maximum) return entryMessage("settings.strong");
		return entryMessage(
				value == 1 ? "settings.low" : "settings.standard");
	}

	static String brightnessLabel(int value) {
		if (value < 0) return entryMessage("settings.dim");
		if (value > 0) return entryMessage("settings.bright");
		return entryMessage("settings.standard");
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
		return value <= 0
				? entryMessage("settings.muted")
				: value * 10 + "%";
	}

	static String performanceLabel(int profile) {
		switch (profile) {
			case 0:
				return entryMessage("settings.quality");
			case 1:
				return entryMessage("settings.balanced");
			default:
				return entryMessage("settings.framerate");
		}
	}

	static String threeLevel(int level) {
		if (level <= 0) return entryMessage("settings.off");
		return level == 1 ? "50%" : "100%";
	}

	static String percentLevel(int level) {
		return (100 + level * 25) + "%";
	}

	static String damageNumbersLabel(int level) {
		if (level <= 0) return entryMessage("settings.off");
		return entryMessage(
				level == 1
						? "settings.large_damage"
						: "settings.all");
	}

	static String aimAssistLabel(int level) {
		if (level <= 0) return entryMessage("settings.off");
		return entryMessage(
				level == 1
						? "settings.light_percent"
						: "settings.standard_percent",
				level == 1 ? 15 : 30);
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

	static Languages nextLanguage(Languages current) {
		return current == Languages.ENGLISH
				? Languages.CHI_SMPL
				: Languages.ENGLISH;
	}

	static String languageLabel() {
		return entryMessage(SPDSettings.language() == Languages.ENGLISH
				? "settings.language.english"
				: "settings.language.chinese");
	}

	private static String entryMessage(String key, Object... args) {
		return BukovMessages.get("bukov.entry." + key, args);
	}
}
