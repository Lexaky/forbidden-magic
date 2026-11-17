package fox.spiteful.forbidden.registry;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import fox.spiteful.forbidden.Forbidden;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Data maps. {@link #WRATH_CAGE_MOBS} replaces FM's hard-coded {@code Config.spawnerMobs}: it lists the mobs a Diabolist
 * Fork can imprint into a crystal and the essentia each one's Wrath Cage burns, and it can be extended by datapacks.
 */
@EventBusSubscriber(modid = Forbidden.MODID)
public final class FMDataMaps {
    public static final DataMapType<EntityType<?>, ResourceKey<IAspect>> WRATH_CAGE_MOBS = DataMapType
            .builder(Forbidden.id("wrath_cage_mobs"), Registries.ENTITY_TYPE, ResourceKey.codec(IAspect.REGISTRY_KEY))
            .synced(ResourceKey.codec(IAspect.REGISTRY_KEY), false).build();

    private FMDataMaps() {}

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(WRATH_CAGE_MOBS);
    }
}
