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
package com.stockkeeperbookmarks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.stockkeeperbookmarks.StockKeeperBookmarks;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

/**
 * The pin action, reachable from the keyboard as well as from the footer button.
 *
 * The + button is the primary control, but its artwork is spliced into Create's {@code renderBg}
 * at the most fragile injection point this mod has, and that injection is declared optional so a
 * Create update drops the button instead of crashing the game. Pinning is the only way to create
 * a bookmark — a fresh keeper starts empty — so it needs a route that does not depend on Create's
 * internal layout at all. This is that route.
 */
@Mod.EventBusSubscriber(modid = StockKeeperBookmarks.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeys {

	public static final String CATEGORY = "key.categories.stockkeeperbookmarks";

	/**
	 * Defaults to the +/= key. Only read while the Stock Keeper screen is open, so it shadows
	 * nothing during normal play.
	 */
	public static final KeyMapping PIN = new KeyMapping("key.stockkeeperbookmarks.pin",
		InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);

	private ModKeys() {
	}

	@SubscribeEvent
	public static void register(RegisterKeyMappingsEvent event) {
		event.register(PIN);
	}
}
