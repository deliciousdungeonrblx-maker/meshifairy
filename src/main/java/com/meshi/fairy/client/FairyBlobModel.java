package com.meshi.fairy.client;

import com.meshi.fairy.FairyBlobEntity;
import com.meshi.fairy.MeshiFairy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FairyBlobModel extends GeoModel<FairyBlobEntity> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "geo/fairy_blob.geo.json");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "animations/fairy_blob.animation.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(MeshiFairy.MODID, "textures/entity/fairyblob.png");

    @Override public ResourceLocation getModelResource(FairyBlobEntity e) { return GEO; }
    @Override public ResourceLocation getTextureResource(FairyBlobEntity e) { return TEX; }
    @Override public ResourceLocation getAnimationResource(FairyBlobEntity e) { return ANIM; }
}
