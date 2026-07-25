package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reusable broad-phase index for realtime hitscan targets.
 *
 * Bodies live in the bucket containing their centre. Queries expand their
 * bucket range by the largest indexed radius, then reject bodies whose own
 * bounds do not intersect the requested AABB. Results retain rebuild order so
 * equal-distance hits resolve exactly as they did in the authoritative roster.
 */
public final class RealtimeBodySpatialIndex {

	private static final float DEFAULT_BUCKET_SIZE = 4f;

	private final float bucketSize;
	private final int columns;
	private final int rows;
	private final ArrayList<Entry>[] buckets;
	private final IdentityHashMap<RealtimeBody, Entry> entries =
			new IdentityHashMap<>();
	private final ArrayList<Entry> queryEntries = new ArrayList<>();
	private final QueryResult queryResult = new QueryResult();
	private int rebuildGeneration;
	private float maximumRadius;

	public RealtimeBodySpatialIndex(int width, int height) {
		this(width, height, DEFAULT_BUCKET_SIZE);
	}

	@SuppressWarnings("unchecked")
	RealtimeBodySpatialIndex(int width, int height, float bucketSize) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException(
					"width and height must be positive");
		}
		if (!(bucketSize > 0f)) {
			throw new IllegalArgumentException(
					"bucket size must be positive");
		}
		this.bucketSize = bucketSize;
		columns = Math.max(1, (int)Math.ceil(width / bucketSize));
		rows = Math.max(1, (int)Math.ceil(height / bucketSize));
		buckets = (ArrayList<Entry>[])new ArrayList<?>[columns * rows];
		for (int i = 0; i < buckets.length; i++) {
			buckets[i] = new ArrayList<>();
		}
	}

	/**
	 * Reconciles membership and authoritative iteration order without
	 * recreating entries for bodies that remain in the roster.
	 */
	public void rebuild(List<RealtimeBody> orderedBodies) {
		if (orderedBodies == null) {
			throw new IllegalArgumentException("ordered bodies are required");
		}
		rebuildGeneration++;
		if (rebuildGeneration == 0) {
			for (Entry entry : entries.values()) {
				entry.generation = 0;
			}
			rebuildGeneration = 1;
		}
		for (ArrayList<Entry> bucket : buckets) {
			bucket.clear();
		}
		maximumRadius = 0f;
		for (int i = 0; i < orderedBodies.size(); i++) {
			RealtimeBody body = orderedBodies.get(i);
			if (body == null || !body.active) continue;
			Entry entry = entries.get(body);
			if (entry == null) {
				entry = new Entry(body);
				entries.put(body, entry);
			}
			entry.order = i;
			entry.generation = rebuildGeneration;
			entry.bucket = bucketIndex(body.x, body.y);
			buckets[entry.bucket].add(entry);
			maximumRadius = Math.max(maximumRadius, body.radius);
		}
		Iterator<Map.Entry<RealtimeBody, Entry>> iterator =
				entries.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().generation != rebuildGeneration) {
				iterator.remove();
			}
		}
	}

	/**
	 * Moves an existing body between buckets after the simulation moves it.
	 */
	public void update(RealtimeBody body) {
		Entry entry = entries.get(body);
		if (entry == null) return;
		if (!body.active) {
			remove(body);
			return;
		}
		maximumRadius = Math.max(maximumRadius, body.radius);
		int nextBucket = bucketIndex(body.x, body.y);
		if (nextBucket == entry.bucket) return;
		buckets[entry.bucket].remove(entry);
		entry.bucket = nextBucket;
		insertByOrder(buckets[nextBucket], entry);
	}

	public void remove(RealtimeBody body) {
		Entry entry = entries.remove(body);
		if (entry == null) return;
		buckets[entry.bucket].remove(entry);
	}

	public Iterable<RealtimeBody> candidates(
			float minX,
			float minY,
			float maxX,
			float maxY) {
		queryEntries.clear();
		if (entries.isEmpty() || minX > maxX || minY > maxY) {
			return queryResult.reset();
		}

		int minColumn = column(minX - maximumRadius);
		int maxColumn = column(maxX + maximumRadius);
		int minRow = row(minY - maximumRadius);
		int maxRow = row(maxY + maximumRadius);
		for (int row = minRow; row <= maxRow; row++) {
			int offset = row * columns;
			for (int column = minColumn; column <= maxColumn; column++) {
				ArrayList<Entry> bucket = buckets[offset + column];
				for (int i = 0; i < bucket.size(); i++) {
					Entry entry = bucket.get(i);
					RealtimeBody body = entry.body;
					if (body.active
							&& body.x + body.radius >= minX
							&& body.x - body.radius <= maxX
							&& body.y + body.radius >= minY
							&& body.y - body.radius <= maxY) {
						queryEntries.add(entry);
					}
				}
			}
		}
		sortByOrder(queryEntries);
		return queryResult.reset();
	}

	public void clear() {
		for (ArrayList<Entry> bucket : buckets) {
			bucket.clear();
		}
		entries.clear();
		queryEntries.clear();
		maximumRadius = 0f;
	}

	int size() {
		return entries.size();
	}

	private int bucketIndex(float x, float y) {
		return row(y) * columns + column(x);
	}

	private int column(float x) {
		return clamp((int)Math.floor(x / bucketSize), columns);
	}

	private int row(float y) {
		return clamp((int)Math.floor(y / bucketSize), rows);
	}

	private static int clamp(int value, int count) {
		return Math.max(0, Math.min(count - 1, value));
	}

	private static void insertByOrder(
			ArrayList<Entry> destination, Entry entry) {
		int index = destination.size();
		destination.add(entry);
		while (index > 0
				&& destination.get(index - 1).order > entry.order) {
			destination.set(index, destination.get(index - 1));
			index--;
		}
		destination.set(index, entry);
	}

	private static void sortByOrder(ArrayList<Entry> entries) {
		int size = entries.size();
		for (int root = size / 2 - 1; root >= 0; root--) {
			siftDown(entries, root, size);
		}
		for (int end = size - 1; end > 0; end--) {
			swap(entries, 0, end);
			siftDown(entries, 0, end);
		}
	}

	private static void siftDown(
			ArrayList<Entry> entries, int root, int size) {
		while (true) {
			int child = root * 2 + 1;
			if (child >= size) return;
			if (child + 1 < size
					&& entries.get(child).order
							< entries.get(child + 1).order) {
				child++;
			}
			if (entries.get(root).order >= entries.get(child).order) {
				return;
			}
			swap(entries, root, child);
			root = child;
		}
	}

	private static void swap(
			ArrayList<Entry> entries, int first, int second) {
		Entry value = entries.get(first);
		entries.set(first, entries.get(second));
		entries.set(second, value);
	}

	private static final class Entry {
		private final RealtimeBody body;
		private int order;
		private int bucket;
		private int generation;

		private Entry(RealtimeBody body) {
			this.body = body;
		}
	}

	private final class QueryResult
			implements Iterable<RealtimeBody>, Iterator<RealtimeBody> {
		private int cursor;

		private QueryResult reset() {
			cursor = 0;
			return this;
		}

		@Override
		public Iterator<RealtimeBody> iterator() {
			cursor = 0;
			return this;
		}

		@Override
		public boolean hasNext() {
			return cursor < queryEntries.size();
		}

		@Override
		public RealtimeBody next() {
			return queryEntries.get(cursor++).body;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
