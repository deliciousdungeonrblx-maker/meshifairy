package com.meshi.fairy;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FairyNet {
    private FairyNet() {}

    // which player face to show on the hud
    public record SelectPayload(UUID id) implements CustomPacketPayload {
        public static final Type<SelectPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "select"));
        public static final StreamCodec<ByteBuf, SelectPayload> CODEC =
                UUIDUtil.STREAM_CODEC.map(SelectPayload::new, SelectPayload::id);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(SelectPayload.TYPE, SelectPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.meshi.fairy.client.FairyClient.showSelection(payload.id())));
    }

    public static void send(ServerPlayer to, UUID selected) {
        PacketDistributor.sendToPlayer(to, new SelectPayload(selected));
    }
}
