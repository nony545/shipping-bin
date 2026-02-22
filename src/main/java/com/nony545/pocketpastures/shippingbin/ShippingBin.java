package com.nony545.pocketpastures.shippingbin;

import com.mojang.logging.LogUtils;
import com.nony545.pocketpastures.shippingbin.config.ShippingBinConfig;
import com.nony545.pocketpastures.shippingbin.pricing.PriceManager;
import com.nony545.pocketpastures.shippingbin.registry.ModBlockEntities;
import com.nony545.pocketpastures.shippingbin.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ShippingBin.MODID)
public class ShippingBin {
    public static final String MODID = "pp_shippingbin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PP_SHIPPINGBIN_TAB =
            CREATIVE_MODE_TABS.register("pp_shippingbin", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.pp_shippingbin"))
                            .icon(() -> new ItemStack(ModBlocks.WOOD_SHIPPING_BIN_ITEM.get()))
                            .displayItems((params, output) -> {
                                output.accept(ModBlocks.WOOD_SHIPPING_BIN_ITEM.get());
                                output.accept(ModBlocks.IRON_SHIPPING_BIN_ITEM.get());
                                output.accept(ModBlocks.GOLD_SHIPPING_BIN_ITEM.get());
                                output.accept(ModBlocks.DIAMOND_SHIPPING_BIN_ITEM.get());
                                output.accept(ModBlocks.NETHERITE_SHIPPING_BIN_ITEM.get());
                            })
                            .build()
            );

    public ShippingBin(IEventBus modEventBus, ModContainer modContainer) {
        // COMMON config -> config/pp_shippingbin-common.toml
        modContainer.registerConfig(ModConfig.Type.COMMON, ShippingBinConfig.SPEC);

        modEventBus.addListener(this::commonSetup);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Datapack/KubeJS pricing reload support
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Pocket Pastures Shipping Bin loaded successfully.");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PriceManager());
        LOGGER.info("Registered PriceManager reload listener.");
    }
}