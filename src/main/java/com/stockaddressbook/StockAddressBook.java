package com.stockaddressbook;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Client-only Create addon. The Stock Keeper's address field normally carries over whatever
 * was typed into it last (it lives on the block entity, not the player), which makes it easy
 * to fire an order at the wrong destination. This replaces that flow with a column of
 * one-click destination buttons and stops the field from inheriting a stale value.
 */
@Mod(value = StockAddressBook.ID, dist = Dist.CLIENT)
public class StockAddressBook {

	public static final String ID = "stockaddressbook";

	public StockAddressBook(ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, AddressBookConfig.SPEC);
	}
}
