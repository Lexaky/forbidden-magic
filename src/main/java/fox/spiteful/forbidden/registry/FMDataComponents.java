package fox.spiteful.forbidden.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fox.spiteful.forbidden.Forbidden;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FMDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Forbidden.MODID);

    /** Remaining centivis of a profane rod's contract (FM NBT {@code contract}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CONTRACT = COMPONENTS.registerComponentType("contract",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Current phase (0-2) of a morphic tool (FM NBT {@code phase}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MORPH_PHASE = COMPONENTS.registerComponentType("morph_phase",
            builder -> builder.persistent(Codec.intRange(0, 2)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Enchantments and names stored for the inactive phases (FM NBT {@code enchantsN} / {@code NameN}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MorphStore>> MORPH_STORE = COMPONENTS.registerComponentType("morph_store",
            builder -> builder.persistent(MorphStore.CODEC).networkSynchronized(MorphStore.STREAM_CODEC));

    /** Entity type id imprinted into a Mob Crystal (FM NBT {@code mob}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.resources.Identifier>> MOB = COMPONENTS.registerComponentType("mob",
            builder -> builder.persistent(net.minecraft.resources.Identifier.CODEC).networkSynchronized(net.minecraft.resources.Identifier.STREAM_CODEC));

    /** Vis stored in the Collar of Pain (FM's TC4 vis amulet NBT), in centivis per primal. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.leclowndu93150.thaumaturge.api.wands.WandVis>> STORED_VIS = COMPONENTS.registerComponentType("stored_vis",
            builder -> builder.persistent(com.leclowndu93150.thaumaturge.api.wands.WandVis.CODEC).networkSynchronized(com.leclowndu93150.thaumaturge.api.wands.WandVis.STREAM_CODEC));

    /** Name of the player who placed a Collar of Pain (FM NBT {@code owner}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> OWNER = COMPONENTS.registerComponentType("owner",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private FMDataComponents() {}

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    /** One saved phase of a morphic tool. */
    public record MorphSlot(ItemEnchantments enchantments, Optional<Component> name) {
        public static final MorphSlot EMPTY = new MorphSlot(ItemEnchantments.EMPTY, Optional.empty());
        public static final Codec<MorphSlot> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemEnchantments.CODEC.optionalFieldOf("enchantments", ItemEnchantments.EMPTY).forGetter(MorphSlot::enchantments),
                ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(MorphSlot::name)).apply(i, MorphSlot::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, MorphSlot> STREAM_CODEC = StreamCodec.composite(
                ItemEnchantments.STREAM_CODEC, MorphSlot::enchantments,
                ComponentSerialization.OPTIONAL_STREAM_CODEC, MorphSlot::name, MorphSlot::new);
    }

    /** Saved state of all three phases; the active phase's slot is stale until the tool switches away from it. */
    public record MorphStore(List<MorphSlot> slots) {
        public static final MorphStore EMPTY = new MorphStore(List.of(MorphSlot.EMPTY, MorphSlot.EMPTY, MorphSlot.EMPTY));
        public static final Codec<MorphStore> CODEC = MorphSlot.CODEC.listOf(3, 3).xmap(MorphStore::new, MorphStore::slots);
        public static final StreamCodec<RegistryFriendlyByteBuf, MorphStore> STREAM_CODEC = MorphSlot.STREAM_CODEC.apply(ByteBufCodecs.list(3)).map(MorphStore::new, MorphStore::slots);

        public MorphStore {
            slots = List.copyOf(slots);
        }

        public MorphSlot get(int phase) {
            return slots.get(phase);
        }

        public MorphStore with(int phase, MorphSlot slot) {
            List<MorphSlot> copy = new ArrayList<>(slots);
            copy.set(phase, slot);
            return new MorphStore(copy);
        }
    }
}
