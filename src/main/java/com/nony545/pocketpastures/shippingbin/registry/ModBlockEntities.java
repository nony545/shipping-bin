package com.nony545.pocketpastures.shippingbin.registry;

import com.nony545.pocketpastures.shippingbin.ShippingBin;
import com.nony545.pocketpastures.shippingbin.block.entity.ShippingBinBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ShippingBin.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShippingBinBlockEntity>> SHIPPING_BIN =
        BLOCK_ENTITIES.register("shipping_bin", () ->
                BlockEntityType.Builder.of(
                        ShippingBinBlockEntity::new,
                        ModBlocks.WOOD_SHIPPING_BIN.get(),
                        ModBlocks.IRON_SHIPPING_BIN.get(),
                        ModBlocks.GOLD_SHIPPING_BIN.get(),
                        ModBlocks.DIAMOND_SHIPPING_BIN.get(),
                        ModBlocks.NETHERITE_SHIPPING_BIN.get()
                ).build(null)
        );
}