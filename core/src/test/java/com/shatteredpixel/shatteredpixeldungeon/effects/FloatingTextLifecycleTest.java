package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.RenderedText;
import com.watabou.utils.GameSettings;
import com.watabou.utils.PlatformSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FloatingTextLifecycleTest {

	private PlatformSupport previousPlatform;
	private Camera previousCamera;
	private Files previousFiles;
	private int previousDefaultZoom;

	@Before
	public void setUp() {
		previousPlatform = Game.platform;
		previousCamera = Camera.main;
		previousFiles = Gdx.files;
		previousDefaultZoom = PixelScene.defaultZoom;
		GameSettings.set(defaultPreferences());
		Gdx.files = defaultFiles();
		Game.platform = new TextOnlyPlatform();
		Camera.main = new Camera(0, 0, 320, 240, 1f);
		PixelScene.defaultZoom = 1;
	}

	@After
	public void tearDown() {
		Game.platform = previousPlatform;
		Camera.main = previousCamera;
		Gdx.files = previousFiles;
		PixelScene.defaultZoom = previousDefaultZoom;
	}

	@Test
	public void identicalLiveTextReusesWordsButKilledTextRebuildsOnce() {
		TestTextBlock block = new TestTextBlock();
		block.text("7");
		RenderedText first = block.firstWord();

		block.text("7");
		assertSame(first, block.firstWord());

		block.hardlight(0x2468AC);
		block.alpha(0.2f);
		block.kill();
		block.revive();
		block.text("7");

		RenderedText rebuilt = block.firstWord();
		assertNotSame(first, rebuilt);
		assertTrue(rebuilt.exists);
		assertTrue(rebuilt.alive);
		assertSame(block, rebuilt.parent);
		assertEquals(1f, rebuilt.alpha(), 0f);
		assertEquals(0x24 / 255f, rebuilt.rm, 0.0001f);
		assertEquals(0x68 / 255f, rebuilt.gm, 0.0001f);
		assertEquals(0xAC / 255f, rebuilt.bm, 0.0001f);
	}

	@Test
	public void differentTextAndMaxWidthStillForceARebuild() {
		TestTextBlock block = new TestTextBlock();
		block.text("alpha");
		RenderedText alpha = block.firstWord();

		block.text("beta");
		RenderedText beta = block.firstWord();
		assertNotSame(alpha, beta);

		block.maxWidth(24);
		assertNotSame(beta, block.firstWord());
		assertEquals(24, block.maxWidth());
		assertEquals("beta", block.text());
	}

	@Test
	public void nullAndEmptyTextClearExistingChildrenAndBounds() {
		TestTextBlock block = new TestTextBlock();
		block.text("visible");
		assertTrue(block.renderedWordCount() > 0);

		block.text(null);
		assertEquals(0, block.renderedWordCount());
		assertEquals(0f, block.width(), 0f);
		assertEquals(0f, block.height(), 0f);

		block.text("again");
		block.text("");
		assertEquals(0, block.renderedWordCount());
		assertEquals(0f, block.width(), 0f);
		assertEquals(0f, block.height(), 0f);
	}

	@Test
	public void pooledFloatingTextRestoresWordsAndIconAlignment() {
		TestFloatingText text = new TestFloatingText();
		text.reset(100, 100, "12", 0xFF4422, FloatingText.NO_ICON, false);
		RenderedText first = text.firstWord();
		text.alpha(0.15f);
		text.kill();

		text.reset(100, 100, "12", 0x44CC88, 0, true);
		RenderedText rebuilt = text.firstWord();
		assertNotSame(first, rebuilt);
		assertTrue(rebuilt.exists);
		assertTrue(rebuilt.alive);
		assertEquals(1f, rebuilt.alpha(), 0f);
		assertTrue(text.firstWordOffset() > 0f);
		assertTrue(text.width() > 0f);

		text.kill();
		text.reset(100, 100, "12", 0x44CC88, 0, false);
		assertEquals(0f, text.firstWordOffset(), 0f);
		assertTrue(text.width() > 0f);

		text.kill();
		text.reset(
				100,
				100,
				"12",
				0x44CC88,
				FloatingText.NO_ICON,
				false);
		assertEquals(0f, text.firstWordOffset(), 0f);
		assertEquals("12", text.text());
		assertTrue(text.firstWord() != null);
	}

	private static final class TestTextBlock extends RenderedTextBlock {

		private TestTextBlock() {
			super(9);
		}

		private RenderedText firstWord() {
			return words.get(0);
		}

		private int renderedWordCount() {
			int count = 0;
			for (RenderedText word : words) {
				if (word != null && word.parent == this) count++;
			}
			return count;
		}
	}

	private static final class TestFloatingText extends FloatingText {

		@Override
		protected Image createIcon(int iconIdx) {
			return new TestIcon();
		}

		private RenderedText firstWord() {
			return words.get(0);
		}

		private float firstWordOffset() {
			return firstWord().x - left();
		}
	}

	private static final class TestIcon extends Image {

		private TestIcon() {
			width = FloatingText.ICON_WIDTH;
			height = FloatingText.ICON_HEIGHT;
		}
	}

	private static final class TextOnlyPlatform extends PlatformSupport {

		@Override
		public void updateDisplaySize() {
		}

		@Override
		public void updateSystemUI() {
		}

		@Override
		public boolean connectedToUnmeteredNetwork() {
			return false;
		}

		@Override
		public boolean supportsVibration() {
			return false;
		}

		@Override
		public void setupFontGenerators(int pageSize, boolean systemFont) {
		}

		@Override
		protected FreeTypeFontGenerator getGeneratorForString(String input) {
			return null;
		}

		@Override
		public BitmapFont getFont(int size, String text, boolean flipped, boolean border) {
			return null;
		}

		@Override
		public String[] splitforTextBlock(String text, boolean multiline) {
			return new String[] {text};
		}
	}

	private static Preferences defaultPreferences() {
		return (Preferences) Proxy.newProxyInstance(
				Preferences.class.getClassLoader(),
				new Class<?>[] {Preferences.class},
				(proxy, method, args) -> {
					String name = method.getName();
					if (name.startsWith("put")) return proxy;
					if (name.equals("get")) return Collections.emptyMap();
					if (name.equals("contains")) return false;
					if (name.startsWith("get") && args != null && args.length == 2) {
						return args[1];
					}
					return null;
				});
	}

	private static Files defaultFiles() {
		File workingDirectory = new File(System.getProperty("user.dir"));
		File assets = new File(workingDirectory, "core/src/main/assets");
		if (!assets.isDirectory()) {
			assets = new File(workingDirectory, "src/main/assets");
		}
		final File assetRoot = assets;
		return (Files) Proxy.newProxyInstance(
				Files.class.getClassLoader(),
				new Class<?>[] {Files.class},
				(proxy, method, args) -> {
					if (args != null && args.length > 0 && args[0] instanceof String) {
						return new FileHandle(new File(assetRoot, (String) args[0]));
					}
					if (method.getReturnType() == String.class) return "";
					return null;
				});
	}

}
