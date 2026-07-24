package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

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
				"移动",
				tokens.colorWithAlpha("panel.surface", 0xBB),
				tokens.color("accent.interact")
		);
		add(movement);

		aimFire = new TouchStick(
				BukovTouchState.Stick.AIM_FIRE,
				"瞄准 · 射击",
				tokens.colorWithAlpha("panel.surface", 0xBB),
				tokens.color("accent.danger")
		);
		add(aimFire);

		interact = action(
				BukovTouchState.Action.INTERACT,
				"交互",
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.interact"));
		reload = action(
				BukovTouchState.Action.RELOAD,
				"换弹",
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("text.secondary"));
		medical = action(
				BukovTouchState.Action.MEDICAL,
				"医疗",
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.extract"));
		drop = action(
				BukovTouchState.Action.DROP,
				"丢弃",
				tokens.colorWithAlpha("panel.surface", 0xDD),
				tokens.color("accent.valuable"));
		backpack = action(
				BukovTouchState.Action.BACKPACK,
				"背包",
				tokens.colorWithAlpha("panel.surface", 0xE0),
				tokens.color("accent.interact"));
		pause = action(
				BukovTouchState.Action.PAUSE,
				"暂停",
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
		private RenderedTextBlock label;
		private PointerArea pointerArea;
		private int pointerId = -1;

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
			topEdge = new ColorBlock(1, 1, accentColor);
			add(topEdge);
			horizontalGuide = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("text.secondary", 0x66));
			add(horizontalGuide);
			verticalGuide = new ColorBlock(
					1, 1,
					tokens.colorWithAlpha("text.secondary", 0x66));
			add(verticalGuide);
			knob = new ColorBlock(1, 1, accentColor);
			knob.alpha(0.80f);
			add(knob);
			label = PixelScene.renderTextBlock(
					text,
					tokens.typographyPx(
							BukovVisualContract.FONT_CAPTION));
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
			label.setRect(x + 2f, y + 3f, width - 4f, 8f);
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
			background.alpha(pressed ? 0.95f : 0.72f);
			knob.alpha(pressed ? 1f : 0.80f);
			label.hardlight(pressed
					? accentColor
					: tokens.color("text.primary"));
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
		private ColorBlock background;
		private ColorBlock edge;
		private RenderedTextBlock label;
		private PointerArea pointerArea;
		private int pointerId = -1;

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
			background = new ColorBlock(1, 1, restingBackground);
			add(background);
			edge = new ColorBlock(1, 1, accentColor);
			add(edge);
			label = PixelScene.renderTextBlock(
					text,
					tokens.typographyPx(
							BukovVisualContract.FONT_CAPTION));
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
			background.x = x;
			background.y = y;
			background.size(width, height);
			edge.x = x;
			edge.y = y;
			edge.size(width, 1.5f);
			label.setRect(x + 1f, y + (height - 8f) * 0.5f, width - 2f, 8f);
			pointerArea.x = x;
			pointerArea.y = y;
			pointerArea.width = width;
			pointerArea.height = height;
		}

		private void setPressed(boolean pressed) {
			background.alpha(pressed ? 1f : 0.82f);
			label.hardlight(pressed
					? accentColor
					: tokens.color("text.primary"));
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
