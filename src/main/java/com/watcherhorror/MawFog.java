package com.watcherhorror;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Чёрный густой туман в Утробе: всё дальше ~45 блоков тонет во тьме. */
@Mod.EventBusSubscriber(modid = WatcherHorror.MODID, value = Dist.CLIENT)
public class MawFog {
    private static boolean inMaw() {
        var l = Minecraft.getInstance().level;
        return l != null && l.dimension() == WatcherHorror.MAW;
    }

    @SubscribeEvent
    public static void color(ViewportEvent.ComputeFogColor e) {
        if (!inMaw()) return;
        e.setRed(0.02F);
        e.setGreen(0.0F);
        e.setBlue(0.0F);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        if (!inMaw()) return;
        e.setNearPlaneDistance(0.0F);
        e.setFarPlaneDistance(45.0F);
        e.setCanceled(true);
    }
}
