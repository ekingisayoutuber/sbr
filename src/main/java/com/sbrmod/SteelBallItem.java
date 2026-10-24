package com.sbrmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Right-click to hurl a rotating steel ball. Damage grows with your Spin level
 * and current Spin charge. Hold it and press B to build charge, heal or dash.
 */
public class SteelBallItem extends Item {
	public SteelBallItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			StandLogic.throwBall(serverPlayer);
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}
}
