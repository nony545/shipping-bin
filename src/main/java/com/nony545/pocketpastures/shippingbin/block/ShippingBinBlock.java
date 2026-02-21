package com.nony545.pocketpastures.shippingbin.block;

import com.mojang.serialization.MapCodec;
import com.nony545.pocketpastures.shippingbin.block.entity.ShippingBinBlockEntity;
import com.nony545.pocketpastures.shippingbin.registry.ModBlockEntities;
import com.nony545.pocketpastures.shippingbin.tier.ShippingTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ShippingBinBlock extends BaseEntityBlock {

    public static final MapCodec<ShippingBinBlock> CODEC = simpleCodec(ShippingBinBlock::new);

    private final ShippingTier tier;

    // Required for codec path
    public ShippingBinBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = ShippingTier.WOOD; // placeholder for codec path
    }

    // Used by ModBlocks (your tiered constructor)
    public ShippingBinBlock(ShippingTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ShippingTier getTier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShippingBinBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type) {

    return level.isClientSide
            ? null
            : createTickerHelper(type,
                ModBlockEntities.SHIPPING_BIN.get(),
                ShippingBinBlockEntity::tick);
}

    // Right-click opens inventory
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
            sp.openMenu(provider, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // Drop contents when block is removed
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ShippingBinBlockEntity bin) {
                Containers.dropContents(level, pos, bin);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}