package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Process-level durability gate. Every child is killed with Runtime.halt
 * against an isolated directory, then production persistence is recreated.
 */
public class BukovProcessCrashRecoveryTest {

	private static final long OLD_CURRENCY = 100L;
	private static final long NEW_CURRENCY = 200L;
	private static final String RAID_ID = "process-crash-settlement";
	private static final String ITEM_UID = "process-crash-loot-uid";

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void processCrashMatrixRecoversWholeStatesAndOneSettlement()
			throws Exception {
		File evidenceRoot = evidenceRoot();
		StringBuilder cases = new StringBuilder();
		runProfileCrashCase(
				evidenceRoot,
				HaltingBukovSaveStorage.CrashPoint.TEMPORARY_WRITE,
				OLD_CURRENCY,
				cases);
		runProfileCrashCase(
				evidenceRoot,
				HaltingBukovSaveStorage.CrashPoint.BACKUP_PROMOTION,
				OLD_CURRENCY,
				cases);
		runProfileCrashCase(
				evidenceRoot,
				HaltingBukovSaveStorage.CrashPoint.PRIMARY_REPLACE,
				NEW_CURRENCY,
				cases);
		runSettlementDeleteCrashCase(evidenceRoot, cases);
		write(
				new File(evidenceRoot, "summary.json"),
				"{\n"
						+ "  \"gate\": \"bukov_process_crash_recovery\",\n"
						+ "  \"status\": \"passed\",\n"
						+ "  \"processesHalted\": 4,\n"
						+ "  \"validRecoveryStates\": true,\n"
						+ "  \"duplicateSettlement\": false,\n"
						+ "  \"duplicateUid\": false,\n"
						+ "  \"cases\": [\n"
						+ cases
						+ "\n  ]\n"
						+ "}\n");
		write(
				new File(evidenceRoot, "summary.txt"),
				"gate=bukov_process_crash_recovery\n"
						+ "status=passed\n"
						+ "processes_halted=4\n"
						+ "temporary_write=old_valid_state\n"
						+ "backup_promotion=old_valid_state\n"
						+ "primary_replace=new_complete_state\n"
						+ "raid_delete=replayed_once_then_deleted\n"
						+ "duplicate_settlement=false\n"
						+ "duplicate_uid=false\n");
	}

	private void runProfileCrashCase(
			File evidenceRoot,
			HaltingBukovSaveStorage.CrashPoint crashPoint,
			long expectedCurrency,
			StringBuilder cases) throws Exception {
		File directory = isolatedCaseDirectory(
				evidenceRoot,
				crashPoint.name().toLowerCase());
		BukovSaveService setup = productionService(directory);
		BukovProfile old = new BukovProfile();
		old.setCurrency(OLD_CURRENCY);
		setup.saveProfile(old);
		// A second old commit guarantees a valid primary and backup before
		// every injected update.
		setup.saveProfile(old);

		int exitCode = runChild(
				directory,
				"profile-save",
				crashPoint);
		assertEquals(
				HaltingBukovSaveStorage.HALT_EXIT_CODE,
				exitCode);
		assertCrashMarker(directory, crashPoint);

		BukovProfile recovered = productionService(directory).loadProfile();
		assertTrue(recovered.currency() == OLD_CURRENCY
				|| recovered.currency() == NEW_CURRENCY);
		assertEquals(expectedCurrency, recovered.currency());
		appendCase(
				cases,
				crashPoint.name(),
				recovered.currency(),
				false,
				false);
	}

	private void runSettlementDeleteCrashCase(
			File evidenceRoot,
			StringBuilder cases) throws Exception {
		File directory = isolatedCaseDirectory(
				evidenceRoot,
				"settlement_raid_delete");
		BukovSaveService setup = productionService(directory);
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(OLD_CURRENCY);
		setup.saveProfile(profile);
		setup.saveProfile(profile);

		RaidSession session = RaidSession.create(0x42554B4F56L, RAID_ID);
		LootTransaction loot = new LootTransaction(RAID_ID, 20f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(new RaidItem(
						ITEM_UID,
						"loot:process_crash",
						1,
						0.25f,
						250,
						true,
						false,
						1f)));
		BukovRaidCheckpoint checkpoint = new BukovRaidCheckpoint(
				session,
				loot,
				Collections.emptyList());
		setup.saveRaidCheckpoint(checkpoint);
		setup.saveRaidCheckpoint(checkpoint);

		int exitCode = runChild(
				directory,
				"settlement-delete",
				HaltingBukovSaveStorage.CrashPoint.RAID_DELETE);
		assertEquals(
				HaltingBukovSaveStorage.HALT_EXIT_CODE,
				exitCode);
		assertCrashMarker(
				directory,
				HaltingBukovSaveStorage.CrashPoint.RAID_DELETE);

		BukovSaveService restarted = productionService(directory);
		BukovProfile committed = restarted.loadProfile();
		BukovRaidCheckpoint recoveredRaid =
				restarted.loadRaidCheckpoint();
		assertNotNull(recoveredRaid);
		assertTrue(committed.isSettled(RAID_ID));
		assertEquals(1, committed.settlements().size());
		assertEquals(1, committed.stash().distinctItemCount());
		assertEquals(1L, committed.stash().totalQuantity());
		assertNotNull(committed.stash().item(ITEM_UID));

		RaidResult replay = new RaidSettlement().settle(
				committed,
				recoveredRaid.loot(),
				RaidOutcome.SUCCESS);
		assertTrue(replay.replayed());
		assertEquals(1, committed.settlements().size());
		assertEquals(1L, committed.stash().totalQuantity());
		restarted.saveProfile(committed);
		restarted.deleteRaid();

		BukovSaveService finalRestart = productionService(directory);
		BukovProfile finalProfile = finalRestart.loadProfile();
		assertNull(finalRestart.loadRaidCheckpoint());
		assertEquals(1, finalProfile.settlements().size());
		assertEquals(1, finalProfile.stash().distinctItemCount());
		assertEquals(1L, finalProfile.stash().totalQuantity());
		assertEquals(ITEM_UID,
				finalProfile.stash().item(ITEM_UID).itemUid());
		appendCase(
				cases,
				"RAID_DELETE",
				finalProfile.currency(),
				false,
				false);
	}

	private File evidenceRoot() throws Exception {
		String configured =
				System.getProperty("bukov.crash.evidence.dir");
		if (configured == null || configured.trim().isEmpty()) {
			configured = System.getenv(
					"BUKOV_CRASH_EVIDENCE_DIR");
		}
		if (configured == null || configured.trim().isEmpty()) {
			return temporaryFolder.newFolder("bukov-process-crash");
		}
		File root = new File(configured).getCanonicalFile();
		if (!root.mkdirs() && !root.isDirectory()) {
			throw new IllegalStateException(
					"unable to create crash evidence directory");
		}
		rejectRealSaveDirectory(root);
		return root;
	}

	private static void rejectRealSaveDirectory(File root) {
		for (String saveFile : new String[]{
				AtomicBukovSaveService.PROFILE_FILE,
				AtomicBukovSaveService.PROFILE_FILE + ".bak",
				AtomicBukovSaveService.RAID_FILE,
				AtomicBukovSaveService.RAID_FILE + ".bak"}) {
			if (new File(root, saveFile).exists()) {
				throw new IllegalArgumentException(
						"crash evidence directory contains a Bukov save: "
								+ root);
			}
		}
	}

	private static File isolatedCaseDirectory(
			File evidenceRoot, String label) throws Exception {
		File directory = Files.createTempDirectory(
				evidenceRoot.toPath(),
				label + "-").toFile().getCanonicalFile();
		write(
				new File(
						directory,
						HaltingBukovSaveStorage.TEST_ROOT_MARKER),
				"isolated Bukov crash test directory\n");
		return directory;
	}

	private static BukovSaveService productionService(File directory) {
		return new AtomicBukovSaveService(
				new FileBukovSaveStorage(directory));
	}

	private static int runChild(
			File directory,
			String action,
			HaltingBukovSaveStorage.CrashPoint crashPoint)
			throws Exception {
		File output = new File(directory, "child-process.log");
		Process process = new ProcessBuilder(
				new File(
						System.getProperty("java.home"),
						"bin/java").getAbsolutePath(),
				"-Dbukov.crash.child=true",
				"-cp",
				childClasspath(),
				BukovSaveCrashProcessHarness.class.getName(),
				action,
				crashPoint.name(),
				directory.getAbsolutePath())
				.redirectErrorStream(true)
				.redirectOutput(output)
				.start();
		try {
			assertTrue(
					"fault-injection child timed out",
					process.waitFor(20L, TimeUnit.SECONDS));
			return process.exitValue();
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
				assertTrue(
						"timed-out child could not be reaped",
						process.waitFor(5L, TimeUnit.SECONDS));
			}
		}
	}

	private static void assertCrashMarker(
			File directory,
			HaltingBukovSaveStorage.CrashPoint crashPoint)
			throws Exception {
		File marker = new File(
				directory,
				HaltingBukovSaveStorage.CRASH_MARKER);
		assertTrue("missing fault-injection marker", marker.isFile());
		String content = new String(
				Files.readAllBytes(marker.toPath()),
				StandardCharsets.UTF_8);
		assertTrue(
				"fault marker recorded the wrong crash point",
				content.contains(
						"\"crashPoint\": \"" + crashPoint.name() + "\""));
		assertTrue(
				"fault marker recorded the wrong halt code",
				content.contains(
						"\"exitCode\": "
								+ HaltingBukovSaveStorage.HALT_EXIT_CODE));
	}

	private static String childClasspath() throws Exception {
		Set<String> entries = new LinkedHashSet<>();
		String configured = System.getProperty("java.class.path", "");
		for (String entry : configured.split(
				java.util.regex.Pattern.quote(File.pathSeparator))) {
			if (!entry.isEmpty()) entries.add(entry);
		}
		for (ClassLoader loader =
				BukovProcessCrashRecoveryTest.class.getClassLoader();
				loader != null;
				loader = loader.getParent()) {
			if (!(loader instanceof URLClassLoader)) continue;
			for (URL url : ((URLClassLoader)loader).getURLs()) {
				if ("file".equals(url.getProtocol())) {
					entries.add(new File(new URI(url.toString()))
							.getAbsolutePath());
				}
			}
		}
		StringBuilder result = new StringBuilder();
		for (String entry : entries) {
			if (result.length() > 0) {
				result.append(File.pathSeparator);
			}
			result.append(entry);
		}
		return result.toString();
	}

	private static void appendCase(
			StringBuilder cases,
			String crashPoint,
			long recoveredCurrency,
			boolean duplicateSettlement,
			boolean duplicateUid) {
		if (cases.length() > 0) cases.append(",\n");
		cases.append("    {\"crashPoint\":\"")
				.append(crashPoint)
				.append("\",\"haltExitCode\":")
				.append(HaltingBukovSaveStorage.HALT_EXIT_CODE)
				.append(",\"recoveredCurrency\":")
				.append(recoveredCurrency)
				.append(",\"duplicateSettlement\":")
				.append(duplicateSettlement)
				.append(",\"duplicateUid\":")
				.append(duplicateUid)
				.append('}');
	}

	private static void write(File target, String content)
			throws Exception {
		try (FileOutputStream output =
				new FileOutputStream(target, false)) {
			output.write(content.getBytes(StandardCharsets.UTF_8));
			output.flush();
			output.getFD().sync();
		}
	}
}
