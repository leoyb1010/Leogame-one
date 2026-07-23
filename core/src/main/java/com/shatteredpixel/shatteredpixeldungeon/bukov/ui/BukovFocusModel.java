package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Small deterministic focus owner for Bukov windows.
 *
 * A child window pushes the parent's current index before opening and pops it
 * on close. The fixed stack keeps focus restoration allocation-free and makes
 * a missing return target impossible during normal UI nesting.
 */
public final class BukovFocusModel {

	private static final int MAX_CHILD_DEPTH = 8;
	private final int[] returnStack = new int[MAX_CHILD_DEPTH];
	private int count;
	private int index;
	private int depth;

	public BukovFocusModel(int count, int defaultIndex) {
		setCount(count);
		focus(defaultIndex);
	}

	public int count() {
		return count;
	}

	public int index() {
		return index;
	}

	public int depth() {
		return depth;
	}

	public void setCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("focus count must be positive");
		}
		this.count = count;
		index = Math.min(index, count - 1);
	}

	public void focus(int target) {
		if (target < 0 || target >= count) {
			throw new IllegalArgumentException("focus target is out of range");
		}
		index = target;
	}

	public void move(int delta) {
		if (delta == 0) {
			return;
		}
		index = wrap(index + (delta < 0 ? -1 : 1));
	}

	public void move(int delta, boolean[] enabled) {
		if (enabled == null || enabled.length != count) {
			throw new IllegalArgumentException(
					"enabled mask must match focus count");
		}
		if (delta == 0 || !hasEnabledTarget(enabled)) {
			return;
		}
		int direction = delta < 0 ? -1 : 1;
		for (int attempt = 0; attempt < count; attempt++) {
			index = wrap(index + direction);
			if (enabled[index]) {
				return;
			}
		}
	}

	public void pushChild() {
		if (depth >= MAX_CHILD_DEPTH) {
			throw new IllegalStateException("focus child stack overflow");
		}
		returnStack[depth++] = index;
	}

	public void popChild() {
		if (depth <= 0) {
			throw new IllegalStateException("focus child stack underflow");
		}
		index = Math.min(returnStack[--depth], count - 1);
	}

	private boolean hasEnabledTarget(boolean[] enabled) {
		for (boolean value : enabled) {
			if (value) {
				return true;
			}
		}
		return false;
	}

	private int wrap(int value) {
		int wrapped = value % count;
		return wrapped < 0 ? wrapped + count : wrapped;
	}
}
