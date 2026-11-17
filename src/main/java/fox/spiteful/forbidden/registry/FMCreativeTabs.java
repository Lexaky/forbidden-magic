package fox.spiteful.forbidden.registry;

import fox.spiteful.forbidden.Forbidden;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FMCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Forbidden.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("forbidden",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.forbidden")).icon(() -> new ItemStack(FMItems.DIABOLIST_FORK.get()))
                    .displayItems((params, output) -> {
                FMItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                // FM's "Imprinting Crystals" tab: one crystal per mob the Wrath Cage can host.
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
                    if (type.builtInRegistryHolder().getData(FMDataMaps.WRATH_CAGE_MOBS) != null) {
                        output.accept(fox.spiteful.forbidden.content.item.MobCrystalItem.imprinted(FMItems.MOB_CRYSTAL.get(), type));
                    }
                });
            }).build());

    private FMCreativeTabs() {}

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
