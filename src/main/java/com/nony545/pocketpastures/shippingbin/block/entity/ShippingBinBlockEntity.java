package com.nony545.pocketpastures.shippingbin.block.entity;

import com.nony545.pocketpastures.shippingbin.block.ShippingBinBlock;
import com.nony545.pocketpastures.shippingbin.pricing.PriceManager;
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
import net.minecraft.world.Containers;
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

    /** Currency placeholder (easy to swap later). */
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

        // Detect day rollover (sleep/time skip safety)
        if (lastSeenDay != -1 && day > lastSeenDay) {
            if (day != lastMidnightSoldDay) {
                lastMidnightSoldDay = day;
                shipAndPayout();
                setChanged();
            }
        }
        lastSeenDay = day;

        // Noon sell for Netherite (TWICE_DAILY)
        if (tier.schedule == ShippingTier.Schedule.TWICE_DAILY) {
            if (timeOfDay >= 6000 && day != lastNoonSoldDay) {
                lastNoonSoldDay = day;
                shipAndPayout();
                setChanged();
            }
        }

        // Midnight sell for all tiers
        if (timeOfDay >= 18000 && day != lastMidnightSoldDay) {
            lastMidnightSoldDay = day;
            shipAndPayout();
            setChanged();
        }
    }

    /**
     * Sells only items with a price in PriceManager.
     * - Currency stacks are never deleted regardless of slot
     * - Unsellables (no price) stay in the bin
     */
    private void shipAndPayout() {
        if (level == null || level.isClientSide) return;

        long payout = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            // Protect currency anywhere in inventory
            if (stack.is(PAYOUT_ITEM)) continue;

            int unitPrice = PriceManager.getUnitPrice(stack);
            if (unitPrice <= 0) {
                // Unsellable: leave it in the bin
                continue;
            }

            // Sell it
            payout += (long) unitPrice * (long) stack.getCount();
            items.set(i, ItemStack.EMPTY);
        }

        if (payout <= 0) return;

        // addPayout takes an int, clamp safely
        int payoutInt = (payout > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) payout;
        addPayout(payoutInt);
    }

    /**
     * Fills inventory with payout currency.
     * If inventory is full, overflow is dropped on the ground.
     */
    private void addPayout(int amount) {
        if (amount <= 0) return;

        ItemStack proto = new ItemStack(PAYOUT_ITEM);
        int maxStack = proto.getMaxStackSize();
        int remaining = amount;

        // 1) Top off existing currency stacks
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(PAYOUT_ITEM)) continue;

            int space = maxStack - stack.getCount();
            if (space <= 0) continue;

            int add = Math.min(space, remaining);
            stack.grow(add);
            items.set(i, stack);
            remaining -= add;
        }

        // 2) Fill empty slots
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) continue;

            int add = Math.min(maxStack, remaining);
            items.set(i, new ItemStack(PAYOUT_ITEM, add));
            remaining -= add;
        }

        // 3) Drop overflow on ground
        if (remaining > 0 && level != null && !level.isClientSide) {
            while (remaining > 0) {
                int drop = Math.min(maxStack, remaining);
                ItemStack dropStack = new ItemStack(PAYOUT_ITEM, drop);

                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        dropStack
                );

                remaining -= drop;
            }
        }

        setChanged();
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
    @Override public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true; // currency can exist anywhere
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && !isRemoved()
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public void clearContent() {
        // Don't shrink the list; just empty stacks
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ---- Hopper Automation ----

    private boolean allowsAutomation() {
        return getTier().automatable;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (!allowsAutomation()) return new int[0];

        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return allowsAutomation();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (!allowsAutomation()) return false;
        if (dir != Direction.DOWN) return false;
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