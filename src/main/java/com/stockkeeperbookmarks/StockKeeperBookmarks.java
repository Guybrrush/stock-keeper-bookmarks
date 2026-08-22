/*
 * Create: Stock Keeper Bookmarks
 * Copyright (C) 2026  Guybrrush
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.stockkeeperbookmarks;

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
@Mod(value = StockKeeperBookmarks.ID, dist = Dist.CLIENT)
public class StockKeeperBookmarks {

	public static final String ID = "stockkeeperbookmarks";

	public StockKeeperBookmarks(ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, AddressBookConfig.SPEC);
	}
}
