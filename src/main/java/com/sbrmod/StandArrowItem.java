package com.sbrmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click to be pierced by the arrow. Grants a random Stand if you have none. */
public class StandArrowItem extends Item {
	public StandArrowItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.success(stack);
		}
		if (StandLogic.grantRandomStand(serverPlayer)) {
			stack.shrink(1);
			return InteractionResultHolder.success(stack);
		}
		StandLogic.msg(serverPlayer, "Your soul already holds a Stand.");
		return InteractionResultHolder.fail(stack);
	}
}
