package fox.spiteful.forbidden.gametest;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.block.WrathCageBlockEntity;
import fox.spiteful.forbidden.content.item.MobCrystalItem;
import fox.spiteful.forbidden.event.FMDropEvents;
import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMDataMaps;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Wrath Cage and imprinting crystal checks for milestone M7. */
final class FMCageTests {
    private static final ResourceKey<IAspect> BESTIA = ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "bestia"));

    private FMCageTests() {}

    static void register(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("cage/data_loaded", helper -> {
            check(helper, BESTIA.equals(EntityType.ZOMBIE.builtInRegistryHolder().getData(FMDataMaps.WRATH_CAGE_MOBS)), "zombie should burn bestia");
            check(helper, Forbidden.id("infernus").equals(EntityType.GHAST.builtInRegistryHolder().getData(FMDataMaps.WRATH_CAGE_MOBS).identifier()), "ghast should burn infernus");
            var firebat = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "firebat"));
            check(helper, firebat.builtInRegistryHolder().getData(FMDataMaps.WRATH_CAGE_MOBS) != null, "Thaumaturge firebat should be in the cage list");
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String key : List.of("infusion/wrath_cage", "crucible/mob_crystal")) {
                check(helper, recipes.byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id(key))).isPresent(), "recipe " + key + " missing");
            }
            helper.succeed();
        });

        add.accept("cage/fork_imprints_blank_crystal", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().add(new ItemStack(FMItems.MOB_CRYSTAL.get()));
            check(helper, !FMDropEvents.imprint(player, EntityType.ENDER_DRAGON), "bosses outside the list must not imprint");
            check(helper, FMDropEvents.imprint(player, EntityType.ZOMBIE), "zombie kill should imprint");
            boolean found = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(FMItems.MOB_CRYSTAL.get()) && MobCrystalItem.mob(s).filter(t -> t == EntityType.ZOMBIE).isPresent()) {
                    found = true;
                }
            }
            check(helper, found, "no zombie crystal in inventory");
            check(helper, !FMDropEvents.imprint(player, EntityType.PIG), "an imprinted crystal must not be overwritten");
            helper.succeed();
        });

        add.accept("cage/fuel_and_spawn", helper -> {
            Setup s = setup(helper, EntityType.PIG);
            check(helper, s.cage.fill(BESTIA, 5) == 5, "cage should accept the mob's essentia");
            boolean spawned = false;
            for (int i = 0; i < 20 && !spawned; i++) {
                s.cage.forceDelay(0);
                WrathCageBlockEntity.serverTick(s.level, s.pos, s.level.getBlockState(s.pos), s.cage);
                spawned = !s.level.getEntities(EntityType.PIG, new AABB(s.pos).inflate(8), e -> true).isEmpty();
            }
            check(helper, spawned, "no pig spawned");
            check(helper, s.cage.stored(BESTIA) == 0, "5 essentia should have been burned for fuel");
            check(helper, s.cage.fuel() < 4, "fuel should drop as mobs spawn: " + s.cage.fuel());
            check(helper, s.cage.spawnDelay() >= 200 && s.cage.spawnDelay() < 300, "delay after a spawn: " + s.cage.spawnDelay());
            helper.succeed();
        });

        add.accept("cage/redstone_pauses", helper -> {
            Setup s = setup(helper, EntityType.PIG);
            s.level.setBlockAndUpdate(s.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            s.cage.fill(BESTIA, 5);
            s.cage.forceDelay(0);
            WrathCageBlockEntity.serverTick(s.level, s.pos, s.level.getBlockState(s.pos), s.cage);
            check(helper, s.cage.stored(BESTIA) == 5 && s.cage.fuel() == 0, "a powered cage must not burn fuel");
            helper.succeed();
        });

        add.accept("cage/desidia_slows_spawns", helper -> {
            Setup s = setup(helper, EntityType.PIG);
            s.cage.fill(WrathCageBlockEntity.DESIDIA, 5);
            boolean spawned = false;
            for (int i = 0; i < 20 && !spawned; i++) {
                s.cage.forceDelay(0);
                WrathCageBlockEntity.serverTick(s.level, s.pos, s.level.getBlockState(s.pos), s.cage);
                spawned = !s.level.getEntities(EntityType.PIG, new AABB(s.pos).inflate(8), e -> true).isEmpty();
            }
            check(helper, spawned, "no pig spawned on desidia fuel");
            check(helper, s.cage.spawnDelay() >= 400, "desidia fuel should add 200 ticks of delay: " + s.cage.spawnDelay());
            helper.succeed();
        });

        add.accept("cage/draws_from_jar", helper -> {
            Setup s = setup(helper, EntityType.PIG);
            BlockPos jarPos = s.pos.below(); // jars only connect through their top face
            s.level.setBlockAndUpdate(jarPos, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "jar_normal")).defaultBlockState());
            IEssentiaTransport jar = EssentiaAccess.transport(s.level, jarPos, Direction.UP);
            check(helper, jar != null, "Thaumaturge jar exposes no essentia transport");
            int added = jar.addEssentia(Aspects.resolve(s.level, BESTIA), 10, Direction.UP);
            check(helper, added > 0, "could not fill the jar with bestia");
            s.cage.forceDelay(1000);
            WrathCageBlockEntity.serverTick(s.level, s.pos, s.level.getBlockState(s.pos), s.cage);
            WrathCageBlockEntity.serverTick(s.level, s.pos, s.level.getBlockState(s.pos), s.cage);
            check(helper, s.cage.stored(BESTIA) == 2, "cage should pull one essentia per tick from the jar, has " + s.cage.stored(BESTIA));
            check(helper, s.cage.getSuctionAmount(Direction.WEST) > 0 && s.cage.getSuctionType(Direction.WEST).is(BESTIA), "cage should ask tubes for bestia");
            helper.succeed();
        });

        add.accept("cage/crystal_swap_and_drop", helper -> {
            Setup s = setup(helper, EntityType.PIG);
            ItemStack previous = s.cage.swapCrystal(EntityType.COW);
            check(helper, MobCrystalItem.mob(previous).filter(t -> t == EntityType.PIG).isPresent(), "swapping should return the pig crystal");
            s.level.destroyBlock(s.pos, false);
            List<ItemEntity> drops = s.level.getEntities(EntityType.ITEM, new AABB(s.pos).inflate(2), e -> e.getItem().is(FMItems.MOB_CRYSTAL.get()));
            check(helper, drops.size() == 1 && MobCrystalItem.mob(drops.getFirst().getItem()).filter(t -> t == EntityType.COW).isPresent(),
                    "breaking the cage should drop the cow crystal, got " + drops);
            helper.succeed();
        });
    }

    private record Setup(ServerLevel level, BlockPos pos, WrathCageBlockEntity cage) {}

    private static Setup setup(GameTestHelper helper, EntityType<?> mob) {
        ServerLevel level = helper.getLevel();
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 1, 4)))) {
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, FMBlocks.WRATH_CAGE.get().defaultBlockState());
        WrathCageBlockEntity cage = (WrathCageBlockEntity) level.getBlockEntity(pos);
        cage.swapCrystal(mob);
        return new Setup(level, pos, cage);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }
}
