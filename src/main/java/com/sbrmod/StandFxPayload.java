package com.sbrmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: draw a Stand silhouette here.
 * Only ever sent to players who own a Stand, which is what makes Stands
 * invisible to everyone else.
 *
 * kind: 1 Tusk, 2 D4C, 3 hostile phantom Stand
 */
public record StandFxPayload(int kind, int act, double x, double y, double z, float yaw) implements CustomPacketPayload {
	public static final ResourceLocation ID = SteelBallRun.id("stand_fx");
	public static final CustomPacketPayload.Type<StandFxPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, StandFxPayload> CODEC =
		StreamCodec.ofMember(StandFxPayload::write, StandFxPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeVarInt(act);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
	}

	private static StandFxPayload read(RegistryFriendlyByteBuf buf) {
		return new StandFxPayload(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
