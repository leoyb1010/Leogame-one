/*
 * Leo's Dungeon Siege personal-edition UI.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/** Title-screen button which uses Leo's lion-and-emerald identity artwork. */
public class LeoStyledButton extends StyledButton {

	private final Image normal;
	private final Image pressed;

	public LeoStyledButton(String label) {
		this(label, 9);
	}

	public LeoStyledButton(String label, int size) {
		super(Chrome.Type.BLANK, label, size);
		bg.visible = false;
		normal = new Image(Assets.Interfaces.LEO_BUTTON);
		pressed = new Image(Assets.Interfaces.LEO_BUTTON_DOWN);
		pressed.visible = false;
		addToBack(pressed);
		addToBack(normal);
	}

	@Override
	protected void layout() {
		super.layout();
		fit(normal);
		fit(pressed);
	}

	private void fit(Image image) {
		image.x = x;
		image.y = y;
		image.scale.x = width / image.texture.width;
		image.scale.y = height / image.texture.height;
	}

	@Override
	protected void onPointerDown() {
		normal.visible = false;
		pressed.visible = true;
		Sample.INSTANCE.play(Assets.Sounds.CLICK);
	}

	@Override
	protected void onPointerUp() {
		normal.visible = true;
		pressed.visible = false;
	}

	@Override
	public void alpha(float value) {
		super.alpha(value);
		normal.alpha(value);
		pressed.alpha(value);
	}
}
