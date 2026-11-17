package fox.spiteful.forbidden.event;

import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.registry.FMEnchantments;
import fox.spiteful.forbidden.registry.FMItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/** Mob drop rules from FM's {@code FMEventHandler#onLivingDrops}, {@code #onSpawn} and {@code #onDeath}. */
@EventBusSubscriber(modid = Forbidden.MODID)
public final class FMDropEvents {
    private FMDropEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        RandomSource rand = level.getRandom();
        Entity killer = event.getSource().getEntity();
        Player player = killer instanceof Player p ? p : null;
        boolean byRealPlayer = player != null && !(player instanceof FakePlayer) && event.isRecentlyHit();
        ItemStack weapon = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
        int looting = enchant(level, weapon, Enchantments.LOOTING);

        // Silverfish: emerald nuggets, better odds in mountains.
        if (FMConfig.silverfishEmeralds() && victim instanceof Silverfish && byRealPlayer) {
            boolean mountain = level.getBiome(victim.blockPosition()).is(Tags.Biomes.IS_MOUNTAIN);
            if (mountain ? rand.nextInt(30) <= 2 + looting * 2 : rand.nextInt(70) <= 1 + looting * 3) {
                add(event, new ItemStack(FMItems.EMERALD_NUGGET.get()));
            }
        }

        // Capitalist: emeralds from villagers and emerald nuggets from monsters instead of looting.
        if (FMConfig.capitalist() && byRealPlayer && (victim instanceof Villager || victim instanceof Enemy)
                && FMEnchantments.level(level, weapon, FMEnchantments.CAPITALIST) > 0 && looting <= 0) {
            if (victim instanceof Villager) {
                add(event, new ItemStack(Items.EMERALD));
            } else if (rand.nextInt(35) < 3) {
                add(event, new ItemStack(FMItems.EMERALD_NUGGET.get()));
            }
        }

        // Axe of the Skulltaker: heads.
        if (player != null && event.isRecentlyHit() && weapon.is(FMItems.SKULL_AXE.get())) {
            skullDrop(event, victim, rand, looting);
        }

        if (level.dimension() != Level.NETHER) {
            return;
        }
        if (!byRealPlayer) {
            // Mobs that die without a player's hand behind it leave sloth shards.
            if (rand.nextInt(30) < 4) {
                add(event, new ItemStack(FMItems.SLOTH_SHARD.get(), 1 + rand.nextInt(3)));
            }
            return;
        }
        if (victim instanceof Enemy) {
            int wrath = 2 + weaponWrath(level, player, weapon, victim);
            int greed = looting;
            if (rand.nextInt(61) <= wrath) {
                add(event, new ItemStack(FMItems.WRATH_SHARD.get()));
            }
            if (victim.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)) {
                add(event, new ItemStack(FMItems.PRIDE_SHARD.get(), 2 + rand.nextInt(1 + looting)));
            }
            if (greed > 0 && rand.nextInt(20) <= greed) {
                add(event, new ItemStack(FMItems.GREED_SHARD.get()));
            }
        }
        if (victim instanceof ZombifiedPiglin && victim.getItemBySlot(EquipmentSlot.MAINHAND).is(FMItems.ENVY_SHARD.get())) {
            add(event, new ItemStack(FMItems.ENVY_SHARD.get()));
        }
    }

    /**
     * FM's wrath bonus: the weapon's damage plus Sharpness/Power, Fire Aspect/Flame against non-fire-immune targets, Smite
     * against undead and Bane of Arthropods against arthropods. FM also added wand focus potency; Thaumaturge foci have
     * no equivalent stat, so that part is dropped.
     */
    private static int weaponWrath(ServerLevel level, Player player, ItemStack weapon, LivingEntity victim) {
        int wrath = Math.max(0, (int) player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1);
        wrath += enchant(level, weapon, Enchantments.SHARPNESS) + enchant(level, weapon, Enchantments.POWER);
        if (!victim.fireImmune()) {
            wrath += enchant(level, weapon, Enchantments.FIRE_ASPECT) + enchant(level, weapon, Enchantments.FLAME);
        }
        if (victim.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
            wrath += enchant(level, weapon, Enchantments.SMITE);
        }
        if (victim.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)) {
            wrath += enchant(level, weapon, Enchantments.BANE_OF_ARTHROPODS);
        }
        return wrath;
    }

    private static void skullDrop(LivingDropsEvent event, LivingEntity victim, RandomSource rand, int looting) {
        if (victim.getClass() == Skeleton.class && rand.nextInt(26) <= 3 + looting) {
            add(event, new ItemStack(Items.SKELETON_SKULL));
        } else if (victim.getClass() == WitherSkeleton.class && rand.nextInt(26) <= 3 + looting) {
            add(event, new ItemStack(Items.WITHER_SKELETON_SKULL));
        } else if (victim.getClass() == Zombie.class && rand.nextInt(26) <= 2 + 2 * looting) {
            add(event, new ItemStack(Items.ZOMBIE_HEAD));
        } else if (victim.getClass() == Creeper.class && rand.nextInt(26) <= 2 + 2 * looting) {
            add(event, new ItemStack(Items.CREEPER_HEAD));
        } else if (victim instanceof Player victimPlayer && rand.nextInt(11) <= 1 + looting) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victimPlayer.getGameProfile()));
            add(event, head);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null || !FMConfig.enchantments() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        int educational = FMEnchantments.level(level, weapon, FMEnchantments.EDUCATIONAL);
        if (educational > 0 && enchant(level, weapon, Enchantments.LOOTING) == 0) {
            event.setDroppedExperience(event.getDroppedExperience() + 3 * event.getOriginalExperience() * educational);
        }
    }

    /** Diabolist Fork kills imprint the first blank crystal in the killer's inventory (FM {@code imprintCrystal}). */
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || !player.getMainHandItem().is(FMItems.DIABOLIST_FORK.get())) {
            return;
        }
        imprint(player, event.getEntity().getType());
    }

    public static boolean imprint(Player player, net.minecraft.world.entity.EntityType<?> type) {
        if (!fox.spiteful.forbidden.content.item.MobCrystalItem.canImprint(type)) {
            return false;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(FMItems.MOB_CRYSTAL.get()) && !stack.has(fox.spiteful.forbidden.registry.FMDataComponents.MOB.get())) {
                stack.set(fox.spiteful.forbidden.registry.FMDataComponents.MOB.get(), net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof ZombifiedPiglin piglin && piglin.level().dimension() == Level.NETHER && event.getLevel().getRandom().nextInt(175) == 1) {
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FMItems.ENVY_SHARD.get()));
        }
    }

    static int enchant(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    static void add(LivingDropsEvent event, ItemStack stack) {
        LivingEntity entity = event.getEntity();
        ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        drop.setPickUpDelay(10);
        event.getDrops().add(drop);
    }
}
