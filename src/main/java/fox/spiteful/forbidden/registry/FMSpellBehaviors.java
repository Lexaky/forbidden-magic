package fox.spiteful.forbidden.registry;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.spell.BlinkUpgradeEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Spell behaviours registered into Thaumaturge's {@code thaumaturge:spell_behavior} registry. */
public final class FMSpellBehaviors {
    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, Forbidden.MODID);

    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<BlinkUpgradeEffects.Hellfire>> HELLFIRE = BEHAVIORS.register("hellfire",
            () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, BlinkUpgradeEffects.Hellfire.CODEC));
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<BlinkUpgradeEffects.Pandemonium>> PANDEMONIUM = BEHAVIORS.register("pandemonium",
            () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, BlinkUpgradeEffects.Pandemonium.CODEC));

    private FMSpellBehaviors() {}

    public static void register(IEventBus modBus) {
        BEHAVIORS.register(modBus);
    }
}
