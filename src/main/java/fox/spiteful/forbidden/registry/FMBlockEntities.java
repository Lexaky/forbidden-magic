package fox.spiteful.forbidden.registry;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.block.WrathCageBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Forbidden.MODID)
public final class FMBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Forbidden.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WrathCageBlockEntity>> WRATH_CAGE = BLOCK_ENTITIES.register("wrath_cage",
            () -> new BlockEntityType<>(WrathCageBlockEntity::new, FMBlocks.WRATH_CAGE.get()));

    private FMBlockEntities() {}

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    /** Exposes the cage to Thaumaturge's essentia network (suction for tubes) and to goggles readouts. */
    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, WRATH_CAGE.get(), (cage, side) -> cage);
        event.registerBlockEntity(EssentiaCapabilities.ASPECT_QUERY, WRATH_CAGE.get(), (cage, side) -> cage);
    }
}
