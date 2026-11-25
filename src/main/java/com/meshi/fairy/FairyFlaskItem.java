package com.meshi.fairy;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

// right click the top of a block to pour it out, makes a fairy blob. you get the glass bottle back
public class FairyFlaskItem extends Item {
    public FairyFlaskItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || ctx.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!(ctx.getLevel() instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        Vec3 at = ctx.getClickLocation();
        FairyBlobEntity blob = ModEntities.FAIRY_BLOB.get().create(server);
        if (blob == null) return InteractionResult.FAIL;
        blob.moveTo(at.x, at.y, at.z, player.getYRot() + 180.0F, 0.0F);
        blob.setOwner(player.getUUID());
        if (!server.noCollision(blob)) return InteractionResult.FAIL;
        server.addFreshEntity(blob);

        server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 14, 0.15, 0.05, 0.15, 0.0);
        server.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.2, at.z, 8, 0.15, 0.1, 0.15, 0.0);
        server.playSound(null, at.x, at.y, at.z, FairyEntity.sfx("item.bucket.empty"), SoundSource.PLAYERS, 0.6f, 1.4f);
        server.playSound(null, at.x, at.y, at.z, FairyEntity.sfx("entity.slime.squish"), SoundSource.NEUTRAL, 1.0f, 0.7f);

        if (!player.getAbilities().instabuild) {
            ItemStack held = ctx.getItemInHand();
            held.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (held.isEmpty()) player.setItemInHand(ctx.getHand(), bottle);
            else if (!player.addItem(bottle)) player.drop(bottle, false);
        }
        return InteractionResult.SUCCESS;
    }
}
