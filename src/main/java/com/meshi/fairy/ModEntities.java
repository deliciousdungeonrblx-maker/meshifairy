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
                    .sized(0.3f, 0.35f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("fairy"));
}
