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
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
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

	private final BukovRaidHudState live = new BukovRaidHudState();

	private BukovUiTokens tokens;
	private int primaryColor;
	private int secondaryColor;
	private int interactColor;
	private int valuableColor;
	private int dangerColor;
	private int extractColor;
	private ColorBlock background;
	private ColorBlock topEdge;
	private ColorBlock healthTrack;
	private ColorBlock healthFill;
	private ColorBlock dangerFill;
	private ColorBlock healthFlash;
	private ColorBlock[] healthSeparators;
	private ColorBlock armorEdge;
	private ColorBlock reloadTrack;
	private ColorBlock reloadFill;
	private ColorBlock interactionTrack;
	private ColorBlock interactionFill;
	private ColorBlock bossTrack;
	private ColorBlock bossFill;
	private ColorBlock navigationBadge;
	private ColorBlock threatBadge;
	private ColorBlock interactionBadge;
	private ColorBlock[] reticle;
	private BukovHitDirectionArc[] hitDirectionArcs;

	private RenderedTextBlock healthText;
	private RenderedTextBlock armorText;
	private RenderedTextBlock statusText;
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
	private int lastInteractionKey = Integer.MIN_VALUE;
	private String lastInteractionLabel;
	private BukovRaidHudState.Interaction lastInteractionType;
	private int lastExtractionKey = Integer.MIN_VALUE;
	private String lastExtractionId;
	private int lastReloadBucket = Integer.MIN_VALUE;
	private float healthFraction;
	private float uiSeconds;
	private float healthFlashRemaining;
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
		reloadTrack = block(tokens.colorWithAlpha("panel.surface", 255));
		reloadFill = block(tokens.colorWithAlpha("accent.valuable", 255));
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
		reticle = new ColorBlock[5];
		for (int index = 0; index < reticle.length; index++) {
			reticle[index] =
					block(tokens.colorWithAlpha("accent.interact", 255));
		}

		healthText = text(8, primaryColor);
		armorText = text(6, valuableColor);
		statusText = text(6, secondaryColor);
		ammoText = text(10, valuableColor);
		weaponText = text(6, secondaryColor);
		objectiveText = text(7, primaryColor);
		objectiveText.align(RenderedTextBlock.CENTER_ALIGN);
		extractionText = text(6, extractColor);
		extractionText.align(RenderedTextBlock.CENTER_ALIGN);
		interactionText = text(7, interactColor);
		interactionText.align(RenderedTextBlock.CENTER_ALIGN);
		timerText = text(6, secondaryColor);
		timerText.align(RenderedTextBlock.RIGHT_ALIGN);
		soundText = text(6, valuableColor);
		soundText.align(RenderedTextBlock.CENTER_ALIGN);
		hitText = text(7, dangerColor);
		bossText = text(7, dangerColor);
		bossText.align(RenderedTextBlock.CENTER_ALIGN);
		bossObjectiveText = text(6, valuableColor);
		bossObjectiveText.align(RenderedTextBlock.CENTER_ALIGN);
		navigationText = text(7, primaryColor);
		navigationText.align(RenderedTextBlock.CENTER_ALIGN);
		threatText = text(7, dangerColor);
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
		refresh();
	}

	public void refresh() {
		applyUiScale(SPDSettings.bukovUiScale());
		if (hudSource != null) {
			hudSource.readRaidHudState(live);
		}
		refreshVitals();
		refreshFirepower();
		refreshMissionAndInteraction();
		refreshCombatAwareness();
		refreshAnimationState();
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
		}
		boolean injured = live.bleedingPerSecond() > 0f
				|| live.fractured()
				|| live.concussionRemaining() > 0f;
		statusText.hardlight(
				injured ? dangerColor : secondaryColor);
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

	private void refreshAnimationState() {
		boolean lowHealth = healthFraction <= 0.30f;
		healthFill.visible = !lowHealth;
		dangerFill.visible = lowHealth;
		healthFlash.visible = healthFlashRemaining > 0f;
		healthFlash.alpha(Math.min(1f, healthFlashRemaining / 0.07f));
		if (lowHealth) {
			dangerFill.alpha(0.58f + 0.42f
					* Math.abs((uiSeconds % 1f) * 2f - 1f));
		}

		boolean lowAmmo = lastCapacity > 0
				&& lastMagazine * 4 <= lastCapacity;
		boolean blinkOn = ((int)Math.floor(uiSeconds * 2f) & 1) == 0;
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
		reloadTrack.visible = live.reloading();
		reloadFill.visible = live.reloading();
		reloadTrack.alpha(awarenessAlpha);
		reloadFill.alpha(awarenessAlpha);
		interactionTrack.visible = live.interactionProgress() > 0f;
		interactionFill.visible = interactionTrack.visible;
		interactionBadge.visible =
				live.interaction() != BukovRaidHudState.Interaction.NONE;
		interactionText.hardlight(
				live.interaction() == BukovRaidHudState.Interaction.LOCKED
						? dangerColor : interactColor);
		extractionText.hardlight(
				live.extractionId() != null && !live.extractionAvailable()
						? dangerColor : extractColor);
		soundText.visible = live.soundVisible();
		soundText.alpha(awarenessAlpha * Math.max(
				0.35f,
				Math.min(1f,
						live.soundStrength()
								* live.soundRemainingSeconds() / 0.9f)));
		hitText.visible = live.hitVisible();
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
		bossFill.hardlight(
				live.bossVulnerable() ? valuableColor : dangerColor);
		navigationText.visible = live.navigationVisible();
		navigationBadge.visible = live.navigationVisible();
		navigationText.alpha(awarenessAlpha);
		navigationBadge.alpha(awarenessAlpha);
		navigationText.hardlight(
				live.navigationAvailable() ? primaryColor : dangerColor);
		threatText.visible = live.threatVisible();
		threatBadge.visible = live.threatVisible();
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
		if (background == null) return;
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

		healthText.setPos(x + PADDING, y + 3f);
		healthTrack.x = x + PADDING;
		healthTrack.y = y + 14f;
		healthTrack.size(leftWidth - PADDING * 2f, 3f);
		positionHealthFill(
				healthTrack.x, healthTrack.y, healthTrack.width(), 3f);
		armorEdge.x = x + PADDING;
		armorEdge.y = y + 20f;
		armorEdge.size(2f, 5f);
		armorText.setPos(x + PADDING + 4f, y + 19f);
		statusText.maxWidth((int)(leftWidth - PADDING * 2f));
		statusText.setPos(x + PADDING, y + 28f);

		objectiveText.maxWidth((int)centerWidth);
		objectiveText.setPos(centerLeft, y + 3f);
		extractionText.maxWidth((int)centerWidth);
		extractionText.setPos(centerLeft, y + 14f);
		interactionText.maxWidth((int)centerWidth);
		interactionText.setPos(centerLeft, y + 25f);
		positionInteractionBar(centerLeft, y + actualHeight - 2f, centerWidth);

		float rightX = x + width - rightWidth;
		ammoText.setPos(rightX, y + 2f);
		weaponText.maxWidth((int)(rightWidth - PADDING));
		weaponText.setPos(rightX, y + 16f);
		reloadTrack.x = rightX;
		reloadTrack.y = y + 26f;
		reloadTrack.size(rightWidth - PADDING, 2f);
		reloadFill.x = rightX;
		reloadFill.y = reloadTrack.y;
		reloadFill.size(
				(rightWidth - PADDING) * live.reloadProgress(), 2f);
		timerText.maxWidth((int)(rightWidth - PADDING));
		timerText.setPos(rightX, y + 24f);
		positionBoss(centerLeft, y + actualHeight + 2f, centerWidth);
	}

	private void layoutCompact(float actualHeight) {
		BukovRaidHudLayout hudLayout =
				BukovRaidHudLayout.calculate(width, uiScaleLevel);
		BukovRaidHudLayout.Rect vitals = hudLayout.vitals;
		BukovRaidHudLayout.Rect firepower = hudLayout.firepower;
		float scaledPadding = PADDING * uiScale;

		healthText.setPos(x + vitals.x, y + vitals.y);
		healthTrack.x = x + vitals.x;
		healthTrack.y = y + vitals.y + 12f * uiScale;
		healthTrack.size(vitals.width, 3f * uiScale);
		positionHealthFill(
				healthTrack.x,
				healthTrack.y,
				vitals.width,
				3f * uiScale);
		armorEdge.x = x + vitals.x;
		armorEdge.y = y + vitals.y + 18f * uiScale;
		armorEdge.size(2f * uiScale, 5f * uiScale);
		armorText.setPos(
				x + vitals.x + scaledPadding,
				y + vitals.y + 17f * uiScale);
		statusText.text(BukovRaidHudLayout.compactLine(
				currentStatusLabel(),
				hudLayout.condition.width,
				uiScaleLevel));
		statusText.maxWidth((int)hudLayout.condition.width);
		statusText.setPos(
				x + hudLayout.condition.x,
				y + hudLayout.condition.y);

		ammoText.setPos(x + firepower.x, y + firepower.y);
		weaponText.text(BukovRaidHudLayout.compactLine(
				currentWeaponLabel(),
				firepower.width,
				uiScaleLevel));
		weaponText.maxWidth((int)firepower.width);
		weaponText.setPos(
				x + firepower.x,
				y + firepower.y + 14f * uiScale);
		reloadTrack.x = x + firepower.x;
		reloadTrack.y = y + firepower.y + 24f * uiScale;
		reloadTrack.size(firepower.width, 2f * uiScale);
		reloadFill.y = reloadTrack.y;
		reloadFill.x = reloadTrack.x;
		reloadFill.size(
				firepower.width * live.reloadProgress(),
				2f * uiScale);
		timerText.maxWidth((int)hudLayout.clock.width);
		timerText.setPos(
				x + hudLayout.clock.x,
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
		objectiveText.maxWidth((int)hudLayout.objective.width);
		objectiveText.setPos(
				x + hudLayout.objective.x,
				y + hudLayout.objective.y);
		positionInteractionBar(
				x + scaledPadding,
				y + actualHeight - 2f * uiScale,
				width - scaledPadding * 2f);
		positionBoss(
				x + scaledPadding,
				y + actualHeight + 2f,
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

		/*
		 * Direction text already carries an arrow. Keep these badges in a
		 * shallow edge rail rather than placing 94-112 world-unit slabs over
		 * the direction they describe, where they can hide the actual enemy.
		 */
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
		interactionBadge.x = feedbackX;
		interactionBadge.y = centerY + aimRadius + 11f;
		interactionBadge.size(feedbackWidth, 13f);
		interactionText.maxWidth((int)feedbackWidth);
		interactionText.setPos(feedbackX, centerY + aimRadius + 14f);
		soundText.maxWidth((int)feedbackWidth);
		soundText.setPos(feedbackX, centerY - aimRadius - 26f);
		hitText.maxWidth((int)feedbackWidth);
		hitText.setPos(feedbackX, centerY - aimRadius - 14f);
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

	private ColorBlock block(int color) {
		ColorBlock result = new ColorBlock(1f, 1f, color);
		add(result);
		return result;
	}

	private RenderedTextBlock text(int size, int color) {
		RenderedTextBlock result = PixelScene.renderTextBlock(
				textSize(size, SPDSettings.bukovUiScale()));
		result.hardlight(color);
		add(result);
		return result;
	}

	public static int textSize(int baseSize, int scaleLevel) {
		if (baseSize <= 0) {
			throw new IllegalArgumentException("baseSize must be positive");
		}
		return baseSize + Math.max(0, Math.min(2, scaleLevel));
	}

	public static String controlHint(boolean desktop) {
		return desktop ? "TAB 背包 · 暂停" : "背包键 · 暂停";
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
