package com.stockkeeperbookmarks.mixin;

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
import com.stockkeeperbookmarks.AddressBookConfig;
import com.stockkeeperbookmarks.BookmarkStore;
import com.stockkeeperbookmarks.client.AddressButton;
import com.stockkeeperbookmarks.client.FooterPatch;
import com.stockkeeperbookmarks.client.ModKeys;

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

	@Unique private static final int STOCKKEEPERBOOKMARKS$ROW_HEIGHT = 16;
	@Unique private static final int STOCKKEEPERBOOKMARKS$ROW_GAP = 5;
	/**
	 * Distance kept clear between the buttons and the panel's drawn border. At 0 the buttons'
	 * outer border lands on the panel's black outline column, sharing that pixel with it.
	 */
	@Unique private static final int STOCKKEEPERBOOKMARKS$COLUMN_GAP = 0;
	/** Slack before a press becomes a drag, so a slightly shaky click still sends. */
	@Unique private static final int STOCKKEEPERBOOKMARKS$DRAG_SLOP = 3;

	/**
	 * Where the panel's drawn left edge actually is.
	 *
	 * Create renders HEADER, BODY and FOOTER from one origin at {@code guiLeft - 15}, so the
	 * visible edge is a property of the sprites rather than of guiLeft. In stock_keeper.png
	 * the body's border — 1px black outline, then the thick grey — starts at column 24, while
	 * the header and footer flare out to column 16; that flare is the wider top and bottom.
	 * Aligning to the body therefore means guiLeft - 15 + 24.
	 */
	@Unique private static final int STOCKKEEPERBOOKMARKS$SHEET_ORIGIN_DX = -15;
	@Unique private static final int STOCKKEEPERBOOKMARKS$BODY_EDGE_U = 24;
	@Unique private static final int STOCKKEEPERBOOKMARKS$BODY_EDGE_DX =
		STOCKKEEPERBOOKMARKS$SHEET_ORIGIN_DX + STOCKKEEPERBOOKMARKS$BODY_EDGE_U;

	/** Share of the Stock Keeper's window height the scrolling column is allowed to occupy. */
	@Unique private static final float STOCKKEEPERBOOKMARKS$VIEWPORT_SHARE = 0.6F;
	@Unique private static final int STOCKKEEPERBOOKMARKS$SCROLLBAR_W = 2;
	@Unique private static final int STOCKKEEPERBOOKMARKS$SCROLLBAR_TRACK = 0x40000000;
	@Unique private static final int STOCKKEEPERBOOKMARKS$SCROLLBAR_THUMB = 0xFFB89A72;
	/** How close to an edge a dragged button must get before the column scrolls itself. */
	@Unique private static final int STOCKKEEPERBOOKMARKS$AUTOSCROLL_EDGE = 10;
	@Unique private static final int STOCKKEEPERBOOKMARKS$AUTOSCROLL_SPEED = 3;

	// --- Footer geometry -----------------------------------------------------------------
	// Create draws its Send button 80x20 at (guiLeft + 145, guiTop + windowHeight - 41).
	// Its unhovered artwork is baked into the FOOTER sheet at (160, 119); the hover variant
	// is the separate SEND_HOVER sprite at (55, 200). Both are 80x20, so the same slice
	// offsets address either state. We repaint that whole 80px span as:
	//     [ 20px + button ][ 4px gap ][ 56px Send ]
	@Unique private static final ResourceLocation STOCKKEEPERBOOKMARKS$SHEET =
		ResourceLocation.fromNamespaceAndPath("create", "textures/gui/stock_keeper.png");

	@Unique private static final int STOCKKEEPERBOOKMARKS$BASE_U = 160, STOCKKEEPERBOOKMARKS$BASE_V = 119;
	@Unique private static final int STOCKKEEPERBOOKMARKS$HOVER_U = 55, STOCKKEEPERBOOKMARKS$HOVER_V = 200;
	/** Plain footer background, the 4px gutter between the address plate and the Send button. */
	@Unique private static final int STOCKKEEPERBOOKMARKS$BG_U = 156, STOCKKEEPERBOOKMARKS$BG_V = 119;

	@Unique private static final int STOCKKEEPERBOOKMARKS$BTN_W = 80;
	@Unique private static final int STOCKKEEPERBOOKMARKS$ROW_DX = 145;
	@Unique private static final int STOCKKEEPERBOOKMARKS$ROW_DY = -41;
	@Unique private static final int STOCKKEEPERBOOKMARKS$ROW_H = 20;

	@Unique private static final int STOCKKEEPERBOOKMARKS$PLUS_W = 20;
	@Unique private static final int STOCKKEEPERBOOKMARKS$GAP_W = 4;
	@Unique private static final int STOCKKEEPERBOOKMARKS$SEND_W =
		STOCKKEEPERBOOKMARKS$BTN_W - STOCKKEEPERBOOKMARKS$PLUS_W - STOCKKEEPERBOOKMARKS$GAP_W;
	@Unique private static final int STOCKKEEPERBOOKMARKS$SEND_DX =
		STOCKKEEPERBOOKMARKS$ROW_DX + STOCKKEEPERBOOKMARKS$PLUS_W + STOCKKEEPERBOOKMARKS$GAP_W;

	/**
	 * Create centres the Send caption on the full-width button (guiLeft + windowWidth - 42).
	 * The shortened button's centre sits this much further right.
	 */
	@Unique private static final int STOCKKEEPERBOOKMARKS$LABEL_SHIFT =
		(STOCKKEEPERBOOKMARKS$PLUS_W + STOCKKEEPERBOOKMARKS$GAP_W) / 2;

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
	@Unique private static final int STOCKKEEPERBOOKMARKS$GRID_TOP_PADDING = 4;

	@Shadow
	private void sendIt() {
		throw new AssertionError("mixin stub");
	}

	/** Sidebar buttons in display order; drag reorders this list in place. */
	@Unique private final List<AddressButton> stockkeeperbookmarks$buttons = new ArrayList<>();
	@Unique private Rect2i stockkeeperbookmarks$reservedArea;
	/** init() also runs on window resize; only the genuine first open should wipe the address. */
	@Unique private boolean stockkeeperbookmarks$openedOnce;

	@Unique private int stockkeeperbookmarks$columnX;
	@Unique private int stockkeeperbookmarks$columnTop;
	@Unique private int stockkeeperbookmarks$viewportHeight;
	@Unique private int stockkeeperbookmarks$scroll;
	/**
	 * Resolved once per init rather than read from config per frame. Beyond saving the
	 * lookups, it keeps the geometry self-consistent: columnX is derived from this width, so
	 * re-reading the config mid-screen could leave the two disagreeing after a config reload.
	 */
	@Unique private int stockkeeperbookmarks$buttonWidth;

	/** Which Stock Keeper this screen is showing; null if the block entity was unavailable. */
	@Unique private String stockkeeperbookmarks$key;

	/** The button under an unreleased left press — not yet known to be a click or a drag. */
	@Unique private AddressButton stockkeeperbookmarks$pressed;
	@Unique private double stockkeeperbookmarks$pressX;
	@Unique private double stockkeeperbookmarks$pressY;
	/** Where inside the button it was grabbed, so it does not snap to the cursor. */
	@Unique private int stockkeeperbookmarks$grabDy;
	@Unique private boolean stockkeeperbookmarks$dragging;
	@Unique private int stockkeeperbookmarks$ghostY;

	/** Set when the pin key was consumed, so its character never reaches the address box. */
	@Unique private boolean stockkeeperbookmarks$swallowChar;

	private StockKeeperRequestScreenMixin(StockKeeperRequestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void stockkeeperbookmarks$addDestinationColumn(CallbackInfo ci) {
		if (!stockkeeperbookmarks$openedOnce) {
			stockkeeperbookmarks$openedOnce = true;
			if (AddressBookConfig.CLEAR_ADDRESS_ON_OPEN.get())
				addressBox.setValue("");
		}

		stockkeeperbookmarks$buttons.clear();
		stockkeeperbookmarks$pressed = null;
		stockkeeperbookmarks$dragging = false;

		// Create resolves contentHolder through ClientLevel.getBlockEntity, so this is the
		// real keeper being looked at — no server round trip needed to tell them apart.
		stockkeeperbookmarks$key = BookmarkStore.keyFor(menu.contentHolder);

		List<String> addresses = BookmarkStore.get(stockkeeperbookmarks$key);
		int width = AddressBookConfig.BUTTON_WIDTH.get();
		stockkeeperbookmarks$buttonWidth = width;
		// Sit flush against the body's border: the buttons' outer border lands on the last
		// pixel before it, so they touch without crossing. The scrollbar lives in the gutter
		// on the far side, which keeps this edge clean.
		stockkeeperbookmarks$columnX = Math.max(STOCKKEEPERBOOKMARKS$SCROLLBAR_W + 4,
			getGuiLeft() + STOCKKEEPERBOOKMARKS$BODY_EDGE_DX - STOCKKEEPERBOOKMARKS$COLUMN_GAP - width);
		// columnTop is the column's topmost drawn pixel — the first button's border line —
		// lined up with the top of Create's first slot row.
		stockkeeperbookmarks$columnTop = itemsY + STOCKKEEPERBOOKMARKS$GRID_TOP_PADDING;

		// The column is as tall as the requested share of the panel, but never runs past
		// the panel's own bottom edge — at small window heights that limit binds first.
		int toWindowBottom = getGuiTop() + windowHeight - stockkeeperbookmarks$columnTop;
		stockkeeperbookmarks$viewportHeight = Math.min(
			Math.round(windowHeight * STOCKKEEPERBOOKMARKS$VIEWPORT_SHARE), toWindowBottom);

		for (String address : addresses) {
			AddressButton button = new AddressButton(width, STOCKKEEPERBOOKMARKS$ROW_HEIGHT, address);
			button.setSelected(address.equals(addressBox.getValue()));
			stockkeeperbookmarks$buttons.add(button);
		}
		stockkeeperbookmarks$scrollTo(stockkeeperbookmarks$scroll);

		stockkeeperbookmarks$reservedArea = new Rect2i(stockkeeperbookmarks$columnLeft(),
			stockkeeperbookmarks$columnTop,
			stockkeeperbookmarks$columnRight() - stockkeeperbookmarks$columnLeft(),
			stockkeeperbookmarks$viewportHeight);
	}

	@Unique private int stockkeeperbookmarks$pitch() {
		return STOCKKEEPERBOOKMARKS$ROW_HEIGHT + STOCKKEEPERBOOKMARKS$ROW_GAP;
	}

	/** Left edge of the column, i.e. the scrollbar gutter that sits outside the buttons. */
	@Unique private int stockkeeperbookmarks$columnLeft() {
		return stockkeeperbookmarks$columnX - 2 - STOCKKEEPERBOOKMARKS$SCROLLBAR_W;
	}

	/** One past the rightmost pixel the column paints — the buttons' outer border. */
	@Unique private int stockkeeperbookmarks$columnRight() {
		return stockkeeperbookmarks$columnX + stockkeeperbookmarks$buttonWidth + 1;
	}

	@Unique private int stockkeeperbookmarks$contentHeight() {
		return stockkeeperbookmarks$buttons.isEmpty() ? 0
			: stockkeeperbookmarks$buttons.size() * stockkeeperbookmarks$pitch() - STOCKKEEPERBOOKMARKS$ROW_GAP;
	}

	/**
	 * The two extra pixels are the column's own border rows: content starts one pixel below
	 * {@code columnTop} and each button paints a border one pixel past its fill. Without them
	 * the last button's bottom border can never be scrolled into view.
	 */
	@Unique private int stockkeeperbookmarks$maxScroll() {
		return Math.max(0, stockkeeperbookmarks$contentHeight() + 2 - stockkeeperbookmarks$viewportHeight);
	}

	/** Clamp to the scrollable range, then re-place the buttons under the new offset. */
	@Unique
	private void stockkeeperbookmarks$scrollTo(int value) {
		stockkeeperbookmarks$scroll = Mth.clamp(value, 0, stockkeeperbookmarks$maxScroll());
		stockkeeperbookmarks$layout();
	}

	/**
	 * Where button fills begin, one pixel below the column's top border line. Buttons paint
	 * their border above their origin, so without this inset the first button's top border
	 * would fall outside the scissor and be clipped away.
	 */
	@Unique private int stockkeeperbookmarks$contentTop() {
		return stockkeeperbookmarks$columnTop + 1;
	}

	/** Snap every button to the slot its current index describes, minus the scroll offset. */
	@Unique
	private void stockkeeperbookmarks$layout() {
		for (int i = 0; i < stockkeeperbookmarks$buttons.size(); i++)
			stockkeeperbookmarks$buttons.get(i).setPosition(stockkeeperbookmarks$columnX,
				stockkeeperbookmarks$contentTop() - stockkeeperbookmarks$scroll
					+ i * stockkeeperbookmarks$pitch());
	}

	@Unique
	private boolean stockkeeperbookmarks$inViewport(double mouseX, double mouseY) {
		return mouseX >= stockkeeperbookmarks$columnLeft()
			&& mouseX < stockkeeperbookmarks$columnRight()
			&& mouseY >= stockkeeperbookmarks$columnTop
			&& mouseY < stockkeeperbookmarks$columnTop + stockkeeperbookmarks$viewportHeight;
	}

	/** A button only counts as hit where it is actually visible, not where it scrolled to. */
	@Unique
	private AddressButton stockkeeperbookmarks$buttonAt(double mouseX, double mouseY) {
		if (!stockkeeperbookmarks$inViewport(mouseX, mouseY))
			return null;
		for (AddressButton button : stockkeeperbookmarks$buttons)
			if (button.contains(mouseX, mouseY))
				return button;
		return null;
	}

	/** A press that ended without becoming a drag: select the destination, maybe send. */
	@Unique
	private void stockkeeperbookmarks$activate(AddressButton button) {
		addressBox.setValue(button.getAddress());
		addressBox.setFocused(false);
		searchBox.setFocused(false);
		for (AddressButton other : stockkeeperbookmarks$buttons)
			other.setSelected(other == button);

		if (AddressBookConfig.CLICK_TO_SEND.get())
			sendIt();
	}

	/**
	 * Rebuilding re-runs init() and rebuilds the column, so it cannot happen while the click
	 * that triggered it is still being dispatched. Defer it to the start of the next tick.
	 */
	@Unique
	private void stockkeeperbookmarks$scheduleRebuild() {
		Minecraft.getInstance().execute(this::rebuildWidgets);
	}

	// --- Footer: pin button + shortened Send ---------------------------------------------

	@Unique private int stockkeeperbookmarks$rowY() {
		return getGuiTop() + windowHeight + STOCKKEEPERBOOKMARKS$ROW_DY;
	}

	@Unique private int stockkeeperbookmarks$plusX() {
		return getGuiLeft() + STOCKKEEPERBOOKMARKS$ROW_DX;
	}

	@Unique
	private boolean stockkeeperbookmarks$isPlusHovered(int mouseX, int mouseY) {
		int x = stockkeeperbookmarks$plusX(), y = stockkeeperbookmarks$rowY();
		return mouseX >= x && mouseX < x + STOCKKEEPERBOOKMARKS$PLUS_W
			&& mouseY >= y && mouseY < y + STOCKKEEPERBOOKMARKS$ROW_H;
	}

	/**
	 * Repaint the button row. Injected at the point Create has finished drawing the footer
	 * and the Send hover, but has not yet drawn the Send caption — so the caption (and its
	 * "sent" fade animation) still lands on top of our artwork untouched.
	 *
	 * {@code require = 0} on purpose. This anchors to a string constant inside renderBg rather
	 * than to a method name, so a Create update can move it out from under us; the whole point
	 * of the sidebar is that it keeps working when that happens, and a mandatory injector would
	 * instead crash the game on startup. {@link FooterPatch} carries the outcome to everything
	 * that assumes this artwork exists.
	 */
	@Inject(method = "renderBg", require = 0,
		at = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.title"))
	private void stockkeeperbookmarks$drawFooterRow(GuiGraphics graphics, float partialTick, int mouseX,
		int mouseY, CallbackInfo ci) {

		FooterPatch.markApplied();

		int x = stockkeeperbookmarks$plusX();
		int y = stockkeeperbookmarks$rowY();

		boolean plusHovered = stockkeeperbookmarks$isPlusHovered(mouseX, mouseY);
		int plusU = plusHovered ? STOCKKEEPERBOOKMARKS$HOVER_U : STOCKKEEPERBOOKMARKS$BASE_U;
		int plusV = plusHovered ? STOCKKEEPERBOOKMARKS$HOVER_V : STOCKKEEPERBOOKMARKS$BASE_V;

		// Pin button: the button's left edge plus fill, then its 1px outline column reused
		// on the right so the square closes cleanly.
		graphics.blit(STOCKKEEPERBOOKMARKS$SHEET, x, y, plusU, plusV,
			STOCKKEEPERBOOKMARKS$PLUS_W - 1, STOCKKEEPERBOOKMARKS$ROW_H);
		graphics.blit(STOCKKEEPERBOOKMARKS$SHEET, x + STOCKKEEPERBOOKMARKS$PLUS_W - 1, y, plusU, plusV,
			1, STOCKKEEPERBOOKMARKS$ROW_H);

		// Gutter, copied from the footer's own background so it matches exactly.
		graphics.blit(STOCKKEEPERBOOKMARKS$SHEET, x + STOCKKEEPERBOOKMARKS$PLUS_W, y,
			STOCKKEEPERBOOKMARKS$BG_U, STOCKKEEPERBOOKMARKS$BG_V, STOCKKEEPERBOOKMARKS$GAP_W,
			STOCKKEEPERBOOKMARKS$ROW_H);

		// Shortened Send: original left edge, then the tail end so the arrow point survives.
		boolean sendHovered = stockkeeperbookmarks$isSendHovered(mouseX, mouseY);
		int sendU = sendHovered ? STOCKKEEPERBOOKMARKS$HOVER_U : STOCKKEEPERBOOKMARKS$BASE_U;
		int sendV = sendHovered ? STOCKKEEPERBOOKMARKS$HOVER_V : STOCKKEEPERBOOKMARKS$BASE_V;
		int sendX = getGuiLeft() + STOCKKEEPERBOOKMARKS$SEND_DX;

		graphics.blit(STOCKKEEPERBOOKMARKS$SHEET, sendX, y, sendU, sendV, 2, STOCKKEEPERBOOKMARKS$ROW_H);
		graphics.blit(STOCKKEEPERBOOKMARKS$SHEET, sendX + 2, y,
			sendU + STOCKKEEPERBOOKMARKS$BTN_W - (STOCKKEEPERBOOKMARKS$SEND_W - 2), sendV,
			STOCKKEEPERBOOKMARKS$SEND_W - 2, STOCKKEEPERBOOKMARKS$ROW_H);

		// "+" glyph, matching the Send caption's ink and baseline.
		String plus = "+";
		graphics.drawString(font, plus,
			x + STOCKKEEPERBOOKMARKS$PLUS_W / 2 - font.width(plus) / 2,
			getGuiTop() + windowHeight - 35, 0x252525, false);
	}

	/**
	 * Nudge Create's Send caption to the centre of the shortened button.
	 *
	 * The slice must be closed at both ends. Create draws the caption twice (a plain branch
	 * and a faded "just sent" branch), and immediately afterwards draws the "Request Sent"
	 * ribbon with the same overload — an open-ended slice shifts that off-centre too.
	 */
	@ModifyArg(method = "renderBg", require = 0,
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.send"),
			to = @At(value = "CONSTANT", args = "stringValue=gui.stock_keeper.request_sent")),
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
		index = 2)
	private int stockkeeperbookmarks$shiftSendCaption(int labelX) {
		return labelX + STOCKKEEPERBOOKMARKS$LABEL_SHIFT;
	}

	@Unique
	private boolean stockkeeperbookmarks$isSendHovered(int mouseX, int mouseY) {
		int x = getGuiLeft() + STOCKKEEPERBOOKMARKS$SEND_DX;
		int y = stockkeeperbookmarks$rowY();
		return mouseX >= x && mouseX < x + STOCKKEEPERBOOKMARKS$SEND_W
			&& mouseY >= y && mouseY < y + STOCKKEEPERBOOKMARKS$ROW_H;
	}

	/**
	 * Shrink Create's own hit test to the shortened button — but only when the footer was really
	 * repainted. Without that check a dropped artwork injection would leave Send drawn full width
	 * while answering clicks on only part of it.
	 */
	@Inject(method = "isConfirmHovered", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$narrowConfirm(int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		if (!FooterPatch.isApplied())
			return;
		cir.setReturnValue(stockkeeperbookmarks$isSendHovered(mouseX, mouseY));
	}

	/** Pin whatever the address field currently holds. */
	@Unique
	private void stockkeeperbookmarks$pin() {
		boolean pinned = BookmarkStore.add(stockkeeperbookmarks$key, addressBox.getValue());
		if (pinned)
			stockkeeperbookmarks$scheduleRebuild();
		// With the tooltips gone, a flat low click is the only cue that a pin was
		// refused — the address was blank or already pinned.
		Minecraft.getInstance()
			.getSoundManager()
			.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pinned ? 1.0F : 0.5F));
	}

	/**
	 * Keyboard route to the pin action, so pinning survives the footer injection being dropped.
	 *
	 * Deliberately live while the address box has focus: typing an address and then pinning it is
	 * the entire flow, and demanding the box be unfocused first would make the key useless exactly
	 * when it is the only control left. The search box is left alone so item searches can still
	 * contain the character.
	 */
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$pinKey(int keyCode, int scanCode, int modifiers,
		CallbackInfoReturnable<Boolean> cir) {

		stockkeeperbookmarks$swallowChar = false;
		if (searchBox.isFocused() || !ModKeys.PIN.matches(keyCode, scanCode))
			return;

		stockkeeperbookmarks$pin();
		// GLFW reports the typed character through a separate callback, so cancelling the key
		// event is not on its own enough to keep a printable bind out of the focused address box.
		stockkeeperbookmarks$swallowChar = true;
		cir.setReturnValue(true);
	}

	/** Drop the character belonging to a keystroke already consumed as a pin. */
	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$swallowPinChar(char codePoint, int modifiers,
		CallbackInfoReturnable<Boolean> cir) {
		if (!stockkeeperbookmarks$swallowChar)
			return;
		stockkeeperbookmarks$swallowChar = false;
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$press(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {

		// Only claim the + area when that button was actually drawn there.
		if (button == 0 && FooterPatch.isApplied()
			&& stockkeeperbookmarks$isPlusHovered((int) mouseX, (int) mouseY)) {
			stockkeeperbookmarks$pin();
			cir.setReturnValue(true);
			return;
		}

		AddressButton hit = stockkeeperbookmarks$buttonAt(mouseX, mouseY);
		if (hit == null)
			return;

		if (button == 1) {
			BookmarkStore.remove(stockkeeperbookmarks$key, hit.getAddress());
			stockkeeperbookmarks$scheduleRebuild();
			cir.setReturnValue(true);
			return;
		}

		if (button != 0)
			return;

		// Hold the press open: only the release knows whether this was a click or a drag.
		stockkeeperbookmarks$pressed = hit;
		stockkeeperbookmarks$pressX = mouseX;
		stockkeeperbookmarks$pressY = mouseY;
		stockkeeperbookmarks$grabDy = (int) (mouseY - hit.getY());
		stockkeeperbookmarks$dragging = false;
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$drag(double mouseX, double mouseY, int button, double dragX,
		double dragY, CallbackInfoReturnable<Boolean> cir) {

		if (stockkeeperbookmarks$pressed == null || button != 0)
			return;

		if (!stockkeeperbookmarks$dragging) {
			if (Math.abs(mouseX - stockkeeperbookmarks$pressX) < STOCKKEEPERBOOKMARKS$DRAG_SLOP
				&& Math.abs(mouseY - stockkeeperbookmarks$pressY) < STOCKKEEPERBOOKMARKS$DRAG_SLOP) {
				cir.setReturnValue(true);
				return;
			}
			stockkeeperbookmarks$dragging = true;
			stockkeeperbookmarks$pressed.setDragging(true);
		}

		stockkeeperbookmarks$updateDrag(mouseY);
		cir.setReturnValue(true);
	}

	/**
	 * Place the lifted button and work out where it would drop.
	 *
	 * Also runs per frame while a drag is held, because auto-scrolling at the viewport edge
	 * has to keep going when the cursor is pressed against it and no longer moving.
	 */
	@Unique
	private void stockkeeperbookmarks$updateDrag(double mouseY) {
		int top = stockkeeperbookmarks$columnTop;
		int bottom = top + stockkeeperbookmarks$viewportHeight;

		if (mouseY < top + STOCKKEEPERBOOKMARKS$AUTOSCROLL_EDGE)
			stockkeeperbookmarks$scrollTo(stockkeeperbookmarks$scroll - STOCKKEEPERBOOKMARKS$AUTOSCROLL_SPEED);
		else if (mouseY > bottom - STOCKKEEPERBOOKMARKS$AUTOSCROLL_EDGE)
			stockkeeperbookmarks$scrollTo(stockkeeperbookmarks$scroll + STOCKKEEPERBOOKMARKS$AUTOSCROLL_SPEED);

		// Keep the lifted copy inside the viewport; it is drawn clipped to it anyway.
		stockkeeperbookmarks$ghostY = Mth.clamp((int) mouseY - stockkeeperbookmarks$grabDy,
			stockkeeperbookmarks$contentTop(), bottom - STOCKKEEPERBOOKMARKS$ROW_HEIGHT);

		// The slots move with the scroll offset, so the drop index is a content-space figure.
		int pitch = stockkeeperbookmarks$pitch();
		int last = stockkeeperbookmarks$buttons.size() - 1;
		int contentY = stockkeeperbookmarks$ghostY - stockkeeperbookmarks$contentTop()
			+ stockkeeperbookmarks$scroll;
		int target = Mth.clamp(Math.round(contentY / (float) pitch), 0, last);

		int current = stockkeeperbookmarks$buttons.indexOf(stockkeeperbookmarks$pressed);
		if (target != current) {
			stockkeeperbookmarks$buttons.remove(current);
			stockkeeperbookmarks$buttons.add(target, stockkeeperbookmarks$pressed);
			stockkeeperbookmarks$layout();
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$scrollColumn(double mouseX, double mouseY, double scrollX,
		double scrollY, CallbackInfoReturnable<Boolean> cir) {
		if (stockkeeperbookmarks$maxScroll() <= 0 || !stockkeeperbookmarks$inViewport(mouseX, mouseY))
			return;
		stockkeeperbookmarks$scrollTo(
			stockkeeperbookmarks$scroll - (int) Math.signum(scrollY) * stockkeeperbookmarks$pitch());
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void stockkeeperbookmarks$release(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {

		if (stockkeeperbookmarks$pressed == null || button != 0)
			return;

		AddressButton released = stockkeeperbookmarks$pressed;
		stockkeeperbookmarks$pressed = null;

		if (stockkeeperbookmarks$dragging) {
			stockkeeperbookmarks$dragging = false;
			released.setDragging(false);
			stockkeeperbookmarks$layout();
			BookmarkStore.set(stockkeeperbookmarks$key, stockkeeperbookmarks$buttons.stream()
				.map(AddressButton::getAddress)
				.toList());
		} else {
			stockkeeperbookmarks$activate(released);
		}
		cir.setReturnValue(true);
	}

	/**
	 * Draw the column, clipped to its viewport. Done here rather than through the screen's
	 * renderable list so the scissor can wrap it and the lifted button always lands on top.
	 *
	 * HEAD, not TAIL, for layering: Create renders its tooltips at the end of renderForeground,
	 * so a TAIL inject would paint this column on top of them.
	 *
	 * Note that the inject position is *not* what keeps tooltips from being clipped — the
	 * explicit flush below is. See the scissor comment there before moving anything.
	 */
	@Inject(method = "renderForeground", at = @At("HEAD"))
	private void stockkeeperbookmarks$drawColumn(GuiGraphics graphics, int mouseX, int mouseY,
		float partialTick, CallbackInfo ci) {

		if (stockkeeperbookmarks$buttons.isEmpty())
			return;

		// A held drag keeps scrolling at the edge even while the cursor sits still, so the
		// drag has to advance per frame and not only on mouse-move events.
		if (stockkeeperbookmarks$dragging)
			stockkeeperbookmarks$updateDrag(mouseY);

		int width = stockkeeperbookmarks$buttonWidth;
		int top = stockkeeperbookmarks$columnTop;
		int bottom = top + stockkeeperbookmarks$viewportHeight;
		AddressButton hovered = stockkeeperbookmarks$dragging ? null
			: stockkeeperbookmarks$buttonAt(mouseX, mouseY);

		// Flush before narrowing the clip. GuiGraphics flushes whatever geometry is still
		// queued at the moment the scissor changes, so anything another mod has in flight —
		// JEI's tooltips are the observed case — would be drawn clipped to this column and
		// come out blank. Same failure mode as the address tooltip below, from the other side:
		// moving to HEAD only changed *whose* geometry was caught by it.
		graphics.flush();

		graphics.enableScissor(stockkeeperbookmarks$columnX - 1, top, stockkeeperbookmarks$columnX + width + 1,
			bottom);
		for (AddressButton button : stockkeeperbookmarks$buttons)
			button.render(graphics, button == hovered);
		if (stockkeeperbookmarks$dragging && stockkeeperbookmarks$pressed != null)
			stockkeeperbookmarks$pressed.renderGhost(graphics, stockkeeperbookmarks$columnX,
				stockkeeperbookmarks$ghostY);
		graphics.disableScissor();

		stockkeeperbookmarks$drawScrollbar(graphics, top);
	}

	@Unique
	private void stockkeeperbookmarks$drawScrollbar(GuiGraphics graphics, int top) {
		int maxScroll = stockkeeperbookmarks$maxScroll();
		if (maxScroll <= 0)
			return;

		int viewport = stockkeeperbookmarks$viewportHeight;
		int x = stockkeeperbookmarks$columnLeft();
		int thumbHeight = Math.max(8, viewport * viewport / stockkeeperbookmarks$contentHeight());
		int thumbY = top + (viewport - thumbHeight) * stockkeeperbookmarks$scroll / maxScroll;

		graphics.fill(x, top, x + STOCKKEEPERBOOKMARKS$SCROLLBAR_W, top + viewport,
			STOCKKEEPERBOOKMARKS$SCROLLBAR_TRACK);
		graphics.fill(x, thumbY, x + STOCKKEEPERBOOKMARKS$SCROLLBAR_W, thumbY + thumbHeight,
			STOCKKEEPERBOOKMARKS$SCROLLBAR_THUMB);
	}

	/** Keep JEI/EMI from drawing on top of the destination column. */
	@Inject(method = "getExtraAreas", at = @At("RETURN"), cancellable = true)
	private void stockkeeperbookmarks$reserveColumn(CallbackInfoReturnable<List<Rect2i>> cir) {
		if (stockkeeperbookmarks$reservedArea == null)
			return;
		List<Rect2i> areas = new ArrayList<>(cir.getReturnValue());
		areas.add(stockkeeperbookmarks$reservedArea);
		cir.setReturnValue(areas);
	}
}
