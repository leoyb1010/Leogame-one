package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmBuild;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovEconomyServiceTest {

	@Test
	public void buyDebitsOnceAndRetryIsIdempotent() throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(2_000L);
		saves.saveProfile(profile);
		BukovEconomyService economy = new BukovEconomyService(saves);

		BukovEconomyService.Receipt first =
				economy.buy("buy-001", "scout_pack_1");
		BukovEconomyService.Receipt retry =
				economy.buy("buy-001", "scout_pack_1");
		BukovProfile persisted = saves.loadProfile();

		assertFalse(first.alreadyCommitted);
		assertTrue(retry.alreadyCommitted);
		assertEquals(-1_600L, first.currencyDelta);
		assertEquals(400L, retry.balanceAfter);
		assertEquals(1, persisted.stash().distinctItemCount());
		assertNotNull(persisted.stash().item("vendor:buy-001"));
	}

	@Test
	public void transactionUidCannotBeReusedForAnotherOffer()
			throws IOException {
		InMemoryBukovSaveService saves = funded(10_000L);
		BukovEconomyService economy = new BukovEconomyService(saves);
		economy.buy("same-id", "bandage_1");

		try {
			economy.buy("same-id", "field_pack_1");
			fail("transaction UID reuse must fail");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("another command"));
		}
		assertEquals(9_780L, saves.loadProfile().currency());
		assertEquals(1, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void buyReceiptRemainsIdempotentAfterPurchasedItemLeavesStash()
			throws IOException {
		InMemoryBukovSaveService saves = funded(10_000L);
		BukovEconomyService economy = new BukovEconomyService(saves);
		BukovEconomyService.Receipt first =
				economy.buy("durable-buy", "bandage_1");
		BukovProfile moved = saves.loadProfile();
		assertNotNull(moved.stash().withdraw(first.itemUid));
		saves.saveProfile(moved);

		BukovEconomyService.Receipt retry =
				economy.buy("durable-buy", "bandage_1");

		assertTrue(retry.alreadyCommitted);
		assertEquals(first.itemUid, retry.itemUid);
		assertEquals(first.currencyDelta, retry.currencyDelta);
		assertEquals(first.balanceAfter, retry.balanceAfter);
		assertEquals(9_780L, saves.loadProfile().currency());
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void saleRemovesExactUidAndCreditsConditionedValue()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(100L);
		profile.stash().deposit(new RaidItem(
				"loot-1",
				"gold_watch",
				1,
				0.12f,
				1_800,
				true,
				false,
				0.5f));
		saves.saveProfile(profile);

		BukovEconomyService.Receipt receipt =
				new BukovEconomyService(saves).sell("sell-001", "loot-1");
		BukovProfile persisted = saves.loadProfile();

		assertEquals(668L, receipt.currencyDelta);
		assertEquals(768L, persisted.currency());
		assertFalse(persisted.stash().contains("loot-1"));
	}

	@Test
	public void saleRetryReturnsDurableCommittedReceiptWithoutDoubleCredit()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(100L);
		profile.stash().deposit(new RaidItem(
				"loot-retry",
				"gold_watch",
				1,
				0.12f,
				1_800,
				true,
				false,
				0.5f));
		saves.saveProfile(profile);
		BukovEconomyService economy = new BukovEconomyService(saves);

		BukovEconomyService.Receipt first =
				economy.sell("sell-retry", "loot-retry");
		BukovEconomyService.Receipt retry =
				economy.sell("sell-retry", "loot-retry");

		assertFalse(first.alreadyCommitted);
		assertTrue(retry.alreadyCommitted);
		assertEquals(first.itemUid, retry.itemUid);
		assertEquals(first.currencyDelta, retry.currencyDelta);
		assertEquals(first.balanceAfter, retry.balanceAfter);
		assertEquals(768L, saves.loadProfile().currency());
		assertFalse(saves.loadProfile().stash().contains("loot-retry"));
	}

	@Test
	public void sellingPhysicalFirearmRemovesItsAttachmentBuild()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem firearm = new RaidItem(
				"sellable-firearm",
				"firearm:needle_9",
				1,
				2.1f,
				850,
				false,
				false,
				1f);
		profile.stash().deposit(firearm);
		FirearmBuild build = new FirearmBuild(firearm.itemUid());
		build.install(FirearmAttachmentCatalog.RED_DOT);
		profile.firearmBuilds().save(build);
		saves.saveProfile(profile);

		new BukovEconomyService(saves).sell(
				"sell-built-firearm", firearm.itemUid());

		BukovProfile persisted = saves.loadProfile();
		assertFalse(persisted.stash().contains(firearm.itemUid()));
		assertEquals(0, persisted.firearmBuilds().size());
	}

	@Test
	public void committedTransactionIdCannotBeReusedForAnotherCommand()
			throws IOException {
		InMemoryBukovSaveService saves = funded(5_000L);
		BukovEconomyService economy = new BukovEconomyService(saves);
		economy.buy("global-command", "bandage_1");

		try {
			economy.buy("global-command", "field_pack_1");
			fail("transaction ID reuse must fail");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("another command"));
		}
		assertEquals(4_780L, saves.loadProfile().currency());
	}

	@Test
	public void selectedAndProvisionedItemsCannotBeSold()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem selected = new RaidItem(
				"selected", "duct_tape", 1, 0.2f, 140,
				false, false, 1f);
		profile.stash().deposit(selected);
		profile.loadout().select(selected.itemUid(), profile.stash());
		profile.stash().deposit(new RaidItem(
				"provision:free", "firearm:needle_9", 1, 0.9f, 850,
				false, false, 1f));
		profile.stash().deposit(new RaidItem(
				"archived-evidence",
				FirstRaidMission.ARCHIVE_DEFINITION_ID,
				1,
				0.2f,
				900,
				false,
				false,
				1f));
		saves.saveProfile(profile);
		BukovEconomyService economy = new BukovEconomyService(saves);

		assertSellFails(economy, "selected");
		assertSellFails(economy, "provision:free");
		assertSellFails(economy, "archived-evidence");
		assertEquals(3, saves.loadProfile().stash().distinctItemCount());
		assertEquals(0L, saves.loadProfile().currency());
	}

	@Test
	public void activeRaidLocksAllTrading() throws IOException {
		InMemoryBukovSaveService saves = funded(5_000L);
		saves.saveRaid(RaidSession.create(7L, "active-raid"));

		try {
			new BukovEconomyService(saves)
					.buy("blocked", "bandage_1");
			fail("active raid must lock vendor");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("active raid"));
		}
		assertEquals(5_000L, saves.loadProfile().currency());
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void failedProfilePublishLeavesBalanceAndStashUntouched()
			throws IOException {
		InMemoryBukovSaveService backing = funded(5_000L);
		BukovSaveService failing = new FailProfileSaveService(backing);

		try {
			new BukovEconomyService(failing)
					.buy("will-fail", "field_pack_1");
			fail("save failure must escape");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("injected"));
		}
		assertEquals(5_000L, backing.loadProfile().currency());
		assertEquals(0, backing.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void failedSalePublishLeavesItemBalanceAndReceiptUncommitted()
			throws IOException {
		InMemoryBukovSaveService backing = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(40L);
		profile.stash().deposit(new RaidItem(
				"unsold",
				"gold_watch",
				1,
				0.12f,
				1_800,
				true,
				false,
				1f));
		backing.saveProfile(profile);
		BukovSaveService failing = new FailProfileSaveService(backing);

		try {
			new BukovEconomyService(failing)
					.sell("sale-will-fail", "unsold");
			fail("save failure must escape");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("injected"));
		}

		assertEquals(40L, backing.loadProfile().currency());
		assertTrue(backing.loadProfile().stash().contains("unsold"));
		BukovEconomyService.Receipt committed =
				new BukovEconomyService(backing)
						.sell("sale-will-fail", "unsold");
		assertFalse(committed.alreadyCommitted);
	}

	@Test
	public void appraisalRejectsNegativeAndOverflowProneInputsAtItemBoundary() {
		RaidItem pristine = new RaidItem(
				"loot", "encrypted_drive", 2, 0.2f, 2_400,
				true, false, 1f);
		RaidItem broken = pristine.withRuntimeState(2, 0f);

		assertEquals(2_640L, BukovEconomyService.appraisal(pristine));
		assertEquals(924L, BukovEconomyService.appraisal(broken));
	}

	private static InMemoryBukovSaveService funded(long currency)
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(currency);
		saves.saveProfile(profile);
		return saves;
	}

	private static void assertSellFails(
			BukovEconomyService economy,
			String itemUid) throws IOException {
		try {
			economy.sell("sell-" + itemUid.replace(':', '-'), itemUid);
			fail("sale should fail: " + itemUid);
		} catch (IllegalStateException expected) {
			// Expected: selected loadout or free recovery provisions.
		}
	}

	private static final class FailProfileSaveService
			implements BukovSaveService {
		private final BukovSaveService delegate;

		private FailProfileSaveService(BukovSaveService delegate) {
			this.delegate = delegate;
		}

		@Override
		public BukovProfile loadProfile() throws IOException {
			return delegate.loadProfile();
		}

		@Override
		public void saveProfile(BukovProfile profile) throws IOException {
			throw new IOException("injected profile save failure");
		}

		@Override
		public RaidSession loadRaid() throws IOException {
			return delegate.loadRaid();
		}

		@Override
		public void saveRaid(RaidSession raid) throws IOException {
			delegate.saveRaid(raid);
		}

		@Override
		public BukovRaidCheckpoint loadRaidCheckpoint() throws IOException {
			return delegate.loadRaidCheckpoint();
		}

		@Override
		public void saveRaidCheckpoint(BukovRaidCheckpoint checkpoint)
				throws IOException {
			delegate.saveRaidCheckpoint(checkpoint);
		}

		@Override
		public void deleteRaid() throws IOException {
			delegate.deleteRaid();
		}
	}
}
