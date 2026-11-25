package com.meshi.fairy.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.meshi.fairy.FairyEntity;
import com.meshi.fairy.MeshiFairy;
import com.meshi.fairy.skin.SkinColor;
import com.meshi.fairy.skin.SkinCompose;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

// one runtime fairy texture per owner, built from their current skin.
// reads the skin through the normal player info lookup so skin mods follow too
public final class SkinFairyTextures {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "textures/entity/fairy.png");

    private record Entry(ResourceLocation skin, int override, ResourceLocation tex) {}

    private static final Map<UUID, Entry> CACHE = new HashMap<>();
    private static int[] basePx;

    private SkinFairyTextures() {}

    public static ResourceLocation textureFor(FairyEntity f, ResourceLocation fallback) {
        UUID id = f.getOwnerId();
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.getConnection() == null) return fallback;
        PlayerInfo info = mc.getConnection().getPlayerInfo(id);
        if (info == null) return fallback;

        ResourceLocation skin = info.getSkin().texture();
        int override = f.getSkinColor();
        Entry e = CACHE.get(id);
        if (e != null && e.skin().equals(skin) && e.override() == override) return e.tex();

        ResourceLocation out = fallback;
        try {
            out = build(mc, id, skin, override);
        } catch (Exception ex) {
            // keep the default look, and don't retry every frame
        }
        CACHE.put(id, new Entry(skin, override, out));
        return out;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static ResourceLocation build(Minecraft mc, UUID id, ResourceLocation skin, int override) throws Exception {
        if (basePx == null) basePx = loadBase(mc);
        int[] skinPx = readSkin(mc, skin);
        int rgb = override >= 0 ? override : SkinColor.pick(skinPx);
        int[] px = SkinCompose.compose(basePx, skinPx, rgb);

        NativeImage img = new NativeImage(SkinCompose.SIZE, SkinCompose.SIZE, false);
        for (int y = 0; y < SkinCompose.SIZE; y++)
            for (int x = 0; x < SkinCompose.SIZE; x++)
                img.setPixelRGBA(x, y, swap(px[y * SkinCompose.SIZE + x]));

        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "skin_fairy/" + id);
        mc.getTextureManager().register(loc, new DynamicTexture(img)); // replaces + closes the old one
        return loc;
    }

    private static int[] loadBase(Minecraft mc) throws Exception {
        try (InputStream in = mc.getResourceManager().open(BASE); NativeImage img = NativeImage.read(in)) {
            int[] px = new int[SkinCompose.SIZE * SkinCompose.SIZE];
            for (int y = 0; y < SkinCompose.SIZE; y++)
                for (int x = 0; x < SkinCompose.SIZE; x++)
                    px[y * SkinCompose.SIZE + x] = swap(img.getPixelRGBA(x, y));
            return px;
        }
    }

    // skin is already on the gpu, read it back (always 64x64 by the time the game registers it)
    private static int[] readSkin(Minecraft mc, ResourceLocation skin) {
        AbstractTexture t = mc.getTextureManager().getTexture(skin);
        try (NativeImage img = new NativeImage(64, 64, false)) {
            RenderSystem.bindTexture(t.getId());
            img.downloadTexture(0, false);
            int[] px = new int[64 * 64];
            for (int y = 0; y < 64; y++)
                for (int x = 0; x < 64; x++)
                    px[y * 64 + x] = swap(img.getPixelRGBA(x, y));
            return px;
        }
    }

    // NativeImage is abgr, the skin code uses argb (the swap goes both ways)
    private static int swap(int c) {
        return (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
    }
}
