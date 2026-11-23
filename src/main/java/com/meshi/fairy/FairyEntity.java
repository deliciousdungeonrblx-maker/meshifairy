package com.meshi.fairy;

import java.util.Optional;
import java.util.UUID;

import com.meshi.fairy.client.FairyClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FairyEntity extends PathfinderMob implements GeoEntity {
    // all tunable, my picks
    public static final int BASE_LIFE = 6000;     // 5 min
    public static final int MAX_LIFE = 12000;     // cap when feeding
    public static final int FOOD_BONUS = 1200;    // +1 min per sweet
    public static final int LIGHT_LEVEL = 10;
    private static final int VOICE_HOLD_MS = 350;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(FairyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ANIM =
            SynchedEntityData.defineId(FairyEntity.class, EntityDataSerializers.INT); // 0 idle, 1 fly, 2 talk
    private static final EntityDataAccessor<Boolean> BUSY =
            SynchedEntityData.defineId(FairyEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life = BASE_LIFE;
    private int busyTicks;
    private int flyHold;
    private volatile long lastVoiceMs;
    private BlockPos lightPos;
    private boolean buzzStarted;

    public FairyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
        b.define(ANIM, 0);
        b.define(BUSY, false);
    }

    // ---- owner / state ----

    public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }
    public void setOwner(UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }
    public boolean isOwner(Player p) { return p.getUUID().equals(getOwnerId()); }
    public boolean isBusy() { return entityData.get(BUSY); }
    public void markVoice() { lastVoiceMs = System.currentTimeMillis(); }

    public void startBusy() {
        busyTicks = 40;
        entityData.set(BUSY, true);
    }

    public void ring() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.4, getZ(), 3, 0.15, 0.1, 0.15, 1.0);
            sl.playSound(null, getX(), getY(), getZ(), sfx("block.note_block.chime"), SoundSource.NEUTRAL, 0.5f, 1.4f);
        }
    }

    public static SoundEvent sfx(String path) {
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(path));
    }

    private ServerPlayer owner() {
        UUID id = getOwnerId();
        if (id == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(id);
    }

    // ---- ticking ----

    @Override
    public void tick() {
        if (level().isClientSide) {
            clientTick();
            super.tick();
            return;
        }
        ServerPlayer owner = owner();
        if (owner == null || !owner.isAlive() || owner.level() != level() || --life <= 0) {
            poof();
            return;
        }
        steer(owner);
        super.tick();
        serverTail();
    }

    private void poof() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.15, getZ(), 6, 0.1, 0.1, 0.1, 0.01);
        }
        discard();
    }

    private void steer(ServerPlayer owner) {
        boolean call = FairyManager.inCall(owner.getUUID());
        Vec3 target = call ? callTarget(owner) : followTarget(owner);
        Vec3 diff = target.subtract(position());
        if (diff.lengthSqr() > 144) {
            moveTo(target.x, target.y, target.z);
            setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 want = diff.scale(0.2);
            double l = want.length();
            if (l > 0.45) want = want.scale(0.45 / l);
            setDeltaMovement(getDeltaMovement().scale(0.4).add(want.scale(0.6)));
        }
        double dx = owner.getX() - getX();
        double dz = owner.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * 57.29578) - 90f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private Vec3 followTarget(ServerPlayer owner) {
        double a = tickCount * 0.035;
        Vec3 head = owner.getEyePosition();
        return new Vec3(head.x + Math.cos(a) * 0.85,
                head.y - 0.1 + Math.sin(tickCount * 0.09) * 0.12,
                head.z + Math.sin(a) * 0.85);
    }

    // eye level, in front of the player (flat direction so it doesn't drift when looking up/down)
    private Vec3 callTarget(ServerPlayer owner) {
        Vec3 dir = Vec3.directionFromRotation(0f, owner.getYRot());
        return owner.getEyePosition().add(dir.scale(1.0)).subtract(0, 0.17, 0);
    }

    // we set the velocity ourselves, no friction/gravity stuff
    @Override
    public void travel(Vec3 input) {
        if (!level().isClientSide) move(MoverType.SELF, getDeltaMovement());
    }

    private void serverTail() {
        if (busyTicks > 0 && --busyTicks == 0) entityData.set(BUSY, false);

        boolean talking = System.currentTimeMillis() - lastVoiceMs < VOICE_HOLD_MS;
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistanceSqr() > 0.0002 || Math.abs(v.y) > 0.02) flyHold = 8;
        else if (flyHold > 0) flyHold--;
        int anim = talking ? 2 : (flyHold > 0 ? 1 : 0);
        if (entityData.get(ANIM) != anim) entityData.set(ANIM, anim);

        updateLight();
    }

    private void updateLight() {
        BlockPos p = blockPosition();
        if (p.equals(lightPos)) return;
        clearLight();
        if (level().getBlockState(p).isAir()) {
            level().setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL), 2);
            lightPos = p;
        }
    }

    private void clearLight() {
        if (lightPos != null && level().getBlockState(lightPos).is(Blocks.LIGHT)) {
            level().setBlock(lightPos, Blocks.AIR.defaultBlockState(), 2);
        }
        lightPos = null;
    }

    private void clientTick() {
        if (!buzzStarted) {
            buzzStarted = true;
            FairyClient.startBuzz(this);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.88f, 0.25f), 0.45f),
                    getX() + (random.nextDouble() - 0.5) * 0.25,
                    getY() + 0.1 + random.nextDouble() * 0.25,
                    getZ() + (random.nextDouble() - 0.5) * 0.25,
                    0, 0, 0);
        }
    }

    // ---- interaction ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwner(player) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;

        if (player.isSecondaryUseActive()) {
            onShiftInteract(sp);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        if (isSweet(held)) {
            feed(player, held);
            return InteractionResult.CONSUME;
        }
        FairyManager.cycle(sp);
        return InteractionResult.CONSUME;
    }

    // todo: appearance customization goes here, nothing for now
    protected void onShiftInteract(ServerPlayer player) {
    }

    private static boolean isSweet(ItemStack s) {
        return s.is(Items.HONEY_BOTTLE) || s.is(Items.SWEET_BERRIES) || s.is(Items.SUGAR);
    }

    private void feed(Player player, ItemStack held) {
        life = Math.min(life + FOOD_BONUS, MAX_LIFE);
        playSound(sfx("entity.generic.eat"), 0.3f, 1.6f);
        if (player.getAbilities().instabuild) return;
        boolean honey = held.is(Items.HONEY_BOTTLE);
        held.shrink(1);
        if (honey) player.addItem(new ItemStack(Items.GLASS_BOTTLE));
    }

    // ---- misc overrides ----

    @Override
    public void setRemoved(Entity.RemovalReason reason) {
        if (!level().isClientSide) {
            clearLight();
            FairyManager.unregister(this);
        }
        super.setRemoved(reason);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) { }
    @Override public boolean causeFallDamage(float f, float m, DamageSource s) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource s) { return !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }

    // ---- geckolib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<FairyEntity> state) {
        String name = switch (entityData.get(ANIM)) {
            case 2 -> "Talking";
            case 1 -> "Fly";
            default -> "Idle";
        };
        return state.setAndContinue(RawAnimation.begin().thenLoop(name));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
