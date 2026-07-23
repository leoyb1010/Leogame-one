package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

/**
 * Eight-way screen-space facing used by the Bukov operator atlas.
 *
 * <p>Rows deliberately follow the manifest order:
 * N, NE, E, SE, S, SW, W, NW.</p>
 */
public enum BukovFacing8 {
	N(0),
	NE(1),
	E(2),
	SE(3),
	S(4),
	SW(5),
	W(6),
	NW(7);

	public final int row;

	BukovFacing8(int row) {
		this.row = row;
	}

	public static BukovFacing8 resolve(float dx, float dy) {
		if (dx == 0f && dy == 0f) {
			return S;
		}
		double angle = Math.atan2(dy, dx);
		int octant = (int)Math.round(angle / (Math.PI / 4d));
		switch (octant) {
			case -2:
				return N;
			case -1:
				return NE;
			case 0:
				return E;
			case 1:
				return SE;
			case 2:
				return S;
			case 3:
				return SW;
			case 4:
			case -4:
				return W;
			case -3:
				return NW;
			default:
				throw new IllegalStateException("Unexpected octant " + octant);
		}
	}
}
