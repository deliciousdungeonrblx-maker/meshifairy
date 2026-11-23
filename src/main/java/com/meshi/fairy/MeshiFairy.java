package com.meshi.fairy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(MeshiFairy.MODID)
public class MeshiFairy {
    public static final String MODID = "meshifairy";

    public MeshiFairy(IEventBus bus) {
        ModEntities.ENTITIES.register(bus);
        bus.addListener(MeshiFairy::attributes);
        bus.addListener(FairyNet::register);
        NeoForge.EVENT_BUS.addListener(MeshiFairy::commands);
        NeoForge.EVENT_BUS.addListener(FairyManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(FairyManager::onServerStopped);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.FAIRY.get(), FairyEntity.attributes().build());
    }

    private static void commands(RegisterCommandsEvent e) {
        FairyCommands.register(e.getDispatcher());
    }
}
