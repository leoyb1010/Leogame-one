package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
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

	private static final int COMPACT_LABEL_REDUCTION_PX = 2;

	public interface Listener {
		void onActionPressed(BukovTouchState.Action action);
	}

	private final BukovTouchState state = new BukovTouchState();
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
		interact.setDisabled(blocked);
		reload.setDisabled(blocked);
		medical.setDisabled(blocked);
		drop.setDisabled(blocked);
		backpack.setDisabled(blocked);
		pause.setDisabled(blocked);
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

	private static String compactActionLabel(String value) {
		if (value == null) {
			return "";
		}
		String normalized = value.trim();
		int count = normalized.codePointCount(0, normalized.length());
		if (count <= 5) {
			return normalized;
		}
		int end = normalized.offsetByCodePoints(0, 4);
		return normalized.substring(0, end) + ".";
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
					if (inputBlocked || pointerId != -1) {
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
					? 0.45f
					: visiblyPressed ? 0.95f : 0.72f);
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
		private ColorBlock background;
		private ColorBlock edge;
		private BukovTouchIcon icon;
		private RenderedTextBlock label;
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
			background = new ColorBlock(1, 1, restingBackground);
			add(background);
			edge = new ColorBlock(
					1, 1, BukovTouchIcon.withFullAlpha(accentColor));
			add(edge);
			icon = new BukovTouchIcon(
					iconFor(action),
					tokens.color("text.primary"),
					accentColor,
					tokens.color("text.disabled"));
			add(icon);
			label = PixelScene.renderTextBlock(
					compactActionLabel(text),
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
					if (inputBlocked || pointerId != -1) {
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
			if (background == null) {
				return;
			}
			float pressedOffset = pointerId != -1 && !disabled ? 1f : 0f;
			shadow.x = x + 1f;
			shadow.y = y + 2f;
			shadow.size(
					Math.max(1f, width - 2f),
					Math.max(1f, height - 2f));
			background.x = x;
			background.y = y + pressedOffset;
			background.size(width, Math.max(1f, height - 2f));
			edge.x = x;
			edge.y = y + pressedOffset;
			edge.size(width, pointerId != -1 && !disabled ? 2f : 1.5f);
			float iconSize = Math.max(
					8f,
					Math.min(
							13f,
							Math.min(width - 4f, height * 0.50f)));
			icon.setRect(
					centerX() - iconSize * 0.5f,
					y + 2f,
					iconSize,
					iconSize);
			label.setRect(
					x + 1f,
					bottom() - 7f,
					Math.max(1f, width - 2f),
					6f);
			pointerArea.x = x;
			pointerArea.y = y;
			pointerArea.width = width;
			pointerArea.height = height;
		}

		private void setPressed(boolean pressed) {
			boolean visiblyPressed = pressed && !disabled;
			background.alpha(disabled
					? 0.48f
					: visiblyPressed ? 1f : 0.82f);
			shadow.alpha(disabled
					? 0.30f
					: visiblyPressed ? 0.35f : 0.90f);
			edge.hardlight(disabled
					? tokens.color("text.disabled")
					: accentColor);
			label.hardlight(disabled
					? tokens.color("text.disabled")
					: visiblyPressed
							? accentColor
							: tokens.color("text.secondary"));
			icon.visualState(visiblyPressed, disabled);
			layout();
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
			if (background != null) {
				setPressed(false);
			}
		}
	}

}
