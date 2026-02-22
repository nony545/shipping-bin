package com.nony545.pocketpastures.shippingbin.block;

import com.nony545.pocketpastures.shippingbin.block.entity.ShippingBinBlockEntity;
import com.nony545.pocketpastures.shippingbin.tier.ShippingTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class ShippingBinBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final ShippingTier tier;

    // Constructor matches ModBlocks usage: (ShippingTier, Properties)
    public ShippingBinBlock(ShippingTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public ShippingTier getTier() {
        return tier;
    }

    // Face player when placed
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // Support rotation (structure blocks etc.)
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    // Support mirroring
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // Register blockstate properties (THIS is the generic signature you were likely missing)
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Create BlockEntity
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShippingBinBlockEntity(pos, state);
    }

    // Server-side ticking (typed)
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;

        return (lvl, pos, st, be) -> {
            if (be instanceof ShippingBinBlockEntity bin) {
                ShippingBinBlockEntity.tick(lvl, pos, st, bin);
            }
        };
    }

    // Drop inventory contents when the block is removed/broken (block item drop comes from loot tables)
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ShippingBinBlockEntity bin) {
                Containers.dropContents(level, pos, bin);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // -------------------------
    // RIGHT CLICK INTERACTION
    // -------------------------

    // Empty hand right-click
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider) {
            player.openMenu(provider);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // Right-click while holding an item (1.21+ uses ItemInteractionResult)
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit
    ) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider) {
            player.openMenu(provider);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}