package com.nony545.pocketpastures.shippingbin.config;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ShippingBinConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PRICE_ENTRIES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("pricing");

        PRICE_ENTRIES = builder
                .comment(
                        "Fallback pricing used only when the datapack does not define a price.",
                        "Format: modid:item=value",
                        "Example: minecraft:carrot=2"
                )
                .defineListAllowEmpty(
                        "prices",
                        List.of(
                                "minecraft:wheat=1",
                                "minecraft:carrot=1",
                                "minecraft:potato=1"
                        ),
                        obj -> obj instanceof String
                );

        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}