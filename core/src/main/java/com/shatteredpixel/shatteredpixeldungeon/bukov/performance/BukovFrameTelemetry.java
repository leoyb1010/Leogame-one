/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

import java.util.Arrays;
import java.util.Locale;

/**
 * Allocation-free frame-delta sampling for a live Bukov render scene.
 *
 * <p>The caller must provide the raw render-callback delta, not the capped or
 * time-scaled simulation delta. The resulting percentiles describe delivered
 * frame pacing, including CPU work, presentation wait and scheduling. They are
 * deliberately labelled as <em>not</em> hardware GPU counters and cannot
 * replace an Instruments/Metal GPU trace.</p>
 *
 * <p>Normal frame sampling only updates primitive fields and a preallocated
 * histogram. A reusable report and its log string are populated only when the
 * fixed reporting interval completes.</p>
 */
public final class BukovFrameTelemetry {

	public static final float DEFAULT_REPORT_INTERVAL_SECONDS = 10f;

	private static final double FRAME_BUDGET_60_HZ_MS = 16.7d;
	private static final double FRAME_BUDGET_30_HZ_MS = 33.3d;
	private static final int HISTOGRAM_BUCKETS_PER_MILLISECOND = 10;
	private static final int MAX_HISTOGRAM_MILLISECONDS = 10_000;
	private static final int HISTOGRAM_BUCKET_COUNT =
			MAX_HISTOGRAM_MILLISECONDS
					* HISTOGRAM_BUCKETS_PER_MILLISECOND
					+ 1;

	private final double reportIntervalSeconds;
	private final int resolutionWidthPx;
	private final int resolutionHeightPx;
	private final int targetRefreshHz;
	private final int[] frameHistogram = new int[HISTOGRAM_BUCKET_COUNT];
	private final Report reusableReport = new Report();

	private double windowSeconds;
	private long frameCount;
	private long framesOver16_7Ms;
	private long framesOver33_3Ms;
	private double maximumFrameMs;

	public BukovFrameTelemetry(
			int resolutionWidthPx,
			int resolutionHeightPx,
			int targetRefreshHz) {
		this(
				DEFAULT_REPORT_INTERVAL_SECONDS,
				resolutionWidthPx,
				resolutionHeightPx,
				targetRefreshHz);
	}

	public BukovFrameTelemetry(
			float reportIntervalSeconds,
			int resolutionWidthPx,
			int resolutionHeightPx,
			int targetRefreshHz) {
		if (!BukovNumbers.isFinite(reportIntervalSeconds)
				|| reportIntervalSeconds <= 0f) {
			throw new IllegalArgumentException(
					"reportIntervalSeconds must be finite and positive");
		}
		if (resolutionWidthPx <= 0 || resolutionHeightPx <= 0) {
			throw new IllegalArgumentException(
					"render resolution must be positive");
		}
		if (targetRefreshHz < 0) {
			throw new IllegalArgumentException(
					"targetRefreshHz cannot be negative");
		}
		this.reportIntervalSeconds = reportIntervalSeconds;
		this.resolutionWidthPx = resolutionWidthPx;
		this.resolutionHeightPx = resolutionHeightPx;
		this.targetRefreshHz = targetRefreshHz;
	}

	/**
	 * Samples one raw render callback delta.
	 *
	 * @return a reusable report when the interval completes, otherwise null.
	 */
	public Report recordFrame(float rawRenderDeltaSeconds) {
		if (!BukovNumbers.isFinite(rawRenderDeltaSeconds)
				|| rawRenderDeltaSeconds <= 0f) {
			return null;
		}

		double frameMs = rawRenderDeltaSeconds * 1_000d;
		int histogramBucket = (int)Math.round(
				frameMs * HISTOGRAM_BUCKETS_PER_MILLISECOND);
		histogramBucket = Math.max(
				0,
				Math.min(HISTOGRAM_BUCKET_COUNT - 1, histogramBucket));
		frameHistogram[histogramBucket]++;

		windowSeconds += rawRenderDeltaSeconds;
		frameCount++;
		if (frameMs > FRAME_BUDGET_60_HZ_MS) {
			framesOver16_7Ms++;
		}
		if (frameMs > FRAME_BUDGET_30_HZ_MS) {
			framesOver33_3Ms++;
		}
		maximumFrameMs = Math.max(maximumFrameMs, frameMs);

		if (windowSeconds < reportIntervalSeconds) {
			return null;
		}

		fillReport();
		resetWindow();
		return reusableReport;
	}

	private void fillReport() {
		long p50Rank = percentileRank(frameCount, 0.50d);
		long p95Rank = percentileRank(frameCount, 0.95d);
		long p99Rank = percentileRank(frameCount, 0.99d);
		long cumulative = 0L;
		double p50Ms = 0d;
		double p95Ms = 0d;
		double p99Ms = 0d;
		boolean p50Found = false;
		boolean p95Found = false;

		for (int bucket = 0; bucket < frameHistogram.length; bucket++) {
			cumulative += frameHistogram[bucket];
			double bucketMs =
					bucket / (double)HISTOGRAM_BUCKETS_PER_MILLISECOND;
			if (!p50Found && cumulative >= p50Rank) {
				p50Ms = bucketMs;
				p50Found = true;
			}
			if (!p95Found && cumulative >= p95Rank) {
				p95Ms = bucketMs;
				p95Found = true;
			}
			if (cumulative >= p99Rank) {
				p99Ms = bucketMs;
				break;
			}
		}

		reusableReport.set(
				windowSeconds,
				frameCount,
				p50Ms,
				p95Ms,
				p99Ms,
				framesOver16_7Ms,
				framesOver33_3Ms,
				maximumFrameMs,
				resolutionWidthPx,
				resolutionHeightPx,
				targetRefreshHz);
	}

	private static long percentileRank(long sampleCount, double percentile) {
		return Math.max(1L, (long)Math.ceil(sampleCount * percentile));
	}

	private void resetWindow() {
		Arrays.fill(frameHistogram, 0);
		windowSeconds = 0d;
		frameCount = 0L;
		framesOver16_7Ms = 0L;
		framesOver33_3Ms = 0L;
		maximumFrameMs = 0d;
	}

	public static final class Report {

		private double windowSeconds;
		private long frameCount;
		private double p50Ms;
		private double p95Ms;
		private double p99Ms;
		private long framesOver16_7Ms;
		private long framesOver33_3Ms;
		private double maximumFrameMs;
		private int resolutionWidthPx;
		private int resolutionHeightPx;
		private int targetRefreshHz;

		private Report() {
		}

		private void set(
				double windowSeconds,
				long frameCount,
				double p50Ms,
				double p95Ms,
				double p99Ms,
				long framesOver16_7Ms,
				long framesOver33_3Ms,
				double maximumFrameMs,
				int resolutionWidthPx,
				int resolutionHeightPx,
				int targetRefreshHz) {
			this.windowSeconds = windowSeconds;
			this.frameCount = frameCount;
			this.p50Ms = p50Ms;
			this.p95Ms = p95Ms;
			this.p99Ms = p99Ms;
			this.framesOver16_7Ms = framesOver16_7Ms;
			this.framesOver33_3Ms = framesOver33_3Ms;
			this.maximumFrameMs = maximumFrameMs;
			this.resolutionWidthPx = resolutionWidthPx;
			this.resolutionHeightPx = resolutionHeightPx;
			this.targetRefreshHz = targetRefreshHz;
		}

		public double windowSeconds() {
			return windowSeconds;
		}

		public long frameCount() {
			return frameCount;
		}

		public double p50Ms() {
			return p50Ms;
		}

		public double p95Ms() {
			return p95Ms;
		}

		public double p99Ms() {
			return p99Ms;
		}

		public long framesOver16_7Ms() {
			return framesOver16_7Ms;
		}

		public long framesOver33_3Ms() {
			return framesOver33_3Ms;
		}

		public double maximumFrameMs() {
			return maximumFrameMs;
		}

		public int resolutionWidthPx() {
			return resolutionWidthPx;
		}

		public int resolutionHeightPx() {
			return resolutionHeightPx;
		}

		public int targetRefreshHz() {
			return targetRefreshHz;
		}

		public String toLogLine() {
			return String.format(
					Locale.ROOT,
					"{\"schema\":\"bukov-render-frame-v1\","
							+ "\"measurement\":\"Gdx.graphics.getDeltaTime\","
							+ "\"hardwareGpuCounter\":false,"
							+ "\"scope\":\"render-callback-frame-pacing\","
							+ "\"frames\":%d,\"windowSeconds\":%.3f,"
							+ "\"p50Ms\":%.3f,\"p95Ms\":%.3f,"
							+ "\"p99Ms\":%.3f,"
							+ "\"framesOver16_7Ms\":%d,"
							+ "\"framesOver33_3Ms\":%d,"
							+ "\"maximumFrameMs\":%.3f,"
							+ "\"resolutionPx\":\"%dx%d\","
							+ "\"targetRefreshHz\":%d}",
					frameCount,
					windowSeconds,
					p50Ms,
					p95Ms,
					p99Ms,
					framesOver16_7Ms,
					framesOver33_3Ms,
					maximumFrameMs,
					resolutionWidthPx,
					resolutionHeightPx,
					targetRefreshHz);
		}
	}
}
