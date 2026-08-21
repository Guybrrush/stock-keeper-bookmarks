package com.stockkeeperbookmarks.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/**
 * One destination in the sidebar.
 *
 * Not a widget: the screen scissors the column to a scrolling viewport and drives every
 * click itself, so being in the screen's renderable list would only fight that — vanilla
 * would draw these unclipped and hover them while scrolled out of sight.
 */
public class AddressButton {

	/** Palette picked to sit next to Create's brown logistics UI without clashing. */
	private static final int BG          = 0xFF2B2018;
	private static final int BG_HOVER    = 0xFF4A372B;
	private static final int BG_SELECTED = 0xFF6B4C33;
	private static final int BG_DRAG     = 0xFF7A5B3C;
	private static final int BORDER      = 0xFF120C09;
	private static final int BORDER_DRAG = 0xFFD8C0A0;
	private static final int TEXT        = 0xFFE8DDD0;
	private static final int TEXT_HOVER  = 0xFFFFFFFF;
	private static final int SHADOW      = 0x60000000;

	private static final int TEXT_PADDING = 4;
	/** Below this the vanilla font stops being legible, so clipping takes over instead. */
	private static final float MIN_TEXT_SCALE = 0.5F;

	private final String address;
	private final int width;
	private final int height;

	/**
	 * Label metrics are resolved once here rather than per frame. Address, width and the
	 * font are all fixed for the life of the button, and {@code Font#width} walks the whole
	 * string — at render time that ran for every visible button on every frame for an
	 * answer that never changed.
	 */
	private final String label;
	private final float scale;
	private final float textY;

	private int x;
	private int y;
	private boolean selected;
	private boolean dragging;

	public AddressButton(int width, int height, String address) {
		this.width = width;
		this.height = height;
		this.address = address;

		Font font = Minecraft.getInstance().font;
		int available = width - 2 * TEXT_PADDING;
		int full = font.width(address);

		// Shrink a long address rather than cutting it, down to the point where it would
		// stop being readable; past that it still has to be clipped.
		this.scale = full > available ? Math.max(MIN_TEXT_SCALE, available / (float) full) : 1.0F;

		String fitted = address;
		if (full * scale > available) {
			fitted = font.plainSubstrByWidth(address, (int) (available / scale));
			if (fitted.length() > 1)
				fitted = fitted.substring(0, fitted.length() - 1) + "…";
		}
		this.label = fitted;
		this.textY = (height - font.lineHeight * scale) / 2.0F;
	}

	public String getAddress() {
		return address;
	}

	public int getY() {
		return y;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** While dragging, the screen paints the floating copy and this slot stays empty. */
	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public void render(GuiGraphics graphics, boolean hovered) {
		if (dragging)
			return;
		int background = selected ? BG_SELECTED : hovered ? BG_HOVER : BG;
		paint(graphics, x, y, background, BORDER, hovered ? TEXT_HOVER : TEXT);
	}

	/** Paint the lifted copy at an arbitrary position, with a shadow to sell the lift. */
	public void renderGhost(GuiGraphics graphics, int ghostX, int ghostY) {
		graphics.fill(ghostX + 2, ghostY + 2, ghostX + width + 2, ghostY + height + 2, SHADOW);
		paint(graphics, ghostX, ghostY, BG_DRAG, BORDER_DRAG, TEXT_HOVER);
	}

	private void paint(GuiGraphics graphics, int atX, int atY, int background, int border, int textColor) {
		graphics.fill(atX - 1, atY - 1, atX + width + 1, atY + height + 1, border);
		graphics.fill(atX, atY, atX + width, atY + height, background);

		Font font = Minecraft.getInstance().font;
		int textX = atX + TEXT_PADDING;

		// Most labels fit outright; only pay for the matrix push when one has to be shrunk.
		if (scale == 1.0F) {
			graphics.drawString(font, label, textX, atY + (int) textY, textColor, false);
			return;
		}

		graphics.pose().pushPose();
		graphics.pose().translate(textX, atY + textY, 0);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(font, label, 0, 0, textColor, false);
		graphics.pose().popPose();
	}
}
