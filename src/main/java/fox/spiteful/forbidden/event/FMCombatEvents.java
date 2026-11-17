package fox.spiteful.forbidden.event;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.damagesource.TTDamageTypes;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.registry.FMWandParts;
import fox.spiteful.forbidden.util.WandBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Damage rules from FM's {@code FMEventHandler#onGetHurt} that concern wand rods. */
@EventBusSubscriber(modid = Forbidden.MODID)
public final class FMCombatEvents {
    public static final TagKey<Item> WANDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thaumaturge", "wands"));

    private FMCombatEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(WANDS)) {
            return;
        }
        DamageSource source = event.getSource();
        // Infernal rod: immune to fire and wither while the wand is in hand.
        if (WandBridge.hasRod(held, FMWandParts.ROD_INFERNAL.getId()) && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.WITHER))) {
            event.setCanceled(true);
            return;
        }
        // Tainted rod: taint damage feeds the wand three random primal vis.
        if (WandBridge.hasRod(held, FMWandParts.ROD_TAINTED.getId()) && source.is(TTDamageTypes.TAINT)) {
            for (int i = 0; i < 3; i++) {
                ResourceKey<IAspect> primal = TTAspects.PRIMALS.get(player.getRandom().nextInt(TTAspects.PRIMALS.size()));
                WandBridge.addVis(held, primal, 100);
            }
        }
    }

    /**
     * Collar of Pain (FM {@code onFeelPain}, lowest priority): damage taken by the wearer becomes vis. Three doses per point
     * of damage, three more if a player hit with a riding crop; in the Nether a hit from another player has a 1/30 chance
     * (4/30 with the crop) to drop a lust shard.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onPain(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!fox.spiteful.forbidden.FMConfig.lustEnabled() || event.getInflictedDamage() <= 0 || !(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack collar = fox.spiteful.forbidden.compat.CuriosBridge.findEquipped(player, fox.spiteful.forbidden.registry.FMItems.SUB_COLLAR.get());
        if (collar.isEmpty()) {
            return;
        }
        boolean byPlayer = event.getSource().getEntity() instanceof Player;
        boolean crop = byPlayer && ((Player) event.getSource().getEntity()).getMainHandItem().getItem() instanceof fox.spiteful.forbidden.content.tool.RidingCropItem;
        if (byPlayer && player.level().dimension() == net.minecraft.world.level.Level.NETHER && player.getRandom().nextInt(30) < (crop ? 4 : 1)) {
            player.spawnAtLocation((net.minecraft.server.level.ServerLevel) player.level(), new ItemStack(fox.spiteful.forbidden.registry.FMItems.LUST_SHARD.get()), 1.0F);
        }
        fox.spiteful.forbidden.content.item.CollarItem.feed(collar, painDoses(event.getInflictedDamage(), crop), player.getRandom());
    }

    public static int painDoses(float damage, boolean crop) {
        return 3 * (int) damage + (crop ? 3 : 0);
    }
}
