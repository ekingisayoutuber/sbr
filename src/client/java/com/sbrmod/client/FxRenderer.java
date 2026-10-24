package com.sbrmod.client;

import com.sbrmod.ShotFxPayload;
import com.sbrmod.StandData;
import com.sbrmod.StandFxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Draws Stands and Stand attacks out of particles.
 *
 * The server only sends Stand effects to Stand users, and this class checks
 * again on the client, so Stands are invisible to everyone else. Using
 * particles also means there are no custom models or textures to supply.
 */
final class FxRenderer {
	private FxRenderer() {
	}

	private static boolean localPlayerIsStandUser(Minecraft client) {
		return client.player != null && ClientStandState.get().stand() != StandData.NONE;
	}

	static void drawStand(Minecraft client, StandFxPayload p) {
		ClientLevel level = client.level;
		if (level == null || !localPlayerIsStandUser(client)) {
			return;
		}
		ParticleOptions type = switch (p.kind()) {
			case StandData.TUSK -> p.act() >= 4 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD;
			case StandData.D4C -> ParticleTypes.ELECTRIC_SPARK;
			default -> ParticleTypes.SOUL_FIRE_FLAME; // hostile phantom
		};

		double yaw = Math.toRadians(p.yaw());
		double rx = -Math.cos(yaw);
		double rz = -Math.sin(yaw);

		// legs + torso column
		for (double h = 0.0; h <= 1.4; h += 0.35) {
			level.addParticle(type, p.x(), p.y() + h, p.z(), 0, 0, 0);
		}
		// shoulders / arms
		for (double s = -0.35; s <= 0.35; s += 0.35) {
			level.addParticle(type, p.x() + rx * s, p.y() + 1.25, p.z() + rz * s, 0, 0, 0);
		}
		// head
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2;
			level.addParticle(type, p.x() + Math.cos(a) * 0.17, p.y() + 1.75, p.z() + Math.sin(a) * 0.17, 0, 0, 0);
		}
	}

	static void drawShot(Minecraft client, ShotFxPayload p) {
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		// Everything except the steel ball (kind 0) is Stand-only.
		if (p.kind() != 0 && !localPlayerIsStandUser(client)) {
			return;
		}

		ParticleOptions line = switch (p.kind()) {
			case 0 -> ParticleTypes.CRIT;
			case 5 -> ParticleTypes.ELECTRIC_SPARK;
			default -> ParticleTypes.END_ROD;
		};
		ParticleOptions spiral = switch (p.kind()) {
			case 3, 4 -> ParticleTypes.SOUL_FIRE_FLAME;
			default -> ParticleTypes.END_ROD;
		};
		double radius = switch (p.kind()) {
			case 2 -> 0.25;
			case 3 -> 0.4;
			case 4 -> 0.8;
			default -> 0.0;
		};

		Vec3 dir = p.to().subtract(p.from());
		double length = dir.length();
		if (length < 0.01) {
			return;
		}
		Vec3 n = dir.scale(1.0 / length);
		// two vectors perpendicular to the path, for spirals
		Vec3 up = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 u = n.cross(up).normalize();
		Vec3 v = n.cross(u).normalize();

		double step = 0.5;
		int i = 0;
		for (double d = 0; d <= length; d += step, i++) {
			Vec3 c = p.from().add(n.scale(d));
			level.addParticle(line, c.x, c.y, c.z, 0, 0, 0);
			if (radius > 0) {
				double a = i * 0.9;
				double ox = (u.x * Math.cos(a) + v.x * Math.sin(a)) * radius;
				double oy = (u.y * Math.cos(a) + v.y * Math.sin(a)) * radius;
				double oz = (u.z * Math.cos(a) + v.z * Math.sin(a)) * radius;
				level.addParticle(spiral, c.x + ox, c.y + oy, c.z + oz, 0, 0, 0);
			}
		}
	}
}
