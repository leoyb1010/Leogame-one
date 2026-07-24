package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialHintState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovRaidHudTutorialPresentationTest {

	@Test
	public void inactiveOrEmptyHintsStayHidden() {
		BukovTutorialHintState state = new BukovTutorialHintState();
		assertEquals(0f, BukovRaidHud.tutorialOpacity(state), 0f);

		state.event = BukovTutorialEvent.FIREARM_PICKUP;
		state.remainingSeconds = 2f;
		assertEquals(0f, BukovRaidHud.tutorialOpacity(state), 0f);
	}

	@Test
	public void activeHintStaysReadableThenFadesAtTheEnd() {
		BukovTutorialHintState state = new BukovTutorialHintState();
		state.event = BukovTutorialEvent.FIREARM_PICKUP;
		state.message = "Fire";
		state.remainingSeconds = 2f;
		assertEquals(1f, BukovRaidHud.tutorialOpacity(state), 0f);

		state.remainingSeconds = 0.25f;
		assertEquals(0.5f, BukovRaidHud.tutorialOpacity(state), 0f);

		state.remainingSeconds = 0f;
		assertEquals(0f, BukovRaidHud.tutorialOpacity(state), 0f);
	}
}
