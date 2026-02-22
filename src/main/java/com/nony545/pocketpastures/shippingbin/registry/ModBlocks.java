package com.nony545.pocketpastures.shippingbin.registry;

import com.nony545.pocketpastures.shippingbin.ShippingBin;
import com.nony545.pocketpastures.shippingbin.block.ShippingBinBlock;
import com.nony545.pocketpastures.shippingbin.tier.ShippingTier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ShippingBin.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ShippingBin.MODID);

    // WOOD
    public static final DeferredBlock<Block> WOOD_SHIPPING_BIN =
            BLOCKS.register("wood_shipping_bin", () ->
                    new ShippingBinBlock(ShippingTier.WOOD,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                                    .strength(0.75F, 0.75F) // furnace-like hardness
                                    .sound(SoundType.WOOD)
                    )
            );
    public static final DeferredItem<BlockItem> WOOD_SHIPPING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("wood_shipping_bin", WOOD_SHIPPING_BIN);

    // IRON
    public static final DeferredBlock<Block> IRON_SHIPPING_BIN =
            BLOCKS.register("iron_shipping_bin", () ->
                    new ShippingBinBlock(ShippingTier.IRON,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                                    .strength(0.75F, .75F)
                                    .sound(SoundType.METAL)
                    )
            );
    public static final DeferredItem<BlockItem> IRON_SHIPPING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("iron_shipping_bin", IRON_SHIPPING_BIN);

    // GOLD
    public static final DeferredBlock<Block> GOLD_SHIPPING_BIN =
            BLOCKS.register("gold_shipping_bin", () ->
                    new ShippingBinBlock(ShippingTier.GOLD,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                                    .strength(0.75F, 0.75F)
                                    .sound(SoundType.METAL)
                    )
            );
    public static final DeferredItem<BlockItem> GOLD_SHIPPING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("gold_shipping_bin", GOLD_SHIPPING_BIN);

    // DIAMOND
    public static final DeferredBlock<Block> DIAMOND_SHIPPING_BIN =
            BLOCKS.register("diamond_shipping_bin", () ->
                    new ShippingBinBlock(ShippingTier.DIAMOND,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                                    .strength(0.75F, 0.75F)
                                    .sound(SoundType.METAL)
                    )
            );
    public static final DeferredItem<BlockItem> DIAMOND_SHIPPING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("diamond_shipping_bin", DIAMOND_SHIPPING_BIN);

    // NETHERITE (still “special” via sound + blast resistance, but breaks like furnace)
    public static final DeferredBlock<Block> NETHERITE_SHIPPING_BIN =
            BLOCKS.register("netherite_shipping_bin", () ->
                    new ShippingBinBlock(ShippingTier.NETHERITE,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                                    .strength(0.75F, 1200.0F) // easy to break, explosion-proof
                                    .sound(SoundType.NETHERITE_BLOCK)
                    )
            );
    public static final DeferredItem<BlockItem> NETHERITE_SHIPPING_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("netherite_shipping_bin", NETHERITE_SHIPPING_BIN);
}