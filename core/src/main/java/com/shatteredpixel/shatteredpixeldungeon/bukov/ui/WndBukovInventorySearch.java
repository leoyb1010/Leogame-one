package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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
	private static final int BUTTON_HEIGHT = 22;
	private static final float ACTION_ICON_SIZE = 10f;
	private static final float ACTION_ICON_LABEL_GAP = 2f;

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
				BukovMessages.get(
						"bukov.economy.search.title"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_SECTION));
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(MARGIN, 4);
		add(title);

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				BukovMessages.get(
						"bukov.economy.search.hint"),
				tokens.scaledTypographyPx(
						BukovVisualContract.FONT_CAPTION));
		hint.hardlight(tokens.color("text.secondary"));
		hint.setPos(MARGIN, 16);
		add(hint);

		int baseTextSize = tokens.scaledTypographyPx(
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
				BukovTouchIcon.Glyph.SEARCH,
				BukovMessages.get("bukov.economy.search.apply"),
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
				BukovTouchIcon.Glyph.DROP,
				BukovMessages.get("bukov.economy.search.clear"),
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

	private static String shortActionLabel(String value) {
		String normalized = value == null ? "" : value.trim();
		int whitespace = normalized.indexOf(' ');
		if (whitespace > 0) {
			normalized = normalized.substring(0, whitespace);
		}
		int codePoints = normalized.codePointCount(0, normalized.length());
		int maximum = normalized.matches("\\p{ASCII}*") ? 7 : 4;
		if (codePoints <= maximum) {
			return normalized;
		}
		return normalized.substring(
				0,
				normalized.offsetByCodePoints(0, maximum - 1))
				+ "…";
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
		private final BukovTouchIcon icon;
		private final RenderedTextBlock label;

		private SearchButton(
				BukovTouchIcon.Glyph glyph,
				String value,
				String token) {
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
			icon = new BukovTouchIcon(
					glyph,
					tokens.color("text.primary"),
					tokens.color(token),
					tokens.color("text.disabled"));
			add(icon);
			label = PixelScene.renderTextBlock(
					shortActionLabel(value),
					tokens.scaledTypographyPx(
							BukovVisualContract.FONT_CAPTION));
			label.hardlight(tokens.color("text.primary"));
			add(label);
		}

		@Override
		protected void onPointerDown() {
			icon.visualState(true, false);
		}

		@Override
		protected void onPointerUp() {
			icon.visualState(false, false);
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
			int labelWidth = Math.max(
					1,
					(int)(width - ACTION_ICON_SIZE
							- ACTION_ICON_LABEL_GAP - 8f));
			label.maxWidth(labelWidth);
			float contentWidth = ACTION_ICON_SIZE
					+ ACTION_ICON_LABEL_GAP + label.width();
			float contentX = x + (width - contentWidth) / 2f;
			icon.setRect(
					contentX,
					y + (height - ACTION_ICON_SIZE) / 2f,
					ACTION_ICON_SIZE,
					ACTION_ICON_SIZE);
			label.setPos(
					contentX + ACTION_ICON_SIZE
							+ ACTION_ICON_LABEL_GAP,
					y + (height - label.height()) / 2f);
		}
	}
}
