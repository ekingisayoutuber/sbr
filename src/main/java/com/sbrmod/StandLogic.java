package com.sbrmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All server-side Stand, Spin and ability behaviour. */
public final class StandLogic {
	public static final int ACTION_SUMMON = 1;
	public static final int ACTION_CYCLE = 2;
	public static final int ACTION_ABILITY = 3;
	public static final int ACTION_SPIN = 4;

	/** Spin XP needed to use Tusk Act 1, 2, 3, 4. */
	private static final int[] TUSK_UNLOCK = {0, 15, 40, 80};
	private static final int XP_CAP = 300;

	private static final Map<String, Long> COOLDOWNS = new HashMap<>();

	private StandLogic() {
	}

	// ------------------------------------------------------------------ data

	public static StandData get(Player player) {
		return player.getAttachedOrElse(ModAttachments.STAND, StandData.EMPTY);
	}

	public static void set(Player player, StandData data) {
		player.setAttached(ModAttachments.STAND, data);
		if (player instanceof ServerPlayer sp) {
			sync(sp);
		}
	}

	/** Send a player their own Stand data. Nobody else ever receives it. */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new StandSyncPayload(get(player)));
	}

	public static boolean isStandUser(Player player) {
		return get(player).stand() != StandData.NONE;
	}

	public static void msg(ServerPlayer player, String text) {
		player.sendSystemMessage(Component.literal(text));
	}

	private static void addXp(ServerPlayer player, int amount) {
		StandData d = get(player);
		if (d.spinXp() >= XP_CAP) {
			return;
		}
		set(player, d.withSpinXp(Math.min(XP_CAP, d.spinXp() + amount)));
	}

	private static boolean ready(ServerPlayer player, String key, int ticks) {
		long now = ((ServerLevel) player.level()).getGameTime();
		String id = player.getUUID() + key;
		Long next = COOLDOWNS.get(id);
		if (next != null && now < next) {
			return false;
		}
		COOLDOWNS.put(id, now + ticks);
		return true;
	}

	// --------------------------------------------------------------- network

	public static void handleAction(ServerPlayer player, int action) {
		switch (action) {
			case ACTION_SUMMON -> toggleSummon(player);
			case ACTION_CYCLE -> cycle(player);
			case ACTION_ABILITY -> useAbility(player);
			case ACTION_SPIN -> spin(player);
			default -> {
			}
		}
	}

	// --------------------------------------------------------------- granting

	/** Called by the Stand Arrow. Returns false if the player already has a Stand. */
	public static boolean grantRandomStand(ServerPlayer player) {
		StandData d = get(player);
		if (d.stand() != StandData.NONE) {
			return false;
		}
		int pick = ((ServerLevel) player.level()).getRandom().nextInt(2) == 0 ? StandData.TUSK : StandData.D4C;
		set(player, d.withStand(pick));
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
		msg(player, "The arrow pierces you... a Stand awakens: " + (pick == StandData.TUSK ? "TUSK" : "D4C"));
		msg(player, "Only Stand users can see it. Press R to summon.");
		return true;
	}

	// ----------------------------------------------------------- summon/cycle

	private static void toggleSummon(ServerPlayer player) {
		StandData d = get(player);
		if (d.stand() == StandData.NONE) {
			msg(player, "You have no Stand. Use a Stand Arrow.");
			return;
		}
		boolean now = !d.summoned();
		set(player, d.withSummoned(now));
		msg(player, now ? standName(d) + " appears." : standName(d) + " fades.");
	}

	private static boolean tuskUnlocked(StandData d, int act) {
		return d.spinXp() >= TUSK_UNLOCK[act - 1];
	}

	private static void cycle(ServerPlayer player) {
		StandData d = get(player);
		if (d.stand() == StandData.NONE) {
			return;
		}
		if (d.stand() == StandData.TUSK) {
			int next = d.act();
			boolean found = false;
			for (int i = 0; i < 4; i++) {
				next = next % 4 + 1;
				if (tuskUnlocked(d, next)) {
					found = true;
					break;
				}
			}
			if (!found || next == d.act()) {
				msg(player, "No other Act unlocked yet. Train your Spin (B with a Steel Ball).");
				return;
			}
			d = d.withAct(next);
		} else {
			d = d.withAct(d.act() % 3 + 1);
		}
		set(player, d);
		msg(player, abilityName(d));
	}

	private static String standName(StandData d) {
		return d.stand() == StandData.TUSK ? "Tusk" : "D4C";
	}

	private static String abilityName(StandData d) {
		if (d.stand() == StandData.TUSK) {
			return switch (d.act()) {
				case 1 -> "Tusk ACT 1: Nail Shot";
				case 2 -> "Tusk ACT 2: Spiral Burst";
				case 3 -> "Tusk ACT 3: Wormhole Nail";
				default -> "Tusk ACT 4: Infinite Rotation";
			};
		}
		return switch (d.act()) {
			case 1 -> "D4C: Dimension Slip";
			case 2 -> "D4C: Love Train";
			default -> "D4C: Parallel Swap";
		};
	}

	// ------------------------------------------------------------- abilities

	private static void useAbility(ServerPlayer player) {
		StandData d = get(player);
		if (d.stand() == StandData.NONE) {
			msg(player, "You have no Stand.");
			return;
		}
		if (!d.summoned()) {
			msg(player, "Summon your Stand first (R).");
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		if (d.stand() == StandData.TUSK) {
			tusk(player, level, d);
		} else {
			d4c(player, level, d);
		}
	}

	private static void tusk(ServerPlayer p, ServerLevel level, StandData d) {
		int lvl = d.spinLevel();
		switch (d.act()) {
			case 1 -> {
				if (!ready(p, "tusk", 8)) {
					return;
				}
				Ray r = ray(level, p, 24, false);
				sendShot(level, 1, r.from, r.to, false);
				if (!r.hits.isEmpty() && damage(level, p, r.hits.get(0), 3.0f + 0.4f * lvl)) {
					addXp(p, 1);
				}
			}
			case 2 -> {
				if (!ready(p, "tusk", 20)) {
					return;
				}
				Ray r = ray(level, p, 28, false);
				sendShot(level, 2, r.from, r.to, false);
				for (LivingEntity target : r.hits) {
					if (damage(level, p, target, 5.0f + 0.5f * lvl)) {
						addXp(p, 1);
					}
				}
			}
			case 3 -> {
				if (!ready(p, "tusk", 40)) {
					return;
				}
				Ray r = ray(level, p, 32, true);
				sendShot(level, 3, r.from, r.to, false);
				if (!r.hits.isEmpty()) {
					LivingEntity target = r.hits.get(0);
					if (damage(level, p, target, 6.0f + 0.6f * lvl)) {
						target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
						addXp(p, 2);
					}
				}
			}
			default -> {
				if (d.charge() < 100) {
					msg(p, "Infinite Rotation needs a full Spin charge (100). Press B holding a Steel Ball.");
					return;
				}
				if (!ready(p, "tusk", 100)) {
					return;
				}
				set(p, get(p).withCharge(0));
				Ray r = ray(level, p, 40, true);
				sendShot(level, 4, r.from, r.to, false);
				if (!r.hits.isEmpty()) {
					LivingEntity target = r.hits.get(0);
					if (damage(level, p, target, 16.0f + lvl)) {
						target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
						target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
						target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
						addXp(p, 4);
					}
				}
			}
		}
	}

	private static void d4c(ServerPlayer p, ServerLevel level, StandData d) {
		switch (d.act()) {
			case 1 -> dimensionSlip(p, level);
			case 2 -> {
				if (!ready(p, "d4c", 400)) {
					return;
				}
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3));
				p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
				msg(p, "Love Train: misfortune bends around you.");
			}
			default -> parallelSwap(p, level);
		}
	}

	private static void dimensionSlip(ServerPlayer p, ServerLevel level) {
		if (!ready(p, "d4c", 40)) {
			return;
		}
		Vec3 look = p.getLookAngle();
		Vec3 start = p.position();
		for (int dist = 10; dist >= 2; dist--) {
			Vec3 delta = look.scale(dist);
			AABB box = p.getBoundingBox().move(delta);
			if (level.noCollision(p, box)) {
				Vec3 dest = start.add(delta);
				sendShot(level, 5, start.add(0, 1, 0), dest.add(0, 1, 0), false);
				moveTo(p, dest);
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
				p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
				return;
			}
		}
		msg(p, "No room to slip through.");
	}

	private static void parallelSwap(ServerPlayer p, ServerLevel level) {
		if (!ready(p, "d4c", 60)) {
			return;
		}
		Ray r = ray(level, p, 16, true);
		if (r.hits.isEmpty()) {
			msg(p, "Nothing to swap with.");
			return;
		}
		LivingEntity target = r.hits.get(0);
		Vec3 mine = p.position();
		Vec3 theirs = target.position();
		sendShot(level, 5, mine.add(0, 1, 0), theirs.add(0, 1, 0), false);
		moveTo(p, theirs);
		moveTo(target, mine);
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
	}

	private static void moveTo(Entity entity, Vec3 pos) {
		if (entity instanceof ServerPlayer sp) {
			sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
		} else {
			entity.setPos(pos.x, pos.y, pos.z);
		}
	}

	// ------------------------------------------------------------------ spin

	private static boolean holdingBall(ServerPlayer p) {
		return p.getMainHandItem().is(ModItems.STEEL_BALL) || p.getOffhandItem().is(ModItems.STEEL_BALL);
	}

	private static void spin(ServerPlayer p) {
		if (!holdingBall(p)) {
			msg(p, "Hold a Steel Ball to use Spin.");
			return;
		}
		StandData d = get(p);
		if (p.isShiftKeyDown()) {
			// Spin Heal
			if (d.charge() < 40) {
				msg(p, "Spin Heal needs 40 charge.");
				return;
			}
			p.heal(4.0f + 0.5f * d.spinLevel());
			set(p, d.withCharge(d.charge() - 40));
		} else if (p.isSprinting()) {
			// Spin Dash
			if (d.charge() < 30) {
				msg(p, "Spin Dash needs 30 charge.");
				return;
			}
			Vec3 look = p.getLookAngle();
			p.setDeltaMovement(look.x * 1.6, Math.max(look.y * 1.2, 0.25), look.z * 1.6);
			p.connection.send(new ClientboundSetEntityMotionPacket(p));
			p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0));
			set(p, d.withCharge(d.charge() - 30));
		} else {
			// Build charge
			int before = d.charge();
			StandData after = d.withCharge(before + 12).withSpinXp(Math.min(XP_CAP, d.spinXp() + 1));
			set(p, after);
			if (before < 100 && after.charge() >= 100) {
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
				p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
				msg(p, "Golden Rotation!");
			}
		}
	}

	/** Steel Ball thrown (right-click). Works for anyone; scales with Spin. */
	public static void throwBall(ServerPlayer p) {
		if (!ready(p, "ball", 15)) {
			return;
		}
		ServerLevel level = (ServerLevel) p.level();
		StandData d = get(p);
		Ray r = ray(level, p, 30, false);
		sendShot(level, 0, r.from, r.to, true);
		if (!r.hits.isEmpty()) {
			float dmg = 4.0f + 0.6f * d.spinLevel() + d.charge() * 0.06f;
			if (damage(level, p, r.hits.get(0), dmg)) {
				addXp(p, 1);
			}
		}
		set(p, get(p).withCharge(get(p).charge() - 20));
	}

	// -------------------------------------------------------------- raycasts

	public record Ray(Vec3 from, Vec3 to, List<LivingEntity> hits) {
	}

	private static Ray ray(ServerLevel level, ServerPlayer p, double range, boolean pierceBlocks) {
		Vec3 from = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		Vec3 to = from.add(look.scale(range));
		if (!pierceBlocks) {
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			if (block.getType() != HitResult.Type.MISS) {
				to = block.getLocation();
			}
		}
		final Vec3 end = to;
		List<Entity> candidates = level.getEntities(p, new AABB(from, end).inflate(1.0),
			e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator());
		List<LivingEntity> hits = new ArrayList<>();
		for (Entity e : candidates) {
			Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(from, end);
			if (hit.isPresent()) {
				hits.add((LivingEntity) e);
			}
		}
		hits.sort(Comparator.comparingDouble(e -> from.distanceToSqr(e.position())));
		return new Ray(from, end, hits);
	}

	private static boolean damage(ServerLevel level, ServerPlayer p, LivingEntity target, float amount) {
		return target.hurt(level.damageSources().playerAttack(p), amount);
	}

	// -------------------------------------------------------------------- fx

	private static void sendShot(ServerLevel level, int kind, Vec3 from, Vec3 to, boolean everyone) {
		ShotFxPayload payload = new ShotFxPayload(kind, from, to);
		for (ServerPlayer sp : level.players()) {
			if (sp.distanceToSqr(from) > 4096 && sp.distanceToSqr(to) > 4096) {
				continue;
			}
			if (!everyone && !isStandUser(sp)) {
				continue;
			}
			ServerPlayNetworking.send(sp, payload);
		}
	}

	/** Send a Stand silhouette to every Stand user near this point. */
	public static void sendBody(ServerLevel level, int kind, int act, double x, double y, double z, float yaw) {
		StandFxPayload payload = new StandFxPayload(kind, act, x, y, z, yaw);
		for (ServerPlayer sp : level.players()) {
			if (sp.distanceToSqr(x, y, z) > 4096) {
				continue;
			}
			if (!isStandUser(sp)) {
				continue; // <- this check is what hides Stands from non-users
			}
			ServerPlayNetworking.send(sp, payload);
		}
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			StandData d = get(p);
			if (d.charge() > 0 && t % 20 == 0) {
				set(p, d.withCharge(d.charge() - 1));
			}
			if (d.stand() != StandData.NONE && d.summoned() && t % 4 == 0) {
				double yaw = Math.toRadians(p.getYRot());
				double fx = -Math.sin(yaw);
				double fz = Math.cos(yaw);
				double rx = -Math.cos(yaw);
				double rz = -Math.sin(yaw);
				double x = p.getX() + rx * 0.9 - fx * 0.6;
				double z = p.getZ() + rz * 0.9 - fz * 0.6;
				double y = p.getY() + 0.2 + Math.sin(t * 0.1) * 0.1;
				sendBody((ServerLevel) p.level(), d.stand(), d.act(), x, y, z, p.getYRot());
			}
		}
		Monsters.tick(server, t);
	}
}
