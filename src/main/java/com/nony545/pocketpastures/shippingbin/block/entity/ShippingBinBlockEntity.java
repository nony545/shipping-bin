package com.nony545.pocketpastures.shippingbin.block.entity;

import com.nony545.pocketpastures.shippingbin.block.ShippingBinBlock;
import com.nony545.pocketpastures.shippingbin.registry.ModBlockEntities;
import com.nony545.pocketpastures.shippingbin.tier.ShippingTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ShippingBinBlockEntity extends BlockEntity implements Container, WorldlyContainer, MenuProvider {

    /** Slot 0 is reserved for payout buffer (never sold, hopper-extract only). */
    private static final int SLOT_PAYOUT = 0;

    /** Placeholder currency for now; swap later to Magic Coins easily. */
    private static final Item PAYOUT_ITEM = Items.DIAMOND;

    private NonNullList<ItemStack> items;

    private long lastNoonSoldDay = -1;
    private long lastMidnightSoldDay = -1;
    private long lastSeenDay = -1;

    public ShippingBinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIPPING_BIN.get(), pos, state);
        this.items = NonNullList.withSize(getTier().slots(), ItemStack.EMPTY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShippingBinBlockEntity be) {
        be.serverTick();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;

        ShippingTier tier = getTier();

        // Detect day rollover (sleep or time skip). If day advanced, we missed midnight.
        if (lastSeenDay != -1 && day > lastSeenDay) {
            if (day != lastMidnightSoldDay) {
                System.out.println("Rollover sell at " + worldPosition + " day=" + day);
                lastMidnightSoldDay = day;
                shipAndPayout();
                setChanged();
            }
        }
        lastSeenDay = day;

        // Netherite: sell at noon (6000) once per day
        if (tier.schedule == ShippingTier.Schedule.TWICE_DAILY) {
            if (timeOfDay >= 6000 && day != lastNoonSoldDay) {
                System.out.println("Noon sell at " + worldPosition + " day=" + day);
                lastNoonSoldDay = day;
                shipAndPayout();
                setChanged();
            }
        }

        // Everyone: sell at midnight (18000) once per day
        if (timeOfDay >= 18000 && day != lastMidnightSoldDay) {
            System.out.println("Midnight sell at " + worldPosition + " day=" + day);
            lastMidnightSoldDay = day;
            shipAndPayout();
            setChanged();
        }
    }

    /**
     * Sells everything in storage (slots 1..end) and pays currency into SLOT 0.
     * Slot 0 is NEVER sold and is reserved for payout only.
     */
    private void shipAndPayout() {
        if (level == null || level.isClientSide) return;

        int totalItems = 0;

        // Count items in storage slots ONLY
        for (int i = 1; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) totalItems += s.getCount();
        }

        if (totalItems <= 0) return;

        // Clear storage slots ONLY
        for (int i = 1; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        // Placeholder: 1 item sold = 1 diamond payout unit
        addPayout(totalItems);
    }

    /**
     * Adds payout currency into SLOT 0 (stacking up to max stack size).
     * Overflow is ignored for now (we can bank it later).
     */
    private void addPayout(int amount) {
        if (amount <= 0) return;

        ItemStack payout = getItem(SLOT_PAYOUT);

        if (payout.isEmpty()) {
            ItemStack currency = new ItemStack(PAYOUT_ITEM);
            int add = Math.min(currency.getMaxStackSize(), amount);
            currency.setCount(add);
            setItem(SLOT_PAYOUT, currency);
            amount -= add;
        } else if (payout.is(PAYOUT_ITEM) && payout.getCount() < payout.getMaxStackSize()) {
            int add = Math.min(amount, payout.getMaxStackSize() - payout.getCount());
            payout.grow(add);
            setItem(SLOT_PAYOUT, payout);
            amount -= add;
        }

        // Overflow policy later:
        // - bank remainder as an int in NBT (recommended)
        // - or drop to world
        // For now: discard overflow to keep behavior predictable.
    }

    public ShippingTier getTier() {
        if (getBlockState().getBlock() instanceof ShippingBinBlock bin) {
            return bin.getTier();
        }
        return ShippingTier.WOOD;
    }

    // ---- MenuProvider (GUI) ----
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pp_shippingbin.shipping_bin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        // Vanilla 9xN chest menus
        return switch (getTier().rows) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, id, playerInv, this, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, id, playerInv, this, 2);
            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, id, playerInv, this, 4);
            default -> new ChestMenu(MenuType.GENERIC_9x3, id, playerInv, this, 3);
        };
    }

    // ---- Container ----
    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Reserve slot 0 as payout-only (blocks manual placement too)
        return slot != SLOT_PAYOUT;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && !isRemoved() && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public void clearContent() {
        // DO NOT items.clear() — that shrinks the list to size 0 and breaks container logic.
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ---- WorldlyContainer (Hoppers) ----

    private boolean allowsAutomation() {
        // Diamond + Netherite should be automatable in your enum
        return getTier().automatable;
    }

    private int[] storageSlots() {
        int size = getContainerSize();
        if (size <= 1) return new int[0];
        int[] slots = new int[size - 1];
        for (int i = 1; i < size; i++) slots[i - 1] = i;
        return slots;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (!allowsAutomation()) return new int[0];

        // DOWN: extract payout only
        if (side == Direction.DOWN) return new int[]{SLOT_PAYOUT};

        // UP + SIDES: insert into storage only
        return storageSlots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (!allowsAutomation()) return false;
        if (slot == SLOT_PAYOUT) return false;
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (!allowsAutomation()) return false;

        // Only pull payout currency from the bottom
        if (dir != Direction.DOWN) return false;
        if (slot != SLOT_PAYOUT) return false;

        return stack.is(PAYOUT_ITEM);
    }

    // ---- NBT ----
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putLong("LastNoonSoldDay", lastNoonSoldDay);
        tag.putLong("LastMidnightSoldDay", lastMidnightSoldDay);
        tag.putLong("LastSeenDay", lastSeenDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(getTier().slots(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        this.lastNoonSoldDay = tag.getLong("LastNoonSoldDay");
        this.lastMidnightSoldDay = tag.getLong("LastMidnightSoldDay");
        this.lastSeenDay = tag.getLong("LastSeenDay");
    }
}