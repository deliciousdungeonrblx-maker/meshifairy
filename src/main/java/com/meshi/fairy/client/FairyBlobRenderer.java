package com.meshi.fairy.client;

import com.meshi.fairy.FairyBlobEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FairyBlobRenderer extends GeoEntityRenderer<FairyBlobEntity> {
    private static final int FULL_BRIGHT = 15728880;
    // the blob model is tiny (like 1/8 size), this scales it up. change this if it looks too small/big in game
    private static final float BLOB_SCALE = 2.0F;
    // label heights in blocks above the blob
    private static final double CLOCK_Y = 0.6D;
    private static final double TEXT_Y = 0.95D;
    // text goes white -> red over the last 2 min of the 10 min stage, hungry = red
    private static final int REDDEN_SECONDS = 120;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF3030;

    private final ItemStack clock = new ItemStack(Items.CLOCK);

    public FairyBlobRenderer(EntityRendererProvider.Context context) {
        super(context, new FairyBlobModel());
        this.shadowRadius = 0.0F;
        withScale(BLOB_SCALE);
    }

    @Override
    public void render(FairyBlobEntity blob, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(blob, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        drawLabel(blob, partialTick, poseStack, bufferSource);
    }

    // same label as the soil mound: small clock w/ [m:ss] above it, always faces the camera
    private void drawLabel(FairyBlobEntity blob, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        int secs = blob.getTimerSeconds();
        if (secs < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        poseStack.pushPose();
        poseStack.translate(0.0D, CLOCK_Y, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.4F, 0.4F, 0.4F);
        mc.getItemRenderer().renderStatic(this.clock, ItemDisplayContext.GROUND, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, mc.level, blob.getId());
        poseStack.popPose();

        boolean hungry = blob.isHungry();
        String time = String.format("[%d:%02d]", secs / 60, secs % 60);
        int color = hungry ? RED : lerpColor(Mth.clamp((REDDEN_SECONDS - secs) / (float) REDDEN_SECONDS, 0.0F, 1.0F));
        Component text = Component.literal(hungry ? "hungry! " + time : time).withStyle(s -> s.withColor(color));
        drawText(blob, text, TEXT_Y, partialTick, poseStack, bufferSource);
    }

    private static int lerpColor(float t) {
        int r = (int) Mth.lerp(t, (WHITE >> 16) & 255, (RED >> 16) & 255);
        int g = (int) Mth.lerp(t, (WHITE >> 8) & 255, (RED >> 8) & 255);
        int b = (int) Mth.lerp(t, WHITE & 255, RED & 255);
        return (r << 16) | (g << 8) | b;
    }

    // vanilla name tag drawing (see SoilMoundRenderer), moved by the difference to land at y
    private void drawText(FairyBlobEntity blob, Component text, double y, float partialTick, PoseStack poseStack,
                          MultiBufferSource bufferSource) {
        Vec3 tag = blob.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, blob.getViewYRot(partialTick));
        if (tag == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, y - (tag.y + 0.5D), 0.0D);
        renderNameTag(blob, text, poseStack, bufferSource, FULL_BRIGHT, partialTick);
        poseStack.popPose();
    }
}
