package com.meshi.fairy.client;

import com.meshi.fairy.MeshiFairy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = MeshiFairy.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    // forget cached skin fairies when leaving a world
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        SkinFairyTextures.clear();
    }
}
