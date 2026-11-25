package com.meshi.fairy;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MeshiFairy.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FairyEntity>> FAIRY =
            ENTITIES.register("fairy", () -> EntityType.Builder.of(FairyEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("fairy"));

    public static final DeferredHolder<EntityType<?>, EntityType<FairyBlobEntity>> FAIRY_BLOB =
            ENTITIES.register("fairy_blob", () -> EntityType.Builder.of(FairyBlobEntity::new, MobCategory.MISC)
                    .sized(0.45f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .fireImmune()
                    .build("fairy_blob"));
}
