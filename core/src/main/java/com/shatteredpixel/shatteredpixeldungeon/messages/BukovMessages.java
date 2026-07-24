package com.shatteredpixel.shatteredpixeldungeon.messages;

import com.badlogic.gdx.Gdx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Properties;

/**
 * Unambiguous access to the standalone Bukov message bundles.
 *
 * {@link Messages} also exposes an object-scoped varargs overload. A direct
 * {@code Messages.get("bukov.*", stringArgument)} call is therefore
 * ambiguous to javac. Keeping this tiny boundary lets Bukov screens use
 * formatted bundle keys without leaking host class lookup semantics.
 */
public final class BukovMessages {

	private BukovMessages() {
	}

	public static String get(String key, Object... arguments) {
		if (Gdx.files == null) {
			return HeadlessEnglish.get(key, arguments);
		}
		return Messages.get(key, arguments);
	}

	/**
	 * Core model tests intentionally run without a libGDX application. They
	 * still exercise player-facing view models, so loading the English source
	 * bundles here keeps those tests deterministic without initializing the
	 * host settings or graphics platform.
	 */
	private static final class HeadlessEnglish {

		private static final String[] BUNDLE_PATHS = {
				"messages/bukov_entry/bukov_entry.properties",
				"messages/bukov_raid/bukov_raid.properties",
				"messages/bukov_economy/bukov_economy.properties"
		};
		private static final Properties VALUES = load();

		private static String get(String key, Object... arguments) {
			String value = VALUES.getProperty(key);
			if (value == null) {
				return Messages.NO_TEXT_FOUND;
			}
			if (arguments == null || arguments.length == 0) {
				return value;
			}
			try {
				return String.format(Locale.ENGLISH, value, arguments);
			} catch (IllegalFormatException error) {
				return value;
			}
		}

		private static Properties load() {
			File assets = locateAssets();
			Properties result = new Properties();
			for (String relativePath : BUNDLE_PATHS) {
				File bundle = new File(assets, relativePath);
				try (InputStreamReader reader = new InputStreamReader(
						new FileInputStream(bundle),
						StandardCharsets.UTF_8)) {
					result.load(reader);
				} catch (IOException error) {
					throw new IllegalStateException(
							"Cannot load headless Bukov bundle "
									+ bundle.getAbsolutePath(),
							error);
				}
			}
			return result;
		}

		private static File locateAssets() {
			File current = new File(
					System.getProperty("user.dir", "."))
					.getAbsoluteFile();
			for (int depth = 0; depth < 8 && current != null; depth++) {
				File moduleAssets = new File(current, "src/main/assets");
				if (hasBundles(moduleAssets)) {
					return moduleAssets;
				}
				File repositoryAssets =
						new File(current, "core/src/main/assets");
				if (hasBundles(repositoryAssets)) {
					return repositoryAssets;
				}
				current = current.getParentFile();
			}
			throw new IllegalStateException(
					"Cannot locate core/src/main/assets for headless Bukov messages");
		}

		private static boolean hasBundles(File assets) {
			for (String relativePath : BUNDLE_PATHS) {
				if (!new File(assets, relativePath).isFile()) {
					return false;
				}
			}
			return true;
		}
	}
}
