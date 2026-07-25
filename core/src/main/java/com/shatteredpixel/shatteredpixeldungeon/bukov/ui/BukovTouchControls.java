package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;

/**
 * Visible, raid-native mobile controls.
 *
 * This component intentionally does not know about GameScene or the realtime
 * simulation. The scene owns it, samples {@link #state()}, and handles action
 * edges through either {@link #consumePressed(BukovTouchState.Action)} or the
 * optional listener.
 */
public final class BukovTouchControls extends Component {

	private static final int COMPACT_LABEL_REDUCTION_PX = 4;
	private static final int ACTION_LABEL_FONT_FLOOR_PX = 5;
	private static final float ACTION_ICON_MAX_PX = 16f;
	private static final float ACTION_ICON_HEIGHT_RATIO = 0.66f;
	private static final float ACTION_LABEL_HEIGHT_PX = 5f;
	private static final float MIN_ACTION_HIT_SIZE_PX = 22f;
	static final float STICK_RESTING_ALPHA = 0.46f;
	static final float STICK_PRESSED_ALPHA = 0.92f;
	static final float STICK_DISABLED_ALPHA = 0.28f;

	public interface Listener {
		void onActionPressed(BukovTouchState.Action action);
	}

	private final BukovTouchState state = new BukovTouchState();
	private final boolean[] actionUnavailable =
			new boolean[BukovTouchState.Action.values().length];
	private BukovUiTokens tokens;
	private TouchStick movement;
	private TouchStick aimFire;
	private TouchAction interact;
	private TouchAction reload;
	private TouchAction medical;
	private TouchAction drop;
	private TouchAction backpack;
	private TouchAction pause;
	private Listener listener;
	private float safeLeft;
	private float safeTop;
	private float safeRight;
	private float safeBottom;
	private float hudBottom;
	private boolean inputBlocked;
	private BukovTouchLayout currentLayout;
	private BukovRaidHudState.Interaction lastLiveInteraction;

	@Override
	protected void createChildren() {
		tokens = BukovUiTokens.loadDefault();
		movement = new TouchStick(
				BukovTouchState.Stick.MOVEMENT,
				BukovMessages.get("bukov.raid.touch.movement"),
				tokens.colorWithAlpha("panel.surface", 0xBB),
				tokens.color("accent.interact")
		);
		add(movement);

		aimFire = new TouchStick(
				BukovTouchState.Stick.AIM_FIRE,
				BukovMessages.get("bukov.raid.touch.aim_fire"),
				tokens.colorWithAlpha("panel.surface", 0xBB),
				tokens.color("accent.danger")
		);
		add(aimFire);

		interact = action(
				BukovTouchState.Action.INTERACT,
				BukovMessages.get("bukov.raid.touch.interact"),
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.interact"));
		reload = action(
				BukovTouchState.Action.RELOAD,
				BukovMessages.get("bukov.raid.touch.reload"),
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("text.secondary"));
		medical = action(
				BukovTouchState.Action.MEDICAL,
				BukovMessages.get("bukov.raid.touch.medical"),
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.extract"));
		drop = action(
				BukovTouchState.Action.DROP,
				BukovMessages.get("bukov.raid.touch.drop"),
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.valuable"));
		backpack = action(
				BukovTouchState.Action.BACKPACK,
				BukovMessages.get("bukov.raid.touch.backpack"),
				tokens.colorWithAlpha("panel.surface", 0xE0),
				tokens.color("accent.interact"));
		pause = action(
				BukovTouchState.Action.PAUSE,
				BukovMessages.get("bukov.raid.touch.pause"),
				tokens.colorWithAlpha("panel.surface", 0xE0),
				tokens.color("text.secondary"));
	}

	private TouchAction action(
			BukovTouchState.Action action,
			String label,
			int background,
			int accent) {
		TouchAction result = new TouchAction(action, label, background, accent);
		add(result);
		return result;
	}

	public BukovTouchControls listener(Listener listener) {
		this.listener = listener;
		return this;
	}

	public BukovTouchState state() {
		return state;
	}

	public BukovTouchLayout currentLayout() {
		return currentLayout;
	}

	public boolean consumePressed(BukovTouchState.Action action) {
		return state.consumePressed(action);
	}

	/**
	 * Applies live gameplay availability without changing the input contract:
	 * unavailable actions are both visibly disabled and unable to create a
	 * pressed/held edge. The caller can update these flags every render frame.
	 */
	public void setActionEnabled(
			BukovTouchState.Action action,
			boolean enabled) {
		if (action == null) {
			throw new IllegalArgumentException("action is required");
		}
		if (actionUnavailable[action.ordinal()] == !enabled) {
			return;
		}
		actionUnavailable[action.ordinal()] = !enabled;
		if (!enabled) {
			state.consumePressed(action);
		}
		TouchAction control = actionControl(action);
		if (control != null) {
			control.setDisabled(actionDisabled(inputBlocked, enabled));
		}
	}

	public boolean actionEnabled(BukovTouchState.Action action) {
		if (action == null) {
			throw new IllegalArgumentException("action is required");
		}
		return !actionUnavailable[action.ordinal()];
	}

	public void liveActionAvailability(
			BukovRaidHudState.Interaction interaction,
			boolean reloadAvailable,
			boolean medicalAvailable) {
		BukovRaidHudState.Interaction resolvedInteraction =
				interaction == null
						? BukovRaidHudState.Interaction.NONE
						: interaction;
		setActionEnabled(
				BukovTouchState.Action.INTERACT,
				BukovRaidHud.interactionActionAvailable(
						resolvedInteraction));
		if (lastLiveInteraction != resolvedInteraction) {
			lastLiveInteraction = resolvedInteraction;
			interact.setGlyph(interactionGlyph(resolvedInteraction));
			interact.setLabel(
					interactionActionLabel(resolvedInteraction));
		}
		setActionEnabled(
				BukovTouchState.Action.RELOAD,
				reloadAvailable);
		setActionEnabled(
				BukovTouchState.Action.MEDICAL,
				medicalAvailable);
	}

	/**
	 * Insets are expressed in logical PixelScene pixels, not device pixels.
	 */
	public BukovTouchControls safeInsets(
			float left,
			float top,
			float right,
			float bottom) {
		safeLeft = Math.max(0f, left);
		safeTop = Math.max(0f, top);
		safeRight = Math.max(0f, right);
		safeBottom = Math.max(0f, bottom);
		layout();
		return this;
	}

	/**
	 * Keeps the backpack and pause buttons below the raid HUD. The value is the
	 * HUD's bottom edge in this component's local logical-pixel coordinates.
	 */
	public BukovTouchControls hudBottom(float bottom) {
		hudBottom = Math.max(0f, bottom);
		layout();
		return this;
	}

	/**
	 * Use while any modal window is open. Blocking is stateful and immediately
	 * cancels all held movement, firing, and actions.
	 */
	public void inputBlocked(boolean blocked) {
		if (inputBlocked == blocked) {
			return;
		}
		inputBlocked = blocked;
		if (blocked) {
			resetInput();
		}
		movement.setDisabled(blocked);
		aimFire.setDisabled(blocked);
		refreshActionAvailability();
		active = !blocked;
	}

	public boolean inputBlocked() {
		return inputBlocked;
	}

	/**
	 * Call on app pause, orientation change, pointer cancel, and before opening
	 * any modal raid window.
	 */
	public void resetInput() {
		state.reset();
		movement.resetInteraction();
		aimFire.resetInteraction();
		interact.resetInteraction();
		reload.resetInteraction();
		medical.resetInteraction();
		drop.resetInteraction();
		backpack.resetInteraction();
		pause.resetInteraction();
	}

	@Override
	public synchronized void destroy() {
		resetInput();
		super.destroy();
	}

	@Override
	protected void layout() {
		if (width <= 0f || height <= 0f || movement == null) {
			return;
		}
		resetInput();
		currentLayout = BukovTouchLayout.calculate(
				width,
				height,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom
		);
		setControlRect(movement, currentLayout.movement);
		setControlRect(aimFire, currentLayout.aimFire);
		setControlRect(interact, currentLayout.interact);
		setControlRect(reload, currentLayout.reload);
		setControlRect(medical, currentLayout.medical);
		setControlRect(drop, currentLayout.drop);
		setControlRect(backpack, currentLayout.backpack);
		setControlRect(pause, currentLayout.pause);
	}

	private void setControlRect(Component component, BukovTouchLayout.Rect rect) {
		component.setRect(x + rect.x, y + rect.y, rect.width, rect.height);
	}

	private PointF pointerPosition(PointerEvent event) {
		if (camera() == null) {
			return new PointF(event.current.x, event.current.y);
		}
		return camera().screenToCamera((int)event.current.x, (int)event.current.y);
	}

	private void refreshActionAvailability() {
		for (BukovTouchState.Action action
				: BukovTouchState.Action.values()) {
			TouchAction control = actionControl(action);
			if (control != null) {
				control.setDisabled(actionDisabled(
						inputBlocked,
						!actionUnavailable[action.ordinal()]));
			}
		}
	}

	private TouchAction actionControl(BukovTouchState.Action action) {
		switch (action) {
			case INTERACT:
				return interact;
			case RELOAD:
				return reload;
			case MEDICAL:
				return medical;
			case DROP:
				return drop;
			case BACKPACK:
				return backpack;
			case PAUSE:
				return pause;
			default:
				throw new IllegalArgumentException(
						"unsupported action: " + action);
		}
	}

	static boolean actionDisabled(
			boolean inputBlocked,
			boolean actionAvailable) {
		return inputBlocked || !actionAvailable;
	}

	static float actionHitSize(float visualSize) {
		return Math.max(MIN_ACTION_HIT_SIZE_PX, visualSize);
	}

	static int actionLabelFontPx(int captionPx) {
		return Math.max(
				ACTION_LABEL_FONT_FLOOR_PX,
				captionPx - COMPACT_LABEL_REDUCTION_PX);
	}

	static float actionIconSize(float width, float height) {
		return Math.max(
				9f,
				Math.min(
						ACTION_ICON_MAX_PX,
						Math.min(
								Math.max(1f, width - 5f),
								Math.max(1f, height)
										* ACTION_ICON_HEIGHT_RATIO)));
	}

	static BukovTouchIcon.Glyph interactionGlyph(
			BukovRaidHudState.Interaction interaction) {
		if (interaction == null) {
			return BukovTouchIcon.Glyph.INTERACT;
		}
		switch (interaction) {
			case SEARCH:
				return BukovTouchIcon.Glyph.SEARCH;
			case PICKUP:
				return BukovTouchIcon.Glyph.BACKPACK;
			case EXTRACT:
				return BukovTouchIcon.Glyph.DEPLOY;
			case PUMP:
				return BukovTouchIcon.Glyph.SETTINGS;
			case MEDICAL:
				return BukovTouchIcon.Glyph.MEDICAL;
			case NONE:
			case LOCKED:
			case UNLOCK:
			default:
				return BukovTouchIcon.Glyph.INTERACT;
		}
	}

	static String interactionActionLabel(
			BukovRaidHudState.Interaction interaction) {
		if (interaction == null
				|| interaction == BukovRaidHudState.Interaction.NONE) {
			return compactActionLabel(
					BukovTouchState.Action.INTERACT,
					BukovMessages.get("bukov.raid.touch.interact"));
		}
		switch (interaction) {
			case SEARCH:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_search");
			case PICKUP:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_pickup");
			case EXTRACT:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_extract");
			case PUMP:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_pump");
			case MEDICAL:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_medical");
			case UNLOCK:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_unlock");
			case LOCKED:
			default:
				return BukovMessages.get(
						"bukov.raid.touch.interaction_locked");
		}
	}

	static BukovTouchIcon.Glyph iconFor(BukovTouchState.Stick stick) {
		if (stick == null) {
			throw new IllegalArgumentException("stick is required");
		}
		return stick == BukovTouchState.Stick.MOVEMENT
				? BukovTouchIcon.Glyph.MOVEMENT
				: BukovTouchIcon.Glyph.AIM_FIRE;
	}

	static BukovTouchIcon.Glyph iconFor(BukovTouchState.Action action) {
		if (action == null) {
			throw new IllegalArgumentException("action is required");
		}
		switch (action) {
			case INTERACT:
				return BukovTouchIcon.Glyph.INTERACT;
			case RELOAD:
				return BukovTouchIcon.Glyph.RELOAD;
			case MEDICAL:
				return BukovTouchIcon.Glyph.MEDICAL;
			case DROP:
				return BukovTouchIcon.Glyph.DROP;
			case BACKPACK:
				return BukovTouchIcon.Glyph.BACKPACK;
			case PAUSE:
				return BukovTouchIcon.Glyph.PAUSE;
			default:
				throw new IllegalArgumentException("unsupported action: " + action);
		}
	}

	static String compactActionLabel(
			BukovTouchState.Action action,
			String value) {
		if (action == null || value == null) {
			return "";
		}
		String normalized = value.trim();
		boolean latin = true;
		for (int index = 0; index < normalized.length();) {
			int codePoint = normalized.codePointAt(index);
			if (codePoint > 0x7F) {
				latin = false;
				break;
			}
			index += Character.charCount(codePoint);
		}
		if (latin) {
			switch (action) {
				case INTERACT:
					return "Use";
				case RELOAD:
					return "Reload";
				case MEDICAL:
					return "Heal";
				case DROP:
					return "Drop";
				case BACKPACK:
					return "Bag";
				case PAUSE:
					return "Pause";
				default:
					break;
			}
		}
		int count = normalized.codePointCount(0, normalized.length());
		if (count <= 4) {
			return normalized;
		}
		int end = normalized.offsetByCodePoints(0, 3);
		return normalized.substring(0, end) + "…";
	}

	private final class TouchStick extends Component {
		private final BukovTouchState.Stick stick;
		private final String text;
		private final int restingBackground;
		private final int accentColor;
		private ColorBlock background;
		private ColorBlock topEdge;
		private ColorBlock horizontalGuide;
		private ColorBlock verticalGuide;
		private ColorBlock knob;
		private BukovTouchIcon icon;
		private RenderedTextBlock label;
		private PointerArea pointerArea;
		private int pointerId = -1;
		private boolean disabled;

		private TouchStick(
				BukovTouchState.Stick stick,
				String text,
				int restingBackground,
				int accentColor) {
			this.stick = stick;
			this.text = text;
			this.restingBackground = restingBackground;
			this.accentColor = accentColor;
			buildChildren();
		}

		@Override
		protected void createChildren() {
			// Built after constructor arguments are assigned.
		}

		private void buildChildren() {
			background = new ColorBlock(1, 1, restingBackground);
			add(background);
			topEdge = new ColorBlock(
					1, 1, BukovTouchIcon.withFullAlpha(accentColor));
			add(topEdge);
			horizontalGuide = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("text.secondary", 0x66));
			add(horizontalGuide);
			verticalGuide = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("text.secondary", 0x66));
			add(verticalGuide);
			knob = new ColorBlock(
					1, 1, BukovTouchIcon.withFullAlpha(accentColor));
			knob.alpha(0.80f);
			add(knob);
			icon = new BukovTouchIcon(
					iconFor(stick),
					tokens.color("text.secondary"),
					accentColor,
					tokens.color("text.disabled"));
			add(icon);
			label = PixelScene.renderTextBlock(
					text,
					Math.max(
							6,
							tokens.typographyPx(
									BukovVisualContract.FONT_CAPTION)
									- COMPACT_LABEL_REDUCTION_PX));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			label.hardlight(tokens.color("text.primary"));
			add(label);
			pointerArea = new PointerArea(0, 0, 0, 0) {
				@Override
				public boolean onSignal(PointerEvent event) {
					if (event != null && pointerId != -1 && event.id != pointerId) {
						return false;
					}
					return super.onSignal(event);
				}

				@Override
				protected void onPointerDown(PointerEvent event) {
					if (inputBlocked || disabled || pointerId != -1) {
						return;
					}
					PointF point = pointerPosition(event);
					if (state.beginStick(
							stick,
							event.id,
							centerX(),
							centerY(),
							Math.max(1f, width * 0.42f),
							point.x,
							point.y)) {
						pointerId = event.id;
						updateKnob(point.x, point.y);
						setPressed(true);
					}
				}

				@Override
				protected void onDrag(PointerEvent event) {
					if (pointerId == event.id) {
						PointF point = pointerPosition(event);
						state.movePointer(event.id, point.x, point.y);
						updateKnob(point.x, point.y);
					}
				}

				@Override
				protected void onPointerUp(PointerEvent event) {
					if (pointerId == event.id) {
						state.endPointer(event.id);
						pointerId = -1;
						centerKnob();
						setPressed(false);
					}
				}
			};
			add(pointerArea);
		}

		@Override
		protected void layout() {
			if (background == null) {
				return;
			}
			background.x = x;
			background.y = y;
			background.size(width, height);
			topEdge.x = x;
			topEdge.y = y;
			topEdge.size(width, 1.5f);
			horizontalGuide.x = x + width * 0.18f;
			horizontalGuide.y = centerY();
			horizontalGuide.size(width * 0.64f, 1f);
			verticalGuide.x = centerX();
			verticalGuide.y = y + height * 0.18f;
			verticalGuide.size(1f, height * 0.64f);
			float iconSize = Math.max(
					9f,
					Math.min(14f, width * 0.20f));
			icon.setRect(
					right() - iconSize - 3f,
					y + 3f,
					iconSize,
					iconSize);
			label.setRect(
					x + 3f,
					y + 3f,
					Math.max(1f, width - iconSize - 8f),
					7f);
			pointerArea.x = x;
			pointerArea.y = y;
			pointerArea.width = width;
			pointerArea.height = height;
			centerKnob();
		}

		private void updateKnob(float pointerX, float pointerY) {
			float dx = pointerX - centerX();
			float dy = pointerY - centerY();
			float radius = Math.max(1f, width * 0.33f);
			float length = (float)Math.sqrt(dx * dx + dy * dy);
			if (length > radius) {
				dx *= radius / length;
				dy *= radius / length;
			}
			float knobSize = Math.max(8f, width * 0.18f);
			knob.size(knobSize, knobSize);
			knob.x = centerX() + dx - knobSize * 0.5f;
			knob.y = centerY() + dy - knobSize * 0.5f;
		}

		private void centerKnob() {
			if (knob == null) {
				return;
			}
			float knobSize = Math.max(8f, width * 0.18f);
			knob.size(knobSize, knobSize);
			knob.x = centerX() - knobSize * 0.5f;
			knob.y = centerY() - knobSize * 0.5f;
		}

		private void setPressed(boolean pressed) {
			boolean visiblyPressed = pressed && !disabled;
			background.alpha(disabled
					? STICK_DISABLED_ALPHA
					: visiblyPressed
							? STICK_PRESSED_ALPHA
							: STICK_RESTING_ALPHA);
			knob.alpha(disabled
					? 0.40f
					: visiblyPressed ? 1f : 0.80f);
			topEdge.hardlight(disabled
					? tokens.color("text.disabled")
					: accentColor);
			label.hardlight(disabled
					? tokens.color("text.disabled")
					: visiblyPressed
							? accentColor
							: tokens.color("text.primary"));
			icon.visualState(visiblyPressed, disabled);
		}

		private void setDisabled(boolean disabled) {
			this.disabled = disabled;
			setPressed(false);
		}

		private void resetInteraction() {
			if (pointerId != -1) {
				state.endPointer(pointerId);
			}
			pointerId = -1;
			if (pointerArea != null) {
				pointerArea.reset();
			}
			centerKnob();
			if (background != null) {
				setPressed(false);
			}
		}
	}

	private final class TouchAction extends Component {
		private final BukovTouchState.Action action;
		private final String text;
		private final int restingBackground;
		private final int accentColor;
		private ColorBlock shadow;
		private NinePatch restingSurface;
		private NinePatch pressedSurface;
		private NinePatch disabledSurface;
		private ColorBlock edge;
		private NinePatch iconPlate;
		private ColorBlock labelDivider;
		private BukovTouchIcon icon;
		private RenderedTextBlock label;
		private String currentLabel;
		private PointerArea pointerArea;
		private int pointerId = -1;
		private boolean disabled;

		private TouchAction(
				BukovTouchState.Action action,
				String text,
				int restingBackground,
				int accentColor) {
			this.action = action;
			this.text = text;
			this.restingBackground = restingBackground;
			this.accentColor = accentColor;
			buildChildren();
		}

		@Override
		protected void createChildren() {
			// Built after constructor arguments are assigned.
		}

		private void buildChildren() {
			shadow = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("ink.shadow", 0xCC));
			add(shadow);
			restingSurface = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON,
					restingBackground);
			add(restingSurface);
			pressedSurface = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_PRESSED,
					tokens.color("panel.deep"));
			pressedSurface.visible = false;
			add(pressedSurface);
			disabledSurface = BukovUiAssets.surface(
					BukovUiAssets.Surface.BUTTON_DISABLED,
					tokens.color("panel.deep"));
			disabledSurface.visible = false;
			add(disabledSurface);
			edge = new ColorBlock(
					1, 1, BukovTouchIcon.withFullAlpha(accentColor));
			add(edge);
			iconPlate = BukovUiAssets.surface(
					BukovUiAssets.Surface.PANEL_RAISED,
					tokens.colorWithAlpha("ink.shadow", 0x78));
			add(iconPlate);
			labelDivider = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("text.secondary", 0x52));
			add(labelDivider);
			icon = new BukovTouchIcon(
					iconFor(action),
					tokens.color("text.primary"),
					accentColor,
					tokens.color("text.disabled"));
			add(icon);
			currentLabel = compactActionLabel(action, text);
			label = PixelScene.renderTextBlock(
					currentLabel,
					actionLabelFontPx(tokens.typographyPx(
							BukovVisualContract.FONT_CAPTION)));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			label.hardlight(tokens.color("text.secondary"));
			add(label);
			pointerArea = new PointerArea(0, 0, 0, 0) {
				@Override
				public boolean onSignal(PointerEvent event) {
					if (event != null && pointerId != -1 && event.id != pointerId) {
						return false;
					}
					return super.onSignal(event);
				}

				@Override
				protected void onPointerDown(PointerEvent event) {
					if (inputBlocked || disabled || pointerId != -1) {
						return;
					}
					if (state.beginAction(action, event.id)) {
						pointerId = event.id;
						setPressed(true);
						if (listener != null) {
							listener.onActionPressed(action);
						}
					}
				}

				@Override
				protected void onPointerUp(PointerEvent event) {
					if (pointerId == event.id) {
						state.endPointer(event.id);
						pointerId = -1;
						setPressed(false);
					}
				}
			};
			add(pointerArea);
		}

		@Override
		protected void layout() {
			if (restingSurface == null) {
				return;
			}
			float pressedOffset = pointerId != -1 && !disabled ? 1f : 0f;
			shadow.x = x + 1f;
			shadow.y = y + 2f;
			shadow.size(
					Math.max(1f, width - 2f),
					Math.max(1f, height - 2f));
			layoutSurface(
					restingSurface,
					y + pressedOffset,
					Math.max(1f, height - 2f));
			layoutSurface(
					pressedSurface,
					y + pressedOffset,
					Math.max(1f, height - 2f));
			layoutSurface(
					disabledSurface,
					y,
					Math.max(1f, height - 2f));
			edge.x = x;
			edge.y = y + pressedOffset;
			edge.size(width, pointerId != -1 && !disabled ? 2f : 1.5f);
			float iconSize = actionIconSize(width, height);
			float iconTop = y + 1.5f + pressedOffset;
			float plateSize = Math.min(
					Math.max(1f, width - 3f),
					iconSize + 3f);
			iconPlate.x = centerX() - plateSize * 0.5f;
			iconPlate.y = iconTop - 1f;
			iconPlate.size(plateSize, iconSize + 2f);
			float dividerY = bottom() - ACTION_LABEL_HEIGHT_PX - 1f;
			labelDivider.x = x + 3f;
			labelDivider.y = dividerY;
			labelDivider.size(Math.max(1f, width - 6f), 0.75f);
			icon.setRect(
					centerX() - iconSize * 0.5f,
					iconTop,
					iconSize,
					iconSize);
			label.setRect(
					x + 1f,
					bottom() - ACTION_LABEL_HEIGHT_PX,
					Math.max(1f, width - 2f),
					ACTION_LABEL_HEIGHT_PX);
			float hitWidth = actionHitSize(width);
			float hitHeight = actionHitSize(height);
			pointerArea.x = x - (hitWidth - width) * 0.5f;
			pointerArea.y = y - (hitHeight - height) * 0.5f;
			pointerArea.width = hitWidth;
			pointerArea.height = hitHeight;
		}

		private void layoutSurface(
				NinePatch surface,
				float surfaceY,
				float surfaceHeight) {
			surface.x = x;
			surface.y = surfaceY;
			surface.size(width, surfaceHeight);
		}

		private void setPressed(boolean pressed) {
			boolean visiblyPressed = pressed && !disabled;
			restingSurface.visible = !disabled && !visiblyPressed;
			pressedSurface.visible = !disabled && visiblyPressed;
			disabledSurface.visible = disabled;
			shadow.alpha(disabled
					? 0.30f
					: visiblyPressed ? 0.35f : 0.90f);
			edge.hardlight(disabled
					? tokens.color("text.disabled")
					: accentColor);
			iconPlate.hardlight(disabled
					? tokens.color("ink.shadow")
					: visiblyPressed
							? accentColor
							: tokens.color("panel.surface"));
			iconPlate.alpha(disabled
					? 0.24f
					: visiblyPressed ? 0.34f : 0.46f);
			labelDivider.hardlight(disabled
					? tokens.color("text.disabled")
					: visiblyPressed
							? accentColor
							: tokens.color("text.secondary"));
			labelDivider.alpha(disabled ? 0.28f : visiblyPressed ? 0.90f : 0.55f);
			label.hardlight(disabled
					? tokens.color("text.disabled")
					: visiblyPressed
							? accentColor
							: tokens.color("text.secondary"));
			icon.visualState(visiblyPressed, disabled);
			layout();
		}

		private void setGlyph(BukovTouchIcon.Glyph glyph) {
			icon.glyph(glyph);
		}

		private void setLabel(String value) {
			String next = value == null ? "" : value.trim();
			if (next.equals(currentLabel)) {
				return;
			}
			currentLabel = next;
			label.text(currentLabel);
		}

		private void setDisabled(boolean disabled) {
			if (this.disabled == disabled) {
				return;
			}
			if (disabled) {
				resetInteraction();
			}
			this.disabled = disabled;
			setPressed(false);
		}

		private void resetInteraction() {
			if (pointerId != -1) {
				state.endPointer(pointerId);
			}
			pointerId = -1;
			if (pointerArea != null) {
				pointerArea.reset();
			}
			if (restingSurface != null) {
				setPressed(false);
			}
		}
	}

}
