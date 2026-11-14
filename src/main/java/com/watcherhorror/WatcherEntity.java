package com.watcherhorror;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WatcherEntity extends PathfinderMob {
    /** 0 - стоит в поле, 1 - смотрит в окно/дверь, 2 - разбивает, 3 - схватил и раскрыл пасть */
    public static final int WATCH = 0, PEEK = 1, SMASH = 2, GRAB = 3;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(WatcherEntity.class, EntityDataSerializers.INT);

    private int life = 0;
    private int stateTicks = 0;
    private int peekTime = 400;
    private BlockPos openingPos;
    private Vec3 holdPos = Vec3.ZERO;
    private float holdYaw;

    public WatcherEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, WATCH);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) this.stateTicks = 0;
    }

    public int getState() { return this.entityData.get(STATE); }
    public int getStateTicks() { return this.stateTicks; }

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.stateTicks = 0;
    }

    /** Режим "стою у окна/двери дома". */
    public void setupHouse(BlockPos opening, int peekTicks) {
        this.openingPos = opening;
        this.peekTime = peekTicks;
        this.setState(PEEK);
    }

    @Override
    public void tick() {
        super.tick();
        this.stateTicks++;
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 128.0D);
        int st = getState();
        boolean maw = this.level().dimension() == WatcherHorror.MAW; // в Утробе не пропадают по таймеру
        if (p == null || (!maw && (++life > 20 * 180 || (st == WATCH && life > 20 * 120)))
                || (st != WATCH && openingPos == null)) {
            this.discard();
            return;
        }

        faceTo(p);
        switch (st) {
            case WATCH -> tickWatch(p);
            case PEEK -> tickPeek(p);
            case SMASH -> tickSmash(p);
            default -> tickGrab(p);
        }
    }

    private void faceTo(Player p) {
        double dx = p.getX() - this.getX();
        double dz = p.getZ() - this.getZ();
        double dy = p.getEyeY() - this.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * 57.2957795D) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.2957795D);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    private void tickWatch(Player p) {
        if (this.distanceTo(p) < 7.0F) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.5F);
            this.discard();
        }
    }

    private void tickPeek(Player p) {
        if (this.distanceTo(p) > 32.0F) { this.discard(); return; }
        BlockState s = level().getBlockState(openingPos);
        if (HouseFinder.isDoor(s) && stateTicks % 100 == 50) {
            this.playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.7F, 0.5F); // стук
        }
        if (stateTicks >= peekTime) setState(SMASH);
    }

    private void tickSmash(Player p) {
        if (stateTicks == 8) {
            breakOpening();
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        }
        if (stateTicks >= 16) startGrab(p);
    }

    private void breakOpening() {
        BlockPos o = openingPos;
        BlockState os = level().getBlockState(o);
        if (HouseFinder.isDoor(os)) {
            level().destroyBlock(o, false, this);
            level().destroyBlock(o.above(), false, this);
            return;
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos q = o.offset(dx, dy, dz);
                    if (HouseFinder.isGlass(level().getBlockState(q))) level().destroyBlock(q, false, this);
                }
    }

    private void startGrab(Player p) {
        if (p instanceof ServerPlayer sp) MawTravel.rememberOrigin(sp);
        Vec3 to = new Vec3(openingPos.getX() + 0.5 - getX(), 0, openingPos.getZ() + 0.5 - getZ()).normalize();
        holdPos = new Vec3(getX() + to.x * 1.2, getY(), getZ() + to.z * 1.2);
        double dx = getX() - holdPos.x, dz = getZ() - holdPos.z;
        holdYaw = (float) (Mth.atan2(dz, dx) * 57.2957795D) - 90.0F;
        setState(GRAB);
    }

    private void tickGrab(Player p) {
        if (p instanceof ServerPlayer sp) {
            if (stateTicks == 1 || sp.distanceToSqr(holdPos) > 0.25D) {
                sp.connection.teleport(holdPos.x, holdPos.y, holdPos.z, holdYaw, -5.0F);
            }
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false));
            if (stateTicks == 1) sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
        }
        if (stateTicks == 2) this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.4F);
        if (stateTicks == 40) this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.3F);
        if (stateTicks >= 80) {
            this.playSound(SoundEvents.GENERIC_EAT, 2.0F, 0.3F);
            if (p instanceof ServerPlayer sp) MawTravel.swallow(sp); // -> тёмный мир
            this.discard();
        }
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
}
