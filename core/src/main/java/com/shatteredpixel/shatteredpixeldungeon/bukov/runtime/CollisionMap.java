package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

public interface CollisionMap {
	int width();
	int height();
	boolean blocked(int x, int y);
}
