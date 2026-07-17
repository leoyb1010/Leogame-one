package com.shatteredpixel.shatteredpixeldungeon.ios;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IOSRuntimeEnvironmentTest {

	@Test
	public void recognizesAppleSimulatorVariables() {
		Map<String, String> environment = new HashMap<>();
		environment.put("SIMULATOR_DEVICE_NAME", "iPhone 17 Pro");
		assertTrue(IOSRuntimeEnvironment.isSimulator(environment));
	}

	@Test
	public void doesNotClassifyARealDeviceAsSimulator() {
		assertFalse(IOSRuntimeEnvironment.isSimulator(Collections.singletonMap("HOME", "/var/mobile")));
	}
}
