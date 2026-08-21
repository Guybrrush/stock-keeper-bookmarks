package com.stockkeeperbookmarks;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AddressBookConfig {

	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<List<? extends String>> ADDRESSES;
	public static final ModConfigSpec.BooleanValue CLICK_TO_SEND;
	public static final ModConfigSpec.BooleanValue CLEAR_ADDRESS_ON_OPEN;
	public static final ModConfigSpec.IntValue BUTTON_WIDTH;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder.comment("Create: Stock Address Book — client settings").push("addressBook");

		ADDRESSES = builder
			.comment("Starting destinations for a Stock Keeper you have not customised yet.",
				"Bookmarks are per Stock Keeper: the first edit to one copies this list into",
				"config/stockkeeperbookmarks-bookmarks.json under that keeper's own entry, and from",
				"then on that keeper is edited there, not here.",
				"Editable in-game: type an address in the field and click the + button to pin it,",
				"right-click a button to remove it, drag buttons to reorder them.")
			.defineListAllowEmpty("addresses",
				new ArrayList<>(List.of("workshop", "crafter", "smelter")),
				() -> "workshop",
				o -> o instanceof String s && !s.isBlank());

		CLICK_TO_SEND = builder
			.comment("true: clicking a destination button sends the order immediately.",
				"false: clicking only selects the destination, you still press Send.")
			.define("clickToSend", true);

		CLEAR_ADDRESS_ON_OPEN = builder
			.comment("Clear the address field each time the Stock Keeper is opened, so it never",
				"inherits the last address used. This is the main safeguard — turning it off",
				"restores Create's default sticky behaviour.")
			.define("clearAddressOnOpen", true);

		BUTTON_WIDTH = builder
			.comment("Width in pixels of the destination buttons.")
			.defineInRange("buttonWidth", 72, 40, 200);

		builder.pop();
		SPEC = builder.build();
	}

	/** Defaults for a Stock Keeper with no saved bookmarks; see {@link BookmarkStore}. */
	public static List<String> addresses() {
		return new ArrayList<>(ADDRESSES.get());
	}
}
