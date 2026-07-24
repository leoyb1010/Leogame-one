package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovFrameTelemetryTest {

	@Test
	public void reportsNearestRankPercentilesAndFrameBudgetMisses() {
		BukovFrameTelemetry telemetry =
				new BukovFrameTelemetry(0.14f, 2560, 1440, 60);

		assertNull(telemetry.recordFrame(0.010f));
		assertNull(telemetry.recordFrame(0.020f));
		assertNull(telemetry.recordFrame(0.030f));
		assertNull(telemetry.recordFrame(0.040f));
		BukovFrameTelemetry.Report report =
				telemetry.recordFrame(0.050f);

		assertEquals(5L, report.frameCount());
		assertEquals(30d, report.p50Ms(), 0.0001d);
		assertEquals(50d, report.p95Ms(), 0.0001d);
		assertEquals(50d, report.p99Ms(), 0.0001d);
		assertEquals(18.333d, report.frameBudgetMs(), 0.001d);
		assertEquals(4L, report.framesOverBudget());
		assertEquals(2L, report.framesOver33_3Ms());
		assertEquals(50d, report.maximumFrameMs(), 0.001d);
		assertEquals(2560, report.resolutionWidthPx());
		assertEquals(1440, report.resolutionHeightPx());
		assertEquals(60, report.targetRefreshHz());
	}

	@Test
	public void ignoresInvalidDeltasAndResetsAfterEachInterval() {
		BukovFrameTelemetry telemetry =
				new BukovFrameTelemetry(0.029f, 1179, 2556, 60);

		assertNull(telemetry.recordFrame(Float.NaN));
		assertNull(telemetry.recordFrame(Float.POSITIVE_INFINITY));
		assertNull(telemetry.recordFrame(0f));
		assertNull(telemetry.recordFrame(-0.01f));
		assertNull(telemetry.recordFrame(0.01f));
		assertNull(telemetry.recordFrame(0.01f));
		BukovFrameTelemetry.Report first =
				telemetry.recordFrame(0.01f);
		assertEquals(3L, first.frameCount());
		assertEquals(10d, first.p50Ms(), 0.0001d);

		assertNull(telemetry.recordFrame(0.02f));
		BukovFrameTelemetry.Report second =
				telemetry.recordFrame(0.02f);
		assertEquals(2L, second.frameCount());
		assertEquals(20d, second.p50Ms(), 0.0001d);
		assertEquals(20d, second.maximumFrameMs(), 0.001d);
		assertEquals(5L, second.sessionFrameCount());
		assertEquals(0.07d, second.sessionSeconds(), 0.0001d);
		assertEquals(10d, second.sessionP50Ms(), 0.0001d);
		assertEquals(20d, second.sessionP95Ms(), 0.0001d);
		assertEquals(20d, second.sessionP99Ms(), 0.0001d);
		assertEquals(2L, second.sessionFramesOverBudget());
		assertEquals(0L, second.sessionFramesOver33_3Ms());
		assertEquals(20d, second.sessionMaximumFrameMs(), 0.001d);
	}

	@Test
	public void logLineStatesMeasurementAndGpuLimitWithoutAmbiguity() {
		BukovFrameTelemetry telemetry =
				new BukovFrameTelemetry(0.009f, 1920, 1080, 144);
		String line = telemetry.recordFrame(0.01f).toLogLine();

		assertTrue(line.contains(
				"\"schema\":\"bukov-render-frame-v3\""));
		assertTrue(line.contains(
				"\"metricKind\":\"cpu-render-callback-frame-pacing\""));
		assertTrue(line.contains(
				"\"measurement\":\"Gdx.graphics.getDeltaTime\""));
		assertTrue(line.contains("\"hardwareGpuCounter\":false"));
		assertTrue(line.contains(
				"\"scope\":\"render-callback-frame-pacing\""));
		assertTrue(line.contains("\"frames\":1"));
		assertTrue(line.contains("\"p50Ms\":10.000"));
		assertTrue(line.contains("\"p95Ms\":10.000"));
		assertTrue(line.contains("\"p99Ms\":10.000"));
		assertTrue(line.contains("\"frameBudgetMs\":7.639"));
		assertTrue(line.contains("\"framesOverBudget\":1"));
		assertTrue(line.contains("\"framesOver33_3Ms\":0"));
		assertTrue(line.contains("\"maximumFrameMs\":10.000"));
		assertTrue(line.contains("\"sessionFrames\":1"));
		assertTrue(line.contains("\"sessionSeconds\":0.010"));
		assertTrue(line.contains("\"sessionP50Ms\":10.000"));
		assertTrue(line.contains("\"sessionP95Ms\":10.000"));
		assertTrue(line.contains("\"sessionP99Ms\":10.000"));
		assertTrue(line.contains("\"sessionFramesOverBudget\":1"));
		assertTrue(line.contains("\"sessionFramesOver33_3Ms\":0"));
		assertTrue(line.contains("\"sessionMaximumFrameMs\":10.000"));
		assertTrue(line.contains("\"resolutionPx\":\"1920x1080\""));
		assertTrue(line.contains("\"targetRefreshHz\":144"));
		assertFalse(line.contains("\"hardwareGpuCounter\":true"));
	}

	@Test
	public void frameBudgetTracksRefreshWithHostSchedulingTolerance() {
		assertEquals(
				18.333d,
				BukovFrameTelemetry.toleratedFrameBudgetMs(60),
				0.001d);
		assertEquals(
				9.167d,
				BukovFrameTelemetry.toleratedFrameBudgetMs(120),
				0.001d);
		assertEquals(
				7.639d,
				BukovFrameTelemetry.toleratedFrameBudgetMs(144),
				0.001d);
	}

	@Test
	public void normalSixtyHertzRoundingIsNotCountedAsDroppedFrame() {
		BukovFrameTelemetry telemetry =
				new BukovFrameTelemetry(0.035f, 1179, 2556, 60);

		assertNull(telemetry.recordFrame(0.017f));
		assertNull(telemetry.recordFrame(0.017f));
		BukovFrameTelemetry.Report report =
				telemetry.recordFrame(0.0184f);

		assertEquals(1L, report.framesOverBudget());
		assertEquals(1L, report.sessionFramesOverBudget());
	}
}
