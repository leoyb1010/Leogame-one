package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovTutorialContractTest {

	@Test
	public void everyHintIsShortAndNonModal() throws Exception {
		assertEquals(8, BukovTutorialEvent.values().length);
		for (String suffix : new String[] {"", "_zh"}) {
			Properties messages = raidMessages(suffix);
			for (BukovTutorialEvent event : BukovTutorialEvent.values()) {
				String message = messages.getProperty(event.messageKey());
				assertTrue(event.messageKey(), message != null);
				assertTrue(event.messageKey(), message.length() <= 38);
				assertTrue(
						event.messageKey(),
						message.split("\\n", -1).length <= 2);
			}
		}
		assertEquals(4f, BukovTutorialGuide.DISPLAY_SECONDS, 0f);
	}

	@Test
	public void reusableHintStateClearsWithoutAllocatingAReplacement() {
		BukovTutorialHintState state = new BukovTutorialHintState();
		state.event = BukovTutorialEvent.BLEEDING;
		state.message = BukovTutorialEvent.BLEEDING.message();
		state.remainingSeconds = 2f;
		assertTrue(state.visible());

		state.clear();

		assertFalse(state.visible());
		assertNull(state.event);
		assertNull(state.message);
		assertEquals(0f, state.remainingSeconds, 0f);
	}

	@Test
	public void profileLedgerRejectsDuplicateClaims() {
		BukovProfile profile = new BukovProfile();
		// Public read boundary is what presentation and tests consume.
		assertFalse(profile.tutorialSeen(BukovTutorialEvent.BOSS_WARNING));
		assertTrue(profile.seenTutorialEvents().isEmpty());
	}

	@Test
	public void guidePersistsOnceAndStopsAfterSecondRaid()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovRaidCoordinator first = start(saves, 1);
		BukovTutorialGuide firstGuide =
				new BukovTutorialGuide(first);
		assertEquals(
				BukovTutorialEvent.FIREARM_PICKUP,
				firstGuide.claim(BukovTutorialEvent.FIREARM_PICKUP));
		assertNull(firstGuide.claim(
				BukovTutorialEvent.FIREARM_PICKUP));
		first.settleDeath();

		BukovRaidCoordinator second = start(saves, 2);
		assertEquals(
				BukovTutorialEvent.BLEEDING,
				new BukovTutorialGuide(second).claim(
						BukovTutorialEvent.BLEEDING));
		second.settleDeath();

		BukovRaidCoordinator third = start(saves, 3);
		assertNull(new BukovTutorialGuide(third).claim(
				BukovTutorialEvent.BOSS_WARNING));
		assertEquals(3, third.session().raidOrdinal());
	}

	private static BukovRaidCoordinator start(
			InMemoryBukovSaveService saves,
			int ordinal) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				ordinal,
				"tutorial-" + ordinal,
				40f,
					Collections.singletonList(
							ExtractionState.basic()));
	}

	private static Properties raidMessages(String suffix) throws Exception {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(
				Paths.get("src/main/assets/messages/bukov_raid/"
						+ "bukov_raid" + suffix + ".properties"),
				StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}
}
