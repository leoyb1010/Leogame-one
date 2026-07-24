package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovOperator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameLog;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

import java.io.IOException;

/**
 * Branded, Bukov-only deployment boundary.
 *
 * This replaces the classic hero-selection and sewer interlevel chain. The
 * worker performs the same durable host save creation/restoration primitives,
 * then the render thread enters GameScene directly.
 */
public final class BukovDeploymentScene extends PixelScene {

	private volatile boolean loaded;
	private volatile Throwable failure;
	private boolean transitioned;
	private RenderedTextBlock status;
	private float dotTimer;
	private int dots;

	{
		inGameScene = true;
	}

	@Override
	public void create() {
		super.create();

		ColorBlock background =
				new ColorBlock(Camera.main.width, Camera.main.height, 0xFF07100E);
		add(background);

		Image artwork = new Image(Assets.Splashes.Bukov.FIRST_RAID);
		// This is continuous-tone loading artwork, not a gameplay pixel atlas.
		// Linear sampling keeps its downscaled Retina presentation clean.
		artwork.texture.filter(SmartTexture.LINEAR, SmartTexture.LINEAR);
		float cover = Math.max(
				Camera.main.width / artwork.width(),
				Camera.main.height / artwork.height());
		artwork.scale.set(cover);
		artwork.x = (Camera.main.width - artwork.width()) / 2f;
		artwork.y = (Camera.main.height - artwork.height()) / 2f;
		add(artwork);

		ColorBlock readability =
				new ColorBlock(Camera.main.width, Camera.main.height, 0xFF03100D);
		readability.alpha(0.28f);
		add(readability);

		RectF insets = getCommonInsets();
		float usableTop = insets.top;
		float usableHeight = Camera.main.height - insets.top - insets.bottom;

		RenderedTextBlock title = renderTextBlock("逃离布科夫", 15);
		title.hardlight(0xFFD6AF58);
		title.setPos(
				(Camera.main.width - title.width()) / 2f,
				usableTop + usableHeight * 0.42f);
		align(title);
		add(title);

		status = renderTextBlock("行动部署中", 8);
		status.hardlight(0xFF47C99A);
		centerStatus(title.bottom() + 10f);
		add(status);

		RenderedTextBlock detail =
				renderTextBlock("正在校验配装、行动员与地图状态", 6);
		detail.hardlight(0xFF9CB4AC);
		detail.setPos(
				(Camera.main.width - detail.width()) / 2f,
				status.bottom() + 6f);
		align(detail);
		add(detail);

		Thread loader = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadOrCreateRaid();
				} catch (Throwable error) {
					failure = error;
				} finally {
					loaded = true;
				}
			}
		}, "Bukov deployment");
		loader.start();
	}

	private void loadOrCreateRaid() throws IOException {
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		GamesInProgress.selectedClass = BukovOperator.HOST_CLASS;
		GamesInProgress.randomizedClass = false;
		Dungeon.daily = Dungeon.dailyReplay = false;

		Actor.fixTime();
		Mob.clearHeldAllies();
		GameLog.wipe();
		ActionIndicator.clearAction();

		BukovSaveService saves = BukovSaveServices.platformDefault();
		BukovRaidCoordinator checkpoint =
				BukovRaidCoordinator.resume(saves);
		BukovProfile deploymentProfile = saves.loadProfile();
		BukovMode.prepareUnlockedMaps(deploymentProfile.unlockedMaps());
		BukovMode.prepareSelectedMap(deploymentProfile.selectedMap());
		BukovRaidMode deploymentMode = checkpoint == null
				? deploymentProfile.selectedRaidMode()
				: checkpoint.session().raidMode();
		BukovMode.prepareRaidMode(deploymentMode);
		if (GamesInProgress.gameExists(BukovMode.SAVE_SLOT)) {
			try {
				Dungeon.loadGame(BukovMode.SAVE_SLOT);
				if (checkpoint != null
						&& checkpoint.session().seed != Dungeon.seed) {
					throw new IOException(
							"行动检查点与宿主存档种子不一致");
				}
				Dungeon.daily = Dungeon.dailyReplay = false;
				BukovOperator.normalize(Dungeon.hero);
				Level level = Dungeon.loadLevel(BukovMode.SAVE_SLOT);
				requireBukovLevel(level);
				Dungeon.switchLevel(level, Dungeon.hero.pos);
				return;
			} catch (IOException | RuntimeException incompatible) {
				recoverIncompatibleRaid(incompatible, saves, checkpoint);
			}
		} else if (checkpoint != null) {
			// A checkpoint cannot recreate the exact host heaps/actors by
			// itself. Settle it once as an interrupted raid before creating a
			// replacement host slot, instead of entering a seed-mismatch loop.
			recoverIncompatibleRaid(
					new IOException("行动检查点缺少宿主存档"),
					saves,
					checkpoint);
		}
		createNewRaid();
	}

	private static void createNewRaid() throws IOException {
		Dungeon.hero = null;
		BukovOperator.prepareNewRaid();
		Dungeon.initSeed();
		Dungeon.init();
		Level level = Dungeon.newLevel();
		requireBukovLevel(level);
		Dungeon.switchLevel(level, -1);
	}

	/**
	 * Ends only the incompatible active raid and host slot. The long-lived
	 * profile/stash is preserved; a valid checkpoint is settled as a failed
	 * action so deployed-item loss and statistics remain transactional.
	 */
	private static void recoverIncompatibleRaid(
			Throwable cause,
			BukovSaveService saves,
			BukovRaidCoordinator interrupted)
			throws IOException {
		ShatteredPixelDungeon.reportException(cause);
		if (interrupted != null) {
			interrupted.settleDeath();
		} else {
			saves.deleteRaid();
		}
		Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);
	}

	private static void requireBukovLevel(Level level) throws IOException {
		if (!(level instanceof BukovLevel)) {
			throw new IOException(
					"行动存档不是布科夫地图，请返回藏身处重新部署");
		}
	}

	@Override
	public void update() {
		super.update();

		if (!loaded) {
			dotTimer += Game.elapsed;
			if (dotTimer >= 0.35f) {
				dotTimer = 0f;
				dots = (dots + 1) % 4;
				StringBuilder label = new StringBuilder("行动部署中");
				for (int i = 0; i < dots; i++) {
					label.append('.');
				}
				status.text(label.toString());
				centerStatus(status.top());
			}
			return;
		}

		if (transitioned) {
			return;
		}
		transitioned = true;

		if (failure == null) {
			Game.switchScene(GameScene.class);
		} else {
			ShatteredPixelDungeon.reportException(failure);
			String message = failure.getMessage();
			if (message == null || message.length() == 0) {
				message = failure.getClass().getSimpleName();
			}
			final String detail = message;
			addToFront(new WndMessage("行动部署失败：\n" + detail) {
				@Override
				public void onBackPressed() {
					super.onBackPressed();
					ShatteredPixelDungeon.switchScene(BukovHubScene.class);
				}
			});
		}
	}

	private void centerStatus(float y) {
		status.setPos((Camera.main.width - status.width()) / 2f, y);
		align(status);
	}
}
