package fox.spiteful.forbidden;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Option names follow FM's {@code forbidden.cfg}; cross-mod options of the original are gone because the
 * mods they targeted do not exist for this Minecraft version.
 */
public final class FMConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue NO_LUST;
    public static final ModConfigSpec.BooleanValue SILVERFISH_EMERALDS;
    public static final ModConfigSpec.BooleanValue CAPITALIST;
    public static final ModConfigSpec.BooleanValue ENCHANTMENTS;
    public static final ModConfigSpec.IntValue WRATH_COST;
    public static final ModConfigSpec.IntValue WRATH_EFFICIENCY;
    public static final ModConfigSpec.BooleanValue WRATH_CRIES_HAVOC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        NO_LUST = b.comment("Disable the effects that produce lust shards (riding crop, collar). The items stay registered.").define("noLust", false);
        SILVERFISH_EMERALDS = b.comment("Silverfish killed by players drop emerald nuggets.").define("silverfishEmeralds", true);
        CAPITALIST = b.comment("Enable the effect of the Capitalist enchantment.").define("capitalistEnchantment", true);
        ENCHANTMENTS = b.comment("Enable the effects of Forbidden Magic enchantments.").define("enchantments", true);
        b.pop();
        b.push("wrath_cage");
        WRATH_COST = b.comment("Essentia consumed per round of spawns. 0 removes the need to fuel the Wrath Cage.").defineInRange("fuelCost", 5, 0, 64);
        WRATH_EFFICIENCY = b.comment("Spawns a Wrath Cage gets per fuel cost.").defineInRange("fuelEfficiency", 4, 1, 64);
        WRATH_CRIES_HAVOC = b.comment("Let the Diabolist Fork imprint ANY non-boss mob, not only those in the forbidden:wrath_cage_mobs data map.")
                .define("criesHavoc", false);
        b.pop();
        SPEC = b.build();
    }

    private FMConfig() {}

    public static boolean lustEnabled() {
        return SPEC.isLoaded() ? !NO_LUST.get() : true;
    }

    public static boolean silverfishEmeralds() {
        return !SPEC.isLoaded() || SILVERFISH_EMERALDS.get();
    }

    public static boolean capitalist() {
        return enchantments() && (!SPEC.isLoaded() || CAPITALIST.get());
    }

    public static int wrathCost() {
        return SPEC.isLoaded() ? WRATH_COST.get() : 5;
    }

    public static int wrathEfficiency() {
        return SPEC.isLoaded() ? WRATH_EFFICIENCY.get() : 4;
    }

    public static boolean wrathCriesHavoc() {
        return SPEC.isLoaded() && WRATH_CRIES_HAVOC.get();
    }

    public static boolean enchantments() {
        return !SPEC.isLoaded() || ENCHANTMENTS.get();
    }
}
