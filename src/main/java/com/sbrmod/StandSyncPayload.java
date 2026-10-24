package com.sbrmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: "here is YOUR Stand data."
 * Only ever sent to the player it belongs to, so other players can never tell
 * who owns a Stand.
 */
public record StandSyncPayload(StandData data) implements CustomPacketPayload {
	public static final ResourceLocation ID = SteelBallRun.id("stand_sync");
	public static final CustomPacketPayload.Type<StandSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, StandSyncPayload> CODEC =
		StreamCodec.composite(StandData.STREAM_CODEC, StandSyncPayload::data, StandSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
