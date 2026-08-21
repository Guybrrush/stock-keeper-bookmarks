package com.stockaddressbook.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.stockaddressbook.AddressBookConfig;
import com.stockaddressbook.BookmarkStore;
import com.stockaddressbook.client.AddressButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin extends AbstractContainerScreen<StockKeeperRequestMenu> {

	@Unique private static final int STOCKADDRESSBOOK$ROW_HEIGHT = 16;
	@Unique private static final int STOCKADDRESSBOOK$ROW_GAP = 5;
	/**
	 * Distance kept clear between the buttons and the panel's drawn border. At 0 the buttons'
	 * outer border lands on the panel's black outline column, sharing that pixel with it.
	 */
	@Unique private static final int STOCKADDRESSBOOK$COLUMN_GAP = 0;
	/** Slack before a press becomes a drag, so a slightly shaky click still sends. */
	@Unique private static final int STOCKADDRESSBOOK$DRAG_SLOP = 3;

	/**
	 * Where the panel's drawn left edge actually is.
	 *
	 * Create renders HEADER, BODY and FOOTER from one origin at {@code guiLeft - 15}, so the
	 * visible edge is a property of the sprites rather than of guiLeft. In stock_keeper.png
	 * the body's border — 1px black outline, then the thick grey — starts at column 24, while
	 * the header and footer flare out to column 16; that flare is the wider top and bottom.
	 * Aligning to the body therefore means guiLeft - 15 + 24.
	 */
	@Unique private static final int STOCKADDRESSBOOK$SHEET_ORIGIN_DX = -15;
	@Unique private static final int STOCKADDRESSBOOK$BODY_EDGE_U = 24;
	@Unique private static final int STOCKADDRESSBOOK$BODY_EDGE_DX =
		STOCKADDRESSBOOK$SHEET_ORIGIN_DX + STOCKADDRESSBOOK$BODY_EDGE_U;

	/** Share of the Stock Keeper's window height the scrolling column is allowed to occupy. */
	@Unique private static final float STOCKADDRESSBOOK$VIEWPORT_SHARE = 0.6F;
	@Unique private static final int STOCKADDRESSBOOK$SCROLLBAR_W = 2;
	@Unique private static final int STOCKADDRESSBOOK$SCROLLBAR_TRACK = 0x40000000;
	@Unique private static final int STOCKADDRESSBOOK$SCROLLBAR_THUMB = 0xFFB89A72;
	/** How close to an edge a dragged button must get before the column scrolls itself. */
	@Unique private static final int STOCKADDRESSBOOK$AUTOSCROLL_EDGE = 10;
	@Unique private static final int STOCKADDRESSBOOK$AUTOSCROLL_SPEED = 3;

	// --- Footer geometry -----------------------------------------------------------------
	// Create draws its Send button 80x20 at (guiLeft + 145, guiTop + windowHeight - 41).
	// Its unhovered artwork is baked into the FOOTER sheet at (160, 119); the hover variant
	// is the separate SEND_HOVER sprite at (55, 200). Both are 80x20, so the same slice
	// offsets address either state. We repaint that whole 80px span as:
	//     [ 20px + button ][ 4px gap ][ 56px Send ]
	@Unique private static final ResourceLocation STOCKADDRESSBOOK$SHEET =
		ResourceLocation.fromNamespaceAndPath("create", "textures/gui/stock_keeper.png");

	@Unique private static final int STOCKADDRESSBOOK$BASE_U = 160, STOCKADDRESSBOOK$BASE_V = 119;
	@Unique private static final int STOCKADDRESSBOOK$HOVER_U = 55, STOCKADDRESSBOOK$HOVER_V = 200;
	/** Plain footer background, the 4px gutter between the address plate and the Send button. */
	@Unique private static final int STOCKADDRESSBOOK$BG_U = 156, STOCKADDRESSBOOK$BG_V = 119;

	@Unique private static final int STOCKADDRESSBOOK$BTN_W = 80;
	@Unique private static final int STOCKADDRESSBOOK$ROW_DX = 145;
	@Unique private static final int STOCKADDRESSBOOK$ROW_DY = -41;
	@Unique private static final int STOCKADDRESSBOOK$ROW_H = 20;

	@Unique private static final int STOCKADDRESSBOOK$PLUS_W = 20;
	@Unique private static final int STOCKADDRESSBOOK$GAP_W = 4;
	@Unique private static final int STOCKADDRESSBOOK$SEND_W =
		STOCKADDRESSBOOK$BTN_W - STOCKADDRESSBOOK$PLUS_W - STOCKADDRESSBOOK$GAP_W;
	@Unique private static final int STOCKADDRESSBOOK$SEND_DX =
		STOCKADDRESSBOOK$ROW_DX + STOCKADDRESSBOOK$PLUS_W + STOCKADDRESSBOOK$GAP_W;

	/**
	 * Create centres the Send caption on the full-width button (guiLeft + windowWidth - 42).
	 * The shortened button's centre sits this much further right.
	 */
	@Unique private static final int STOCKADDRESSBOOK$LABEL_SHIFT =
		(STOCKADDRESSBOOK$PLUS_W + STOCKADDRESSBOOK$GAP_W) / 2;

	@Shadow public EditBox searchBox;
	@Shadow public AddressEditBox addressBox;
	@Shadow int windowHeight;
	/** Top of Create's item grid — {@code getHoveredSlot} rows off this, so row 0 starts here. */
	@Shadow int itemsY;

	/**
	 * Create places its first slot row at {@code itemsY + 4}, or {@code itemsY + 20} when
	 * categories are present and a header row has to fit above it. We anchor to the 4, the
	 * grid's own top padding: it is the true content top, and it does not make the column
	 * jump by 16px as categories come and go with the stock.
	 */
	@Unique private static final int STOCKADDRESSBOOK$GRID_TOP_PADDING = 4;

	@Shadow
	private void sendIt() {
		throw new AssertionError("mixin stub");
	}

	/** Sidebar buttons in display order; drag reorders this list in place. */
	@Unique private final List<AddressButton> stockaddressbook$buttons = new ArrayList<>();
	@Unique private Rect2i stockaddressbook$reservedArea;
	/** init() also runs on window resize; only the genuine first open should wipe the address. */
	@Unique private boolean stockaddressbook$openedOnce;

	@Unique private int stockaddressbook$columnX;
	@Unique private int stockaddressbook$columnTop;
	@Unique private int stockaddressbook$viewportHeight;
	@Unique private int stockaddressbook$scroll;
	/**
	 * Resolved once per init rather than read from config per frame. Beyond saving the
	 * lookups, it keeps the geometry self-consistent: columnX is derived from this width, so
	 * re-reading the config mid-screen could leave the two disagreeing after a config reload.
	 */
	@Unique private int stockaddressbook$buttonWidth;

	/** Which Stock Keeper this screen is showing; null if the block entity was unavailable. */
	@Unique private String stockaddressbook$key;

	/** The button under an unreleased left press — not yet known to be a click or a drag. */
	@Unique private AddressButton stockaddressbook$pressed;
	@Unique private double stockaddressbook$pressX;
	@Unique private double stockaddressbook$pressY;
	/** Where inside the button it was grabbed, so it does not snap to the cursor. */
	@Unique private int stockaddressbook$grabDy;
	@Unique private boolean stockaddressbook$dragging;
	@Unique private int stockaddressbook$ghostY;

	private StockKeeperRequestScreenMixin(StockKeeperRequestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void stockaddressbook$addDestinationColumn(CallbackInfo ci) {
		if (!stockaddressbook$openedOnce) {
			stockaddressbook$openedOnce = true;
			if (AddressBookConfig.CLEAR_ADDRESS_ON_OPEN.get())
				addressBox.setValue("");
		}

		stockaddressbook$buttons.clear();
		stockaddressbook$pressed = null;
		stockaddressbook$dragging = false;

		// Create resolves contentHolder through ClientLevel.getBlockEntity, so this is the
		// real keeper being looked at — no server round trip needed to tell them apart.
		stockaddressbook$key = BookmarkStore.keyFor(menu.contentHolder);

		List<String> addresses = BookmarkStore.get(stockaddressbook$key);
		int width = AddressBookConfig.BUTTON_WIDTH.get();
		stockaddressbook$buttonWidth = width;
		// Sit flush against the body's border: the buttons' outer border lands on the last
		// pixel before it, so they touch without crossing. The scrollbar lives in the gutter
		// on the far side, which keeps this edge clean.
		stockaddressbook$columnX = Math.max(STOCKADDRESSBOOK$SCROLLBAR_W + 4,
			getGuiLeft() + STOCKADDRESSBOOK$BODY_EDGE_DX - STOCKADDRESSBOOK$COLUMN_GAP - width);
		// columnTop is the column's topmost drawn pixel — the first button's border line —
		// lined up with the top of Create's first slot row.
		stockaddressbook$columnTop = itemsY + STOCKADDRESSBOOK$GRID_TOP_PADDING;

		// The column is as tall as the requested share of the panel, but never runs past
		// the panel's own bottom edge — at small window heights that limit binds first.
		int toWindowBottom = getGuiTop() + windowHeight - stockaddressbook$columnTop;
		stockaddressbook$viewportHeight = Math.min(
			Math.round(windowHeight * STOCKADDRESSBOOK$VIEWPORT_SHARE), toWindowBottom);

		for (String address : addresses) {
			AddressButton button = new AddressButton(width, STOCKADDRESSBOOK$ROW_HEIGHT, address);
			button.setSelected(address.equals(addressBox.getValue()));
			stockaddressbook$buttons.add(button);
		}
		stockaddressbook$scrollTo(stockaddressbook$scroll);

		stockaddressbook$reservedArea = new Rect2i(stockaddressbook$columnLeft(),
			stockaddressbook$columnTop,
			stockaddressbook$columnRight() - stockaddressbook$columnLeft(),
			stockaddressbook$viewportHeight);
	}

	@Unique private int stockaddressbook$pitch() {
		return STOCKADDRESSBOOK$ROW_HEIGHT + STOCKADDRESSBOOK$ROW_GAP;
	}

	/** Left edge of the column, i.e. the scrollbar gutter that sits outside the buttons. */
	@Unique private int stockaddressbook$columnLeft() {
		return stockaddressbook$columnX - 2 - STOCKADDRESSBOOK$SCROLLBAR_W;
	}

	/** One past the rightmost pixel the column paints — the buttons' outer border. */
	@Unique private int stockaddressbook$columnRight() {
		return stockaddressbook$columnX + stockaddressbook$buttonWidth + 1;
	}

	@Unique private int stockaddressbook$contentHeight() {
		return stockaddressbook$buttons.isEmpty() ? 0
			: stockaddressbook$buttons.size() * stockaddressbook$pitch() - STOCKADDRESSBOOK$ROW_GAP;
	}

	/**
	 * The two extra pixels are the column's own border rows: content starts one pixel below
	 * {@code columnTop} and each button paints a border one pixel past its fill. Without them
	 * the last button's bottom border can never be scrolled into view.
	 */
	@Unique private int stockaddressbook$maxScroll() {
		return Math.max(0, stockaddressbook$contentHeight() + 2 - stockaddressbook$viewportHeight);
	}

	/** Clamp to the scrollable range, then re-place the buttons under the new offset. */
	@Unique
	private void stockaddressbook$scrollTo(int value) {
		stockaddressbook$scroll = Mth.clamp(value, 0, stockaddressbook$maxScroll());
		stockaddressbook$layout();
	}

	/**
	 * Where button fills begin, one pixel below the column's top border line. Buttons paint
	 * their border above their origin, so without this inset the first button's top border
	 * would fall outside the scissor and be clipped away.
	 */
	@Unique private int stockaddressbook$contentTop() {
		return stockaddressbook$columnTop + 1;
	}

	/** Snap every button to the slot its current index describes, minus the scroll offset. */
	@Unique
	private void stockaddressbook$layout() {
		for (int i = 0; i < stockaddressbook$buttons.size(); i++)
			stockaddressbook$buttons.get(i).setPosition(stockaddressbook$columnX,
				stockaddressbook$contentTop() - stockaddressbook$scroll
					+ i * stockaddressbook$pitch());
	}

	@Unique
	private boolean stockaddressbook$inViewport(double mouseX, double mouseY) {
		return mouseX >= stockaddressbook$columnLeft()
			&& mouseX < stockaddressbook$columnRight()
			&& mouseY >= stockaddressbook$columnTop
			&& mouseY < stockaddressbook$columnTop + stockaddressbook$viewportHeight;
	}

	/** A button only counts as hit where it is actually visible, not where it scrolled to. */
	@Unique
	private AddressButton stockaddressbook$buttonAt(double mouseX, double mouseY) {
		if (!stockaddressbook$inViewport(mouseX, mouseY))
			return null;
		for (AddressButton button : stockaddressbook$buttons)
			if (button.contains(mouseX, mouseY))
				return button;
		return null;
	}

	/** A press that ended without becoming a drag: select the destination, maybe send. */
	@Unique
	private void stockaddressbook$activate(AddressButton button) {
		addressBox.setValue(button.getAddress());
		addressBox.setFocused(false);
		searchBox.setFocused(false);
		for (AddressButton other : stockaddressbook$buttons)
			other.setSelected(other == button);

		if (AddressBookConfig.CLICK_TO_SEND.get())
			sendIt();
	}

	/**
	 * Rebuilding re-runs init() and rebuilds the column, so it cannot happen while the click
	 * that triggered it is still being dispatched. Defer it to the start of the next tick.
	 */
	@Unique
	private void stockaddressbook$scheduleRebuild() {
		Minecraft.getInstance().execute(this::rebuildWidgets);
	}

	// --- Footer: pin button + shortened Send ---------------------------------------------

	@Unique private int stockaddressbook$rowY() {
		return getGuiTop() + windowHeight + STOCKADDRESSBOOK$ROW_DY;
	}

	@Unique private int stockaddressbook$plusX() {
		return getGuiLeft() + STOCKADDRESSBOOK$ROW_DX;
	}

	@Unique
	private boolean stockaddressbook$isPlusHovered(int mouseX, int mouseY) {
		int x = stockaddressbook$plusX(), y = stockaddressbook$rowY();
		return mouseX >= x && mouseX < x + STOCKADDRESSBOOK$PLUS_W
			&& mouseY >= y && mouseY < y + STOCKADDRESSBOOK$ROW_H;
	}

	/**
	 * Repaint the button row. Injected at the point Create has finished drawing the footer
	 * and the Send hover, but has not yet drawn the Send caption — so the caption (and its
	 * "sent" fade animation) still lands on top of our artwork untouched.
	 */
	@Inject(method = "renderBg",
		at = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.title"))
	private void stockaddressbook$drawFooterRow(GuiGraphics graphics, float partialTick, int mouseX,
		int mouseY, CallbackInfo ci) {

		int x = stockaddressbook$plusX();
		int y = stockaddressbook$rowY();

		boolean plusHovered = stockaddressbook$isPlusHovered(mouseX, mouseY);
		int plusU = plusHovered ? STOCKADDRESSBOOK$HOVER_U : STOCKADDRESSBOOK$BASE_U;
		int plusV = plusHovered ? STOCKADDRESSBOOK$HOVER_V : STOCKADDRESSBOOK$BASE_V;

		// Pin button: the button's left edge plus fill, then its 1px outline column reused
		// on the right so the square closes cleanly.
		graphics.blit(STOCKADDRESSBOOK$SHEET, x, y, plusU, plusV,
			STOCKADDRESSBOOK$PLUS_W - 1, STOCKADDRESSBOOK$ROW_H);
		graphics.blit(STOCKADDRESSBOOK$SHEET, x + STOCKADDRESSBOOK$PLUS_W - 1, y, plusU, plusV,
			1, STOCKADDRESSBOOK$ROW_H);

		// Gutter, copied from the footer's own background so it matches exactly.
		graphics.blit(STOCKADDRESSBOOK$SHEET, x + STOCKADDRESSBOOK$PLUS_W, y,
			STOCKADDRESSBOOK$BG_U, STOCKADDRESSBOOK$BG_V, STOCKADDRESSBOOK$GAP_W,
			STOCKADDRESSBOOK$ROW_H);

		// Shortened Send: original left edge, then the tail end so the arrow point survives.
		boolean sendHovered = stockaddressbook$isSendHovered(mouseX, mouseY);
		int sendU = sendHovered ? STOCKADDRESSBOOK$HOVER_U : STOCKADDRESSBOOK$BASE_U;
		int sendV = sendHovered ? STOCKADDRESSBOOK$HOVER_V : STOCKADDRESSBOOK$BASE_V;
		int sendX = getGuiLeft() + STOCKADDRESSBOOK$SEND_DX;

		graphics.blit(STOCKADDRESSBOOK$SHEET, sendX, y, sendU, sendV, 2, STOCKADDRESSBOOK$ROW_H);
		graphics.blit(STOCKADDRESSBOOK$SHEET, sendX + 2, y,
			sendU + STOCKADDRESSBOOK$BTN_W - (STOCKADDRESSBOOK$SEND_W - 2), sendV,
			STOCKADDRESSBOOK$SEND_W - 2, STOCKADDRESSBOOK$ROW_H);

		// "+" glyph, matching the Send caption's ink and baseline.
		String plus = "+";
		graphics.drawString(font, plus,
			x + STOCKADDRESSBOOK$PLUS_W / 2 - font.width(plus) / 2,
			getGuiTop() + windowHeight - 35, 0x252525, false);
	}

	/**
	 * Nudge Create's Send caption to the centre of the shortened button.
	 *
	 * The slice must be closed at both ends. Create draws the caption twice (a plain branch
	 * and a faded "just sent" branch), and immediately afterwards draws the "Request Sent"
	 * ribbon with the same overload — an open-ended slice shifts that off-centre too.
	 */
	@ModifyArg(method = "renderBg",
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.send"),
			to = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.request_sent")),
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
		index = 2)
	private int stockaddressbook$shiftSendCaption(int labelX) {
		return labelX + STOCKADDRESSBOOK$LABEL_SHIFT;
	}

	@Unique
	private boolean stockaddressbook$isSendHovered(int mouseX, int mouseY) {
		int x = getGuiLeft() + STOCKADDRESSBOOK$SEND_DX;
		int y = stockaddressbook$rowY();
		return mouseX >= x && mouseX < x + STOCKADDRESSBOOK$SEND_W
			&& mouseY >= y && mouseY < y + STOCKADDRESSBOOK$ROW_H;
	}

	/** Shrink Create's own hit test to the shortened button. */
	@Inject(method = "isConfirmHovered", at = @At("HEAD"), cancellable = true)
	private void stockaddressbook$narrowConfirm(int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(stockaddressbook$isSendHovered(mouseX, mouseY));
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void stockaddressbook$press(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {

		if (button == 0 && stockaddressbook$isPlusHovered((int) mouseX, (int) mouseY)) {
			boolean pinned = BookmarkStore.add(stockaddressbook$key, addressBox.getValue());
			if (pinned)
				stockaddressbook$scheduleRebuild();
			// With the tooltips gone, a flat low click is the only cue that a pin was
			// refused — the address was blank or already pinned.
			Minecraft.getInstance()
				.getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(),
					pinned ? 1.0F : 0.5F));
			cir.setReturnValue(true);
			return;
		}

		AddressButton hit = stockaddressbook$buttonAt(mouseX, mouseY);
		if (hit == null)
			return;

		if (button == 1) {
			BookmarkStore.remove(stockaddressbook$key, hit.getAddress());
			stockaddressbook$scheduleRebuild();
			cir.setReturnValue(true);
			return;
		}

		if (button != 0)
			return;

		// Hold the press open: only the release knows whether this was a click or a drag.
		stockaddressbook$pressed = hit;
		stockaddressbook$pressX = mouseX;
		stockaddressbook$pressY = mouseY;
		stockaddressbook$grabDy = (int) (mouseY - hit.getY());
		stockaddressbook$dragging = false;
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void stockaddressbook$drag(double mouseX, double mouseY, int button, double dragX,
		double dragY, CallbackInfoReturnable<Boolean> cir) {

		if (stockaddressbook$pressed == null || button != 0)
			return;

		if (!stockaddressbook$dragging) {
			if (Math.abs(mouseX - stockaddressbook$pressX) < STOCKADDRESSBOOK$DRAG_SLOP
				&& Math.abs(mouseY - stockaddressbook$pressY) < STOCKADDRESSBOOK$DRAG_SLOP) {
				cir.setReturnValue(true);
				return;
			}
			stockaddressbook$dragging = true;
			stockaddressbook$pressed.setDragging(true);
		}

		stockaddressbook$updateDrag(mouseY);
		cir.setReturnValue(true);
	}

	/**
	 * Place the lifted button and work out where it would drop.
	 *
	 * Also runs per frame while a drag is held, because auto-scrolling at the viewport edge
	 * has to keep going when the cursor is pressed against it and no longer moving.
	 */
	@Unique
	private void stockaddressbook$updateDrag(double mouseY) {
		int top = stockaddressbook$columnTop;
		int bottom = top + stockaddressbook$viewportHeight;

		if (mouseY < top + STOCKADDRESSBOOK$AUTOSCROLL_EDGE)
			stockaddressbook$scrollTo(stockaddressbook$scroll - STOCKADDRESSBOOK$AUTOSCROLL_SPEED);
		else if (mouseY > bottom - STOCKADDRESSBOOK$AUTOSCROLL_EDGE)
			stockaddressbook$scrollTo(stockaddressbook$scroll + STOCKADDRESSBOOK$AUTOSCROLL_SPEED);

		// Keep the lifted copy inside the viewport; it is drawn clipped to it anyway.
		stockaddressbook$ghostY = Mth.clamp((int) mouseY - stockaddressbook$grabDy,
			stockaddressbook$contentTop(), bottom - STOCKADDRESSBOOK$ROW_HEIGHT);

		// The slots move with the scroll offset, so the drop index is a content-space figure.
		int pitch = stockaddressbook$pitch();
		int last = stockaddressbook$buttons.size() - 1;
		int contentY = stockaddressbook$ghostY - stockaddressbook$contentTop()
			+ stockaddressbook$scroll;
		int target = Mth.clamp(Math.round(contentY / (float) pitch), 0, last);

		int current = stockaddressbook$buttons.indexOf(stockaddressbook$pressed);
		if (target != current) {
			stockaddressbook$buttons.remove(current);
			stockaddressbook$buttons.add(target, stockaddressbook$pressed);
			stockaddressbook$layout();
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void stockaddressbook$scrollColumn(double mouseX, double mouseY, double scrollX,
		double scrollY, CallbackInfoReturnable<Boolean> cir) {
		if (stockaddressbook$maxScroll() <= 0 || !stockaddressbook$inViewport(mouseX, mouseY))
			return;
		stockaddressbook$scrollTo(
			stockaddressbook$scroll - (int) Math.signum(scrollY) * stockaddressbook$pitch());
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void stockaddressbook$release(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {

		if (stockaddressbook$pressed == null || button != 0)
			return;

		AddressButton released = stockaddressbook$pressed;
		stockaddressbook$pressed = null;

		if (stockaddressbook$dragging) {
			stockaddressbook$dragging = false;
			released.setDragging(false);
			stockaddressbook$layout();
			BookmarkStore.set(stockaddressbook$key, stockaddressbook$buttons.stream()
				.map(AddressButton::getAddress)
				.toList());
		} else {
			stockaddressbook$activate(released);
		}
		cir.setReturnValue(true);
	}

	/**
	 * Draw the column, clipped to its viewport. Done here rather than through the screen's
	 * renderable list so the scissor can wrap it and the lifted button always lands on top.
	 *
	 * HEAD, not TAIL. Create renders its tooltips at the end of renderForeground — the
	 * address-box one last of all, immediately before the return. Injecting at TAIL put our
	 * enableScissor call right after that tooltip was drawn but before its geometry had
	 * necessarily been flushed, so the tooltip got clipped to this column's narrow rect and
	 * came out blank. Drawing first is also the correct layering: tooltips belong on top.
	 */
	@Inject(method = "renderForeground", at = @At("HEAD"))
	private void stockaddressbook$drawColumn(GuiGraphics graphics, int mouseX, int mouseY,
		float partialTick, CallbackInfo ci) {

		if (stockaddressbook$buttons.isEmpty())
			return;

		// A held drag keeps scrolling at the edge even while the cursor sits still, so the
		// drag has to advance per frame and not only on mouse-move events.
		if (stockaddressbook$dragging)
			stockaddressbook$updateDrag(mouseY);

		int width = stockaddressbook$buttonWidth;
		int top = stockaddressbook$columnTop;
		int bottom = top + stockaddressbook$viewportHeight;
		AddressButton hovered = stockaddressbook$dragging ? null
			: stockaddressbook$buttonAt(mouseX, mouseY);

		graphics.enableScissor(stockaddressbook$columnX - 1, top, stockaddressbook$columnX + width + 1,
			bottom);
		for (AddressButton button : stockaddressbook$buttons)
			button.render(graphics, button == hovered);
		if (stockaddressbook$dragging && stockaddressbook$pressed != null)
			stockaddressbook$pressed.renderGhost(graphics, stockaddressbook$columnX,
				stockaddressbook$ghostY);
		graphics.disableScissor();

		stockaddressbook$drawScrollbar(graphics, top);
	}

	@Unique
	private void stockaddressbook$drawScrollbar(GuiGraphics graphics, int top) {
		int maxScroll = stockaddressbook$maxScroll();
		if (maxScroll <= 0)
			return;

		int viewport = stockaddressbook$viewportHeight;
		int x = stockaddressbook$columnLeft();
		int thumbHeight = Math.max(8, viewport * viewport / stockaddressbook$contentHeight());
		int thumbY = top + (viewport - thumbHeight) * stockaddressbook$scroll / maxScroll;

		graphics.fill(x, top, x + STOCKADDRESSBOOK$SCROLLBAR_W, top + viewport,
			STOCKADDRESSBOOK$SCROLLBAR_TRACK);
		graphics.fill(x, thumbY, x + STOCKADDRESSBOOK$SCROLLBAR_W, thumbY + thumbHeight,
			STOCKADDRESSBOOK$SCROLLBAR_THUMB);
	}

	/** Keep JEI/EMI from drawing on top of the destination column. */
	@Inject(method = "getExtraAreas", at = @At("RETURN"), cancellable = true)
	private void stockaddressbook$reserveColumn(CallbackInfoReturnable<List<Rect2i>> cir) {
		if (stockaddressbook$reservedArea == null)
			return;
		List<Rect2i> areas = new ArrayList<>(cir.getReturnValue());
		areas.add(stockaddressbook$reservedArea);
		cir.setReturnValue(areas);
	}
}
