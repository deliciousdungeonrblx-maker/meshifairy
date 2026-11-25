package com.meshi.fairy.client;

import com.meshi.fairy.FairyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FairyRenderer extends GeoEntityRenderer<FairyEntity> {
    public FairyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FairyModel());
        this.shadowRadius = 0f;
        withScale(0.24f); // model is 2.1 blocks tall, this makes it ~0.5 (a player head)
    }

    // wings have semi transparent pixels
    @Override
    public RenderType getRenderType(FairyEntity e, ResourceLocation tex, MultiBufferSource buf, float pt) {
        return RenderType.entityTranslucent(tex);
    }
}
