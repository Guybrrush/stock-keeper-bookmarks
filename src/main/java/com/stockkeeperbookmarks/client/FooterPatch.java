package com.stockkeeperbookmarks.client;

/**
 * Whether the footer artwork injection actually applied.
 *
 * The injections that repaint Create's footer are anchored to string constants inside
 * {@code renderBg} rather than to a method name, which makes them the most likely thing here to
 * stop matching after a Create update. They are declared {@code require = 0} so that failure is a
 * missing button rather than a crash on startup — but everything else that assumes the repainted
 * footer has to follow suit, or the screen answers clicks for a button nobody can see and shrinks
 * the hit box of a Send button still drawn full width.
 *
 * Set on the first frame the injection draws, read only on later input events, all on the render
 * thread.
 */
public final class FooterPatch {

	private static boolean applied;

	private FooterPatch() {
	}

	public static void markApplied() {
		applied = true;
	}

	public static boolean isApplied() {
		return applied;
	}
}
