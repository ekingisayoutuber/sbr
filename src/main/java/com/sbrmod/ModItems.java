package com.sbrmod;

import java.util.function.Function;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item STAND_ARROW = register("stand_arrow", StandArrowItem::new, new Item.Properties().stacksTo(16));
	public static final Item STEEL_BALL = register("steel_ball", SteelBallItem::new, new Item.Properties().stacksTo(1));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		return Registry.register(BuiltInRegistries.ITEM, SteelBallRun.id(name), factory.apply(properties));
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(STAND_ARROW);
			entries.accept(STEEL_BALL);
		});
	}
}
