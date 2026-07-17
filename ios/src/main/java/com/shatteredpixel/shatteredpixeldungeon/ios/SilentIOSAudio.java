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

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.iosrobovm.DisabledIOSAudio;
import com.badlogic.gdx.files.FileHandle;

final class SilentIOSAudio extends DisabledIOSAudio {

	private static final Sound SILENT_SOUND = new Sound() {
		@Override public long play() { return -1; }
		@Override public long play(float volume) { return -1; }
		@Override public long play(float volume, float pitch, float pan) { return -1; }
		@Override public long loop() { return -1; }
		@Override public long loop(float volume) { return -1; }
		@Override public long loop(float volume, float pitch, float pan) { return -1; }
		@Override public void stop() { }
		@Override public void pause() { }
		@Override public void resume() { }
		@Override public void dispose() { }
		@Override public void stop(long soundId) { }
		@Override public void pause(long soundId) { }
		@Override public void resume(long soundId) { }
		@Override public void setLooping(long soundId, boolean looping) { }
		@Override public void setPitch(long soundId, float pitch) { }
		@Override public void setVolume(long soundId, float volume) { }
		@Override public void setPan(long soundId, float pan, float volume) { }
	};

	@Override
	public Sound newSound(FileHandle fileHandle) {
		return SILENT_SOUND;
	}

	@Override
	public Music newMusic(FileHandle fileHandle) {
		return new Music() {
			private boolean looping;
			private float volume = 1f;

			@Override public void play() { }
			@Override public void pause() { }
			@Override public void stop() { }
			@Override public boolean isPlaying() { return false; }
			@Override public void setLooping(boolean value) { looping = value; }
			@Override public boolean isLooping() { return looping; }
			@Override public void setVolume(float value) { volume = value; }
			@Override public float getVolume() { return volume; }
			@Override public void setPan(float pan, float volume) { this.volume = volume; }
			@Override public void setPosition(float position) { }
			@Override public float getPosition() { return 0; }
			@Override public void dispose() { }
			@Override public void setOnCompletionListener(OnCompletionListener listener) { }
		};
	}
}
