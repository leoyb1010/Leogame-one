/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSAudio;

/**
 * Simulator-only recovery around the real RoboVM/ObjectAL audio backend.
 *
 * A successful initialization always wins. The silent backend exists only so
 * a Java-visible native initialization failure does not prevent simulator QA
 * of the rest of the game. Native process aborts cannot be recovered here and
 * must be fixed in the backend/toolchain instead of being silently hidden.
 */
final class IOSAudioStartupPolicy {

	static final String BACKEND_PROPERTY = "bukov.ios.audioBackend";
	static final String NATIVE_BACKEND = "native";
	static final String SILENT_FALLBACK_BACKEND = "silent-fallback";

	interface Factory {
		IOSAudio create();
	}

	interface FailureLogger {
		void failed(String message, Throwable failure);
	}

	static final FailureLogger SYSTEM_ERROR_LOGGER =
			new FailureLogger() {
				@Override
				public void failed(String message, Throwable failure) {
					System.err.println(message);
					if (failure != null) {
						failure.printStackTrace(System.err);
					}
				}
			};

	static IOSAudio createForSimulator(
			Factory nativeFactory,
			FailureLogger logger) {
		if (nativeFactory == null || logger == null) {
			throw new IllegalArgumentException(
					"nativeFactory and logger are required");
		}
		try {
			IOSAudio audio = nativeFactory.create();
			if (audio == null) {
				throw new IllegalStateException(
						"RoboVM audio factory returned null");
			}
			System.setProperty(BACKEND_PROPERTY, NATIVE_BACKEND);
			return audio;
		} catch (RuntimeException | LinkageError failure) {
			System.setProperty(
					BACKEND_PROPERTY,
					SILENT_FALLBACK_BACKEND);
			logger.failed(
					"[Escape from Bukov][iOS] Simulator native audio "
							+ "initialization failed; continuing with the "
							+ "silent fallback backend.",
					failure);
			return new SilentIOSAudio();
		}
	}

	static boolean usingSilentFallback() {
		return SILENT_FALLBACK_BACKEND.equals(
				System.getProperty(BACKEND_PROPERTY));
	}

	private IOSAudioStartupPolicy() {
	}
}
