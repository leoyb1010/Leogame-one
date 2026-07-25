package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Keeps authored UI animation timing in ui_tokens.json without sweeping
 * simulation, ballistic-causality, save cadence, or input-repeat rules into
 * the presentation contract.
 */
public class BukovMotionTokenBoundaryGuardTest {

	private static final Path UI = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui");
	private static final Path SCENES = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes");
	private static final String[] PLAYER_SCENES = {
			"TitleScene.java",
			"WelcomeScene.java",
			"BukovHubScene.java",
			"BukovDeploymentScene.java",
			"GameScene.java"
	};
	private static final Pattern NUMERIC_LITERAL = Pattern.compile(
			"[-+]?\\d+(?:\\.\\d+)?[fFdDlL]?");
	private static final Pattern NUMERIC_CONSTANT = Pattern.compile(
			"\\bstatic\\s+final\\s+(?:int|long|float|double)\\s+"
					+ "([A-Z][A-Z0-9_]*)\\s*=\\s*"
					+ "[-+]?\\d+(?:\\.\\d+)?[fFdDlL]?\\s*;");
	private static final Pattern VISUAL_DURATION_ROLE = Pattern.compile(
			"(?:^|_)(?:ANIMATION|DURATION|FADE|HOLD|LIFETIME|"
					+ "REVEAL|TICK|TRANSITION)(?:_|$)");

	@Test
	public void bukovUiAndPlayerScenesDoNotReauthorVisualDurationConstants()
			throws Exception {
		for (Path path : playerMotionSources()) {
			String source = source(path);
			Matcher constants = NUMERIC_CONSTANT.matcher(source);
			while (constants.find()) {
				String name = constants.group(1);
				if (VISUAL_DURATION_ROLE.matcher(name).find()) {
					fail(path.getFileName() + " reauthors visual duration "
							+ name + " outside ui_tokens.json");
				}
			}
		}
	}

	@Test
	public void tweenAndMotionSchedulerDurationsAreNeverNumericLiterals()
			throws Exception {
		for (Path path : playerMotionSources()) {
			String source = source(path);
			for (Call call : calls(source, "motionScheduler.start")) {
				assertFalse(
						path.getFileName()
								+ " passes a literal motion scheduler duration",
						isNumeric(call.lastArgument()));
			}
			for (Call call : calls(source, "new Tweener")) {
				if (call.arguments.size() < 2
						|| !isNumeric(call.arguments.get(1))) {
					continue;
				}
				assertTrue(
						path.getFileName()
								+ " authors a Tween duration outside motion tokens",
						isClassicOnlyIntroTween(path, source, call));
			}
		}
	}

	@Test
	public void hudVisualClocksConsumeNamedMotionRoles() throws Exception {
		String timeline = withoutWhitespace(
				source(UI.resolve("BukovCombatHudTimeline.java")));
		for (String role : new String[] {
				"hud.idleHold",
				"hud.fade",
				"hud.damageArc",
				"hud.killConfirm"
			}) {
			assertTrue(role, timeline.contains(
					"tokens.motionSeconds(\"" + role + "\")"));
		}

		String hud = withoutWhitespace(
				source(UI.resolve("BukovRaidHud.java")));
		for (String role : new String[] {
				"hud.damageArc",
				"hud.killConfirm",
				"hud.soundRing"
		}) {
			assertTrue(role, hud.contains(
					"tokens.motionSeconds(\"" + role + "\")"));
		}

		String world = withoutWhitespace(source(Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java")));
		assertTrue(world.contains(
				"BukovUiTokens.loadDefault().motionSeconds("
						+ "\"hud.soundRing\")"));
		assertFalse(world.contains("KEY_SOUND_LIFETIME_SECONDS"));
	}

	@Test
	public void productionTokenGateRunsThisBoundaryTest() throws Exception {
		String gate = source(Paths.get("../scripts/bukov_ui_tokens_check.sh"));
		assertTrue(gate.contains("*BukovMotionTokenBoundaryGuardTest"));
	}

	private static List<Path> playerMotionSources() throws Exception {
		List<Path> result = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(UI)) {
			for (Path path : (Iterable<Path>)paths
					.filter(value -> value.toString().endsWith(".java"))
					::iterator) {
				result.add(path);
			}
		}
		for (String scene : PLAYER_SCENES) {
			result.add(SCENES.resolve(scene));
		}
		return result;
	}

	private static boolean isClassicOnlyIntroTween(
			Path path, String source, Call call) {
		if (!"GameScene.java".equals(path.getFileName().toString())
				|| call.arguments.size() != 2
				|| !"scene".equals(call.arguments.get(0).trim())
				|| !"2f".equals(call.arguments.get(1).trim())) {
			return false;
		}
		int method = source.lastIndexOf(
				"public static void endIntro()", call.start);
		if (method < 0) return false;
		String guardedPrefix = source.substring(method, call.start);
		return guardedPrefix.contains("if (BukovMode.active())")
				&& guardedPrefix.contains("return;");
	}

	private static List<Call> calls(String source, String marker) {
		List<Call> result = new ArrayList<>();
		int from = 0;
		while (from < source.length()) {
			int start = source.indexOf(marker, from);
			if (start < 0) break;
			int open = source.indexOf('(', start + marker.length());
			if (open < 0) break;
			int close = matchingParenthesis(source, open);
			if (close < 0) {
				fail("Unclosed call after " + marker);
			}
			result.add(new Call(
					start,
					splitTopLevelArguments(
							source.substring(open + 1, close))));
			from = close + 1;
		}
		return result;
	}

	private static int matchingParenthesis(String source, int open) {
		int depth = 0;
		boolean quoted = false;
		boolean escaped = false;
		for (int index = open; index < source.length(); index++) {
			char value = source.charAt(index);
			if (quoted) {
				if (escaped) {
					escaped = false;
				} else if (value == '\\') {
					escaped = true;
				} else if (value == '"') {
					quoted = false;
				}
				continue;
			}
			if (value == '"') {
				quoted = true;
			} else if (value == '(') {
				depth++;
			} else if (value == ')' && --depth == 0) {
				return index;
			}
		}
		return -1;
	}

	private static List<String> splitTopLevelArguments(String body) {
		List<String> result = new ArrayList<>();
		int depth = 0;
		int start = 0;
		boolean quoted = false;
		boolean escaped = false;
		for (int index = 0; index < body.length(); index++) {
			char value = body.charAt(index);
			if (quoted) {
				if (escaped) {
					escaped = false;
				} else if (value == '\\') {
					escaped = true;
				} else if (value == '"') {
					quoted = false;
				}
				continue;
			}
			if (value == '"') {
				quoted = true;
			} else if (value == '(' || value == '[' || value == '{') {
				depth++;
			} else if (value == ')' || value == ']' || value == '}') {
				depth--;
			} else if (value == ',' && depth == 0) {
				result.add(body.substring(start, index));
				start = index + 1;
			}
		}
		result.add(body.substring(start));
		return result;
	}

	private static boolean isNumeric(String value) {
		return NUMERIC_LITERAL.matcher(value.trim()).matches();
	}

	private static String withoutWhitespace(String value) {
		return value.replaceAll("\\s+", "");
	}

	private static String source(Path path) throws Exception {
		return new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8);
	}

	private static final class Call {
		final int start;
		final List<String> arguments;

		Call(int start, List<String> arguments) {
			this.start = start;
			this.arguments = arguments;
		}

		String lastArgument() {
			return arguments.get(arguments.size() - 1);
		}
	}
}
