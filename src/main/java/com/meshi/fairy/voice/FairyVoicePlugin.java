package com.meshi.fairy.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.meshi.fairy.FairyEntity;
import com.meshi.fairy.FairyManager;
import com.meshi.fairy.MeshiFairy;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

// runs on the voice chat thread, so only touch the concurrent bits of FairyManager / volatile stuff on the entity
@ForgeVoicechatPlugin
public class FairyVoicePlugin implements VoicechatPlugin {
    private volatile VoicechatServerApi api;
    private final Map<UUID, Relay> relays = new ConcurrentHashMap<>();

    private static final class Relay {
        final Chirp chirp;
        EntityAudioChannel channel;
        UUID channelFairy;

        Relay(VoicechatServerApi api) {
            this.chirp = new Chirp(api.createEncoder(), api.createDecoder());
        }
    }

    @Override
    public String getPluginId() {
        return MeshiFairy.MODID;
    }

    @Override
    public void registerEvents(EventRegistration reg) {
        reg.registerEvent(VoicechatServerStartedEvent.class, e -> api = e.getVoicechat());
        reg.registerEvent(MicrophonePacketEvent.class, this::onMic);
    }

    private void onMic(MicrophonePacketEvent e) {
        VoicechatServerApi a = api;
        if (a == null) return;
        VoicechatConnection con = e.getSenderConnection();
        if (con == null) return;
        if (!(con.getPlayer().getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        UUID me = sp.getUUID();
        UUID other = FairyManager.partnerOf(me);
        if (other == null) return;
        FairyEntity mine = FairyManager.fairyOf(me);
        FairyEntity theirs = FairyManager.fairyOf(other);
        if (mine == null || theirs == null) return;

        Relay r = relays.computeIfAbsent(me, k -> new Relay(a));
        if (r.channel == null || !theirs.getUUID().equals(r.channelFairy)) {
            r.channel = a.createEntityAudioChannel(UUID.randomUUID(), a.fromEntity(theirs));
            r.channelFairy = theirs.getUUID();
        }
        if (r.channel == null) return;

        byte[] data = e.getPacket().getOpusEncodedData();
        if (data == null || data.length == 0) { // sender stopped talking
            for (byte[] out : r.chirp.flush()) r.channel.send(out);
            r.channel.flush();
            return;
        }
        mine.markVoice();
        theirs.markVoice();
        for (byte[] out : r.chirp.process(data)) r.channel.send(out);
    }
}
