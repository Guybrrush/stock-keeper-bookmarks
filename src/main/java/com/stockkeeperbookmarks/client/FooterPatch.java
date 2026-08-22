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
