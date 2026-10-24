package com.sbrmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sbrmod.ActionPayload;
import com.sbrmod.ShotFxPayload;
import com.sbrmod.StandData;
import com.sbrmod.StandFxPayload;
import com.sbrmod.StandLogic;
import com.sbrmod.StandSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SteelBallRunClient implements ClientModInitializer {
	private static final String CATEGORY = "key.categories.steelballrun";

	private static KeyMapping summonKey;
	private static KeyMapping cycleKey;
	private static KeyMapping abilityKey;
	private static KeyMapping spinKey;

	@Override
	public void onInitializeClient() {
		summonKey = register("summon", InputConstants.KEY_R);
		cycleKey = register("cycle", InputConstants.KEY_G);
		abilityKey = register("ability", InputConstants.KEY_V);
		spinKey = register("spin", InputConstants.KEY_B);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			poll(summonKey, StandLogic.ACTION_SUMMON);
			poll(cycleKey, StandLogic.ACTION_CYCLE);
			poll(abilityKey, StandLogic.ACTION_ABILITY);
			poll(spinKey, StandLogic.ACTION_SPIN);
		});

		ClientPlayNetworking.registerGlobalReceiver(StandSyncPayload.TYPE,
			(payload, context) -> ClientStandState.set(payload.data()));
		ClientPlayNetworking.registerGlobalReceiver(StandFxPayload.TYPE,
			(payload, context) -> FxRenderer.drawStand(context.client(), payload));
		ClientPlayNetworking.registerGlobalReceiver(ShotFxPayload.TYPE,
			(payload, context) -> FxRenderer.drawShot(context.client(), payload));

		// Forget any Stand data when leaving a world.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientStandState.set(StandData.EMPTY));

		HudRenderCallback.EVENT.register(SteelBallRunClient::renderHud);
	}

	private static KeyMapping register(String name, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.steelballrun." + name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
	}

	private static void poll(KeyMapping key, int action) {
		while (key.consumeClick()) {
			ClientPlayNetworking.send(new ActionPayload(action));
		}
	}

	/** Bottom-left HUD: Stand box, ability pips, Spin charge bar, Spin mastery bar. */
	private static void renderHud(GuiGraphics g, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}
		StandData d = ClientStandState.get();
		if (d.stand() == StandData.NONE && d.charge() == 0 && d.spinXp() == 0) {
			return;
		}
		int h = mc.getWindow().getGuiScaledHeight();
		int x = 8;
		int y = h - 52;

		if (d.stand() != StandData.NONE) {
			int color = d.stand() == StandData.TUSK ? 0xFFE8C14A : 0xFF4AA3E8;
			g.fill(x, y, x + 12, y + 12, 0xFFFFFFFF);
			g.fill(x + 1, y + 1, x + 11, y + 11, 0xFF101010);
			if (d.summoned()) {
				g.fill(x + 2, y + 2, x + 10, y + 10, color);
			}
			for (int i = 0; i < d.act(); i++) {
				g.fill(x + 18 + i * 7, y + 4, x + 22 + i * 7, y + 8, color);
			}
		}

		int barW = 80;
		g.fill(x, y + 16, x + barW, y + 21, 0x99000000);
		g.fill(x, y + 16, x + barW * d.charge() / 100, y + 21, 0xFFFFC107);
		int mastery = Math.min(barW, barW * d.spinXp() / 80);
		g.fill(x, y + 23, x + barW, y + 26, 0x99000000);
		g.fill(x, y + 23, x + mastery, y + 26, 0xFF26C6DA);
	}
}
