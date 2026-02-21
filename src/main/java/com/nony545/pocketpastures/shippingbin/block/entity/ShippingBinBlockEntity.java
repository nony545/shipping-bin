package com.nony545.pocketpastures.shippingbin.block.entity;

import com.nony545.pocketpastures.shippingbin.block.ShippingBinBlock;
import com.nony545.pocketpastures.shippingbin.registry.ModBlockEntities;
import com.nony545.pocketpastures.shippingbin.tier.ShippingTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ShippingBinBlockEntity extends BlockEntity implements Container, MenuProvider {

    private NonNullList<ItemStack> items;
    private long lastSoldDay = -1;

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

    // Sell once per day, anytime after midnight
    if (timeOfDay >= 18000 && day != lastSoldDay) {


        System.out.println("Selling at " + worldPosition + " day=" + day);


        lastSoldDay = day;
        shipAndPayoutDiamonds();
        setChanged();
    }
}

    private void shipAndPayoutDiamonds() {
        if (level == null || level.isClientSide) return;

        int totalItems = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty()) totalItems += s.getCount();
        }
        if (totalItems <= 0) return;

        // Clear inventory
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        // Pay diamonds: 1 item = 1 diamond (placeholder)
        spawnDiamonds(totalItems);
    }

    private void spawnDiamonds(int amount) {
        if (level == null) return;

        BlockPos p = getBlockPos().above();
        while (amount > 0) {
            int stackSize = Math.min(64, amount);
            amount -= stackSize;

            ItemStack diamonds = new ItemStack(Items.DIAMOND, stackSize);
            ItemEntity ent = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, diamonds);
            ent.setDefaultPickUpDelay();
            level.addFreshEntity(ent);
        }
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
    @Override public boolean isEmpty() { for (ItemStack s : items) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && !isRemoved() && player.distanceToSqr(worldPosition.getCenter()) <= 64.0; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    // ---- NBT ----
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putLong("LastSoldDay", lastSoldDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(getTier().slots(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        this.lastSoldDay = tag.getLong("LastSoldDay");
    }
}