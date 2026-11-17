package fox.spiteful.forbidden.registry;

import com.google.common.collect.ImmutableSet;
import fox.spiteful.forbidden.Forbidden;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Heretic villager (FM {@code VillagerHereticManager}, villager id 666). FM villagers picked professions at random;
 * modern villagers take one from a job site, so the Heretic works at an Umbral Rose Bush. Trades are data
 * ({@code data/forbidden/trade_set/heretic/*} and {@code villager_trade/heretic/*}).
 */
public final class FMVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Forbidden.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, Forbidden.MODID);

    public static final ResourceKey<PoiType> HERETIC_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Forbidden.id("heretic"));
    public static final DeferredHolder<PoiType, PoiType> HERETIC_POI = POI_TYPES.register("heretic",
            () -> new PoiType(FMBlocks.UMBRAL_BUSH.get().getStateDefinition().getPossibleStates().stream()
                    .filter(state -> state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER).collect(Collectors.toSet()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> HERETIC = PROFESSIONS.register("heretic", () -> {
        Int2ObjectMap<ResourceKey<TradeSet>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 3; level++) {
            trades.put(level, ResourceKey.create(Registries.TRADE_SET, Forbidden.id("heretic/level_" + level)));
        }
        return new VillagerProfession(Component.translatable("entity.forbidden.villager.heretic"), poi -> poi.is(HERETIC_POI_KEY), poi -> poi.is(HERETIC_POI_KEY),
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CLERIC, trades);
    });

    private FMVillagers() {}

    public static void register(IEventBus modBus) {
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
    }
}
