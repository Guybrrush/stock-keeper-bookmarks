package com.stockkeeperbookmarks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.stockkeeperbookmarks.StockKeeperBookmarks;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * The pin action, reachable from the keyboard as well as from the footer button.
 *
 * The + button is the primary control, but its artwork is spliced into Create's {@code renderBg}
 * at the most fragile injection point this mod has, and that injection is declared optional so a
 * Create update drops the button instead of crashing the game. Pinning is the only way to create
 * a bookmark — a fresh keeper starts empty — so it needs a route that does not depend on Create's
 * internal layout at all. This is that route.
 */
@EventBusSubscriber(modid = StockKeeperBookmarks.ID, value = Dist.CLIENT)
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
