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
	public static final ModConfigSpec.EnumValue<DisplayMode> DISPLAY_MODE;
	public static final ModConfigSpec.BooleanValue COLLAPSED;
	public static final ModConfigSpec.BooleanValue AUTO_FOCUS_SEARCH;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder.comment("Create: Stock Address Book — client settings").push("addressBook");

		ADDRESSES = builder
			.comment("Starting destinations for a Stock Keeper you have not customised yet.",
				"Empty by default: pin the destinations you use with the + button in-game.",
				"Bookmarks are per Stock Keeper: the first edit to one copies this list into",
				"config/stockkeeperbookmarks-bookmarks.json under that keeper's own entry, and from",
				"then on that keeper is edited there, not here.",
				"Editable in-game: type an address in the field and click the + button to pin it,",
				"right-click a button to remove it, drag buttons to reorder them.")
			.defineListAllowEmpty("addresses",
				new ArrayList<>(),
				() -> "workshop",
				o -> o instanceof String s && !s.isBlank());

		CLICK_TO_SEND = builder
			.comment("true: clicking a destination button sends the order immediately.",
				"false: clicking only selects the destination, you still press Send.")
			.define("clickToSend", true);

		AUTO_FOCUS_SEARCH = builder
			.comment("Focus the item search box when the Stock Keeper is opened, so a search can",
				"be typed straight away without clicking into it first.",
				"Toggled in-game with Ctrl+click on the name-tag button.",
				"Note that while the search box holds focus the pin key types into it rather",
				"than pinning, which is the same rule that applies whenever you are searching.")
			.define("autoFocusSearch", true);

		CLEAR_ADDRESS_ON_OPEN = builder
			.comment("Clear the address field each time the Stock Keeper is opened, so it never",
				"inherits the last address used. This is the main safeguard — turning it off",
				"restores Create's default sticky behaviour.")
			.define("clearAddressOnOpen", true);

		BUTTON_WIDTH = builder
			.comment("Width in pixels of the destination buttons.",
				"In WIDE display mode this is the minimum rather than the fixed width.")
			.defineInRange("buttonWidth", 72, 40, 200);

		DISPLAY_MODE = builder
			.comment("How bookmark labels are laid out. Cycled in-game by left-clicking the",
				"name-tag button above the list, which also carries a tooltip of the controls.",
				"The _TOOLTIPS variants add a hover tooltip naming any address that had to be",
				"cut short or shrunk to fit.",
				"FIT:      fixed width; a long address shrinks, then takes an ellipsis.",
				"ELLIPSED: fixed width; a long address is cut at full size, never shrunk.",
				"MINIMAL:  a column only a few characters wide, cut at full size.",
				"WIDE:     the column grows to the longest address, as far as the space to",
				"          the left of the panel allows; at a high GUI scale that space can",
				"          run out, and the label is cut after all.",
				"WRAP:     fixed width and taller rows; a long address runs onto a second line.")
			.defineEnum("displayMode", DisplayMode.FIT);

		COLLAPSED = builder
			.comment("Hide the bookmark list, leaving only the name-tag button.",
				"Toggled in-game by Shift+clicking that button. The display mode above is kept,",
				"so showing the list again restores the layout that was in use.")
			.define("collapsed", false);

		builder.pop();
		SPEC = builder.build();
	}

	/** Defaults for a Stock Keeper with no saved bookmarks; see {@link BookmarkStore}. */
	public static List<String> addresses() {
		return new ArrayList<>(ADDRESSES.get());
	}
}
