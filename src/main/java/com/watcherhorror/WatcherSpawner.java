package com.watcherhorror;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WatcherSpawner {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p)) return;
        ServerLevel level = p.serverLevel();
        if (level.dimension() == WatcherHorror.MAW) { // игрок в тёмном мире монстра
            MawTravel.tickMaw(p, level);
            return;
        }
        if (p.tickCount % 100 != 0) return; // проверка раз в 5 сек
        if (level.dimension() != Level.OVERWORLD) return;
        if (!level.getEntitiesOfClass(WatcherEntity.class, p.getBoundingBox().inflate(128)).isEmpty()) return;

        RandomSource r = level.getRandom();

        // 1) игрок в доме -> Наблюдатель приходит к окну или двери
        HouseFinder.Spot spot = HouseFinder.find(level, p);
        if (spot != null) {
            float chance = !level.isDay() ? 0.35F : 0.12F;
            if (r.nextFloat() > chance) return;
            WatcherEntity w = WatcherHorror.WATCHER.get().create(level);
            if (w == null) return;
            BlockPos st = spot.stand();
            w.moveTo(st.getX() + 0.5, st.getY(), st.getZ() + 0.5, 0, 0);
            w.setupHouse(spot.opening(), 20 * (12 + r.nextInt(20)));
            level.addFreshEntity(w);
            p.playNotifySound(SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.6F, 0.5F);
            return;
        }

        // 2) игрок на улице -> стоит вдалеке и смотрит
        if (r.nextInt(12) != 0) return;
        for (int i = 0; i < 10; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 22 + r.nextDouble() * 10;
            int x = Mth.floor(p.getX() + Math.cos(ang) * dist);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * dist);
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getFluidState(pos.below()).isEmpty()) continue;

            WatcherEntity w = WatcherHorror.WATCHER.get().create(level);
            if (w == null) return;
            w.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            level.addFreshEntity(w);
            p.playNotifySound(SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.6F, 0.5F);
            return;
        }
    }
}
