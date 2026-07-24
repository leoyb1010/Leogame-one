package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import java.util.Arrays;

/**
 * Bounded, allocation-free grid navigation for one realtime enemy.
 *
 * <p>Paths are cached and searches are staggered per stable enemy id. A search
 * is repeated only when the target tile changes, the cached path becomes
 * invalid, or movement has remained blocked long enough to count as stuck.
 * The bounded BFS deliberately falls back to the visited tile closest to the
 * target, so a large or temporarily unreachable map cannot stall a frame.</p>
 */
public final class RealtimeEnemyNavigator {

	public static final float REPATH_INTERVAL_SECONDS = 0.35f;
	public static final float UNREACHABLE_RETRY_SECONDS = 0.75f;
	public static final float STUCK_REPATH_SECONDS = 0.32f;
	public static final int DEFAULT_NODE_BUDGET = 2048;

	public static final class Intent {
		private float desiredX;
		private float desiredY;
		private boolean followingPath;
		private boolean targetUnreachable;

		private void set(
				float desiredX,
				float desiredY,
				boolean followingPath,
				boolean targetUnreachable) {
			this.desiredX = desiredX;
			this.desiredY = desiredY;
			this.followingPath = followingPath;
			this.targetUnreachable = targetUnreachable;
		}

		public float desiredX() {
			return desiredX;
		}

		public float desiredY() {
			return desiredY;
		}

		public boolean followingPath() {
			return followingPath;
		}

		public boolean targetUnreachable() {
			return targetUnreachable;
		}
	}

	private static final int[] DIRECTION_X = {1, 0, -1, 0};
	private static final int[] DIRECTION_Y = {0, 1, 0, -1};

	private final int width;
	private final int height;
	private final int stableKey;
	private final int nodeBudget;
	private final int[] queue;
	private final int[] parents;
	private final int[] visited;
	private final int[] path;

	private int visitStamp;
	private int cachedTargetCell = -1;
	private int pathLength;
	private int pathIndex;
	private int searchSequence;
	private int lastExpandedNodes;
	private int totalExpandedNodes;
	private int totalSearches;
	private float repathRemaining;
	private float stuckSeconds;
	private float observedX;
	private float observedY;
	private boolean hasObservedPosition;
	private boolean targetUnreachable;
	private boolean forceRepath;
	private boolean initialized;
	private boolean commandedMovement;

	public RealtimeEnemyNavigator(int stableKey, int width, int height) {
		this(stableKey, width, height, DEFAULT_NODE_BUDGET);
	}

	RealtimeEnemyNavigator(
			int stableKey,
			int width,
			int height,
			int nodeBudget) {
		if (width < 3 || height < 3) {
			throw new IllegalArgumentException(
					"navigation map must be at least 3x3");
		}
		if (nodeBudget <= 0) {
			throw new IllegalArgumentException("nodeBudget must be positive");
		}
		this.width = width;
		this.height = height;
		this.stableKey = stableKey;
		this.nodeBudget = Math.min(nodeBudget, width * height);
		queue = new int[width * height];
		parents = new int[width * height];
		visited = new int[width * height];
		path = new int[width * height];
		int slot = floorMod(stableKey, 8);
		repathRemaining = slot * (REPATH_INTERVAL_SECONDS / 8f);
	}

	/**
	 * Emits a direction for this fixed step. Direct LOS keeps combat movement
	 * responsive; otherwise the intent follows a cached grid path.
	 */
	public void step(
			float dt,
			float selfX,
			float selfY,
			float targetX,
			float targetY,
			boolean directLineOfSight,
			float directDesiredX,
			float directDesiredY,
			CollisionMap map,
			Intent out) {
		requireFiniteNonNegative(dt, "dt");
		requireFinite(selfX, "selfX");
		requireFinite(selfY, "selfY");
		requireFinite(targetX, "targetX");
		requireFinite(targetY, "targetY");
		requireFinite(directDesiredX, "directDesiredX");
		requireFinite(directDesiredY, "directDesiredY");
		if (map == null || map.width() != width || map.height() != height) {
			throw new IllegalArgumentException(
					"map dimensions must match navigator");
		}
		if (out == null) {
			throw new IllegalArgumentException("out is required");
		}

		repathRemaining -= dt;
		float desiredLengthSquared = directDesiredX * directDesiredX
				+ directDesiredY * directDesiredY;
		commandedMovement = desiredLengthSquared > 0.000001f;
		if (!commandedMovement || directLineOfSight) {
			out.set(
					directDesiredX,
					directDesiredY,
					false,
					false);
			return;
		}

		int startCell = cellFor(selfX, selfY);
		int targetCell = cellFor(targetX, targetY);
		boolean targetChanged = targetCell != cachedTargetCell;
		if (!initialized && repathRemaining > 0f && !forceRepath) {
			// Initial staggering avoids 30 enemies searching on one frame.
			out.set(
					directDesiredX,
					directDesiredY,
					false,
					false);
			return;
		}

		advancePastReachedWaypoints(startCell, selfX, selfY);
		boolean pathInvalid = pathIndex < pathLength
				&& map.blocked(
						path[pathIndex] % width,
						path[pathIndex] / width);
		boolean exhaustedAwayFromTarget =
				pathIndex >= pathLength && startCell != targetCell;
		boolean retryUnreachable =
				targetUnreachable && repathRemaining <= 0f;
		if (forceRepath
				|| targetChanged
				|| pathInvalid
				|| retryUnreachable
				|| (!targetUnreachable && exhaustedAwayFromTarget)) {
			rebuildPath(startCell, targetCell, map);
		}

		if (pathIndex >= pathLength) {
			if (startCell == targetCell) {
				out.set(
						directDesiredX,
						directDesiredY,
						false,
						false);
			} else {
				// Do not repeatedly push into a known wall while waiting to retry.
				commandedMovement = false;
				out.set(0f, 0f, false, targetUnreachable);
			}
			return;
		}

		int nextCell = path[pathIndex];
		float deltaX = nextCell % width + 0.5f - selfX;
		float deltaY = nextCell / width + 0.5f - selfY;
		float distanceSquared = deltaX * deltaX + deltaY * deltaY;
		if (distanceSquared <= 0.000001f) {
			commandedMovement = false;
			out.set(0f, 0f, true, targetUnreachable);
			return;
		}
		float inverseDistance = 1f / (float)Math.sqrt(distanceSquared);
		out.set(
				deltaX * inverseDistance,
				deltaY * inverseDistance,
				true,
				targetUnreachable);
	}

	/**
	 * Feeds actual post-collision movement back into the cache. A blocked
	 * mover invalidates its path after a short grace period.
	 */
	public void observePosition(float dt, float x, float y) {
		requireFiniteNonNegative(dt, "dt");
		requireFinite(x, "x");
		requireFinite(y, "y");
		if (!hasObservedPosition) {
			observedX = x;
			observedY = y;
			hasObservedPosition = true;
			return;
		}
		float deltaX = x - observedX;
		float deltaY = y - observedY;
		float movedSquared = deltaX * deltaX + deltaY * deltaY;
		observedX = x;
		observedY = y;
		if (commandedMovement && movedSquared < 0.000004f) {
			stuckSeconds += dt;
			if (stuckSeconds >= STUCK_REPATH_SECONDS) {
				forceRepath = true;
				stuckSeconds = 0f;
			}
		} else {
			stuckSeconds = 0f;
		}
	}

	public int lastExpandedNodes() {
		return lastExpandedNodes;
	}

	public int totalExpandedNodes() {
		return totalExpandedNodes;
	}

	public int totalSearches() {
		return totalSearches;
	}

	private void rebuildPath(
			int startCell,
			int targetCell,
			CollisionMap map) {
		beginVisit();
		int head = 0;
		int tail = 0;
		queue[tail++] = startCell;
		visited[startCell] = visitStamp;
		parents[startCell] = -1;
		int bestCell = startCell;
		int bestDistance = manhattan(startCell, targetCell);
		boolean found = startCell == targetCell;
		int expanded = 0;
		int directionOffset = floorMod(stableKey + searchSequence, 4);

		while (head < tail && expanded < nodeBudget && !found) {
			int cell = queue[head++];
			expanded++;
			int x = cell % width;
			int y = cell / width;
			for (int i = 0; i < 4; i++) {
				int direction = (directionOffset + i) & 3;
				int nextX = x + DIRECTION_X[direction];
				int nextY = y + DIRECTION_Y[direction];
				if (nextX < 0 || nextY < 0
						|| nextX >= width || nextY >= height
						|| map.blocked(nextX, nextY)) {
					continue;
				}
				int next = nextX + nextY * width;
				if (visited[next] == visitStamp) {
					continue;
				}
				visited[next] = visitStamp;
				parents[next] = cell;
				queue[tail++] = next;
				int distance = manhattan(next, targetCell);
				if (distance < bestDistance
						|| (distance == bestDistance && next < bestCell)) {
					bestDistance = distance;
					bestCell = next;
				}
				if (next == targetCell) {
					bestCell = next;
					found = true;
					break;
				}
			}
		}

		pathLength = 0;
		pathIndex = 0;
		int cursor = bestCell;
		while (cursor != startCell
				&& cursor >= 0
				&& pathLength < path.length) {
			path[pathLength++] = cursor;
			cursor = parents[cursor];
		}
		for (int left = 0, right = pathLength - 1;
				left < right;
				left++, right--) {
			int value = path[left];
			path[left] = path[right];
			path[right] = value;
		}

		targetUnreachable = !found;
		cachedTargetCell = targetCell;
		lastExpandedNodes = expanded;
		totalExpandedNodes += expanded;
		totalSearches++;
		searchSequence++;
		initialized = true;
		forceRepath = false;
		stuckSeconds = 0f;
		repathRemaining = targetUnreachable
				? UNREACHABLE_RETRY_SECONDS
				: REPATH_INTERVAL_SECONDS;
	}

	private void advancePastReachedWaypoints(
			int startCell,
			float selfX,
			float selfY) {
		while (pathIndex < pathLength) {
			int waypoint = path[pathIndex];
			if (waypoint == startCell) {
				float centerX = waypoint % width + 0.5f;
				float centerY = waypoint / width + 0.5f;
				float deltaX = centerX - selfX;
				float deltaY = centerY - selfY;
				if (deltaX * deltaX + deltaY * deltaY > 0.10f) {
					return;
				}
				pathIndex++;
			} else {
				return;
			}
		}
	}

	private void beginVisit() {
		visitStamp++;
		if (visitStamp == 0) {
			Arrays.fill(visited, 0);
			visitStamp = 1;
		}
	}

	private int cellFor(float x, float y) {
		int cellX = Math.max(0, Math.min(width - 1, (int)Math.floor(x)));
		int cellY = Math.max(0, Math.min(height - 1, (int)Math.floor(y)));
		return cellX + cellY * width;
	}

	private int manhattan(int first, int second) {
		return Math.abs(first % width - second % width)
				+ Math.abs(first / width - second / width);
	}

	private static int floorMod(int value, int divisor) {
		int result = value % divisor;
		return result < 0 ? result + divisor : result;
	}

	private static void requireFiniteNonNegative(
			float value,
			String label) {
		requireFinite(value, label);
		if (value < 0f) {
			throw new IllegalArgumentException(
					label + " must not be negative");
		}
	}

	private static void requireFinite(float value, String label) {
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			throw new IllegalArgumentException(label + " must be finite");
		}
	}
}
