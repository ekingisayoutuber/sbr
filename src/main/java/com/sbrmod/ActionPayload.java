package com.sbrmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: "I pressed a Stand key." */
public record ActionPayload(int action) implements CustomPacketPayload {
	public static final ResourceLocation ID = SteelBallRun.id("action");
	public static final CustomPacketPayload.Type<ActionPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.VAR_INT, ActionPayload::action, ActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
