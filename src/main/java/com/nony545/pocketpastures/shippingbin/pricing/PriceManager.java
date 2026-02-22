package com.nony545.pocketpastures.shippingbin.pricing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class PriceManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    // This is the datapack path: data/pp_shippingbin/prices/shipping_prices.json
    public static final ResourceLocation FILE_ID =
            ResourceLocation.fromNamespaceAndPath("pp_shippingbin", "prices/shipping_prices");

    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();

    public PriceManager() {
        super(GSON, "pp_shippingbin/prices");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        // We only care about one file: pp_shippingbin:prices/shipping_prices
        JsonElement el = objects.get(FILE_ID);
        PRICES.clear();

        if (el == null || !el.isJsonObject()) return;

        JsonObject root = el.getAsJsonObject();
        JsonObject pricesObj = root.has("prices") && root.get("prices").isJsonObject()
                ? root.getAsJsonObject("prices")
                : null;

        if (pricesObj == null) return;

        for (String key : pricesObj.keySet()) {
            try {
                ResourceLocation itemId = ResourceLocation.parse(key);
                int price = pricesObj.get(key).getAsInt();
                if (price <= 0) continue;

                // Only store if the item actually exists
                if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                    PRICES.put(itemId, price);
                }
            } catch (Exception ignored) {
                // If someone puts bad json/id, we just skip it
            }
        }
    }

    /** price per single item. 0 = unsellable */
    public static int getUnitPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return PRICES.getOrDefault(id, 0);
    }
}