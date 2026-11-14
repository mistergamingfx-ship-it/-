package com.watcherhorror;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(WatcherHorror.MODID)
public class WatcherHorror {
    public static final String MODID = "watcherhorror";

    /** Тёмный мир монстра: data/watcherhorror/dimension/maw.json */
    public static final ResourceKey<Level> MAW =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MODID, "maw"));

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10)
                    .build("watcher"));

    public WatcherHorror() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        bus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.register(new WatcherSpawner());
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(WATCHER.get(), WatcherEntity.createAttributes());
    }
}
