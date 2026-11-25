package com.meshi.fairy;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(MeshiFairy.MODID)
public class MeshiFairy {
    public static final String MODID = "meshifairy";

    public MeshiFairy(IEventBus bus) {
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        bus.addListener(MeshiFairy::tabs);
        NeoForge.EVENT_BUS.addListener(MeshiFairy::brewing);
        bus.addListener(MeshiFairy::attributes);
        bus.addListener(FairyNet::register);
        NeoForge.EVENT_BUS.addListener(MeshiFairy::commands);
        NeoForge.EVENT_BUS.addListener(FairyManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(FairyManager::onServerStopped);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.FAIRY.get(), FairyEntity.attributes().build());
    }

    private static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) e.accept(ModItems.FAIRY_MASH.get());
        if (e.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) e.accept(ModItems.FAIRY_FLASK.get());
    }

    private static void brewing(RegisterBrewingRecipesEvent e) {
        e.getBuilder().addRecipe(new FairyFlaskBrewing());
    }

    private static void commands(RegisterCommandsEvent e) {
        FairyCommands.register(e.getDispatcher());
    }
}
