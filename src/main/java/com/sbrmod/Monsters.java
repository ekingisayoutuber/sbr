package com.sbrmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The scary stuff.
 *
 * Rotting Horrors: some zombies, skeletons and husks spawn as huge-health
 * monsters that fill nearby players' vision with darkness and play a heartbeat.
 * Everyone can see them.
 *
 * Phantom Stands: some spawn invisible and fast. Non-Stand users just get hit by
 * something they can't see. Stand users see a spectral silhouette.
 *
 * Built on vanilla mobs so no custom models or renderers are needed.
 */
public final class Monsters {
	private static final String TAG_CHECKED = "sbr_checked";
	private static final String TAG_HORROR = "sbr_horror";
	private static final String TAG_PHANTOM = "sbr_phantom";

	private static final float HORROR_CHANCE = 0.04f;
	private static final float PHANTOM_CHANCE = 0.03f;

	private static final List<Mob> HORRORS = new ArrayList<>();
	private static final List<Mob> PHANTOMS = new ArrayList<>();

	private Monsters() {
	}

	public static void onLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof Mob mob)) {
			return;
		}
		Set<String> tags = entity.getTags();
		if (tags.contains(TAG_HORROR)) {
			HORRORS.add(mob);
			return;
		}
		if (tags.contains(TAG_PHANTOM)) {
			PHANTOMS.add(mob);
			return;
		}
		if (tags.contains(TAG_CHECKED)) {
			return;
		}
		EntityType<?> type = entity.getType();
		if (type != EntityType.ZOMBIE && type != EntityType.SKELETON && type != EntityType.HUSK) {
			return;
		}
		entity.addTag(TAG_CHECKED);
		float roll = level.getRandom().nextFloat();
		if (roll < HORROR_CHANCE) {
			makeHorror(mob);
		} else if (roll < HORROR_CHANCE + PHANTOM_CHANCE) {
			makePhantom(mob);
		}
	}

	private static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	private static void makeHorror(Mob mob) {
		setBase(mob, Attributes.MAX_HEALTH, 60.0);
		setBase(mob, Attributes.ATTACK_DAMAGE, 9.0);
		setBase(mob, Attributes.MOVEMENT_SPEED, 0.30);
		setBase(mob, Attributes.FOLLOW_RANGE, 40.0);
		mob.setHealth(60.0f);
		mob.setCustomName(Component.literal("Rotting Horror"));
		mob.addTag(TAG_HORROR);
		HORRORS.add(mob);
	}

	private static void makePhantom(Mob mob) {
		setBase(mob, Attributes.MAX_HEALTH, 30.0);
		setBase(mob, Attributes.ATTACK_DAMAGE, 6.0);
		setBase(mob, Attributes.MOVEMENT_SPEED, 0.34);
		setBase(mob, Attributes.FOLLOW_RANGE, 40.0);
		mob.setHealth(30.0f);
		mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		mob.addTag(TAG_PHANTOM);
		PHANTOMS.add(mob);
	}

	public static void tick(MinecraftServer server, int t) {
		HORRORS.removeIf(m -> m.isRemoved() || !m.isAlive());
		PHANTOMS.removeIf(m -> m.isRemoved() || !m.isAlive());

		if (t % 20 == 0) {
			for (Mob horror : HORRORS) {
				ServerLevel level = (ServerLevel) horror.level();
				boolean someoneNear = false;
				for (ServerPlayer sp : level.players()) {
					if (sp.distanceToSqr(horror) < 400.0) {
						sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
						someoneNear = true;
					}
				}
				if (someoneNear && level.getRandom().nextInt(4) == 0) {
					level.playSound(null, horror.getX(), horror.getY(), horror.getZ(),
						SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5f, 0.6f);
				}
			}
		}

		if (t % 4 == 0) {
			for (Mob phantom : PHANTOMS) {
				StandLogic.sendBody((ServerLevel) phantom.level(), 3, 1,
					phantom.getX(), phantom.getY(), phantom.getZ(), phantom.getYRot());
			}
		}
	}
}
