package com.meshi.fairy;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FairyCommands {
    private FairyCommands() {}

    // temporary until the potion exists. op only for now
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fairy")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("summon").executes(c -> summon(c.getSource().getPlayerOrException(), c.getSource())))
                .then(Commands.literal("dismiss").executes(c -> dismiss(c.getSource().getPlayerOrException(), c.getSource()))));
    }

    private static int summon(ServerPlayer p, CommandSourceStack src) {
        ServerLevel level = p.serverLevel();
        FairyEntity f = ModEntities.FAIRY.get().create(level);
        if (f == null) return 0;
        Vec3 pos = p.getEyePosition().add(Vec3.directionFromRotation(0f, p.getYRot()));
        f.moveTo(pos.x, pos.y, pos.z);
        f.setOwner(p.getUUID());
        level.addFreshEntity(f);
        FairyManager.register(f);
        src.sendSuccess(() -> Component.literal("fairy summoned"), false);
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
