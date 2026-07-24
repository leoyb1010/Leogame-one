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
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovDeploymentHandoff;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHostRecoveryPolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiAssets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovVisualContract;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameLog;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.FileUtils;
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
		BukovUiTokens tokens = BukovUiTokens.loadDefault();

		ColorBlock background =
				new ColorBlock(
						Camera.main.width,
						Camera.main.height,
						tokens.colorWithAlpha("ink.loading", 255));
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
				new ColorBlock(
						Camera.main.width,
						Camera.main.height,
						tokens.colorWithAlpha("ink.shadow", 255));
		readability.alpha(0.28f);
		add(readability);

		RectF insets = getCommonInsets();
		float safeWidth = Camera.main.width
				- insets.left - insets.right;
		float safeHeight = Camera.main.height
				- insets.top - insets.bottom;
		float panelWidth = Math.min(
				280f,
				BukovVisualContract.contentWidth(
						safeWidth, landscape()));
		float panelHeight = 84f;
		float panelX = BukovVisualContract.centeredLeft(
				insets.left, safeWidth, panelWidth);
		float panelY = insets.top
				+ Math.max(BukovVisualContract.OUTER_MARGIN,
						(safeHeight - panelHeight) * 0.48f);

		NinePatch panel = BukovUiAssets.surface(
				BukovUiAssets.Surface.PANEL,
				tokens.colorWithAlpha("ink.background", 228));
		panel.x = panelX;
		panel.y = panelY;
		panel.size(panelWidth, panelHeight);
		add(panel);
		ColorBlock edge = new ColorBlock(
				2f, panelHeight, tokens.color("accent.interact"));
		edge.x = panelX;
		edge.y = panelY;
		add(edge);
		ColorBlock rule = new ColorBlock(
				panelWidth, 1f, tokens.color("panel.border"));
		rule.x = panelX;
		rule.y = panelY + 21f;
		add(rule);

		RenderedTextBlock eyebrow = renderTextBlock(
				"ACTION CHECK  /  行动检查",
				BukovVisualContract.FONT_CAPTION);
		eyebrow.hardlight(tokens.color("text.secondary"));
		eyebrow.setPos(
				panelX + BukovVisualContract.CARD_PADDING,
				panelY + 5f);
		add(eyebrow);

		RenderedTextBlock title = renderTextBlock(
				"逃离布科夫",
				BukovVisualContract.FONT_TITLE);
		title.hardlight(tokens.color("accent.valuable"));
		title.setPos(
				panelX + BukovVisualContract.CARD_PADDING,
				rule.y + 6f);
		align(title);
		add(title);

		status = renderTextBlock(
				"行动部署中",
				BukovVisualContract.FONT_SECTION);
		status.hardlight(tokens.color("accent.extract"));
		status.setPos(
				panelX + BukovVisualContract.CARD_PADDING,
				title.bottom() + 5f);
		add(status);

		RenderedTextBlock detail = renderTextBlock(
				"正在校验：配装 · 行动员 · 地图种子 · 检查点",
				BukovVisualContract.FONT_CAPTION);
		detail.hardlight(tokens.color("text.secondary"));
		detail.maxWidth(Math.max(
				1,
				(int)(panelWidth
						- BukovVisualContract.CARD_PADDING * 2f)));
		detail.setPos(
				panelX + BukovVisualContract.CARD_PADDING,
				status.bottom() + 5f);
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
		BukovDeploymentHandoff.clear();
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
		boolean hostExists =
				GamesInProgress.gameExists(BukovMode.SAVE_SLOT);
		BukovHostRecoveryPolicy.Action recovery =
				BukovHostRecoveryPolicy.decide(
						checkpoint != null,
						hostExists);
		switch (recovery) {
			case RESUME_MATCHED_HOST:
				resumeMatchedHost(checkpoint);
				return;
			case SETTLE_INTERRUPTED_CHECKPOINT:
				settleInterruptedCheckpoint(
						new IOException("行动检查点缺少宿主存档"),
						checkpoint);
				throw new IOException(
						"上次行动地图缺失，已按中断行动完成结算；"
								+ "请返回藏身处重新配装");
			case ARCHIVE_ORPHAN_HOST:
				archiveVerifiedOrphanHost();
				createNewRaid();
				return;
			case CREATE_NEW_HOST:
			default:
				createNewRaid();
		}
	}

	private static void createNewRaid() throws IOException {
		Dungeon.hero = null;
		BukovOperator.prepareNewRaid();
		Dungeon.initSeed();
		Dungeon.init();
		Level level = Dungeon.newLevel();
		requireBukovLevel(level);
		Dungeon.switchLevel(level, -1);
		BukovDeploymentHandoff.authorizeFreshHost(Dungeon.seed);
	}

	/**
	 * Resumes only after both documents exist and the loaded host is confirmed
	 * to be a Bukov map. A non-Bukov or unreadable slot is preserved so this
	 * recovery path can never delete an unrelated user save.
	 */
	private static void resumeMatchedHost(
			BukovRaidCoordinator checkpoint) throws IOException {
		Level level;
		try {
			level = loadVerifiedBukovHost();
		} catch (IOException | RuntimeException incompatible) {
			settleInterruptedCheckpoint(incompatible, checkpoint);
			throw preservedHostFailure(incompatible);
		}
		if (checkpoint.session().seed != Dungeon.seed) {
			IOException mismatch = new IOException(
					"行动检查点与宿主存档种子不一致");
			ShatteredPixelDungeon.reportException(mismatch);
			archiveOrphanBukovHost();
			checkpoint.settleDeath();
			throw new IOException(
					"行动地图与检查点不匹配，旧地图已安全归档且"
							+ "行动已结算；请返回藏身处重新配装");
		}
		Dungeon.switchLevel(level, Dungeon.hero.pos);
	}

	/**
	 * A checkpoint alone cannot reconstruct exact host actors and heaps.
	 * Settlement creates the durable raid receipt before control returns to the
	 * hideout, so retrying this recovery can never return the loadout twice.
	 */
	private static void settleInterruptedCheckpoint(
			Throwable cause,
			BukovRaidCoordinator interrupted) throws IOException {
		ShatteredPixelDungeon.reportException(cause);
		if (interrupted == null) {
			throw new IOException(
					"Missing coordinator for interrupted checkpoint",
					cause);
		}
		interrupted.settleDeath();
	}

	private static Level loadVerifiedBukovHost() throws IOException {
		Dungeon.loadGame(BukovMode.SAVE_SLOT);
		Dungeon.daily = Dungeon.dailyReplay = false;
		BukovOperator.normalize(Dungeon.hero);
		Level level = Dungeon.loadLevel(BukovMode.SAVE_SLOT);
		requireBukovLevel(level);
		return level;
	}

	private static void archiveVerifiedOrphanHost() throws IOException {
		try {
			loadVerifiedBukovHost();
		} catch (IOException | RuntimeException incompatible) {
			ShatteredPixelDungeon.reportException(incompatible);
			throw preservedHostFailure(incompatible);
		}
		archiveOrphanBukovHost();
	}

	/**
	 * Moves a confirmed Bukov host directory into a recoverable archive. It
	 * never calls Dungeon.deleteGame(), and it runs only after BukovLevel was
	 * successfully decoded from the reserved product slot.
	 */
	private static String archiveOrphanBukovHost() throws IOException {
		FileHandle source = FileUtils.getFileHandle(
				GamesInProgress.gameFolder(BukovMode.SAVE_SLOT));
		if (source == null || !source.exists() || !source.isDirectory()) {
			throw new IOException("待归档的布科夫宿主存档不存在");
		}
		FileHandle archiveRoot =
				FileUtils.getFileHandle("bukov_orphan_archives");
		try {
			archiveRoot.mkdirs();
			String baseName = "game"
					+ BukovMode.SAVE_SLOT
					+ "-seed"
					+ Dungeon.seed
					+ "-"
					+ System.currentTimeMillis();
			FileHandle target = archiveRoot.child(baseName);
			int collision = 0;
			while (target.exists()) {
				target = archiveRoot.child(
						baseName + "-" + (++collision));
			}
			source.moveTo(target);
			FileHandle archivedGame = target.child("game.dat");
			if (source.exists()
					|| !archivedGame.exists()
					|| archivedGame.length() <= 1L) {
				throw new IOException(
						"布科夫孤儿宿主存档归档校验失败");
			}
			GamesInProgress.delete(BukovMode.SAVE_SLOT);
			return target.path();
		} catch (RuntimeException failure) {
			throw new IOException(
					"无法安全归档布科夫孤儿宿主存档",
					failure);
		}
	}

	private static IOException preservedHostFailure(Throwable cause) {
		return new IOException(
				"宿主存档无法确认属于布科夫，已原样保留；未创建新行动",
				cause);
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
		// Keep the loading label aligned to the shared card grid while its
		// animated ellipsis changes width.
		status.setPos(status.left(), y);
		align(status);
	}
}
