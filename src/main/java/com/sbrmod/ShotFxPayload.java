package com.sbrmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Server -> client: draw a particle trail from one point to another.
 *
 * kind 0 steel ball (everyone sees it)
 * kind 1 plain nail          kind 2 narrow spiral
 * kind 3 wormhole spiral     kind 4 Infinite Rotation
 * kind 5 D4C slip/swap line
 * Kinds 1-5 are only sent to Stand users.
 */
public record ShotFxPayload(int kind, Vec3 from, Vec3 to) implements CustomPacketPayload {
	public static final ResourceLocation ID = SteelBallRun.id("shot_fx");
	public static final CustomPacketPayload.Type<ShotFxPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ShotFxPayload> CODEC =
		StreamCodec.ofMember(ShotFxPayload::write, ShotFxPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeDouble(from.x);
		buf.writeDouble(from.y);
		buf.writeDouble(from.z);
		buf.writeDouble(to.x);
		buf.writeDouble(to.y);
		buf.writeDouble(to.z);
	}

	private static ShotFxPayload read(RegistryFriendlyByteBuf buf) {
		int kind = buf.readVarInt();
		Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		Vec3 to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		return new ShotFxPayload(kind, from, to);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
