package com.meshi.fairy;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MeshiFairy.MODID);

    public static final DeferredItem<Item> FAIRY_MASH =
            ITEMS.register("fairy_mash", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FAIRY_FLASK =
            ITEMS.register("fairy_flask", () -> new FairyFlaskItem(new Item.Properties().stacksTo(1)));
}
