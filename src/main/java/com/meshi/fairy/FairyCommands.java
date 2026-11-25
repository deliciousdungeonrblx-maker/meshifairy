package com.meshi.fairy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class FairyCommands {
    private FairyCommands() {}

    // temporary until the potion exists. op only for now
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fairy")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("summon").executes(c -> summon(c.getSource().getPlayerOrException(), c.getSource())))
                .then(Commands.literal("color")
                        .then(Commands.literal("reset").executes(c -> color(c.getSource().getPlayerOrException(), c.getSource(), -1)))
                        .then(Commands.argument("hex", StringArgumentType.word())
                                .executes(c -> colorHex(c.getSource().getPlayerOrException(), c.getSource(),
                                        StringArgumentType.getString(c, "hex")))))
                .then(Commands.literal("dismiss").executes(c -> dismiss(c.getSource().getPlayerOrException(), c.getSource()))));
    }

    private static int summon(ServerPlayer p, CommandSourceStack src) {
        Vec3 pos = p.getEyePosition().add(Vec3.directionFromRotation(0f, p.getYRot()));
        if (spawnFairy(p, pos.x, pos.y, pos.z) == null) return 0;
        src.sendSuccess(() -> Component.literal("fairy summoned"), false);
        return 1;
    }

    // used by the summon command and when a blob hatches
    public static FairyEntity spawnFairy(ServerPlayer p, double x, double y, double z) {
        ServerLevel level = p.serverLevel();
        FairyEntity f = ModEntities.FAIRY.get().create(level);
        if (f == null) return null;
        f.moveTo(x, y, z);
        f.setOwner(p.getUUID());
        f.setSkinColor(savedColor(p));
        level.addFreshEntity(f);
        FairyManager.register(f);
        return f;
    }

    // manual skin color is kept on the player so it survives re-summons
    private static final String COLOR_KEY = "meshifairy_color";

    private static int savedColor(ServerPlayer p) {
        CompoundTag t = p.getPersistentData();
        return t.contains(COLOR_KEY) ? t.getInt(COLOR_KEY) : -1;
    }

    private static int colorHex(ServerPlayer p, CommandSourceStack src, String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (h.length() != 6) throw new NumberFormatException();
            return color(p, src, Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            src.sendFailure(Component.literal("use a hex color like ffd8cb"));
            return 0;
        }
    }

    private static int color(ServerPlayer p, CommandSourceStack src, int rgb) {
        CompoundTag t = p.getPersistentData();
        if (rgb < 0) t.remove(COLOR_KEY);
        else t.putInt(COLOR_KEY, rgb);
        FairyEntity f = FairyManager.fairyOf(p.getUUID());
        if (f != null) f.setSkinColor(rgb);
        src.sendSuccess(() -> Component.literal(rgb < 0 ? "fairy color back to auto" : "fairy color set"), false);
        return 1;
    }

    private static int dismiss(ServerPlayer p, CommandSourceStack src) {
        FairyEntity f = FairyManager.fairyOf(p.getUUID());
        if (f == null) return 0;
        f.discard();
        src.sendSuccess(() -> Component.literal("fairy dismissed"), false);
        return 1;
    }
}
