package com.watcherhorror;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class WatcherRenderer extends MobRenderer<WatcherEntity, WatcherModel> {
    private static final ResourceLocation TEX =
            new ResourceLocation(WatcherHorror.MODID, "textures/entity/watcher.png");
    private static final RenderType EYES =
            RenderType.eyes(new ResourceLocation(WatcherHorror.MODID, "textures/entity/watcher_eyes.png"));

    public WatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WatcherModel(ctx.bakeLayer(ClientSetup.LAYER)), 0.0F);
        this.addLayer(new EyesLayer<WatcherEntity, WatcherModel>(this) {
            @Override public RenderType renderType() { return EYES; }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity e) { return TEX; }
}
