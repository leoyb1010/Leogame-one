package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

/**
 * Reusable Bukov button for a semantic icon and short label.
 *
 * The icon comes from the project-owned Bukov atlas. Passing no icon keeps
 * the component compatible with existing text-only button call sites, while
 * icon-only mode retains the label as its desktop tooltip.
 */
public class BukovIconLabelButton extends Button {

	private static final float HORIZONTAL_PADDING = 5f;
	private static final float ICON_LABEL_GAP = 3f;
	private static final float MAX_ICON_SIZE = 14f;

	private final boolean touch;
	private final BukovTouchIcon.Glyph glyph;
	private final BukovUiTokens tokens;
	private NinePatch restingSurface;
	private NinePatch pressedSurface;
	private NinePatch focusedSurface;
	private NinePatch disabledSurface;
	private BukovTouchIcon iconView;
	private RenderedTextBlock labelView;
	private String labelText;
	private boolean iconOnly;
	private boolean enabled = true;
	private boolean focused;
	private boolean pressed;
	private float trailingInset;

	public BukovIconLabelButton(String label, boolean touch) {
		this(null, label, touch, false);
	}

	public BukovIconLabelButton(
			BukovTouchIcon.Glyph glyph,
			String label,
			boolean touch) {
		this(glyph, label, touch, false);
	}

	public BukovIconLabelButton(
			BukovTouchIcon.Glyph glyph,
			String label,
			boolean touch,
			boolean iconOnly) {
		if (label == null || label.length() == 0) {
			throw new IllegalArgumentException(
					"an accessible label is required");
		}
		if (iconOnly && glyph == null) {
			throw new IllegalArgumentException(
					"icon-only mode requires a glyph");
		}
		this.touch = touch;
		this.glyph = glyph;
		this.labelText = label;
		this.iconOnly = iconOnly;
		tokens = BukovUiTokens.loadDefault();
		buildChildren();
		refreshVisualState();
	}

	@Override
	protected void createChildren() {
		// Button input is safe during super construction; visuals are not.
		super.createChildren();
	}

	private void buildChildren() {
		restingSurface = BukovUiAssets.surface(
				BukovUiAssets.Surface.BUTTON,
				tokens.color("panel.surface"));
		addToBack(restingSurface);
		pressedSurface = BukovUiAssets.surface(
				BukovUiAssets.Surface.BUTTON_PRESSED,
				tokens.color("panel.deep"));
		addToBack(pressedSurface);
		focusedSurface = BukovUiAssets.surface(
				BukovUiAssets.Surface.BUTTON_FOCUSED,
				tokens.color("accent.interact"));
		addToBack(focusedSurface);
		disabledSurface = BukovUiAssets.surface(
				BukovUiAssets.Surface.BUTTON_DISABLED,
				tokens.color("panel.deep"));
		addToBack(disabledSurface);

		if (glyph != null) {
			iconView = new BukovTouchIcon(
					glyph,
					tokens.color("text.primary"),
					tokens.color("accent.interact"),
					tokens.color("text.disabled"));
			add(iconView);
		}
		labelView = PixelScene.renderTextBlock(
				labelText,
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		labelView.hardlight(tokens.color("text.primary"));
		add(labelView);
	}

	public static float recommendedHeight(boolean touch) {
		return BukovVisualContract.controlHeight(touch);
	}

	public float recommendedHeight() {
		return recommendedHeight(touch);
	}

	/**
	 * Touch layouts cannot accidentally shrink below the authored iOS target.
	 */
	@Override
	public Component setSize(float width, float height) {
		return super.setSize(width, touchHeight(height));
	}

	@Override
	public Component setRect(
			float x, float y, float width, float height) {
		return super.setRect(x, y, width, touchHeight(height));
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
		refreshVisualState();
	}

	public boolean isFocused() {
		return focused;
	}

	public void enable(boolean enabled) {
		this.enabled = enabled;
		active = enabled;
		if (!enabled) pressed = false;
		refreshVisualState();
	}

	public boolean enabled() {
		return enabled;
	}

	public void iconOnly(boolean iconOnly) {
		if (iconOnly && glyph == null) {
			throw new IllegalArgumentException(
					"icon-only mode requires a glyph");
		}
		this.iconOnly = iconOnly;
		layout();
	}

	public boolean iconOnly() {
		return iconOnly;
	}

	public void text(String value) {
		if (value == null) {
			throw new IllegalArgumentException("label is required");
		}
		if (value.length() == 0) {
			throw new IllegalArgumentException(
					"an accessible label is required");
		}
		labelText = value;
		labelView.text(value);
		layout();
	}

	public String text() {
		return labelText;
	}

	public void contentRightInset(float inset) {
		if (Float.isNaN(inset) || Float.isInfinite(inset) || inset < 0f) {
			throw new IllegalArgumentException(
					"content inset must be finite and non-negative");
		}
		trailingInset = inset;
		layout();
	}

	@Override
	protected void onPointerDown() {
		if (!enabled) return;
		pressed = true;
		refreshVisualState();
	}

	@Override
	protected void onPointerUp() {
		if (!enabled) return;
		pressed = false;
		refreshVisualState();
	}

	@Override
	protected String hoverText() {
		return iconOnly && labelText.length() > 0
				? labelText
				: null;
	}

	@Override
	protected void layout() {
		super.layout();
		if (restingSurface == null) return;

		layoutSurface(restingSurface);
		layoutSurface(pressedSurface);
		layoutSurface(focusedSurface);
		layoutSurface(disabledSurface);

		boolean showLabel = !iconOnly && labelText.length() > 0;
		labelView.visible = showLabel;
		float iconSize = Math.max(
				1f,
				Math.min(MAX_ICON_SIZE, height - 6f));
		float verticalOffset = pressed && enabled ? 1f : 0f;

		if (iconView == null) {
			layoutCenteredLabel(verticalOffset);
		} else if (!showLabel) {
			iconView.setRect(
					x + (width - iconSize) * 0.5f,
					y + (height - iconSize) * 0.5f,
					iconSize,
					iconSize);
		} else {
			float iconLeft = x + HORIZONTAL_PADDING;
			iconView.setRect(
					iconLeft,
					y + (height - iconSize) * 0.5f,
					iconSize,
					iconSize);
			float textLeft =
					iconLeft + iconSize + ICON_LABEL_GAP;
			float textWidth = Math.max(
					1f,
					right() - HORIZONTAL_PADDING
							- trailingInset - textLeft);
			labelView.maxWidth((int) textWidth);
			labelView.setRect(
					textLeft,
					y + (height - labelView.height()) * 0.5f
							+ verticalOffset,
					textWidth,
					labelView.height());
		}
	}

	private void layoutCenteredLabel(float verticalOffset) {
		float textWidth = Math.max(
				1f,
				width - HORIZONTAL_PADDING * 2f
						- trailingInset);
		labelView.maxWidth((int) textWidth);
		labelView.align(RenderedTextBlock.CENTER_ALIGN);
		labelView.setRect(
				x + HORIZONTAL_PADDING,
				y + (height - labelView.height()) * 0.5f
						+ verticalOffset,
				textWidth,
				labelView.height());
	}

	private void layoutSurface(NinePatch surface) {
		surface.x = x;
		surface.y = y;
		surface.size(width, height);
	}

	private void refreshVisualState() {
		if (restingSurface == null) return;
		restingSurface.visible =
				enabled && !pressed && !focused;
		pressedSurface.visible = enabled && pressed;
		focusedSurface.visible =
				enabled && !pressed && focused;
		disabledSurface.visible = !enabled;
		int foreground = !enabled
				? tokens.color("text.disabled")
				: pressed || focused
						? tokens.color("accent.interact")
						: tokens.color("text.primary");
		labelView.hardlight(foreground);
		labelView.alpha(enabled ? 1f : 0.64f);
		if (iconView != null) {
			iconView.visualState(pressed, !enabled);
		}
		layout();
	}

	private float touchHeight(float requestedHeight) {
		return touch
				? Math.max(requestedHeight, recommendedHeight())
				: requestedHeight;
	}
}
