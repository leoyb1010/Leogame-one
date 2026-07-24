package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.TextInput;
import com.watabou.utils.DeviceCompat;

/** Bukov-native single-line stash search without inherited dungeon chrome. */
public final class WndBukovInventorySearch extends Window {

	public interface Result {
		void apply(String query);
	}

	private static final int WIDTH = 150;
	private static final int HEIGHT = 70;
	private static final int MARGIN = 5;
	private static final int BUTTON_HEIGHT = 18;

	private final BukovUiTokens tokens;
	private final Result result;
	private final TextInput textBox;
	private final String initialQuery;
	private boolean completed;

	public WndBukovInventorySearch(String initialQuery, Result result) {
		super(0, 0, new NinePatch(
				TextureCache.createSolid(
						BukovUiTokens.loadDefault().colorWithAlpha(
								"ink.background", 255)),
				0));
		if (result == null) {
			throw new IllegalArgumentException("result is required");
		}
		this.result = result;
		this.initialQuery = normalize(initialQuery);
		tokens = BukovUiTokens.loadDefault();
		resize(
				BukovWindowLayout.safeWidth(WIDTH),
				BukovWindowLayout.safeHeight(HEIGHT));

		RenderedTextBlock title = PixelScene.renderTextBlock(
				"仓库搜索 / STASH SEARCH",
				tokens.typographyPx(
						BukovVisualContract.FONT_SECTION));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(MARGIN, 4);
		add(title);

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				"名称、类别、稀有度或物品代号",
				tokens.typographyPx(
						BukovVisualContract.FONT_CAPTION));
		hint.hardlight(tokens.color("text.secondary"));
		hint.setPos(MARGIN, 16);
		add(hint);

		int baseTextSize = tokens.typographyPx(
				BukovVisualContract.FONT_BODY);
		int textSize = Math.max(
				baseTextSize,
				(int)PixelScene.uiCamera.zoom * baseTextSize);
		textBox = new TextInput(
				new NinePatch(
						TextureCache.createSolid(
								tokens.colorWithAlpha(
										"panel.surface", 255)),
						0),
				false,
				textSize) {
			@Override
			public void enterPressed() {
				submit(getText());
			}
		};
		textBox.setMaxLength(32);
		textBox.setText(this.initialQuery);
		add(textBox);
		textBox.setRect(
				MARGIN,
				26,
				width - MARGIN * 2,
				16);

		float buttonWidth = (width - MARGIN * 2 - 3f) / 2f;
		SearchButton apply = new SearchButton(
				"应用筛选",
				"accent.interact") {
			@Override
			protected void onClick() {
				submit(textBox.getText());
			}
		};
		apply.setRect(
				MARGIN,
				47,
				buttonWidth,
				BUTTON_HEIGHT);
		add(apply);

		SearchButton clear = new SearchButton(
				"清除搜索",
				"panel.border") {
			@Override
			protected void onClick() {
				submit("");
			}
		};
		clear.setRect(
				apply.right() + 3f,
				47,
				buttonWidth,
				BUTTON_HEIGHT);
		add(clear);

		// Match the engine's proven text-entry behavior: iOS does not resize
		// the logical game surface when its software keyboard appears.
		if (!DeviceCompat.hasHardKeyboard()) {
			offset(0, -(int)(Game.height / (4 * camera.zoom)));
			boundOffsetWithMargin(0);
			textBox.setRect(
					textBox.left(),
					textBox.top(),
					textBox.width(),
					textBox.height());
		}
		PointerEvent.clearKeyboardThisPress = false;
	}

	private void submit(String query) {
		restore(normalize(query));
	}

	private void restore(String query) {
		if (completed) {
			return;
		}
		completed = true;
		hide();
		result.apply(query);
	}

	private static String normalize(String query) {
		return query == null ? "" : query.trim();
	}

	@Override
	public void onBackPressed() {
		restore(initialQuery);
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (textBox != null) {
			textBox.setRect(
					textBox.left(),
					textBox.top(),
					textBox.width(),
					textBox.height());
		}
	}

	private class SearchButton extends Button {

		private final ColorBlock surface;
		private final ColorBlock edge;
		private final RenderedTextBlock label;

		private SearchButton(String value, String token) {
			surface = new ColorBlock(
					1,
					1,
					tokens.colorWithAlpha(token, 42));
			addToBack(surface);
			edge = new ColorBlock(
					1,
					1,
					tokens.color(token));
			add(edge);
			label = PixelScene.renderTextBlock(
					value,
					tokens.typographyPx(
							BukovVisualContract.FONT_BODY));
			label.hardlight(tokens.color("text.primary"));
			label.align(RenderedTextBlock.CENTER_ALIGN);
			add(label);
		}

		@Override
		protected void layout() {
			super.layout();
			surface.x = x;
			surface.y = y;
			surface.size(width, height);
			edge.x = x;
			edge.y = y + height - 1f;
			edge.size(width, 1f);
			label.maxWidth(Math.max(1, (int)width - 4));
			label.setPos(
					x + (width - label.width()) / 2f,
					y + (height - label.height()) / 2f);
		}
	}
}
