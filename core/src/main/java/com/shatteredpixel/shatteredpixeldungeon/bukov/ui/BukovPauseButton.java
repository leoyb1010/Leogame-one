package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

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
		label = PixelScene.renderTextBlock(
				"暂停",
				tokens.typographyPx(
						BukovVisualContract.FONT_BODY));
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
		label.setRect(x + 2, y + (height - 9) / 2f, width - 4, 9);
	}
}
