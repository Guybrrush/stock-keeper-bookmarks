package com.stockkeeperbookmarks;

import java.util.Locale;

/**
 * How bookmark labels are laid out, and whether an abbreviated one explains itself on hover.
 *
 * Two axes — a layout, and tooltips on or off — flattened into a single cycle. They are flat
 * rather than orthogonal because the button has one left-click to spend: the alternative was a
 * modifier-click to toggle tooltips, which is harder to discover than simply cycling past the
 * variant. Cycling eight modes is only tolerable because the header tooltip names the current
 * one and re-renders under the cursor after each click, so the cycle is never blind.
 *
 * Collapsing is deliberately *not* one of these. It is a separate flag on Shift+click, so that
 * hiding the list and restoring it does not lose the layout that was chosen.
 */
public enum DisplayMode {

	FIT(Layout.FIT, false),
	FIT_TOOLTIPS(Layout.FIT, true),
	ELLIPSED(Layout.ELLIPSED, false),
	ELLIPSED_TOOLTIPS(Layout.ELLIPSED, true),
	MINIMAL(Layout.MINIMAL, false),
	MINIMAL_TOOLTIPS(Layout.MINIMAL, true),
	WIDE(Layout.WIDE, false),
	WIDE_TOOLTIPS(Layout.WIDE, true),
	WRAP(Layout.WRAP, false),
	WRAP_TOOLTIPS(Layout.WRAP, true);

	/** What a label does when it does not fit the width it is given. */
	public enum Layout {
		/** Shrink toward the legibility floor first, then take an ellipsis. */
		FIT,
		/** Never shrink — go straight to an ellipsis, at full size. */
		ELLIPSED,
		/** Ellipsed, in a column only wide enough for a few characters. */
		MINIMAL,
		/**
		 * Grow the column instead, as far as the space to its left allows.
		 *
		 * Still abbreviates when that space runs out, which is why it has a tooltip variant:
		 * the address box caps how long an address can be, so at an ordinary window size the
		 * column can always grow to fit one — but at a high GUI scale, or a small window, the
		 * clamp binds first and the label is cut after all.
		 */
		WIDE,
		/** Keep the width and run onto a second line. */
		WRAP
	}

	private final Layout layout;
	private final boolean tooltips;

	DisplayMode(Layout layout, boolean tooltips) {
		this.layout = layout;
		this.tooltips = tooltips;
	}

	public Layout layout() {
		return layout;
	}

	/** Whether a label that had to be abbreviated names itself in full on hover. */
	public boolean tooltips() {
		return tooltips;
	}

	public DisplayMode next() {
		DisplayMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}

	/** Ten modes is a long way round; right-clicking steps back rather than all the way on. */
	public DisplayMode previous() {
		DisplayMode[] all = values();
		return all[(ordinal() + all.length - 1) % all.length];
	}

	public String translationKey() {
		return "stockkeeperbookmarks.mode." + name().toLowerCase(Locale.ROOT);
	}
}
