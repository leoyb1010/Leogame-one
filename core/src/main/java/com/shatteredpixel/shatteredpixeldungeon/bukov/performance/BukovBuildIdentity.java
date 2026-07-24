package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.watabou.noosa.Game;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Runtime identity embedded by the platform build.
 *
 * <p>Final frame evidence must identify its exact source commit and platform.
 * The desktop build supplies a classpath properties file, while the iOS
 * launcher supplies the same values from its generated Info.plist.</p>
 */
public final class BukovBuildIdentity {

	private static final String RESOURCE =
			"bukov-build-identity.properties";
	private static final String UNKNOWN = "unknown";

	private final String sourceCommit;
	private final String buildId;
	private final String platform;

	private BukovBuildIdentity(
			String sourceCommit,
			String buildId,
			String platform) {
		this.sourceCommit = sourceCommit;
		this.buildId = buildId;
		this.platform = platform;
	}

	public static BukovBuildIdentity current() {
		Properties embedded = new Properties();
		try (InputStream input = BukovBuildIdentity.class
				.getResourceAsStream("/" + RESOURCE)) {
			if (input != null) embedded.load(input);
		} catch (IOException ignored) {
			// The gate rejects unknown identity; gameplay must still launch.
		}

		String sourceCommit = value(
				"bukov.sourceCommit",
				embedded.getProperty("source_commit"));
		String platform = value(
				"bukov.platform",
				embedded.getProperty("platform"));
		if (UNKNOWN.equals(platform)) {
			String osName = System.getProperty("os.name", "")
					.toLowerCase(Locale.ROOT);
			if (osName.contains("mac")) platform = "macOS";
		}
		String buildId = value(
				"bukov.buildId",
				embedded.getProperty("build_id"));
		if (UNKNOWN.equals(buildId)
				&& !UNKNOWN.equals(sourceCommit)
				&& !UNKNOWN.equals(platform)) {
			buildId = sourceCommit
					+ "-"
					+ Game.versionCode
					+ "-"
					+ platform.toLowerCase(Locale.ROOT);
		}
		return new BukovBuildIdentity(sourceCommit, buildId, platform);
	}

	private static String value(String systemProperty, String embedded) {
		String runtime = System.getProperty(systemProperty);
		if (runtime != null && !runtime.trim().isEmpty()) {
			return runtime.trim();
		}
		if (embedded != null && !embedded.trim().isEmpty()) {
			return embedded.trim();
		}
		return UNKNOWN;
	}

	public String sourceCommit() {
		return sourceCommit;
	}

	public String buildId() {
		return buildId;
	}

	public String platform() {
		return platform;
	}
}
