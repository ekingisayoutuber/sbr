package com.sbrmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteelBallRun implements ModInitializer {
	public static final String MOD_ID = "steelballrun";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModAttachments.initialize();

		PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StandFxPayload.TYPE, StandFxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ShotFxPayload.TYPE, ShotFxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StandSyncPayload.TYPE, StandSyncPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE,
			(payload, context) -> StandLogic.handleAction(context.player(), payload.action()));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> StandLogic.sync(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> StandLogic.sync(newPlayer));

		ServerTickEvents.END_SERVER_TICK.register(StandLogic::tick);
		ServerEntityEvents.ENTITY_LOAD.register(Monsters::onLoad);

		LOGGER.info("Steel Ball Run loaded. Stay on the road.");
	}
}
