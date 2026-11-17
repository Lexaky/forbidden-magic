package fox.spiteful.forbidden;

import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMCreativeTabs;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMItems;
import fox.spiteful.forbidden.registry.FMWandParts;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forbidden Magic for Thaumaturge. Content that Thaumaturge models as data (aspects, research, recipes, item aspects,
 * enchantments) lives in {@code src/main/resources/data}; this class only wires the code-side registries.
 */
@Mod(Forbidden.MODID)
public final class Forbidden {
    public static final String MODID = "forbidden";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public Forbidden(IEventBus modBus, ModContainer container) {
        FMDataComponents.register(modBus);
        FMWandParts.register(modBus);
        FMBlocks.register(modBus);
        FMItems.register(modBus);
        fox.spiteful.forbidden.registry.FMBlockEntities.register(modBus);
        fox.spiteful.forbidden.registry.FMSpellBehaviors.register(modBus);
        fox.spiteful.forbidden.registry.FMVillagers.register(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(fox.spiteful.forbidden.content.wand.HellfireTrigger::register));
        FMCreativeTabs.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, FMConfig.SPEC);
        if (Boolean.getBoolean("forbidden.gametest")) {
            modBus.addListener(fox.spiteful.forbidden.gametest.FMGameTests::register);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
