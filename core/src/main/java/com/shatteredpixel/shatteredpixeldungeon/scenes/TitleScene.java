/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.LeoIdentityConfig;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Fireball;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.LeoStyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndLeoWelcome;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.ColorMath;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.RectF;

import java.util.Date;

public class TitleScene extends PixelScene {

	private RenderedTextBlock title;
	private RenderedTextBlock motto;
	private Image titleFrame;
	private Image menuPanel;
	private Fireball leftFB;
	private Fireball rightFB;

	private StyledButton btnPlay;
	private StyledButton btnRankings;
	private StyledButton btnJournal;
	private StyledButton btnChanges;
	private StyledButton btnSettings;
	private StyledButton btnAbout;

	private BitmapText version;
	private IconButton btnFade;
	private ExitButton btnExit;

	@Override
	public void create() {
		
		super.create();

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;
		
		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		Image BG = new Image(landscape()
				? Assets.Splashes.Title.LEO_LANDSCAPE
				: Assets.Splashes.Title.LEO_PORTRAIT);
		float bgScale = Math.max(w / BG.width(), h / BG.height());
		BG.scale.set(bgScale);
		BG.x = (w - BG.width()) / 2f;
		BG.y = (h - BG.height()) / 2f;
		add(BG);

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		titleFrame = new Image(Assets.Interfaces.LEO_TITLE_EMBLEM);
		float frameWidth = Math.min(w * 0.92f, landscape() ? 230 : 148);
		titleFrame.scale.set(frameWidth / titleFrame.width());
		titleFrame.x = insets.left + (w - titleFrame.width()) / 2f;
		titleFrame.y = insets.top + 2;
		add(titleFrame);

		title = renderTextBlock(LeoIdentityConfig.gameTitle(), landscape() ? 18 : 15);
		title.align(RenderedTextBlock.CENTER_ALIGN);
		title.hardlight(LeoIdentityConfig.ANTIQUE_GOLD);
		add( title );

		float topRegion = Math.max(titleFrame.height() + 8, h*0.31f);

		motto = renderTextBlock(LeoIdentityConfig.motto(), landscape() ? 7 : 6);
		motto.align(RenderedTextBlock.CENTER_ALIGN);
		motto.hardlight(LeoIdentityConfig.EMERALD);
		motto.maxWidth((int) (frameWidth * 0.72f));
		add(motto);

		// Center the title and motto as one visual group inside the emblem.
		float identityHeight = title.height() + 3 + motto.height();
		float identityTop = titleFrame.y + titleFrame.height() * 0.585f - identityHeight / 2f;
		title.setPos(insets.left + (w - title.width()) / 2f, identityTop);
		motto.setPos(insets.left + (w - motto.width()) / 2f, title.bottom() + 3);
		align(title);
		align(motto);

		leftFB = placeTorch(title.left() - 10, title.bottom() + 5);
		rightFB = placeTorch(title.right() + 10, title.bottom() + 5);

		menuPanel = new Image(Assets.Interfaces.LEO_MENU_PANEL);
		add(menuPanel);
		
		btnPlay = new LeoStyledButton(Messages.get(this,
				GamesInProgress.checkAll().isEmpty() ? "new_expedition" : "continue_expedition")){
			@Override
			protected void onClick() {
				if (GamesInProgress.checkAll().size() == 0){
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
				} else {
					ShatteredPixelDungeon.switchNoFade( StartScene.class );
				}
			}
			
			@Override
			protected boolean onLongClick() {
				//making it easier to start runs quickly while debugging
				if (DeviceCompat.isDebug()) {
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
					return true;
				}
				return super.onLongClick();
			}
		};
		btnPlay.icon(Icons.get(Icons.ENTER));
		add(btnPlay);

		btnRankings = new LeoStyledButton(Messages.get(this, "rankings")){
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade( RankingsScene.class );
			}
		};
		btnRankings.icon(Icons.get(Icons.RANKINGS));
		add(btnRankings);
		Dungeon.daily = Dungeon.dailyReplay = false;

		btnJournal = new LeoStyledButton(Messages.get(this, "journal")){
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade( JournalScene.class );
			}
		};
		btnJournal.icon(Icons.get(Icons.JOURNAL));
		add(btnJournal);

		btnChanges = new ChangesButton(Messages.get(this, "changes"));
		btnChanges.icon(Icons.get(Icons.CHANGES));
		add(btnChanges);

		btnSettings = new SettingsButton(Messages.get(this, "settings"));
		add(btnSettings);

		btnAbout = new LeoStyledButton(Messages.get(this, "about")){
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchScene( AboutScene.class );
			}
		};
		btnAbout.icon(Icons.get(Icons.SHPX));
		add(btnAbout);
		
		final int BTN_HEIGHT = 20;
		int GAP = Math.max(2, (int)(h - topRegion - 3*BTN_HEIGHT)/4);

		float buttonAreaWidth = landscape() ? PixelScene.MIN_WIDTH_L-6 : PixelScene.MIN_WIDTH_P-2;
		float btnAreaLeft = insets.left + (w - buttonAreaWidth) / 2f;
		if (landscape()) {
			btnPlay.setRect(btnAreaLeft, insets.top + topRegion+GAP, buttonAreaWidth, BTN_HEIGHT);
			align(btnPlay);
			btnRankings.setRect(btnPlay.left(), btnPlay.bottom()+GAP, (float)(Math.floor(buttonAreaWidth/3f)-1), BTN_HEIGHT);
			btnJournal.setRect(btnRankings.right()+2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			btnChanges.setRect(btnJournal.right()+2, btnJournal.top(), btnRankings.width(), BTN_HEIGHT);
			btnSettings.setRect(btnRankings.left(), btnRankings.bottom()+GAP, (buttonAreaWidth/2)-1, BTN_HEIGHT);
			btnAbout.setRect(btnSettings.right()+2, btnSettings.top(), btnSettings.width(), BTN_HEIGHT);
		} else {
			btnPlay.setRect(btnAreaLeft, insets.top + topRegion+GAP, buttonAreaWidth, BTN_HEIGHT);
			align(btnPlay);
			btnRankings.setRect(btnPlay.left(), btnPlay.bottom()+GAP, (btnPlay.width()/2)-1, BTN_HEIGHT);
			btnJournal.setRect(btnRankings.right()+2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			btnChanges.setRect(btnRankings.left(), btnRankings.bottom()+GAP, (float)(Math.floor(buttonAreaWidth/3f)-1), BTN_HEIGHT);
			btnSettings.setRect(btnChanges.right()+2, btnChanges.top(), btnChanges.width(), BTN_HEIGHT);
			btnAbout.setRect(btnSettings.right()+2, btnSettings.top(), btnChanges.width(), BTN_HEIGHT);
		}

		float panelLeft = btnPlay.left() - 6;
		float panelTop = btnPlay.top() - 6;
		float panelWidth = btnPlay.width() + 12;
		float panelHeight = btnAbout.bottom() - panelTop + 6;
		menuPanel.x = panelLeft;
		menuPanel.y = panelTop;
		menuPanel.scale.x = panelWidth / menuPanel.texture.width;
		menuPanel.scale.y = panelHeight / menuPanel.texture.height;

		version = new BitmapText( "v" + Game.version, pixelFont);
		version.measure();
		version.hardlight( 0x888888 );
		version.x = insets.left + w - version.width() - (DeviceCompat.isDesktop() ? 4 : 8);
		version.y = insets.top + h - version.height() - (DeviceCompat.isDesktop() ? 2 : 4);
		add( version );

		btnFade = new IconButton(Icons.CHEVRON.get()){
			@Override
			protected void onClick() {
				enable(false);
				parent.add(new Tweener(parent, 0.5f) {
					@Override
					protected void updateValues(float progress) {
						if (!btnFade.active) {
							uiAlpha = 1 - progress;
							updateFade();
						}
					}
				});
			}
		};
		btnFade.icon().originToCenter();
		btnFade.icon().angle = 180f;
		btnFade.setRect(btnAreaLeft + (buttonAreaWidth-16)/2, camera.main.height - 16 - insets.bottom, 16, 16);
		add(btnFade);

		PointerArea fadeResetter = new PointerArea(0, 0, Camera.main.width, Camera.main.height){
			@Override
			public boolean onSignal(PointerEvent event) {
				if (event != null && event.type == PointerEvent.Type.UP && !btnPlay.active){
					parent.add(new Tweener(parent, 0.5f) {
						@Override
						protected void updateValues(float progress) {
							uiAlpha = progress;
							updateFade();
							if (progress >= 1){
								btnFade.enable(true);
							}
						}
					});
				}
				return false;
			}
		};
		add(fadeResetter);

		if (DeviceCompat.isDesktop()) {
			btnExit = new ExitButton();
			btnExit.setPos( w - btnExit.width(), 0 );
			add( btnExit );
		}

		Badges.loadGlobal();
		if (!SPDSettings.leoIdentityGranted()) {
			add(new WndLeoWelcome());
		} else if (Badges.isUnlocked(Badges.Badge.VICTORY) && !SPDSettings.victoryNagged()) {
			SPDSettings.victoryNagged(true);
			add(new WndVictoryCongrats());
		}

		fadeIn();
	}

	private float uiAlpha;

	public void updateFade() {
		float alpha = GameMath.gate(0f, uiAlpha, 1f);

		title.alpha(alpha);
		motto.alpha(alpha);
		titleFrame.alpha(alpha);
		menuPanel.alpha(alpha);
		leftFB.am = alpha;
		rightFB.am = alpha;

		btnPlay.enable(alpha != 0);
		btnRankings.enable(alpha != 0);
		btnJournal.enable(alpha != 0);
		btnChanges.enable(alpha != 0);
		btnSettings.enable(alpha != 0);
		btnAbout.enable(alpha != 0);

		btnPlay.alpha(alpha);
		btnRankings.alpha(alpha);
		btnJournal.alpha(alpha);
		btnChanges.alpha(alpha);
		btnSettings.alpha(alpha);
		btnAbout.alpha(alpha);

		version.alpha(alpha);
		btnFade.icon().alpha(alpha);
		if (btnExit != null){
			btnExit.enable(alpha != 0);
			btnExit.icon().alpha(alpha);
		}

	}

	private Fireball placeTorch(float x, float y ) {
		Fireball fb = new Fireball();
		fb.x = x - fb.width()/2f;
		fb.y = y - fb.height();

		align(fb);
		add( fb );
		return fb;
	}

	private static class NewsButton extends StyledButton {

		public NewsButton(Chrome.Type type, String label ){
			super(type, label);
			if (SPDSettings.news()) News.checkForNews();
		}

		int unreadCount = -1;

		@Override
		public void update() {
			super.update();

			if (unreadCount == -1 && News.articlesAvailable()){
				long lastRead = SPDSettings.newsLastRead();
				if (lastRead == 0){
					if (News.articles().get(0) != null) {
						SPDSettings.newsLastRead(News.articles().get(0).date.getTime());
					}
				} else {
					unreadCount = News.unreadArticles(new Date(SPDSettings.newsLastRead()));
					if (unreadCount > 0) {
						unreadCount = Math.min(unreadCount, 9);
						text(text() + "(" + unreadCount + ")");
					}
				}
			}

			if (unreadCount > 0){
				textColor(ColorMath.interpolate( 0xFFFFFF, Window.SHPX_COLOR, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
		protected void onClick() {
			super.onClick();
			ShatteredPixelDungeon.switchNoFade( NewsScene.class );
		}
	}

	private static class ChangesButton extends LeoStyledButton {

		public ChangesButton(String label){
			super(label);
			if (SPDSettings.updates()) Updates.checkForUpdate();
		}

		boolean updateShown = false;

		@Override
		public void update() {
			super.update();

			if (!updateShown && Updates.updateAvailable()){
				updateShown = true;
				text(Messages.get(TitleScene.class, "update"));
			}

			if (updateShown){
				textColor(ColorMath.interpolate( 0xFFFFFF, Window.SHPX_COLOR, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
		protected void onClick() {
			if (Updates.updateAvailable()){
				AvailableUpdateData update = Updates.updateData();

				ShatteredPixelDungeon.scene().addToFront( new WndOptions(
						Icons.get(Icons.CHANGES),
						update.versionName == null ? Messages.get(this,"title") : Messages.get(this,"versioned_title", update.versionName),
						update.desc == null ? Messages.get(this,"desc") : update.desc,
						Messages.get(this,"update"),
						Messages.get(this,"changes")
				) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							Updates.launchUpdate(Updates.updateData());
						} else if (index == 1){
							ChangesScene.changesSelected = 0;
							ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
						}
					}
				});

			} else {
				ChangesScene.changesSelected = 0;
				ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
			}
		}

	}

	private static class SettingsButton extends LeoStyledButton {

		public SettingsButton(String label){
			super(label);
			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				icon(Icons.get(Icons.LANGS));
				icon.hardlight(1.5f, 0, 0);
			} else {
				icon(Icons.get(Icons.PREFS));
			}
		}

		@Override
		public void update() {
			super.update();

			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				textColor(ColorMath.interpolate( 0xFFFFFF, CharSprite.NEGATIVE, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
		protected void onClick() {
			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				WndSettings.last_index = 5;
			}
			ShatteredPixelDungeon.scene().add(new WndSettings());
		}
	}

	private static class SupportButton extends StyledButton{

		public SupportButton( Chrome.Type type, String label ){
			super(type, label);
			icon(Icons.get(Icons.GOLD));
			textColor(Window.TITLE_COLOR);
		}

		@Override
		protected void onClick() {
			ShatteredPixelDungeon.switchNoFade(SupporterScene.class);
		}
	}
}
