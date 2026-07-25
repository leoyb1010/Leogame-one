package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.utils.Callback;

/** Small raid-native pause affordance for touch devices without a back key. */
public final class BukovPauseButton extends Button {

	private final Callback callback;
	private BukovUiTokens tokens;
	private ColorBlock background;
	private ColorBlock edge;
	private BukovTouchIcon icon;
	private RenderedTextBlock label;

	public BukovPauseButton(Callback callback) {
		this.callback = callback;
	}

	@Override
	protected void createChildren() {
		super.createChildren();
		tokens = BukovUiTokens.loadDefault();
		background = new ColorBlock(
				1, 1, tokens.colorWithAlpha("panel.surface", 224));
		addToBack(background);
		edge = new ColorBlock(1, 1, tokens.color("accent.interact"));
		add(edge);
		icon = new BukovTouchIcon(
				BukovTouchIcon.Glyph.PAUSE,
				tokens.color("text.primary"),
				tokens.color("accent.interact"),
				tokens.color("text.disabled"));
		add(icon);
		label = PixelScene.renderTextBlock(
				BukovMessages.get("bukov.raid.pause.button"),
				tokens.typographyPx(
						BukovVisualContract.FONT_CAPTION));
		label.hardlight(tokens.color("text.primary"));
		label.align(RenderedTextBlock.CENTER_ALIGN);
		add(label);
	}

	@Override
	protected void onClick() {
		if (callback != null) {
			callback.call();
		}
	}

	@Override
	protected void layout() {
		super.layout();
		background.x = x;
		background.y = y;
		background.size(width, height);
		edge.x = x;
		edge.y = y;
		edge.size(width, 1);
		float iconSize = Math.max(8f, Math.min(12f, height - 4f));
		icon.setRect(
				x + 4f,
				y + (height - iconSize) * 0.5f,
				iconSize,
				iconSize);
		// setRect only positions a RenderedTextBlock; without an explicit fit
		// the label keeps its natural width and draws straight over the icon.
		float labelWidth = Math.max(1f, width - iconSize - 11f);
		label.text(BukovRaidHudLayout.compactLine(
				BukovMessages.get("bukov.raid.pause.button"),
				labelWidth,
				0));
		label.setRect(
				x + iconSize + 7f,
				y + (height - 7) / 2f,
				labelWidth,
				7);
	}
}
