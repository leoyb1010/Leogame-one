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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Point;

public class SPDSettings extends GameSettings {
	
	//Version info
	
	public static final String KEY_VERSION      = "version";
	
	public static void version( int value)  {
		put( KEY_VERSION, value );
	}
	
	public static int version() {
		return getInt( KEY_VERSION, 0 );
	}

	//Display
	
	public static final String KEY_FULLSCREEN	= "fullscreen"; //used to hide navbars on mobile
	public static final String KEY_LANDSCAPE	= "force_landscape";
	public static final String KEY_ZOOM			= "zoom";
	public static final String KEY_BRIGHTNESS	= "brightness";
	public static final String KEY_GRID 	    = "visual_grid";
	public static final String KEY_CAMERA_FOLLOW= "camera_follow";
	public static final String KEY_SCREEN_SHAKE = "screen_shake";
	public static final String KEY_COMBAT_FEEDBACK = "combat_feedback";
	
	public static void fullscreen( boolean value ) {
		put( KEY_FULLSCREEN, value );
		
		ShatteredPixelDungeon.updateSystemUI();
	}
	
	public static boolean fullscreen() {
		// Desktop launches must remain usable beside the user's other apps.
		// Mobile system-bar behavior still has its own platform-side handling.
		return getBoolean( KEY_FULLSCREEN, !DeviceCompat.isDesktop() );
	}

	public static void landscape( boolean value ){
		put( KEY_LANDSCAPE, value );
		((ShatteredPixelDungeon)ShatteredPixelDungeon.instance).updateDisplaySize();
	}

	public static boolean landscape(){
		return getBoolean(KEY_LANDSCAPE, false);
	}
	
	public static void zoom( int value ) {
		put( KEY_ZOOM, value );
	}
	
	public static int zoom() {
		return getInt( KEY_ZOOM, 0 );
	}
	
	public static void brightness( int value ) {
		put( KEY_BRIGHTNESS, value );
		GameScene.updateFog();
	}
	
	public static int brightness() {
		return getInt( KEY_BRIGHTNESS, 0, -1, 1 );
	}
	
	public static void visualGrid( int value ){
		put( KEY_GRID, value );
		GameScene.updateMap();
	}
	
	public static int visualGrid() {
		return getInt( KEY_GRID, 0, -1, 2 );
	}

	public static void cameraFollow( int value ){
		put( KEY_CAMERA_FOLLOW, value );
	}

	public static int cameraFollow() {
		return getInt( KEY_CAMERA_FOLLOW, 4, 1, 4 );
	}

	public static void screenShake( int value ){
		put( KEY_SCREEN_SHAKE, value );
	}

	public static int screenShake() {
		return getInt( KEY_SCREEN_SHAKE, 2, 0, 4 );
	}

	public static void combatFeedback(int value) {
		put(KEY_COMBAT_FEEDBACK, value);
	}

	public static int combatFeedback() {
		return getInt(KEY_COMBAT_FEEDBACK, 2, 0, 2);
	}
	
	//Interface

	public static final String KEY_UI_SIZE 	    = "full_ui";
	public static final String KEY_SCALE		= "scale";
	public static final String KEY_QUICK_SWAP	= "quickslot_swapper";
	public static final String KEY_FLIPTOOLBAR	= "flipped_ui";
	public static final String KEY_FLIPTAGS 	= "flip_tags";
	public static final String KEY_BARMODE		= "toolbar_mode";
	public static final String KEY_SLOTWATERSKIN= "quickslot_waterskin";
	public static final String KEY_SYSTEMFONT	= "system_font";
	public static final String KEY_VIBRATION    = "vibration";

	public static final String KEY_GAMES_SORT    = "games_sort";

	//0 = mobile, 1 = mixed (large without inventory in main UI), 2 = large
	public static void interfaceSize( int value ){
		put( KEY_UI_SIZE, value );
	}

	public static int interfaceSize(){
		int size = getInt( KEY_UI_SIZE, DeviceCompat.isDesktop() ? 2 : 0 );
		if (size > 0){
			//force mobile UI if there is not enough space for full UI
			float wMin = Game.width / PixelScene.MIN_WIDTH_FULL;
			float hMin = Game.height / PixelScene.MIN_HEIGHT_FULL;
			if (Math.min(wMin, hMin) < 2*Game.density){
				size = 0;
			}
		}
		return size;
	}

	public static void scale( int value ) {
		put( KEY_SCALE, value );
	}

	public static int scale() {
		return getInt( KEY_SCALE, 0 );
	}
	
	public static void quickSwapper(boolean value ){ put( KEY_QUICK_SWAP, value ); }
	
	public static boolean quickSwapper(){ return getBoolean( KEY_QUICK_SWAP, true); }
	
	public static void flipToolbar( boolean value) {
		put(KEY_FLIPTOOLBAR, value );
	}
	
	public static boolean flipToolbar(){ return getBoolean(KEY_FLIPTOOLBAR, false); }
	
	public static void flipTags( boolean value) {
		put(KEY_FLIPTAGS, value );
	}
	
	public static boolean flipTags(){ return getBoolean(KEY_FLIPTAGS, false); }
	
	public static void toolbarMode( String value ) {
		put( KEY_BARMODE, value );
	}
	
	public static String toolbarMode() {
		return getString(KEY_BARMODE, PixelScene.landscape() ? "GROUP" : "SPLIT");
	}

	public static void quickslotWaterskin( boolean value ){
		put( KEY_SLOTWATERSKIN, value);
	}

	public static boolean quickslotWaterskin(){
		return getBoolean( KEY_SLOTWATERSKIN, true );
	}

	public static void systemFont(boolean value){
		put(KEY_SYSTEMFONT, value);
	}

	public static boolean systemFont(){
		return getBoolean(KEY_SYSTEMFONT,
				(language() == Languages.CHI_SMPL || language() == Languages.CHI_TRAD
						|| language() == Languages.KOREAN || language() == Languages.JAPANESE));
	}

	public static void vibration(boolean value){
		put(KEY_VIBRATION, value);
	}

	public static boolean vibration(){
		return getBoolean(KEY_VIBRATION, true);
	}

	public static String gamesInProgressSort(){
		return getString(KEY_GAMES_SORT, "level");
	}

	public static void gamesInProgressSort(String value){
		put(KEY_GAMES_SORT, value);
	}

	//Game State
	
	public static final String KEY_LAST_CLASS	= "last_class";
	public static final String KEY_CHALLENGES	= "challenges";
	public static final String KEY_CUSTOM_SEED	= "custom_seed";
	public static final String KEY_LAST_DAILY	= "last_daily";
	public static final String KEY_INTRO		= "intro";

	public static final String KEY_SUPPORT_NAGGED= "support_nagged";
	public static final String KEY_VICTORY_NAGGED= "victory_nagged";
	
	public static void intro( boolean value ) {
		put( KEY_INTRO, value );
	}
	
	public static boolean intro() {
		return getBoolean( KEY_INTRO, true );
	}
	
	public static void lastClass( int value ) {
		put( KEY_LAST_CLASS, value );
	}
	
	public static int lastClass() {
		return getInt( KEY_LAST_CLASS, 0, 0, 3 );
	}
	
	public static void challenges( int value ) {
		put( KEY_CHALLENGES, value );
	}
	
	public static int challenges() {
		return getInt( KEY_CHALLENGES, 0, 0, Challenges.MAX_VALUE );
	}

	public static void customSeed( String value ){
		put( KEY_CUSTOM_SEED, value );
	}

	public static String customSeed() {
		return getString( KEY_CUSTOM_SEED, "", 20);
	}

	public static void lastDaily( long value ){
		put( KEY_LAST_DAILY, value );
	}

	public static long lastDaily() {
		return getLong( KEY_LAST_DAILY, 0);
	}

	public static void supportNagged( boolean value ) {
		put( KEY_SUPPORT_NAGGED, value );
	}

	public static boolean supportNagged() {
		return getBoolean(KEY_SUPPORT_NAGGED, false);
	}

	public static void victoryNagged( boolean value ) {
		put( KEY_VICTORY_NAGGED, value );
	}

	public static boolean victoryNagged() {
		return getBoolean(KEY_VICTORY_NAGGED, false);
	}

	//Input

	public static final String KEY_CONTROLLER_SENS  = "controller_sens";
	public static final String KEY_MOVE_SENS        = "move_sens";
	public static final String KEY_BUKOV_UI_SCALE = "bukov_ui_scale";
	public static final String KEY_BUKOV_REDUCE_MOTION = "bukov_reduce_motion";
	public static final String KEY_BUKOV_REDUCE_FLASHES = "bukov_reduce_flashes";
	public static final String KEY_BUKOV_COLORBLIND = "bukov_colorblind";
	public static final String KEY_BUKOV_SOUND_VISUALIZATION =
			"bukov_sound_visualization";
	public static final String KEY_BUKOV_VIBRATION_LEVEL =
			"bukov_vibration_level";
	public static final String KEY_BUKOV_DAMAGE_NUMBERS =
			"bukov_damage_numbers";
	public static final String KEY_BUKOV_AIM_ASSIST = "bukov_aim_assist";
	public static final String KEY_BUKOV_LEFT_INNER_DEAD_ZONE =
			"bukov_left_inner_dead_zone";
	public static final String KEY_BUKOV_LEFT_OUTER_DEAD_ZONE =
			"bukov_left_outer_dead_zone";
	public static final String KEY_BUKOV_RIGHT_INNER_DEAD_ZONE =
			"bukov_right_inner_dead_zone";
	public static final String KEY_BUKOV_RIGHT_OUTER_DEAD_ZONE =
			"bukov_right_outer_dead_zone";
	public static final String KEY_BUKOV_AIM_CURVE = "bukov_aim_curve";
	public static final String KEY_BUKOV_TRIGGER_PRESS =
			"bukov_trigger_press";
	public static final String KEY_BUKOV_TRIGGER_RELEASE =
			"bukov_trigger_release";
	public static final String KEY_BUKOV_MASTER_VOLUME =
			"bukov_master_volume";
	public static final String KEY_BUKOV_MUSIC_VOLUME =
			"bukov_music_volume";
	public static final String KEY_BUKOV_SFX_VOLUME =
			"bukov_sfx_volume";
	public static final String KEY_BUKOV_AMBIENCE_VOLUME =
			"bukov_ambience_volume";
	public static final String KEY_BUKOV_PERFORMANCE_PROFILE =
			"bukov_performance_profile";
	public static final String KEY_BUKOV_FIRST_RUN_CALIBRATION =
			"bukov_first_run_calibration";

	private static final int BUKOV_CALIBRATION_PENDING = 1;
	private static final int BUKOV_CALIBRATION_CONSUMED = 2;

	public static void controllerPointerSensitivity( int value ){
		put( KEY_CONTROLLER_SENS, value );
	}

	public static int controllerPointerSensitivity(){
		return getInt(KEY_CONTROLLER_SENS, 5, 1, 10);
	}

	public static void movementHoldSensitivity( int value ){
		put( KEY_MOVE_SENS, value );
	}

	public static int movementHoldSensitivity(){
		return getInt(KEY_MOVE_SENS, 3, 0, 4);
	}

	public static void bukovUiScale(int value) {
		put(KEY_BUKOV_UI_SCALE, value);
	}

	public static int bukovUiScale() {
		return getInt(KEY_BUKOV_UI_SCALE, 0, 0, 2);
	}

	/**
	 * Queues calibration only from the verified new-install welcome path.
	 * An absent key therefore means an existing profile and never causes a
	 * surprise migration prompt.
	 */
	public static void scheduleBukovFirstRunCalibration() {
		if (!contains(KEY_BUKOV_FIRST_RUN_CALIBRATION)) {
			put(KEY_BUKOV_FIRST_RUN_CALIBRATION, BUKOV_CALIBRATION_PENDING);
		}
	}

	/**
	 * Returns true exactly once and persists consumption before the window is
	 * shown, so closing the app or backing out cannot trap the player in setup.
	 */
	public static boolean consumeBukovFirstRunCalibration() {
		if (getInt(KEY_BUKOV_FIRST_RUN_CALIBRATION, 0)
				!= BUKOV_CALIBRATION_PENDING) {
			return false;
		}
		put(KEY_BUKOV_FIRST_RUN_CALIBRATION, BUKOV_CALIBRATION_CONSUMED);
		return true;
	}

	public static void bukovReduceMotion(boolean value) {
		put(KEY_BUKOV_REDUCE_MOTION, value);
	}

	public static boolean bukovReduceMotion() {
		return getBoolean(KEY_BUKOV_REDUCE_MOTION, false);
	}

	public static void bukovReduceFlashes(boolean value) {
		put(KEY_BUKOV_REDUCE_FLASHES, value);
	}

	public static boolean bukovReduceFlashes() {
		return getBoolean(KEY_BUKOV_REDUCE_FLASHES, false);
	}

	public static void bukovColorblindAssist(boolean value) {
		put(KEY_BUKOV_COLORBLIND, value);
	}

	public static boolean bukovColorblindAssist() {
		return getBoolean(KEY_BUKOV_COLORBLIND, false);
	}

	public static void bukovSoundVisualization(boolean value) {
		put(KEY_BUKOV_SOUND_VISUALIZATION, value);
	}

	public static boolean bukovSoundVisualization() {
		return getBoolean(KEY_BUKOV_SOUND_VISUALIZATION, false);
	}

	public static void bukovControllerVibration(int value) {
		put(KEY_BUKOV_VIBRATION_LEVEL, value);
		vibration(value > 0);
	}

	public static int bukovControllerVibration() {
		if (!contains(KEY_BUKOV_VIBRATION_LEVEL)) {
			return vibration() ? 2 : 0;
		}
		return getInt(KEY_BUKOV_VIBRATION_LEVEL, 2, 0, 2);
	}

	public static void bukovDamageNumbers(int value) {
		put(KEY_BUKOV_DAMAGE_NUMBERS, value);
	}

	public static int bukovDamageNumbers() {
		return getInt(KEY_BUKOV_DAMAGE_NUMBERS, 1, 0, 2);
	}

	public static void bukovAimAssist(int value) {
		put(KEY_BUKOV_AIM_ASSIST, value);
	}

	public static int bukovAimAssist() {
		return getInt(KEY_BUKOV_AIM_ASSIST, 2, 0, 2);
	}

	public static void bukovLeftInnerDeadZone(int percent) {
		requireBukovRange(percent, 10, 25, "left inner dead zone");
		put(KEY_BUKOV_LEFT_INNER_DEAD_ZONE, percent);
	}

	public static int bukovLeftInnerDeadZone() {
		return getInt(KEY_BUKOV_LEFT_INNER_DEAD_ZONE, 16, 10, 25);
	}

	public static void bukovLeftOuterDeadZone(int percent) {
		requireBukovRange(percent, 90, 100, "left outer dead zone");
		put(KEY_BUKOV_LEFT_OUTER_DEAD_ZONE, percent);
	}

	public static int bukovLeftOuterDeadZone() {
		return getInt(KEY_BUKOV_LEFT_OUTER_DEAD_ZONE, 96, 90, 100);
	}

	public static void bukovRightInnerDeadZone(int percent) {
		requireBukovRange(percent, 10, 25, "right inner dead zone");
		put(KEY_BUKOV_RIGHT_INNER_DEAD_ZONE, percent);
	}

	public static int bukovRightInnerDeadZone() {
		return getInt(KEY_BUKOV_RIGHT_INNER_DEAD_ZONE, 16, 10, 25);
	}

	public static void bukovRightOuterDeadZone(int percent) {
		requireBukovRange(percent, 90, 100, "right outer dead zone");
		put(KEY_BUKOV_RIGHT_OUTER_DEAD_ZONE, percent);
	}

	public static int bukovRightOuterDeadZone() {
		return getInt(KEY_BUKOV_RIGHT_OUTER_DEAD_ZONE, 96, 90, 100);
	}

	public static void bukovAimCurve(int value) {
		put(KEY_BUKOV_AIM_CURVE, value);
	}

	public static int bukovAimCurve() {
		return getInt(KEY_BUKOV_AIM_CURVE, 1, 0, 1);
	}

	public static void bukovTriggerThresholds(
			int pressPercent,
			int releasePercent) {
		if (pressPercent < 55
				|| pressPercent > 75
				|| releasePercent < 35
				|| releasePercent > 55
				|| releasePercent >= pressPercent) {
			throw new IllegalArgumentException(
					"trigger release threshold must be below press threshold");
		}
		put(KEY_BUKOV_TRIGGER_PRESS, pressPercent);
		put(KEY_BUKOV_TRIGGER_RELEASE, releasePercent);
	}

	public static int bukovTriggerPress() {
		return getInt(KEY_BUKOV_TRIGGER_PRESS, 65, 55, 75);
	}

	public static int bukovTriggerRelease() {
		return getInt(KEY_BUKOV_TRIGGER_RELEASE, 45, 35, 55);
	}

	public static void bukovMasterVolume(int value) {
		requireBukovRange(value, 0, 10, "master volume");
		put(KEY_BUKOV_MASTER_VOLUME, value);
	}

	public static int bukovMasterVolume() {
		return getInt(KEY_BUKOV_MASTER_VOLUME, 10, 0, 10);
	}

	public static void bukovMusicVolume(int value) {
		requireBukovRange(value, 0, 10, "music volume");
		put(KEY_BUKOV_MUSIC_VOLUME, value);
	}

	public static int bukovMusicVolume() {
		return getInt(KEY_BUKOV_MUSIC_VOLUME, 8, 0, 10);
	}

	public static void bukovSfxVolume(int value) {
		requireBukovRange(value, 0, 10, "SFX volume");
		put(KEY_BUKOV_SFX_VOLUME, value);
	}

	public static int bukovSfxVolume() {
		return getInt(KEY_BUKOV_SFX_VOLUME, 10, 0, 10);
	}

	public static void bukovAmbienceVolume(int value) {
		requireBukovRange(value, 0, 10, "ambience volume");
		put(KEY_BUKOV_AMBIENCE_VOLUME, value);
	}

	public static int bukovAmbienceVolume() {
		return getInt(KEY_BUKOV_AMBIENCE_VOLUME, 8, 0, 10);
	}

	public static void bukovPerformanceProfile(int value) {
		requireBukovRange(value, 0, 2, "performance profile");
		put(KEY_BUKOV_PERFORMANCE_PROFILE, value);
	}

	/** Defaults to high frame rate without changing the fixed simulation step. */
	public static int bukovPerformanceProfile() {
		return getInt(KEY_BUKOV_PERFORMANCE_PROFILE, 2, 0, 2);
	}

	public static float bukovVolumeGain(int value) {
		requireBukovRange(value, 0, 10, "volume");
		float normalized = value / 10f;
		return normalized * normalized;
	}

	private static void requireBukovRange(
			int value,
			int minimum,
			int maximum,
			String label) {
		if (value < minimum || value > maximum) {
			throw new IllegalArgumentException(
					label + " must be between "
							+ minimum + " and " + maximum);
		}
	}

	//Connectivity

	public static final String KEY_NEWS     = "news";
	public static final String KEY_UPDATES	= "updates";
	public static final String KEY_BETAS	= "betas";
	public static final String KEY_WIFI     = "wifi";

	public static final String KEY_NEWS_LAST_READ = "news_last_read";

	public static void news(boolean value){
		put(KEY_NEWS, value);
	}

	public static boolean news(){
		return getBoolean(KEY_NEWS, true);
	}

	public static void updates(boolean value){
		put(KEY_UPDATES, value);
	}

	public static boolean updates(){
		return getBoolean(KEY_UPDATES, true);
	}

	public static void betas(boolean value){
		put(KEY_BETAS, value);
	}

	public static boolean betas(){
		return getBoolean(KEY_BETAS, Game.version.contains("BETA") || Game.version.contains("RC"));
	}

	public static void WiFi(boolean value){
		put(KEY_WIFI, value);
	}

	public static boolean WiFi(){
		return getBoolean(KEY_WIFI, true);
	}

	public static void newsLastRead(long lastRead){
		put(KEY_NEWS_LAST_READ, lastRead);
	}

	public static long newsLastRead(){
		return getLong(KEY_NEWS_LAST_READ, 0);
	}

	//Audio
	
	public static final String KEY_MUSIC		= "music";
	public static final String KEY_MUSIC_VOL    = "music_vol";
	public static final String KEY_SOUND_FX		= "soundfx";
	public static final String KEY_SFX_VOL      = "sfx_vol";
	public static final String KEY_IGNORE_SILENT= "ignore_silent";
	public static final String KEY_MUSIC_BG     = "music_bg";
	
	public static void music( boolean value ) {
		Music.INSTANCE.enable( value );
		put( KEY_MUSIC, value );
	}
	
	public static boolean music() {
		return getBoolean( KEY_MUSIC, true );
	}
	
	public static void musicVol( int value ){
		Music.INSTANCE.volume(value*value/100f);
		put( KEY_MUSIC_VOL, value );
	}
	
	public static int musicVol(){
		return getInt( KEY_MUSIC_VOL, 10, 0, 10 );
	}
	
	public static void soundFx( boolean value ) {
		Sample.INSTANCE.enable( value );
		put( KEY_SOUND_FX, value );
	}
	
	public static boolean soundFx() {
		return getBoolean( KEY_SOUND_FX, true );
	}
	
	public static void SFXVol( int value ) {
		Sample.INSTANCE.volume(value*value/100f);
		put( KEY_SFX_VOL, value );
	}
	
	public static int SFXVol() {
		return getInt( KEY_SFX_VOL, 10, 0, 10 );
	}

	public static void ignoreSilentMode( boolean value ){
		put( KEY_IGNORE_SILENT, value);
		Game.platform.setHonorSilentSwitch(!value);
	}

	public static boolean ignoreSilentMode(){
		return getBoolean( KEY_IGNORE_SILENT, false);
	}

	public static void playMusicInBackground( boolean value ){
		put( KEY_MUSIC_BG, value);
	}

	public static boolean playMusicInBackground(){
		return getBoolean( KEY_MUSIC_BG, true);
	}
	
	//Languages
	
	public static final String KEY_LANG         = "language";
	
	public static void language(Languages lang) {
		put( KEY_LANG, lang.code());
	}
	
	public static Languages language() {
		String code = getString(KEY_LANG, null);
		return Languages.ENGLISH.code().equals(code) ? Languages.ENGLISH : Languages.CHI_SMPL;
	}

	//Window management (desktop only atm)
	
	public static final String KEY_WINDOW_WIDTH     = "window_width";
	public static final String KEY_WINDOW_HEIGHT    = "window_height";
	public static final String KEY_WINDOW_MAXIMIZED = "window_maximized";
	public static final String KEY_FULLSCREEN_MONITOR = "fullscreen_monitor";

	public static void windowResolution( Point p ){
		put(KEY_WINDOW_WIDTH, p.x);
		put(KEY_WINDOW_HEIGHT, p.y);
	}
	
	public static Point windowResolution(){
		return new Point(
				getInt( KEY_WINDOW_WIDTH, 1100, 720, Integer.MAX_VALUE ),
				getInt( KEY_WINDOW_HEIGHT, 680, 400, Integer.MAX_VALUE )
		);
	}
	
	public static void windowMaximized( boolean value ){
		put( KEY_WINDOW_MAXIMIZED, value );
	}
	
	public static boolean windowMaximized(){
		return getBoolean( KEY_WINDOW_MAXIMIZED, false );
	}

	public static void fulLScreenMonitor( int value ){
		put( KEY_FULLSCREEN_MONITOR, value);
	}

	public static int fulLScreenMonitor(){
		return getInt( KEY_FULLSCREEN_MONITOR, 0 );
	}
}
