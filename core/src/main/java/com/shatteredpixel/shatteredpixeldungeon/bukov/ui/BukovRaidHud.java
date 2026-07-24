/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RaidObjectiveSource;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.ControllerHandler;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PointF;

/**
 * Compact realtime raid HUD. Persistent information stays in a shallow safe-
 * area bar while aim stays in play-space and navigation/threat information
 * stays in a shallow edge rail instead of covering combatants.
 * It only reads presentation state and never owns or advances simulation.
 */
public final class BukovRaidHud extends Component {

	private static final float PADDING = 4f;
	private static final float AWARENESS_SIDE_MARGIN = 6f;
	private static final float AWARENESS_GAP = 4f;
	private static final int STATUS_BLEEDING = 0;
	private static final int STATUS_FRACTURE = 1;
	private static final int STATUS_CONCUSSION = 2;
	private static final int STATUS_COUNT = 3;

	private final BukovRaidHudState live = new BukovRaidHudState();
	private final BukovReloadRingModel reloadRing =
			new BukovReloadRingModel();

	private BukovUiTokens tokens;
	private int primaryColor;
	private int secondaryColor;
	private int interactColor;
	private int valuableColor;
	private int dangerColor;
	private int extractColor;
	private int panelSurfaceColor;
	private ColorBlock background;
	private ColorBlock topEdge;
	private ColorBlock healthTrack;
	private ColorBlock healthFill;
	private ColorBlock dangerFill;
	private ColorBlock healthFlash;
	private ColorBlock[] healthSeparators;
	private ColorBlock armorEdge;
	private ColorBlock[] reloadSegments;
	private ColorBlock[] weaponGlyph;
	private ColorBlock interactionTrack;
	private ColorBlock interactionFill;
	private ColorBlock bossTrack;
	private ColorBlock bossFill;
	private ColorBlock navigationBadge;
	private ColorBlock threatBadge;
	private ColorBlock interactionBadge;
	private ColorBlock[] reticle;
	private BukovHitDirectionArc[] hitDirectionArcs;
	private BukovSoundDirectionArc[] soundDirectionArcs;
	private Image[] injuryIcons;
	private Image healthIcon;
	private Image armorIcon;
	private Image ammoIcon;
	private Image interactionIcon;
	private Image objectiveIcon;
	private Image timerIcon;
	private Image soundIcon;
	private Image hitIcon;

	private RenderedTextBlock healthText;
	private RenderedTextBlock armorText;
	private RenderedTextBlock statusText;
	private RenderedTextBlock[] injuryTimers;
	private RenderedTextBlock medicalHintText;
	private RenderedTextBlock ammoText;
	private RenderedTextBlock weaponText;
	private RenderedTextBlock objectiveText;
	private RenderedTextBlock extractionText;
	private RenderedTextBlock interactionText;
	private RenderedTextBlock timerText;
	private RenderedTextBlock soundText;
	private RenderedTextBlock hitText;
	private RenderedTextBlock bossText;
	private RenderedTextBlock bossObjectiveText;
	private RenderedTextBlock navigationText;
	private RenderedTextBlock threatText;

	private Hero hero;
	private FirearmRegistry firearmRegistry;
	private AmmoRegistry ammoRegistry;
	private RaidSession raidSession;
	private RaidObjectiveSource objectiveSource;
	private BukovRaidHudSource hudSource;
	private String fallbackObjective = BukovHudFormat.DEFAULT_OBJECTIVE;

	private int lastHp = Integer.MIN_VALUE;
	private int lastMaxHp = Integer.MIN_VALUE;
	private int lastShield = Integer.MIN_VALUE;
	private int lastArmorMin = Integer.MIN_VALUE;
	private int lastArmorMax = Integer.MIN_VALUE;
	private int lastMagazine = Integer.MIN_VALUE;
	private int lastCapacity = Integer.MIN_VALUE;
	private int lastReserve = Integer.MIN_VALUE;
	private String lastWeapon;
	private boolean lastAutomatic;
	private String lastObjective;
	private int lastClockSecond = Integer.MIN_VALUE;
	private int lastStatusKey = Integer.MIN_VALUE;
	private int lastMedicalInputMode = Integer.MIN_VALUE;
	private int lastInteractionKey = Integer.MIN_VALUE;
	private String lastInteractionLabel;
	private BukovRaidHudState.Interaction lastInteractionType;
	private int lastExtractionKey = Integer.MIN_VALUE;
	private String lastExtractionId;
	private int lastReloadBucket = Integer.MIN_VALUE;
	private float healthFraction;
	private float uiSeconds;
	private float healthFlashRemaining;
	private boolean injuryIndicatorsVisible;
	private int uiScaleLevel = -1;
	private float uiScale = 1f;

	public BukovRaidHud() {
		super();
		ammoRegistry = new AmmoRegistry();
		ammoRegistry.loadDefault();
		refresh();
	}

	@Override
	protected void createChildren() {
		tokens = BukovUiTokens.loadDefault();
		primaryColor = tokens.color("text.primary");
		secondaryColor = tokens.color("text.secondary");
		interactColor = tokens.color("accent.interact");
		valuableColor = tokens.color("accent.valuable");
		dangerColor = tokens.color("accent.danger");
		extractColor = tokens.color("accent.extract");
		panelSurfaceColor = tokens.color("panel.surface");

		background = block(tokens.colorWithAlpha("ink.background", 224));
		topEdge = block(tokens.colorWithAlpha("accent.interact", 255));
		healthTrack = block(tokens.colorWithAlpha("panel.surface", 255));
		healthFill = block(tokens.colorWithAlpha("accent.extract", 255));
		dangerFill = block(tokens.colorWithAlpha("accent.danger", 255));
		healthFlash = block(tokens.colorWithAlpha("text.primary", 255));
		healthFlash.visible = false;
		healthSeparators = new ColorBlock[11];
		for (int index = 0; index < healthSeparators.length; index++) {
			healthSeparators[index] =
					block(tokens.colorWithAlpha("panel.result", 208));
		}
		armorEdge = block(tokens.colorWithAlpha("accent.valuable", 255));
		reloadSegments =
				new ColorBlock[BukovReloadRingModel.SEGMENT_COUNT];
		for (int index = 0; index < reloadSegments.length; index++) {
			reloadSegments[index] =
					block(tokens.colorWithAlpha("panel.surface", 255));
			reloadSegments[index].visible = false;
		}
		weaponGlyph = new ColorBlock[3];
		for (int index = 0; index < weaponGlyph.length; index++) {
			weaponGlyph[index] =
					block(tokens.colorWithAlpha("text.secondary", 255));
		}
		interactionTrack =
				block(tokens.colorWithAlpha("panel.surface", 255));
		interactionFill =
				block(tokens.colorWithAlpha("accent.interact", 255));
		bossTrack = block(tokens.colorWithAlpha("panel.surface", 255));
		bossFill = block(tokens.colorWithAlpha("accent.danger", 255));
		navigationBadge =
				block(tokens.colorWithAlpha("panel.result", 184));
		threatBadge = block(tokens.colorWithAlpha("ink.shadow", 184));
		interactionBadge =
				block(tokens.colorWithAlpha("panel.deep", 232));
		hitDirectionArcs = new BukovHitDirectionArc[
				BukovCombatHudTimeline.MAX_HIT_DIRECTIONS];
		for (int index = 0; index < hitDirectionArcs.length; index++) {
			hitDirectionArcs[index] =
					new BukovHitDirectionArc(dangerColor);
			add(hitDirectionArcs[index]);
		}
		soundDirectionArcs = new BukovSoundDirectionArc[
				BukovSoundRingModel.SEGMENT_COUNT];
		for (int index = 0; index < soundDirectionArcs.length; index++) {
			soundDirectionArcs[index] =
					new BukovSoundDirectionArc(valuableColor);
			soundDirectionArcs[index].direction(
					BukovRaidHudState.Direction.values()[index]);
			add(soundDirectionArcs[index]);
		}
		reticle = new ColorBlock[5];
		for (int index = 0; index < reticle.length; index++) {
			reticle[index] =
					block(tokens.colorWithAlpha("accent.interact", 255));
		}

		healthIcon = hudIcon(
				BukovUiAssets.HudElement.HEALTH, extractColor);
		armorIcon = hudIcon(
				BukovUiAssets.HudElement.ARMOR, valuableColor);
		ammoIcon = hudIcon(
				BukovUiAssets.HudElement.AMMO, valuableColor);
		interactionIcon = hudIcon(
				BukovUiAssets.HudElement.INTERACT, interactColor);
		objectiveIcon = hudIcon(
				BukovUiAssets.HudElement.OBJECTIVE, primaryColor);
		timerIcon = hudIcon(
				BukovUiAssets.HudElement.TIMER, secondaryColor);
		soundIcon = hudIcon(
				BukovUiAssets.HudElement.SOUND, valuableColor);
		hitIcon = hudIcon(
				BukovUiAssets.HudElement.HIT, dangerColor);

		injuryIcons = new Image[] {
				BukovUiAssets.icon(
						BukovUiAssets.StatusIcon.BLEEDING,
						dangerColor),
				BukovUiAssets.icon(
						BukovUiAssets.StatusIcon.FRACTURE,
						valuableColor),
				BukovUiAssets.icon(
						BukovUiAssets.StatusIcon.CONCUSSION,
						interactColor)
		};
		int[] injuryColors = {
				dangerColor, valuableColor, interactColor
		};
		injuryTimers = new RenderedTextBlock[STATUS_COUNT];
		for (int index = 0; index < STATUS_COUNT; index++) {
			Image icon = injuryIcons[index];
			icon.hardlight(injuryColors[index]);
			icon.visible = false;
			add(icon);
			injuryTimers[index] = text(
					BukovVisualContract.FONT_CAPTION,
					injuryColors[index]);
			injuryTimers[index].visible = false;
		}

		healthText = text(BukovVisualContract.FONT_BODY, primaryColor);
		armorText = text(BukovVisualContract.FONT_CAPTION, valuableColor);
		statusText = text(BukovVisualContract.FONT_CAPTION, secondaryColor);
		medicalHintText = text(
				BukovVisualContract.FONT_CAPTION, extractColor);
		ammoText = text(BukovVisualContract.FONT_BODY, valuableColor);
		weaponText = text(BukovVisualContract.FONT_CAPTION, secondaryColor);
		objectiveText = text(BukovVisualContract.FONT_BODY, primaryColor);
		objectiveText.align(RenderedTextBlock.CENTER_ALIGN);
		extractionText = text(
				BukovVisualContract.FONT_CAPTION, extractColor);
		extractionText.align(RenderedTextBlock.CENTER_ALIGN);
		interactionText = text(
				BukovVisualContract.FONT_BODY, interactColor);
		interactionText.align(RenderedTextBlock.CENTER_ALIGN);
		timerText = text(
				BukovVisualContract.FONT_CAPTION, secondaryColor);
		timerText.align(RenderedTextBlock.RIGHT_ALIGN);
		soundText = text(
				BukovVisualContract.FONT_CAPTION, valuableColor);
		soundText.align(RenderedTextBlock.CENTER_ALIGN);
		hitText = text(BukovVisualContract.FONT_BODY, dangerColor);
		bossText = text(BukovVisualContract.FONT_BODY, dangerColor);
		bossText.align(RenderedTextBlock.CENTER_ALIGN);
		bossObjectiveText = text(
				BukovVisualContract.FONT_CAPTION, valuableColor);
		bossObjectiveText.align(RenderedTextBlock.CENTER_ALIGN);
		navigationText = text(
				BukovVisualContract.FONT_BODY, primaryColor);
		navigationText.align(RenderedTextBlock.CENTER_ALIGN);
		threatText = text(BukovVisualContract.FONT_BODY, dangerColor);
		threatText.align(RenderedTextBlock.CENTER_ALIGN);
	}

	public BukovRaidHud bind(
			Hero hero,
			FirearmRegistry firearmRegistry,
			RaidSession raidSession) {
		return bind(hero, firearmRegistry, raidSession, null);
	}

	public BukovRaidHud bind(
			Hero hero,
			FirearmRegistry firearmRegistry,
			RaidSession raidSession,
			RaidObjectiveSource objectiveSource) {
		this.hero = hero;
		this.firearmRegistry = firearmRegistry;
		this.raidSession = raidSession;
		this.objectiveSource = objectiveSource;
		hudSource = objectiveSource instanceof BukovRaidHudSource
				? (BukovRaidHudSource)objectiveSource : null;
		refresh();
		return this;
	}

	public void objective(String objective) {
		fallbackObjective = BukovHudFormat.objective(objective);
		refresh();
	}

	public static float preferredHeight(float availableWidth) {
		return BukovRaidHudLayout.preferredHeight(availableWidth, 0);
	}

	public static float preferredHeight(
			float availableWidth, int scaleLevel) {
		return BukovRaidHudLayout.preferredHeight(
				availableWidth, scaleLevel);
	}

	public static float scaleMultiplier(int scaleLevel) {
		return BukovRaidHudLayout.scaleMultiplier(scaleLevel);
	}

	@Override
	public void update() {
		super.update();
		float elapsed = Math.max(0f, Game.elapsed);
		uiSeconds += elapsed;
		healthFlashRemaining = Math.max(0f, healthFlashRemaining - elapsed);
		refresh(elapsed);
	}

	public void refresh() {
		refresh(0f);
	}

	private void refresh(float elapsedSeconds) {
		applyUiScale(SPDSettings.bukovUiScale());
		if (hudSource != null) {
			hudSource.readRaidHudState(live);
		}
		refreshVitals();
		refreshMedicalHint();
		refreshFirepower();
		refreshMissionAndInteraction();
		refreshCombatAwareness();
		refreshAnimationState(elapsedSeconds);
		layout();
	}

	private void applyUiScale(int scaleLevel) {
		int clamped = Math.max(0, Math.min(2, scaleLevel));
		if (clamped == uiScaleLevel) return;
		uiScaleLevel = clamped;
		uiScale = scaleMultiplier(clamped);
		// Never use RenderedTextBlock.zoom here. It compounds with the 3x iOS
		// UI camera and can explode glyphs across the whole viewport. Font size
		// is selected once during construction; live setting changes only grow
		// safe geometry, reticle, badges and touch affordances.
		if (width > 0f) {
			height = preferredHeight(width, clamped);
		}
	}

	private void refreshVitals() {
		int hp = hero == null ? 0 : hero.HP;
		int maxHp = hero == null ? 1 : Math.max(1, hero.HT);
		int shield = hero == null ? 0 : hero.shielding();
		healthFraction = BukovHudFormat.healthFraction(hp, maxHp);
		if (hp != lastHp || maxHp != lastMaxHp || shield != lastShield) {
			if (lastHp != Integer.MIN_VALUE && hp < lastHp) {
				healthFlashRemaining = 0.07f;
			}
			lastHp = hp;
			lastMaxHp = maxHp;
			lastShield = shield;
			healthText.text(BukovHudFormat.health(hp, maxHp, shield));
		}

		Armor armor = hero == null || hero.belongings == null
				? null : hero.belongings.armor();
		int armorMin = armor == null ? -1 : Math.max(0, armor.DRMin());
		int armorMax = armor == null ? -1 : Math.max(armorMin, armor.DRMax());
		if (armorMin != lastArmorMin || armorMax != lastArmorMax) {
			lastArmorMin = armorMin;
			lastArmorMax = armorMax;
			armorText.text(armor == null
					? BukovHudFormat.armor(null, null)
					: BukovHudFormat.armor(armorMin, armorMax));
		}

		int statusKey = statusKey();
		if (statusKey != lastStatusKey) {
			lastStatusKey = statusKey;
			statusText.text(BukovHudFormat.status(
					live.bleedingPerSecond(),
					live.fractured(),
					live.painSeverity(),
					live.concussionRemaining(),
					live.stimulantRemaining()));
			injuryTimers[STATUS_BLEEDING].text(
					BukovHudFormat.injuryRemaining(
							live.bleedingPerSecond() > 0f,
							0f));
			injuryTimers[STATUS_FRACTURE].text(
					BukovHudFormat.injuryRemaining(
							live.fractured(),
							0f));
			injuryTimers[STATUS_CONCUSSION].text(
					BukovHudFormat.injuryRemaining(
							live.concussionRemaining() > 0f,
							live.concussionRemaining()));
		}
		boolean bleeding = live.bleedingPerSecond() > 0f;
		boolean fractured = live.fractured();
		boolean concussed = live.concussionRemaining() > 0f;
		boolean injured = bleeding || fractured || concussed;
		injuryIndicatorsVisible = injured;
		statusText.visible = !injured;
		setInjuryIndicatorVisible(STATUS_BLEEDING, bleeding);
		setInjuryIndicatorVisible(STATUS_FRACTURE, fractured);
		setInjuryIndicatorVisible(STATUS_CONCUSSION, concussed);
		statusText.hardlight(
				injured ? dangerColor : secondaryColor);
	}

	private void refreshMedicalHint() {
		boolean controller = ControllerHandler.controllerActive;
		boolean desktop = DeviceCompat.isDesktop();
		int inputMode = controller ? 1 : desktop ? 0 : 2;
		if (inputMode != lastMedicalInputMode) {
			lastMedicalInputMode = inputMode;
			medicalHintText.text(medicalHint(desktop, controller));
		}
		boolean urgent = healthFraction <= 0.7f
				|| live.bleedingPerSecond() > 0f
				|| live.fractured()
				|| live.concussionRemaining() > 0f;
		medicalHintText.hardlight(urgent ? dangerColor : extractColor);
		medicalHintText.alpha(urgent ? 1f : 0.72f);
	}

	private void refreshFirepower() {
		Firearm firearm = equippedFirearm();
		String weaponName;
		boolean automatic;
		int magazine;
		int capacity;
		int reserve;
		if (hudSource != null) {
			weaponName = live.weaponName();
			automatic = live.automaticFire();
			magazine = live.magazine();
			capacity = live.magazineCapacity();
			reserve = live.reserve();
		} else if (firearm != null && firearmRegistry != null) {
			FirearmDefinition definition = firearm.definition(firearmRegistry);
			weaponName = definition.name;
			automatic = definition.fireMode
					== com.shatteredpixel.shatteredpixeldungeon.bukov
							.combat.firearms.FireMode.AUTO;
			magazine = firearm.magazineAmmo();
			capacity = definition.magazineSize;
			reserve = reserveAmmo(firearm);
		} else {
			weaponName = null;
			automatic = false;
			magazine = 0;
			capacity = 0;
			reserve = 0;
		}
		if (magazine != lastMagazine
				|| capacity != lastCapacity
				|| reserve != lastReserve) {
			lastMagazine = magazine;
			lastCapacity = capacity;
			lastReserve = reserve;
			ammoText.text(BukovHudFormat.tacticalAmmo(
					weaponName, magazine, capacity, reserve));
		}
		if (!same(lastWeapon, weaponName) || lastAutomatic != automatic) {
			lastWeapon = weaponName;
			lastAutomatic = automatic;
			weaponText.text(BukovHudFormat.weapon(weaponName, automatic));
		}
	}

	private void refreshMissionAndInteraction() {
		String objective = hudSource != null && live.objective() != null
				? live.objective()
				: objectiveSource == null
						? fallbackObjective : objectiveSource.raidObjective();
		objective = BukovHudFormat.objective(objective);
		if (!objective.equals(lastObjective)) {
			lastObjective = objective;
			objectiveText.text("任务 · " + objective);
		}

		float elapsed = hudSource != null
				? live.raidElapsedSeconds()
				: raidSession == null ? 0f : raidSession.elapsedSeconds;
		int clockSecond = Math.max(0, (int)Math.floor(elapsed));
		if (clockSecond != lastClockSecond) {
			lastClockSecond = clockSecond;
			timerText.text(
					"行动 " + BukovHudFormat.clock(elapsed)
							+ (DeviceCompat.isDesktop()
									? "\n" + controlHint(true) : ""));
		}

		int extractionKey = extractionKey();
		if (extractionKey != lastExtractionKey
				|| !same(lastExtractionId, live.extractionId())) {
			lastExtractionKey = extractionKey;
			lastExtractionId = live.extractionId();
			extractionText.text(BukovHudFormat.extraction(
					live.availableExtractions(),
					live.extractionId(),
					live.extractionAvailable(),
					live.extractionActive(),
					live.extractionProgress(),
					live.extractionSeconds()));
		}

		int interactionKey = interactionKey();
		if (interactionKey != lastInteractionKey
				|| lastInteractionType != live.interaction()
				|| !same(lastInteractionLabel, live.interactionLabel())) {
			lastInteractionKey = interactionKey;
			lastInteractionType = live.interaction();
			lastInteractionLabel = live.interactionLabel();
			interactionText.text(BukovHudFormat.interaction(
					live.interaction(),
					live.interactionLabel(),
					live.interactionProgress(),
					live.interactionSeconds(),
					DeviceCompat.isDesktop()));
		}
	}

	private void refreshAnimationState(float elapsedSeconds) {
		boolean reduceMotion = SPDSettings.bukovReduceMotion();
		boolean lowHealth = healthFraction <= 0.30f;
		healthFill.visible = !lowHealth;
		dangerFill.visible = lowHealth;
		healthFlash.visible = healthFlashRemaining > 0f;
		healthFlash.alpha(Math.min(1f, healthFlashRemaining / 0.07f));
		if (lowHealth) {
			dangerFill.alpha(reduceMotion
					? 1f
					: 0.58f + 0.42f
							* Math.abs((uiSeconds % 1f) * 2f - 1f));
		}

		boolean lowAmmo = lastCapacity > 0
				&& lastMagazine * 4 <= lastCapacity;
		boolean blinkOn = reduceMotion
				|| ((int)Math.floor(uiSeconds * 2f) & 1) == 0;
		float awarenessAlpha = live.combatAwarenessAlpha();
		ammoText.hardlight(
				lowAmmo ? dangerColor : valuableColor);
		ammoText.alpha(
				awarenessAlpha
						* (lowAmmo && !blinkOn ? 0.55f : 1f));
		weaponText.alpha(awarenessAlpha);

		int reloadBucket = Math.round(live.reloadProgress() * 100f);
		if (reloadBucket != lastReloadBucket) {
			lastReloadBucket = reloadBucket;
			weaponText.text(live.reloading()
					? BukovHudFormat.reload(true, live.reloadProgress())
					: BukovHudFormat.weapon(lastWeapon, lastAutomatic));
		}
		reloadRing.update(
				elapsedSeconds,
				live.reloading(),
				live.reloadProgress(),
				Math.max(0, lastMagazine),
				reduceMotion,
				tokens.motionMs("fast") / 1000f);
		int filledReloadSegments = reloadRing.filledSegmentCount();
		boolean reloadVisible = reloadRing.visible(live.reloading());
		for (int index = 0; index < reloadSegments.length; index++) {
			ColorBlock segment = reloadSegments[index];
			segment.visible = reloadVisible;
			segment.hardlight(index < filledReloadSegments
					? valuableColor : panelSurfaceColor);
			segment.alpha(awarenessAlpha);
		}
		for (ColorBlock piece : weaponGlyph) {
			piece.visible = lastCapacity > 0;
			piece.hardlight(lowAmmo ? dangerColor : secondaryColor);
			piece.alpha(awarenessAlpha);
		}
		interactionTrack.visible = live.interactionProgress() > 0f;
		interactionFill.visible = interactionTrack.visible;
		interactionBadge.visible =
				live.interaction() != BukovRaidHudState.Interaction.NONE;
		interactionIcon.visible = interactionBadge.visible;
		interactionText.hardlight(
				live.interaction() == BukovRaidHudState.Interaction.LOCKED
						? dangerColor : interactColor);
		extractionText.hardlight(
				live.extractionId() != null && !live.extractionAvailable()
						? dangerColor : extractColor);
		float soundAlpha = BukovSoundRingModel.alpha(live);
		int soundSegment =
				BukovSoundRingModel.segmentIndex(live.soundDirection());
		boolean longSoundArc =
				BukovSoundRingModel.longArc(live.soundCategory());
		int soundColor = longSoundArc ? dangerColor : valuableColor;
		soundText.visible = live.soundVisible();
		soundIcon.visible =
				DeviceCompat.isDesktop() && live.soundVisible();
		soundText.alpha(soundAlpha);
		for (int index = 0; index < soundDirectionArcs.length; index++) {
			BukovSoundDirectionArc arc = soundDirectionArcs[index];
			arc.visible = soundAlpha > 0f && index == soundSegment;
			if (!arc.visible) continue;
			arc.longArc(longSoundArc);
			arc.hardlight(soundColor);
			arc.alpha(soundAlpha);
		}
		hitText.visible = live.hitVisible();
		hitIcon.visible =
				DeviceCompat.isDesktop() && live.hitVisible();
		float strongestHitAlpha = 0f;
		for (int index = 0; index < hitDirectionArcs.length; index++) {
			boolean visible = index < live.hitCount();
			BukovHitDirectionArc arc = hitDirectionArcs[index];
			arc.visible = visible;
			if (!visible) continue;
			arc.direction(live.hitDirection(index));
			float lifetimeAlpha = Math.max(
					0f,
					Math.min(
							1f,
							live.hitRemainingSeconds(index)
									/ BukovCombatHudTimeline
											.HIT_LIFETIME_SECONDS));
			float arcAlpha = lifetimeAlpha
					* (0.55f + 0.45f * live.hitStrength(index));
			arc.alpha(arcAlpha);
			strongestHitAlpha = Math.max(
					strongestHitAlpha,
					arcAlpha);
		}
		hitText.alpha(strongestHitAlpha);
		bossText.visible = live.bossActive();
		bossObjectiveText.visible = live.bossActive();
		bossTrack.visible = live.bossActive();
		bossFill.visible = live.bossActive();
		objectiveText.visible = !live.bossActive();
		extractionText.visible = !live.bossActive();
		objectiveIcon.visible = !live.bossActive();
		bossFill.hardlight(
				live.bossVulnerable() ? valuableColor : dangerColor);
		boolean textEdgeRail = DeviceCompat.isDesktop();
		navigationText.visible =
				textEdgeRail && live.navigationVisible();
		navigationBadge.visible =
				textEdgeRail && live.navigationVisible();
		navigationText.alpha(awarenessAlpha);
		navigationBadge.alpha(awarenessAlpha);
		navigationText.hardlight(
				live.navigationAvailable() ? primaryColor : dangerColor);
		threatText.visible = textEdgeRail && live.threatVisible();
		threatBadge.visible = textEdgeRail && live.threatVisible();
		threatText.alpha(awarenessAlpha);
		threatBadge.alpha(awarenessAlpha);
		threatText.hardlight(
				live.threatUrgent() ? dangerColor : valuableColor);
		int reticleColor = lastCapacity <= 0 || lastMagazine <= 0
				? dangerColor
				: live.firing() ? valuableColor : interactColor;
		for (ColorBlock piece : reticle) {
			piece.visible = live.aimVisible();
			piece.hardlight(reticleColor);
		}
	}

	private void refreshCombatAwareness() {
		soundText.text(BukovCombatHudFormat.sound(live));
		hitText.text(BukovCombatHudFormat.hit(live));
		bossText.text(BukovCombatHudFormat.bossTitle(live));
		bossObjectiveText.text(
				BukovCombatHudFormat.bossObjective(live));
		navigationText.text(BukovCombatHudFormat.navigation(live));
		threatText.text(BukovCombatHudFormat.threat(live));
	}

	@Override
	protected void layout() {
		// Components are constructed before GameScene assigns their rectangle.
		// refresh() is allowed during that phase, but no geometry may be derived
		// from the still-zero width.
		if (background == null || width <= 0f) return;
		float actualHeight = height > 0f ? height : preferredHeight(width);
		background.x = x;
		background.y = y;
		background.size(width, actualHeight);
		topEdge.x = x;
		topEdge.y = y;
		topEdge.size(width, 1f);
		if (width >= BukovRaidHudLayout.WIDE_THRESHOLD) {
			layoutWide(actualHeight);
		} else {
			layoutCompact(actualHeight);
		}
		layoutCombatOverlay(actualHeight);
	}

	private void layoutWide(float actualHeight) {
		statusText.text(currentStatusLabel());
		weaponText.text(currentWeaponLabel());
		objectiveText.text("任务 · " + BukovHudFormat.objective(lastObjective));
		float leftWidth = Math.min(88f, width * 0.28f);
		float rightWidth = Math.min(78f, width * 0.24f);
		float centerLeft = x + leftWidth + PADDING;
		float centerRight = x + width - rightWidth - PADDING;
		float centerWidth = Math.max(34f, centerRight - centerLeft);

		positionHudIcon(healthIcon, x + PADDING, y + 3f);
		healthText.setPos(x + PADDING + 10f, y + 3f);
		healthTrack.x = x + PADDING;
		healthTrack.y = y + 14f;
		healthTrack.size(leftWidth - PADDING * 2f, 3f);
		positionHealthFill(
				healthTrack.x, healthTrack.y, healthTrack.width(), 3f);
		positionHudIcon(armorIcon, x + PADDING, y + 19f);
		armorEdge.x = x + PADDING + 10f;
		armorEdge.y = y + 20f;
		armorEdge.size(2f, 5f);
		armorText.setPos(x + PADDING + 14f, y + 19f);
		statusText.maxWidth((int)(leftWidth - PADDING * 2f));
		statusText.setPos(x + PADDING, y + 28f);
		positionInjuryIndicators(
				x + PADDING,
				y + 27f,
				leftWidth - PADDING * 2f,
				9f,
				7f);
		medicalHintText.visible = true;
		medicalHintText.maxWidth((int)(leftWidth - PADDING * 2f));
		medicalHintText.setPos(x + PADDING, y + 36f);

		positionHudIcon(objectiveIcon, centerLeft, y + 3f);
		objectiveText.maxWidth((int)Math.max(1f, centerWidth - 10f));
		objectiveText.setPos(centerLeft + 10f, y + 3f);
		extractionText.maxWidth((int)centerWidth);
		extractionText.setPos(centerLeft, y + 14f);
		positionHudIcon(interactionIcon, centerLeft, y + 25f);
		interactionText.maxWidth((int)Math.max(1f, centerWidth - 10f));
		interactionText.setPos(centerLeft + 10f, y + 25f);
		positionInteractionBar(centerLeft, y + actualHeight - 2f, centerWidth);

		float rightX = x + width - rightWidth;
		float reloadSize = BukovRaidHudLayout.RELOAD_RING_SIZE;
		float reloadX = x + width - PADDING - reloadSize;
		positionHudIcon(ammoIcon, rightX, y + 2f);
		ammoText.setPos(rightX + 10f, y + 2f);
		ammoText.maxWidth((int)Math.max(
				1f, rightWidth - PADDING - reloadSize - 13f));
		weaponText.maxWidth((int)Math.max(
				1f, rightWidth - PADDING - reloadSize - 3f));
		weaponText.setPos(rightX, y + 16f);
		positionReloadRing(reloadX, y + 3f, reloadSize);
		positionHudIcon(timerIcon, rightX, y + 25f);
		timerText.maxWidth((int)(rightWidth - PADDING - 10f));
		timerText.setPos(rightX + 10f, y + 24f);
		positionBoss(centerLeft, y + 2f, centerWidth);
	}

	private void layoutCompact(float actualHeight) {
		BukovRaidHudLayout hudLayout =
				BukovRaidHudLayout.calculate(width, uiScaleLevel);
		BukovRaidHudLayout.Rect vitals = hudLayout.vitals;
		BukovRaidHudLayout.Rect firepower = hudLayout.firepower;
		BukovRaidHudLayout.Rect medicalHint = hudLayout.medicalHint;
		float scaledPadding = PADDING * uiScale;

		positionHudIcon(
				healthIcon,
				x + vitals.x,
				y + vitals.y);
		healthText.setPos(
				x + vitals.x + 10f * uiScale,
				y + vitals.y);
		healthTrack.x = x + vitals.x;
		healthTrack.y = y + vitals.y + 12f * uiScale;
		healthTrack.size(vitals.width, 3f * uiScale);
		positionHealthFill(
				healthTrack.x,
				healthTrack.y,
				vitals.width,
				3f * uiScale);
		positionHudIcon(
				armorIcon,
				x + vitals.x,
				y + vitals.y + 17f * uiScale);
		armorEdge.x = x + vitals.x + 10f * uiScale;
		armorEdge.y = y + vitals.y + 18f * uiScale;
		armorEdge.size(2f * uiScale, 5f * uiScale);
		armorText.setPos(
				x + vitals.x + 14f * uiScale,
				y + vitals.y + 17f * uiScale);
		statusText.text(BukovRaidHudLayout.compactLine(
				currentStatusLabel(),
				hudLayout.condition.width,
				uiScaleLevel));
		statusText.maxWidth((int)hudLayout.condition.width);
		statusText.setPos(
				x + hudLayout.condition.x,
				y + hudLayout.condition.y);

		BukovRaidHudLayout.Rect reloadRing =
				BukovRaidHudLayout.compactReloadRing(
						width, uiScaleLevel);
		float reloadSize = reloadRing.width;
		float reloadX = x + reloadRing.x;
		float firepowerCopyWidth = Math.max(
				1f, firepower.width - reloadSize - 13f * uiScale);
		positionHudIcon(
				ammoIcon,
				x + firepower.x,
				y + firepower.y);
		ammoText.maxWidth((int)firepowerCopyWidth);
		ammoText.setPos(
				x + firepower.x + 10f * uiScale,
				y + firepower.y);
		weaponText.text(BukovRaidHudLayout.compactLine(
				currentWeaponLabel(),
				firepowerCopyWidth,
				uiScaleLevel));
		weaponText.maxWidth((int)firepowerCopyWidth);
		weaponText.setPos(
				x + firepower.x,
				y + firepower.y + 14f * uiScale);
		positionReloadRing(
				reloadX,
				y + reloadRing.y,
				reloadSize);
		float injuryWidth =
				medicalHint.right() - hudLayout.condition.x;
		positionInjuryIndicators(
				x + hudLayout.condition.x,
				y + hudLayout.condition.y,
				injuryWidth,
				hudLayout.condition.height,
				7f * uiScale);
		medicalHintText.visible = !injuryIndicatorsVisible;
		medicalHintText.maxWidth((int)medicalHint.width);
		medicalHintText.setPos(
				x + medicalHint.x,
				y + medicalHint.y);
		positionHudIcon(
				timerIcon,
				x + hudLayout.clock.x,
				y + hudLayout.clock.y);
		timerText.maxWidth((int)Math.max(
				1f, hudLayout.clock.width - 10f * uiScale));
		timerText.setPos(
				x + hudLayout.clock.x + 10f * uiScale,
				y + hudLayout.clock.y);

		extractionText.maxWidth((int)hudLayout.extraction.width);
		extractionText.setPos(
				x + hudLayout.extraction.x,
				y + hudLayout.extraction.y);
		objectiveText.text("任务 · "
				+ BukovRaidHudLayout.compactObjective(
						BukovHudFormat.objective(lastObjective),
						hudLayout.objective.width - 24f * uiScale,
						uiScaleLevel));
		positionHudIcon(
				objectiveIcon,
				x + hudLayout.objective.x,
				y + hudLayout.objective.y);
		objectiveText.maxWidth((int)Math.max(
				1f, hudLayout.objective.width - 10f * uiScale));
		objectiveText.setPos(
				x + hudLayout.objective.x + 10f * uiScale,
				y + hudLayout.objective.y);
		positionInteractionBar(
				x + scaledPadding,
				y + actualHeight - 2f * uiScale,
				width - scaledPadding * 2f);
		positionBoss(
				x + scaledPadding,
				y + hudLayout.extraction.y,
				width - scaledPadding * 2f);
	}

	private void layoutCombatOverlay(float actualHeight) {
		float viewportWidth = camera == null ? x + width : camera.width;
		float viewportHeight = camera == null
				? y + actualHeight + 160f : camera.height;
		float centerX = viewportWidth * 0.5f;
		float centerY = Math.max(
				y + actualHeight + 42f,
				viewportHeight * 0.52f);
		float minViewport = Math.min(viewportWidth, viewportHeight);
		float aimRadius = clamp(
				minViewport * 0.095f,
				24f * uiScale,
				42f * uiScale);
		float crosshairX = centerX + live.aimX() * aimRadius;
		float crosshairY = centerY + live.aimY() * aimRadius;
		PointF desktopPointer = desktopPointerInHud();
		if (desktopPointer != null) {
			crosshairX = clamp(
					desktopPointer.x,
					4f,
					Math.max(4f, viewportWidth - 4f));
			crosshairY = clamp(
					desktopPointer.y,
					y + actualHeight + 3f,
					Math.max(y + actualHeight + 3f, viewportHeight - 4f));
		}
		positionReticle(crosshairX, crosshairY);
		float playableTop = y + actualHeight + 3f;
		float arcCenterY = (playableTop + viewportHeight) * 0.5f;
		float arcRadiusX = Math.max(
				12f,
				viewportWidth * 0.5f - 8f);
		float arcRadiusY = Math.max(
				12f,
				(viewportHeight - playableTop) * 0.5f - 8f);
		for (BukovHitDirectionArc arc : hitDirectionArcs) {
			arc.fit(
					viewportWidth * 0.5f,
					arcCenterY,
					arcRadiusX,
					arcRadiusY);
		}
		float soundRadius = clamp(
				minViewport * 0.11f,
				25f * uiScale,
				40f * uiScale);
		float soundCenterX = playerHudX(viewportWidth);
		float soundCenterY = playerHudY(arcCenterY, playableTop, viewportHeight);
		for (BukovSoundDirectionArc arc : soundDirectionArcs) {
			arc.fit(
					soundCenterX,
					soundCenterY,
					soundRadius);
		}

		/*
		 * Direction text already carries an arrow. Keep these badges in a
		 * shallow edge rail rather than placing 94-112 world-unit slabs over
		 * the direction they describe, where they can hide the actual enemy.
		 */
		if (DeviceCompat.isDesktop()) {
			float awarenessWidth = awarenessBadgeWidth(
					viewportWidth, uiScale);
			float awarenessY = y + actualHeight + 10f;
			positionBadge(
					navigationBadge,
					navigationText,
					AWARENESS_SIDE_MARGIN + awarenessWidth * 0.5f,
					awarenessY,
					awarenessWidth,
					viewportWidth,
					viewportHeight,
					y + actualHeight + 3f);

			positionBadge(
					threatBadge,
					threatText,
					viewportWidth - AWARENESS_SIDE_MARGIN
							- awarenessWidth * 0.5f,
					awarenessY,
					awarenessWidth,
					viewportWidth,
					viewportHeight,
					y + actualHeight + 3f);

			float feedbackWidth = Math.min(
					160f * uiScale, viewportWidth - 12f);
			float feedbackX = centerX - feedbackWidth * 0.5f;
			float feedbackY = clamp(
					centerY + aimRadius + 11f,
					playableTop,
					Math.max(playableTop, viewportHeight - 17f));
			interactionBadge.x = feedbackX;
			interactionBadge.y = feedbackY;
			interactionBadge.size(feedbackWidth, 13f);
			interactionText.maxWidth((int)feedbackWidth);
			interactionText.setPos(feedbackX, feedbackY + 3f);
			soundText.maxWidth((int)feedbackWidth);
			float soundY =
					Math.max(playableTop, centerY - aimRadius - 26f);
			soundText.setPos(
					feedbackX,
					soundY);
			positionHudIcon(soundIcon, feedbackX + 3f, soundY);
			hitText.maxWidth((int)feedbackWidth);
			float hitY = Math.max(
					playableTop + 9f,
					centerY - aimRadius - 14f);
			hitText.setPos(
					feedbackX,
					hitY);
			positionHudIcon(hitIcon, feedbackX + 3f, hitY);
		} else {
			/*
			 * Mobile already has directional sound/damage arcs. Keep only the
			 * actionable interaction copy, in the left rail opposite the two
			 * navigation buttons, so no text slab covers either touch stick.
			 */
			BukovRaidHudLayout.Rect feedback =
					BukovRaidHudLayout.mobileFeedback(
							viewportWidth,
							viewportHeight,
							x,
							y + actualHeight);
			interactionBadge.x = feedback.x;
			interactionBadge.y = feedback.y;
			interactionBadge.size(feedback.width, feedback.height);
			interactionText.maxWidth((int)feedback.width);
			interactionText.setPos(
					feedback.x,
					feedback.y + 3f);
			soundText.visible = false;
			hitText.visible = false;
			soundIcon.visible = false;
			hitIcon.visible = false;
		}
		positionHudIcon(
				interactionIcon,
				interactionBadge.x + 3f,
				interactionBadge.y + 2f);
	}

	private PointF desktopPointerInHud() {
		if (!DeviceCompat.isDesktop()
				|| ControllerHandler.controllerActive
				|| camera == null) {
			return null;
		}
		PointF pointer = PointerEvent.currentHoverPos();
		if (pointer == null) {
			return null;
		}
		return camera.screenToCamera((int)pointer.x, (int)pointer.y);
	}

	private float playerHudX(float fallback) {
		if (hero == null || hero.sprite == null
				|| Camera.main == null || camera == null) {
			return fallback * 0.5f;
		}
		float worldX = hero.sprite.x + hero.sprite.width() * 0.5f;
		float screenX = (worldX - Camera.main.scroll.x)
				* Camera.main.zoom + Camera.main.x;
		float uiX = (screenX - camera.x) / camera.zoom + camera.scroll.x;
		return clamp(uiX, 4f, Math.max(4f, fallback - 4f));
	}

	private float playerHudY(
			float fallback,
			float playableTop,
			float viewportHeight) {
		if (hero == null || hero.sprite == null
				|| Camera.main == null || camera == null) {
			return fallback;
		}
		float worldY = hero.sprite.y + hero.sprite.height() * 0.5f;
		float screenY = (worldY - Camera.main.scroll.y)
				* Camera.main.zoom + Camera.main.y;
		float uiY = (screenY - camera.y) / camera.zoom + camera.scroll.y;
		return clamp(
				uiY,
				playableTop + 4f,
				Math.max(playableTop + 4f, viewportHeight - 4f));
	}

	private void positionReticle(float centerX, float centerY) {
		float gap = 3f * uiScale;
		float length = (live.firing() ? 6f : 4f) * uiScale;
		reticle[0].x = centerX - gap - length;
		reticle[0].y = centerY;
		reticle[0].size(length, 1f);
		reticle[1].x = centerX + gap;
		reticle[1].y = centerY;
		reticle[1].size(length, 1f);
		reticle[2].x = centerX;
		reticle[2].y = centerY - gap - length;
		reticle[2].size(1f, length);
		reticle[3].x = centerX;
		reticle[3].y = centerY + gap;
		reticle[3].size(1f, length);
		reticle[4].x = centerX;
		reticle[4].y = centerY;
		reticle[4].size(1f, 1f);
	}

	private void positionBadge(
			ColorBlock badge,
			RenderedTextBlock label,
			float centerX,
			float centerY,
			float badgeWidth,
			float viewportWidth,
			float viewportHeight,
			float minimumY) {
		float badgeX = clamp(
				centerX - badgeWidth * 0.5f,
				4f,
				Math.max(4f, viewportWidth - badgeWidth - 4f));
		float badgeY = clamp(
				centerY - 6f,
				minimumY,
				Math.max(minimumY, viewportHeight - 15f));
		badge.x = badgeX;
		badge.y = badgeY;
		badge.size(badgeWidth, 12f);
		label.maxWidth((int)(badgeWidth - 4f));
		label.setPos(badgeX + 2f, badgeY + 2f);
	}

	static float awarenessBadgeWidth(
			float viewportWidth, float uiScale) {
		float availablePerBadge = Math.max(
				36f,
				(viewportWidth
						- AWARENESS_SIDE_MARGIN * 2f
						- AWARENESS_GAP) * 0.5f);
		float scaledWidth = 84f + 24f
				* (clamp(uiScale, 1f, 1.5f) - 1f);
		return Math.min(
				scaledWidth,
				availablePerBadge);
	}

	private void positionBoss(float barX, float textY, float barWidth) {
		bossText.maxWidth((int)barWidth);
		bossText.setPos(barX, textY);
		bossObjectiveText.maxWidth((int)barWidth);
		bossObjectiveText.setPos(barX, textY + 9f);
		bossTrack.x = barX;
		bossTrack.y = textY + 17f;
		bossTrack.size(barWidth, 2f);
		bossFill.x = barX;
		bossFill.y = bossTrack.y;
		bossFill.size(
				barWidth * live.bossHealthFraction(), 2f);
	}

	private void positionHealthFill(
			float barX,
			float barY,
			float barWidth,
			float barHeight) {
		float fillWidth = barWidth * healthFraction;
		healthFill.x = dangerFill.x = barX;
		healthFlash.x = barX;
		healthFill.y = dangerFill.y = healthFlash.y = barY;
		healthFill.size(fillWidth, barHeight);
		dangerFill.size(fillWidth, barHeight);
		healthFlash.size(fillWidth, barHeight);
		int segmentCount = Math.max(
				1,
				(int)Math.ceil(Math.max(1, lastMaxHp) / 10f));
		for (int index = 0; index < healthSeparators.length; index++) {
			ColorBlock separator = healthSeparators[index];
			separator.visible = index + 1 < segmentCount;
			if (!separator.visible) continue;
			separator.x = barX
					+ barWidth * (index + 1f) / segmentCount;
			separator.y = barY;
			separator.size(1f, barHeight);
		}
	}

	private void positionInteractionBar(
			float barX,
			float barY,
			float barWidth) {
		interactionTrack.x = barX;
		interactionTrack.y = barY;
		interactionTrack.size(barWidth, 2f);
		interactionFill.x = barX;
		interactionFill.y = barY;
		interactionFill.size(
				barWidth * live.interactionProgress(), 2f);
	}

	private void setInjuryIndicatorVisible(
			int index, boolean visible) {
		injuryIcons[index].visible = visible;
		injuryTimers[index].visible = visible;
	}

	private void positionInjuryIndicators(
			float rowX,
			float rowY,
			float rowWidth,
			float rowHeight,
			float iconSize) {
		float slotWidth = Math.max(1f, rowWidth / STATUS_COUNT);
		float gap = Math.max(1f, iconSize * 0.12f);
		for (int index = 0; index < STATUS_COUNT; index++) {
			float slotX = rowX + slotWidth * index;
			Image icon = injuryIcons[index];
			float currentWidth = Math.max(1f, icon.width());
			float iconScale = icon.scale.x
					* iconSize / currentWidth;
			icon.scale.set(iconScale);
			icon.x = slotX;
			icon.y = rowY
					+ Math.max(0f, (rowHeight - iconSize) * 0.5f);

			RenderedTextBlock timer = injuryTimers[index];
			timer.maxWidth((int)Math.max(
					1f, slotWidth - iconSize - gap));
			timer.setPos(
					slotX + iconSize + gap,
					rowY + Math.max(
							0f,
							(rowHeight - timer.height()) * 0.5f));
		}
	}

	private void positionReloadRing(
			float ringX,
			float ringY,
			float ringSize) {
		float thickness = Math.max(1f, ringSize * 0.12f);
		float gap = Math.max(1f, ringSize * 0.06f);
		float segmentLength = Math.max(
				1f, (ringSize - thickness * 2f - gap) * 0.5f);
		float second = thickness + segmentLength + gap;

		// Clockwise from the upper-left segment.
		positionSegment(0, ringX + thickness, ringY,
				segmentLength, thickness);
		positionSegment(1, ringX + second, ringY,
				segmentLength, thickness);
		positionSegment(2, ringX + ringSize - thickness,
				ringY + thickness, thickness, segmentLength);
		positionSegment(3, ringX + ringSize - thickness,
				ringY + second, thickness, segmentLength);
		positionSegment(4, ringX + second,
				ringY + ringSize - thickness, segmentLength, thickness);
		positionSegment(5, ringX + thickness,
				ringY + ringSize - thickness, segmentLength, thickness);
		positionSegment(6, ringX,
				ringY + second, thickness, segmentLength);
		positionSegment(7, ringX,
				ringY + thickness, thickness, segmentLength);

		weaponGlyph[0].x = ringX + ringSize * 0.25f;
		weaponGlyph[0].y = ringY + ringSize * 0.38f;
		weaponGlyph[0].size(ringSize * 0.45f, ringSize * 0.18f);
		weaponGlyph[1].x = ringX + ringSize * 0.70f;
		weaponGlyph[1].y = ringY + ringSize * 0.41f;
		weaponGlyph[1].size(ringSize * 0.18f, ringSize * 0.10f);
		weaponGlyph[2].x = ringX + ringSize * 0.43f;
		weaponGlyph[2].y = ringY + ringSize * 0.54f;
		weaponGlyph[2].size(ringSize * 0.15f, ringSize * 0.22f);
	}

	private void positionSegment(
			int index,
			float segmentX,
			float segmentY,
			float segmentWidth,
			float segmentHeight) {
		ColorBlock segment = reloadSegments[index];
		segment.x = segmentX;
		segment.y = segmentY;
		segment.size(segmentWidth, segmentHeight);
	}

	private ColorBlock block(int color) {
		ColorBlock result = new ColorBlock(1f, 1f, color);
		add(result);
		return result;
	}

	private Image hudIcon(
			BukovUiAssets.HudElement element, int fallbackColor) {
		Image result = BukovUiAssets.hud(element, fallbackColor);
		result.scale.set(0.5f);
		result.alpha(0.9f);
		add(result);
		return result;
	}

	private void positionHudIcon(Image icon, float iconX, float iconY) {
		icon.scale.set(0.5f * uiScale);
		icon.x = iconX;
		icon.y = iconY;
	}

	private RenderedTextBlock text(String typography, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				tokens.typographyPx(typography));
		result.hardlight(color);
		add(result);
		return result;
	}

	public static String controlHint(boolean desktop) {
		return desktop ? "TAB 背包 · 暂停" : "背包键 · 暂停";
	}

	public static String medicalHint(
			boolean desktop, boolean controller) {
		if (controller) return "方向键 · 快速医疗";
		return desktop
				? "1–4 / H · 快速医疗"
				: "医疗键 · 快速医疗";
	}

	private String currentStatusLabel() {
		return BukovHudFormat.status(
				live.bleedingPerSecond(),
				live.fractured(),
				live.painSeverity(),
				live.concussionRemaining(),
				live.stimulantRemaining());
	}

	private String currentWeaponLabel() {
		return live.reloading()
				? BukovHudFormat.reload(true, live.reloadProgress())
				: BukovHudFormat.weapon(lastWeapon, lastAutomatic);
	}

	private Firearm equippedFirearm() {
		if (hero == null || hero.belongings == null) return null;
		KindOfWeapon weapon = hero.belongings.weapon();
		return weapon instanceof Firearm ? (Firearm)weapon : null;
	}

	private int reserveAmmo(Firearm firearm) {
		if (hero == null
				|| hero.belongings == null
				|| firearmRegistry == null) {
			return 0;
		}
		FirearmDefinition definition = firearm.definition(firearmRegistry);
		int result = 0;
		for (Item item : hero.belongings) {
			if (item instanceof AmmoStack
					&& ammoRegistry.compatible(
							((AmmoStack)item).definitionId(),
							definition.caliber)) {
				result += item.quantity();
			}
		}
		return result;
	}

	private int statusKey() {
		int result = Math.round(live.bleedingPerSecond() * 10f);
		result = result * 31 + (live.fractured() ? 1 : 0);
		result = result * 31 + Math.round(live.painSeverity() * 10f);
		result = result * 31 + Math.round(live.concussionRemaining() * 10f);
		return result * 31 + Math.round(live.stimulantRemaining() * 10f);
	}

	private int interactionKey() {
		int result = live.interaction().ordinal();
		result = result * 131 + Math.round(live.interactionProgress() * 100f);
		return result * 131 + Math.round(live.interactionSeconds() * 10f);
	}

	private int extractionKey() {
		int result = live.availableExtractions();
		result = result * 31 + (live.extractionAvailable() ? 1 : 0);
		result = result * 31 + (live.extractionActive() ? 1 : 0);
		result = result * 131 + Math.round(live.extractionProgress() * 100f);
		return result * 131 + Math.round(live.extractionSeconds() * 10f);
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static boolean same(String first, String second) {
		return first == null ? second == null : first.equals(second);
	}
}
