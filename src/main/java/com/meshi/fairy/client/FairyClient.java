package com.meshi.fairy.client;

import java.util.UUID;

import com.meshi.fairy.FairyEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class FairyClient {
    private static final long SHOW_MS = 5000;
    private static final long FADE_MS = 500;
    private static UUID selected;
    private static long shownAt;

    private FairyClient() {}

    public static void startBuzz(FairyEntity f) {
        Minecraft.getInstance().getSoundManager().play(new BuzzSound(f));
    }

    public static void showSelection(UUID id) {
        selected = id;
        shownAt = Util.getMillis();
    }

    // face icon above hotbar + health, fades after 5s
    public static void renderHud(GuiGraphics g, DeltaTracker dt) {
        if (selected == null) return;
        long age = Util.getMillis() - shownAt;
        if (age > SHOW_MS) {
            selected = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        PlayerInfo info = mc.getConnection().getPlayerInfo(selected);
        if (info == null) return;

        float alpha = age > SHOW_MS - FADE_MS ? (SHOW_MS - age) / (float) FADE_MS : 1f;
        int x = g.guiWidth() / 2 - 8;
        int y = g.guiHeight() - 75;
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, alpha);
        PlayerFaceRenderer.draw(g, info.getSkin(), x, y, 16);
        g.setColor(1f, 1f, 1f, 1f);
    }
}
