package com.meshi.fairy.client;

import com.meshi.fairy.FairyEntity;
import com.meshi.fairy.MeshiFairy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class FairyModel extends GeoModel<FairyEntity> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "geo/fairy.geo.json");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "animations/fairy.animation.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "textures/entity/fairy.png");

    @Override public ResourceLocation getModelResource(FairyEntity e) { return GEO; }
    @Override public ResourceLocation getTextureResource(FairyEntity e) { return TEX; }
    @Override public ResourceLocation getAnimationResource(FairyEntity e) { return ANIM; }

    // busy shake is done here on the Head bone so the animation json stays untouched
    @Override
    public void setCustomAnimations(FairyEntity e, long id, AnimationState<FairyEntity> state) {
        super.setCustomAnimations(e, id, state);
        if (!e.isBusy()) return;
        var head = getAnimationProcessor().getBone("Head");
        if (head == null) return;
        float t = e.tickCount + state.getPartialTick();
        head.setRotY(head.getRotY() + (float) Math.sin(t * 1.1) * 0.55f);
    }
}
