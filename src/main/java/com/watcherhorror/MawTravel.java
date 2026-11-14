package com.watcherhorror;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/** Тёмный мир монстра ("Утроба"): вход после проглатывания и возврат через время. */
public class MawTravel {
    /** Сколько секунд игрок сидит в Утробе, прежде чем его выплюнет обратно. */
    public static final int MAW_SECONDS = 300;

    private static final String RET = "watcherhorror_return";
    private static final String TICKS = "watcherhorror_maw_ticks";
    private static final double FLOOR_Y = -59.0D; // верх пола flat-мира (см. dimension/maw.json)

    /** Запоминаем, откуда забрали игрока. */
    public static void rememberOrigin(ServerPlayer p) {
        CompoundTag r = new CompoundTag();
        r.putString("dim", p.level().dimension().location().toString());
        r.putDouble("x", p.getX());
        r.putDouble("y", p.getY());
        r.putDouble("z", p.getZ());
        r.putFloat("yaw", p.getYRot());
        p.getPersistentData().put(RET, r);
    }

    public static void swallow(ServerPlayer p) {
        ServerLevel maw = p.server.getLevel(WatcherHorror.MAW);
        if (maw == null) { // на случай если измерение не загрузилось
            p.hurt(p.level().damageSources().magic(), 1000.0F);
            return;
        }
        p.getPersistentData().putInt(TICKS, 0);
        p.teleportTo(maw, 0.5D, FLOOR_Y, 0.5D, 0.0F, 0.0F);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
        p.displayClientMessage(Component.literal("Он проглотил тебя. Ты в его утробе... Не смотри ему в глаза."), true);
    }

    /** Вызывается каждый тик, пока игрок в Утробе. */
    public static void tickMaw(ServerPlayer p, ServerLevel level) {
        CompoundTag t = p.getPersistentData();
        int ticks = t.getInt(TICKS) + 1;
        t.putInt(TICKS, ticks);

        if (ticks % 40 == 0) { // сердцебиение
            p.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.0F, 0.8F);
        }
        if (ticks % 60 == 0) spawnWatchers(p, level);

        int total = MAW_SECONDS * 20;
        if (ticks % 1200 == 0 && ticks < total) {
            int left = (total - ticks) / 20;
            p.displayClientMessage(Component.literal("Тьма сжимается... осталось " + left + " сек."), true);
        }
        if (ticks >= total) leave(p);
    }

    private static void spawnWatchers(ServerPlayer p, ServerLevel level) {
        if (level.getEntitiesOfClass(WatcherEntity.class, p.getBoundingBox().inflate(60)).size() >= 5) return;
        RandomSource r = level.getRandom();
        for (int i = 0; i < 6; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 20 + r.nextDouble() * 14;
            int x = Mth.floor(p.getX() + Math.cos(ang) * dist);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * dist);
            if (!level.hasChunkAt(new BlockPos(x, (int) FLOOR_Y, z))) continue;
            WatcherEntity w = WatcherHorror.WATCHER.get().create(level);
            if (w == null) return;
            w.moveTo(x + 0.5, FLOOR_Y, z + 0.5, 0, 0);
            level.addFreshEntity(w);
            return;
        }
    }

    public static void leave(ServerPlayer p) {
        CompoundTag t = p.getPersistentData();
        ServerLevel target = p.server.overworld();
        double x, y, z;
        float yaw = 0.0F;
        if (t.contains(RET)) {
            CompoundTag r = t.getCompound(RET);
            ResourceLocation id = ResourceLocation.tryParse(r.getString("dim"));
            if (id != null) {
                ServerLevel l = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                if (l != null) target = l;
            }
            x = r.getDouble("x"); y = r.getDouble("y"); z = r.getDouble("z");
            yaw = r.getFloat("yaw");
        } else {
            BlockPos sp = target.getSharedSpawnPos();
            x = sp.getX() + 0.5; y = sp.getY(); z = sp.getZ() + 0.5;
        }
        t.remove(RET);
        t.putInt(TICKS, 0);
        p.teleportTo(target, x, y, z, yaw, 0.0F);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        p.displayClientMessage(Component.literal("Он выплюнул тебя обратно..."), true);
    }
}
