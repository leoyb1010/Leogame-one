package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BukovBalanceRuntimeWiringTest {

	@Test
	public void routeRequiresUniqueOrderedTraversalIntoItsExtractionRoom() {
		BukovRaidLayout layout = new BukovRaidLayout();
		layout.routes.add(new BukovRaidLayout.Route(
				"safe_long",
				BukovRaidLayout.RouteRisk.SAFE,
				Arrays.asList("spawn", "shared", "safe-only", "extract")));
		layout.routes.add(new BukovRaidLayout.Route(
				"balanced_mid",
				BukovRaidLayout.RouteRisk.BALANCED,
				Arrays.asList(
						"spawn", "shared", "balanced-only", "extract")));
		layout.routes.add(new BukovRaidLayout.Route(
				"high_risk_short",
				BukovRaidLayout.RouteRisk.HIGH_RISK,
				Arrays.asList("spawn", "high-only", "extract")));

		assertEquals(
				"",
				BukovRealtimeWorld.resolvedRouteId(
						layout,
						Arrays.asList("spawn", "shared")));
		assertEquals(
				"balanced_mid",
				BukovRealtimeWorld.resolvedRouteId(
						layout,
						Arrays.asList(
								"spawn",
								"shared",
								"detour",
								"balanced-only",
								"extract")));
		assertEquals(
				"",
				BukovRealtimeWorld.resolvedRouteId(
						layout,
						Arrays.asList(
								"spawn",
								"shared",
								"balanced-only")));
		assertEquals(
				"",
				BukovRealtimeWorld.resolvedRouteId(
						layout,
						Arrays.asList(
								"spawn",
								"shared",
								"safe-only",
								"balanced-only",
								"extract")));
	}
}
