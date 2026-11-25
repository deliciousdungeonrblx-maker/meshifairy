package com.meshi.fairy;

import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

// the blob a fairy flask makes. sits on the ground, needs blood (half a heart) every 10 min, 4 times.
// every stage is a 10 min countdown, at 0:00 its hungry and the owner has a short grace time to feed it, else it dies.
// 4th feeding -> Hatch animation -> the fairy pops out. timer runs on world time so it keeps going while away
// test: /summon meshifairy:fairy_blob ~ ~ ~ {StageTicks:200,GraceTicks:200}  (no owner = first one to feed it)
public class FairyBlobEntity extends Entity implements GeoEntity {
    public static final int FEEDINGS = 4;
    public static final int STAGE_TICKS = 12000; // 10 min
    public static final int GRACE_TICKS = 2400;  // 2 min
    // Hatch animation: the figure pops (scale 0) at 1.04s, ends at 2s
    private static final int HATCH_POP_TICK = 21;
    private static final int HATCH_END_TICK = 40;

    // seconds left of the current phase (-1 = nothing to show), hungry = in the grace time
    private static final EntityDataAccessor<Integer> TIMER_SECONDS =
            SynchedEntityData.defineId(FairyBlobEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HUNGRY =
            SynchedEntityData.defineId(FairyBlobEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HATCHING =
            SynchedEntityData.defineId(FairyBlobEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("Idle");
    private static final RawAnimation HATCH = RawAnimation.begin().thenPlay("Hatch");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID owner;
    private int fed;
    private long hungryAt = -1L; // -1 = timer not started yet
    private long dieAt = -1L;
    private int stageTicks = STAGE_TICKS;
    private int graceTicks = GRACE_TICKS;
    private boolean wasHungry;
    private int hatchTick = -1;

    public FairyBlobEntity(EntityType<? extends FairyBlobEntity> type, Level level) {
        super(type, level);
    }

    public UUID getOwnerId() { return owner; }
    public void setOwner(UUID id) { owner = id; }
    public int getTimerSeconds() { return entityData.get(TIMER_SECONDS); }
    public boolean isHungry() { return entityData.get(HUNGRY); }
    public boolean isHatching() { return entityData.get(HATCHING); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(TIMER_SECONDS, -1);
        b.define(HUNGRY, false);
        b.define(HATCHING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        fed = tag.getInt("Fed");
        if (tag.contains("HungryAt")) hungryAt = tag.getLong("HungryAt");
        if (tag.contains("DieAt")) dieAt = tag.getLong("DieAt");
        if (tag.getInt("StageTicks") > 0) stageTicks = tag.getInt("StageTicks");
        if (tag.getInt("GraceTicks") > 0) graceTicks = tag.getInt("GraceTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Fed", fed);
        if (hungryAt >= 0) {
            tag.putLong("HungryAt", hungryAt);
            tag.putLong("DieAt", dieAt);
        }
        tag.putInt("StageTicks", stageTicks);
        tag.putInt("GraceTicks", graceTicks);
    }

    // acts like decoration while it grows: nothing hurts or pushes it
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean isPushable() { return false; }

    // ---- feeding ----

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level() instanceof ServerLevel server)) return InteractionResult.sidedSuccess(true);
        if (fed >= FEEDINGS || hatchTick >= 0) return InteractionResult.sidedSuccess(false);

        if (owner != null && !player.getUUID().equals(owner)) {
            player.displayClientMessage(Component.literal("this blob belongs to someone else"), true);
            return InteractionResult.sidedSuccess(false);
        }
        long now = server.getGameTime();
        if (hungryAt < 0 || now < hungryAt) {
            player.displayClientMessage(Component.literal("it isn't hungry yet"), true);
            return InteractionResult.sidedSuccess(false);
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            if (player.getHealth() <= 1.0F) {
                player.displayClientMessage(Component.literal("you're too weak to feed it"), true);
                return InteractionResult.sidedSuccess(false);
            }
            player.hurt(player.damageSources().magic(), 1.0F); // half a heart
        }
        if (owner == null) owner = player.getUUID();

        fed++;
        double x = getX(), y = getY(), z = getZ();
        server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 0.25, z, 4, 0.12, 0.1, 0.12, 0.1);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.3, z, 6, 0.2, 0.15, 0.2, 0.0);
        sound(server, "entity.slime.squish", 1.0f, 0.8f);
        sound(server, "entity.generic.eat", 0.8f, 1.2f);

        if (fed >= FEEDINGS) {
            startHatch(server);
        } else {
            hungryAt = now + stageTicks;
            dieAt = hungryAt + graceTicks;
            wasHungry = false;
            entityData.set(HUNGRY, false);
        }
        return InteractionResult.sidedSuccess(false);
    }

    // ---- ticking ----

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        // falls if the block under it goes away
        if (level().noCollision(this, getBoundingBox().move(0.0, -0.02, 0.0))) {
            setDeltaMovement(getDeltaMovement().add(0.0, -0.04, 0.0));
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }

        if (fed >= FEEDINGS || hatchTick >= 0) {
            tickHatch(server);
            return;
        }
        long now = server.getGameTime();
        if (hungryAt < 0) {
            hungryAt = now + stageTicks;
            dieAt = hungryAt + graceTicks;
        }
        boolean hungry = now >= hungryAt;
        entityData.set(HUNGRY, hungry);
        long target = hungry ? dieAt : hungryAt;
        entityData.set(TIMER_SECONDS, (int) Math.max(0L, (target - now + 19L) / 20L));

        if (hungry && !wasHungry) {
            sound(server, "entity.slime.squish", 1.0f, 0.5f);
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.4, getZ(), 3, 0.15, 0.1, 0.15, 0.0);
        }
        wasHungry = hungry;
        if (hungry && tickCount % 60 == 0) {
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.4, getZ(), 1, 0.15, 0.1, 0.15, 0.0);
        }
        if (now >= dieAt) die(server);
    }

    private void die(ServerLevel server) {
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 12, 0.2, 0.1, 0.2, 0.02);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.15, 0.1, 0.15, 0.01);
        sound(server, "entity.slime.death", 1.0f, 0.6f);
        discard();
    }

    private void startHatch(ServerLevel server) {
        hatchTick = 0;
        entityData.set(HATCHING, true);
        entityData.set(HUNGRY, false);
        entityData.set(TIMER_SECONDS, -1);
    }

    private void tickHatch(ServerLevel server) {
        if (hatchTick < 0) startHatch(server); // loaded mid hatch
        hatchTick++;
        if (hatchTick == 1) sound(server, "block.amethyst_block.resonate", 1.0f, 1.2f);
        if (hatchTick == HATCH_POP_TICK) pop(server);
        if (hatchTick >= HATCH_END_TICK) discard();
    }

    // the figure pops, the fairy takes its place
    private void pop(ServerLevel server) {
        double x = getX(), y = getY() + 0.3, z = getZ();
        server.sendParticles(ParticleTypes.POOF, x, y, z, 14, 0.15, 0.15, 0.15, 0.03);
        server.sendParticles(ParticleTypes.END_ROD, x, y, z, 18, 0.25, 0.25, 0.25, 0.05);
        sound(server, "block.amethyst_block.chime", 1.2f, 1.4f);
        sound(server, "entity.firework_rocket.twinkle", 0.8f, 1.2f);
        ServerPlayer p = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (p != null) FairyCommands.spawnFairy(p, x, y + 0.1, z);
    }

    private void sound(ServerLevel server, String id, float vol, float pitch) {
        server.playSound(null, getX(), getY(), getZ(), FairyEntity.sfx(id), SoundSource.NEUTRAL, vol, pitch);
    }

    // ---- animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<FairyBlobEntity> state) {
        return state.setAndContinue(isHatching() ? HATCH : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
