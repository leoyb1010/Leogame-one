package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeInputProductionWiringTest {

	@Test
	public void gameInputSamplesOncePerRenderAndDrainsPerFixedStep()
			throws Exception {
		String system = source("RealtimeRaidSystem.java");
		String input = source("RealtimeInput.java");
		String world = source("BukovRealtimeWorld.java");

		int sample = system.indexOf("world.sampleInput();");
		int advance = system.indexOf("clock.advanceWhile(renderDelta");
		assertTrue(sample >= 0);
		assertTrue(sample < advance);
		assertTrue(system.contains(
				"private void fixedUpdate(float dt) {\n"
						+ "\t\tworld.beginFixedStep();\n"
						+ "\t\tworld.pollInput();"));

		String sampleBody = between(
				input,
				"public void sample(RealtimeBody heroBody) {",
				"public InputFrame consumeFixedStep() {");
		assertTrue(sampleBody.contains("isButtonJustPressed("));
		assertTrue(sampleBody.contains("anyKeyJustPressed("));
		assertTrue(sampleBody.contains("edgeLatch.capture(frame)"));

		String consumeBody = between(
				input,
				"public InputFrame consumeFixedStep() {",
				"public InputFrame poll(RealtimeBody heroBody) {");
		assertTrue(consumeBody.contains("edgeLatch.drainTo(fixedFrame)"));
		assertFalse(consumeBody.contains("Gdx.input"));
		assertFalse(consumeBody.contains("consumePressed("));

		assertTrue(world.contains(
				"public void sampleInput() {\n"
						+ "\t\tinput.sample(heroBody);"));
		assertTrue(world.contains(
				"public void pollInput() {\n"
						+ "\t\tinputFrame = input.consumeFixedStep();"));
	}

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ file)),
				StandardCharsets.UTF_8);
	}

	private static String between(
			String source, String start, String end) {
		int startIndex = source.indexOf(start);
		assertTrue("missing start marker: " + start, startIndex >= 0);
		int endIndex = source.indexOf(end, startIndex + start.length());
		assertTrue("missing end marker: " + end, endIndex > startIndex);
		return source.substring(startIndex, endIndex);
	}
}
