package fox.spiteful.forbidden.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMDataMaps;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Imprinting Crystal (FM {@code ItemMobCrystal}). Blank until a Diabolist Fork kill imprints a mob into it; an imprinted
 * crystal is named after the mob and shows the essentia its Wrath Cage burns.
 */
public final class MobCrystalItem extends Item {
    /** Essentia used for mobs outside the data map when "cries havoc" is on (FM fell back to Lucrum). */
    public static final ResourceKey<IAspect> FALLBACK_ASPECT = ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "desiderium"));

    public MobCrystalItem(Properties properties) {
        super(properties);
    }

    public static Optional<EntityType<?>> mob(ItemStack stack) {
        Identifier id = stack.get(FMDataComponents.MOB.get());
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static ItemStack imprinted(Item item, EntityType<?> type) {
        ItemStack stack = new ItemStack(item);
        stack.set(FMDataComponents.MOB.get(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return stack;
    }

    /** Whether a mob of this type may be imprinted (data map entry, or any non-boss with "cries havoc"). */
    public static boolean canImprint(EntityType<?> type) {
        return aspectFor(type).isPresent();
    }

    public static Optional<ResourceKey<IAspect>> aspectFor(EntityType<?> type) {
        ResourceKey<IAspect> mapped = type.builtInRegistryHolder().getData(FMDataMaps.WRATH_CAGE_MOBS);
        if (mapped != null) {
            return Optional.of(mapped);
        }
        if (FMConfig.wrathCriesHavoc() && !type.builtInRegistryHolder().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                && type.getCategory() != net.minecraft.world.entity.MobCategory.MISC) {
            return Optional.of(FALLBACK_ASPECT);
        }
        return Optional.empty();
    }

    @Override
    public Component getName(ItemStack stack) {
        return mob(stack).<Component>map(type -> Component.translatable("item.forbidden.mob_crystal.imprinted", type.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(FMDataComponents.MOB.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        mob(stack).flatMap(MobCrystalItem::aspectFor).ifPresent(aspect -> builder.accept(
                Component.translatable("aspect." + aspect.identifier().getNamespace() + "." + aspect.identifier().getPath()).withStyle(ChatFormatting.DARK_PURPLE)));
    }
}
