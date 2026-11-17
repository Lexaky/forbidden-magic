package fox.spiteful.forbidden.registry;

import fox.spiteful.forbidden.Forbidden;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Keys of FM's enchantments. The enchantments themselves are data ({@code data/forbidden/enchantment/*.json}); their
 * gameplay effects are implemented by event handlers, so this class only resolves keys to levels.
 */
public final class FMEnchantments {
    public static final ResourceKey<Enchantment> WRATH = key("wrath");
    public static final ResourceKey<Enchantment> CAPITALIST = key("capitalist");
    public static final ResourceKey<Enchantment> CONSUMING = key("consuming");
    public static final ResourceKey<Enchantment> EDUCATIONAL = key("educational");
    public static final ResourceKey<Enchantment> CORRUPTING = key("corrupting");
    public static final ResourceKey<Enchantment> FIERY_CORE = key("fiery_core");
    public static final ResourceKey<Enchantment> IMPACT = key("impact");
    public static final ResourceKey<Enchantment> VOIDTOUCHED = key("voidtouched");

    private FMEnchantments() {}

    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }
        return level.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(lookup -> lookup.get(key))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) holder, stack)).orElse(0);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Forbidden.id(path));
    }
}
