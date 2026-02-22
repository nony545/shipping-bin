package com.nony545.pocketpastures.shippingbin.pricing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nony545.pocketpastures.shippingbin.config.ShippingBinConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    // datapack file: data/pp_shippingbin/prices/shipping_prices.json
    // with folder = "prices", this becomes: pp_shippingbin:shipping_prices
    private static final ResourceLocation FILE_ID =
            ResourceLocation.fromNamespaceAndPath("pp_shippingbin", "shipping_prices");

    // Datapack-driven prices (highest priority)
    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();

    // COMMON config fallback prices (lowest priority)
    private static final Map<ResourceLocation, Integer> CONFIG_PRICES = new HashMap<>();

    public PriceManager() {
        // Looks for: data/<namespace>/prices/*.json
        super(GSON, "prices");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {

        PRICES.clear();

        // Load datapack file (if present)
        JsonElement element = objects.get(FILE_ID);
        if (element != null && element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();
            if (root.has("prices") && root.get("prices").isJsonObject()) {
                JsonObject pricesObj = root.getAsJsonObject("prices");

                for (String key : pricesObj.keySet()) {
                    try {
                        ResourceLocation itemId = ResourceLocation.parse(key);
                        int price = pricesObj.get(key).getAsInt();
                        if (price <= 0) continue;

                        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                            PRICES.put(itemId, price);
                        }
                    } catch (Exception ignored) { }
                }
            }
        }

        // Always refresh config fallback too (so restarting after editing config works cleanly)
        loadConfigPrices();

        // Optional debug:
        // System.out.println("[PP] Loaded datapack price entries: " + PRICES.size()
        //        + " | config fallback entries: " + CONFIG_PRICES.size());
    }

    private static void loadConfigPrices() {
        CONFIG_PRICES.clear();

        List<? extends String> entries;
        try {
            entries = ShippingBinConfig.PRICE_ENTRIES.get();
        } catch (Exception e) {
            // Config not registered yet or not available on this side
            return;
        }

        for (String entry : entries) {
            try {
                // Format: modid:item=value
                String[] split = entry.split("=");
                if (split.length != 2) continue;

                ResourceLocation id = ResourceLocation.parse(split[0].trim());
                int price = Integer.parseInt(split[1].trim());
                if (price <= 0) continue;

                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    CONFIG_PRICES.put(id, price);
                }
            } catch (Exception ignored) { }
        }
    }

    /**
     * Price per single item.
     * 0 = unsellable.
     *
     * Priority:
     * 1) datapack
     * 2) common config fallback
     */
    public static int getUnitPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        Integer dp = PRICES.get(id);
        if (dp != null) return dp;

        Integer cfg = CONFIG_PRICES.get(id);
        if (cfg != null) return cfg;

        return 0;
    }
}