package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmBuild;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.EffectiveFirearmStats;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Profile-owned attachment builds keyed by physical firearm UID. */
public final class BukovFirearmBuilds implements Bundlable {

	private static final String BUILDS = "builds";
	private final Map<String, FirearmBuild> builds = new LinkedHashMap<>();

	public BukovFirearmBuilds() {
	}

	public FirearmBuild build(String firearmUid) {
		requireUid(firearmUid);
		FirearmBuild build = builds.get(firearmUid);
		return build == null ? null : build.copy();
	}

	public void save(FirearmBuild build) {
		if (build == null) throw new IllegalArgumentException("build is required");
		requireUid(build.firearmUid());
		builds.put(build.firearmUid(), build.copy());
	}

	public boolean remove(String firearmUid) {
		requireUid(firearmUid);
		return builds.remove(firearmUid) != null;
	}

	public EffectiveFirearmStats effectiveStats(
			String firearmUid, FirearmDefinition definition) {
		FirearmBuild build = builds.get(requireUid(firearmUid));
		return (build == null ? new FirearmBuild(firearmUid) : build)
				.effectiveStats(definition);
	}

	public int size() {
		return builds.size();
	}

	BukovFirearmBuilds copy() {
		BukovFirearmBuilds result = new BukovFirearmBuilds();
		for (FirearmBuild build : builds.values()) result.save(build);
		return result;
	}

	void replaceWith(BukovFirearmBuilds replacement) {
		builds.clear();
		for (FirearmBuild build : replacement.builds.values()) save(build);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(BUILDS, builds.values());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		builds.clear();
		Collection<Bundlable> stored = bundle.getCollection(BUILDS);
		for (Bundlable entry : stored) {
			if (!(entry instanceof FirearmBuild)) {
				throw new IllegalStateException(
						"Unexpected stored firearm build entry");
			}
			FirearmBuild build = (FirearmBuild) entry;
			if (builds.put(build.firearmUid(), build.copy()) != null) {
				throw new IllegalStateException(
						"Duplicate stored firearm build: " + build.firearmUid());
			}
		}
	}

	private static String requireUid(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("firearmUid is required");
		}
		return value;
	}
}
