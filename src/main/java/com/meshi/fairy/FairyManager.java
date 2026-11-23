package com.meshi.fairy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// fairy registry + selection/call state. maps read from the voice thread are concurrent
public final class FairyManager {
    private static final int RING_DELAY = 200; // 10s of no cycling

    private static final Map<UUID, FairyEntity> FAIRIES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PARTNER = new ConcurrentHashMap<>();
    private static final Map<UUID, Sel> SEL = new HashMap<>();

    private record Sel(UUID target, int at) {}

    private FairyManager() {}

    public static void register(FairyEntity f) {
        UUID o = f.getOwnerId();
        if (o == null) return;
        FairyEntity old = FAIRIES.put(o, f);
        if (old != null && old != f) old.discard();
    }

    public static void unregister(FairyEntity f) {
        UUID o = f.getOwnerId();
        if (o != null && FAIRIES.remove(o, f)) {
            hangUp(o);
            SEL.remove(o);
        }
    }

    public static FairyEntity fairyOf(UUID owner) { return FAIRIES.get(owner); }
    public static UUID partnerOf(UUID owner) { return PARTNER.get(owner); }
    public static boolean inCall(UUID owner) { return PARTNER.containsKey(owner); }

    private static void hangUp(UUID a) {
        UUID b = PARTNER.remove(a);
        if (b != null) PARTNER.remove(b);
    }

    // right click on your own fairy
    public static void cycle(ServerPlayer p) {
        UUID me = p.getUUID();
        if (PARTNER.containsKey(me)) { // clicking during a call hangs up
            hangUp(me);
            SEL.remove(me);
            FairyNet.send(p, me);
            return;
        }
        List<UUID> opts = new ArrayList<>();
        opts.add(me);
        FAIRIES.keySet().stream()
                .filter(u -> !u.equals(me))
                .sorted(Comparator.comparing(u -> nameOf(p.server, u)))
                .forEach(opts::add);

        Sel cur = SEL.get(me);
        int idx = cur == null ? 0 : Math.max(0, opts.indexOf(cur.target()));
        UUID next = opts.get((idx + 1) % opts.size());
        if (next.equals(me)) SEL.remove(me);
        else SEL.put(me, new Sel(next, p.server.getTickCount()));
        FairyNet.send(p, next);
    }

    private static String nameOf(MinecraftServer s, UUID id) {
        ServerPlayer sp = s.getPlayerList().getPlayer(id);
        return sp == null ? "" : sp.getGameProfile().getName();
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (SEL.isEmpty()) return;
        MinecraftServer s = e.getServer();
        int now = s.getTickCount();
        new HashMap<>(SEL).forEach((caller, sel) -> {
            if (now - sel.at() < RING_DELAY) return;
            SEL.remove(caller);
            ServerPlayer cp = s.getPlayerList().getPlayer(caller);
            FairyEntity mine = FAIRIES.get(caller);
            FairyEntity theirs = FAIRIES.get(sel.target());
            if (cp == null || mine == null) return;
            if (theirs == null) {
                FairyNet.send(cp, caller);
                return;
            }
            if (PARTNER.containsKey(sel.target())) { // already on a call
                mine.startBusy();
                FairyNet.send(cp, caller);
                return;
            }
            SEL.remove(sel.target());
            PARTNER.put(caller, sel.target());
            PARTNER.put(sel.target(), caller);
            theirs.ring();
        });
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        FAIRIES.clear();
        PARTNER.clear();
        SEL.clear();
    }
}
