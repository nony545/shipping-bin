package com.nony545.pocketpastures.shippingbin.pricing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import com.nony545.pocketpastures.shippingbin.config.ShippingBinConfig;

import java.util.HashMap;
import java.util.Map;

public class PriceManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    // This corresponds to: data/pp_shippingbin/prices/shipping_prices.json
    // When the folder is "prices", the id becomes pp_shippingbin:shipping_prices
    private static final ResourceLocation FILE_ID =
            ResourceLocation.fromNamespaceAndPath("pp_shippingbin", "shipping_prices");

    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();

    public PriceManager() {
        // IMPORTANT: folder under data/<namespace>/ is just "prices"
        super(GSON, "prices");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {

        PRICES.clear();

        JsonElement element = objects.get(FILE_ID);
        if (element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();

        if (!root.has("prices") || !root.get("prices").isJsonObject()) {
            return;
        }

        JsonObject pricesObj = root.getAsJsonObject("prices");

        for (String key : pricesObj.keySet()) {
            try {
                ResourceLocation itemId = ResourceLocation.parse(key);
                int price = pricesObj.get(key).getAsInt();
                if (price <= 0) continue;

                if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                    PRICES.put(itemId, price);
                }
            } catch (Exception ignored) {}
        }

        // Optional debug:
        // System.out.println("[PP] Loaded price entries: " + PRICES.size());
    }

    /** price per single item. 0 = unsellable */
    public static int getUnitPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return PRICES.getOrDefault(id, 0);
    }
}