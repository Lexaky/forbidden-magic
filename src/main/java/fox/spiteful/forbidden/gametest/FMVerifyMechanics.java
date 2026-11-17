package fox.spiteful.forbidden.gametest;

import static fox.spiteful.forbidden.gametest.FMVerifyTests.check;
import static fox.spiteful.forbidden.gametest.FMVerifyTests.count;
import static fox.spiteful.forbidden.gametest.FMVerifyTests.enchanted;
import static fox.spiteful.forbidden.gametest.FMVerifyTests.expect;
import static fox.spiteful.forbidden.gametest.FMVerifyTests.nether;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.api.research.IResearchStage;
import com.leclowndu93150.thaumaturge.api.research.KnowledgeReward;
import com.leclowndu93150.thaumaturge.api.research.ResearchParent;
import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.SpellSummary;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.compat.CuriosBridge;
import fox.spiteful.forbidden.content.block.WrathCageBlockEntity;
import fox.spiteful.forbidden.content.item.CollarItem;
import fox.spiteful.forbidden.content.wand.TaintedRodUpdate;
import fox.spiteful.forbidden.event.FMCombatEvents;
import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMEnchantments;
import fox.spiteful.forbidden.registry.FMItems;
import fox.spiteful.forbidden.registry.FMVillagers;
import fox.spiteful.forbidden.util.WandBridge;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;

/** Verification of the remaining mechanics through Thaumaturge, Curios and vanilla systems running for real. */
final class FMVerifyMechanics {
    private static final ResourceKey<IAspect> BESTIA = ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "bestia"));

    private FMVerifyMechanics() {}

    static void register(FMGameTests.Registrar r) {
        r.add("verify/infernal_and_tainted_rods_in_combat", helper -> {
            ServerLevel level = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand("forbidden:infernal"));
            LivingIncomingDamageEvent fire = new LivingIncomingDamageEvent(player, new DamageContainer(level.damageSources().inFire(), 4.0F));
            FMCombatEvents.onIncomingDamage(fire);
            check(helper, fire.isCanceled(), "infernal rod should block fire damage");
            LivingIncomingDamageEvent wither = new LivingIncomingDamageEvent(player, new DamageContainer(level.damageSources().wither(), 4.0F));
            FMCombatEvents.onIncomingDamage(wither);
            check(helper, wither.isCanceled(), "infernal rod should block wither damage");
            LivingIncomingDamageEvent fall = new LivingIncomingDamageEvent(player, new DamageContainer(level.damageSources().fall(), 4.0F));
            FMCombatEvents.onIncomingDamage(fall);
            check(helper, !fall.isCanceled(), "infernal rod must not block other damage");

            ItemStack tainted = wand("forbidden:tainted");
            player.setItemInHand(InteractionHand.MAIN_HAND, tainted);
            var taint = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(com.leclowndu93150.thaumaturge.api.damagesource.TTDamageTypes.TAINT);
            FMCombatEvents.onIncomingDamage(new LivingIncomingDamageEvent(player, new DamageContainer(new net.minecraft.world.damagesource.DamageSource(taint), 2.0F)));
            int total = TTAspects.PRIMALS.stream().mapToInt(a -> WandBridge.vis(tainted, a)).sum();
            check(helper, total == 300, "taint damage should feed the tainted rod 3 vis, got " + total + " centivis");
            helper.succeed();
        });

        r.add("verify/tainted_rod_recharges_on_tainted_ground", helper -> {
            ServerLevel level = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
            ItemStack wand = wand("forbidden:tainted");
            player.tickCount = 100;
            new TaintedRodUpdate().onUpdate(wand, player);
            check(helper, WandBridge.vis(wand, TTAspects.AER) == 0, "no recharge on clean ground");
            BlockPos at = player.blockPosition();
            TaintApi.addEcologicalPressure(level, at, 1000.0F);
            check(helper, TaintApi.isTainted(level, at), "Thaumaturge did not consider the ground tainted after adding pressure");
            new TaintedRodUpdate().onUpdate(wand, player);
            TaintApi.cleanEcologicalPressure(level, at, 1000.0F);
            for (var primal : TTAspects.PRIMALS) {
                check(helper, WandBridge.vis(wand, primal) == 100, primal.identifier() + " should gain 1 vis on tainted ground");
            }
            helper.succeed();
        });

        r.add("verify/riding_crop", helper -> {
            ServerLevel level = helper.getLevel();
            FakePlayer rider = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "fm_rider"));
            Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
            ItemStack crop = new ItemStack(FMItems.RIDING_CROP.get());
            crop.getItem().hurtEnemy(crop, pig, rider);
            check(helper, pig.getEffect(MobEffects.SPEED) != null && pig.getEffect(MobEffects.SPEED).getAmplifier() == 5, "pigs get Speed VI");
            Player target = helper.makeMockPlayer(GameType.SURVIVAL);
            crop.getItem().hurtEnemy(crop, target, rider);
            check(helper, target.hasEffect(MobEffects.SPEED) && target.hasEffect(MobEffects.HASTE) && target.hasEffect(MobEffects.STRENGTH), "players get speed, haste, strength");
            // Riding: lash the mount. NeoForge fake players refuse to ride, so a mock player rides here.
            pig.removeAllEffects();
            Player jockey = helper.makeMockPlayer(GameType.SURVIVAL);
            jockey.setPos(pig.getX(), pig.getY(), pig.getZ());
            boolean mounted = jockey.startRiding(pig, true, false);
            check(helper, mounted && jockey.getVehicle() == pig, "the test rider could not mount the pig");
            float before = pig.getHealth();
            jockey.setItemInHand(InteractionHand.MAIN_HAND, crop);
            crop.getItem().use(level, jockey, InteractionHand.MAIN_HAND);
            check(helper, pig.getHealth() < before && pig.hasEffect(MobEffects.SPEED),
                    "lashing should hurt the mount and speed it up: health " + before + " -> " + pig.getHealth() + ", speed " + pig.hasEffect(MobEffects.SPEED));
            jockey.stopRiding();
            // Lust shards in the Nether: 1/15 per hit.
            ServerLevel nether = nether(helper);
            nether.getChunk(0, 0);
            FakePlayer netherRider = FakePlayerFactory.get(nether, new GameProfile(UUID.randomUUID(), "fm_nether_rider"));
            Pig victim = EntityType.PIG.create(nether, EntitySpawnReason.COMMAND);
            victim.setPos(0.5, 64, 0.5);
            List<ItemStack> shards = DropCapture.during(nether, () -> {
                for (int i = 0; i < 1500; i++) {
                    crop.getItem().hurtEnemy(crop, victim, netherRider);
                }
            });
            expect(helper, (int) shards.stream().filter(s -> s.is(FMItems.LUST_SHARD.get())).count(), 1500, 1.0 / 15, "lust shard from the riding crop");
            helper.succeed();
        });

        r.add("verify/purifier_clears_flux_goo", helper -> {
            ServerLevel level = helper.getLevel();
            var goo = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "flux_goo"));
            check(helper, goo != Blocks.AIR, "thaumaturge:flux_goo block not found");
            BlockPos center = helper.absolutePos(new BlockPos(2, 1, 2));
            level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
            List<BlockPos> placed = List.of(center.offset(1, 1, 0), center.offset(-1, 1, 0), center.offset(0, 2, 1));
            for (BlockPos p : placed) {
                level.setBlockAndUpdate(p, goo.defaultBlockState());
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack shovel = new ItemStack(FMItems.TAINT_SHOVEL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
            var result = shovel.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(center.getCenter(), Direction.UP, center, false)));
            check(helper, result.consumesAction(), "the shovel should act on flux goo");
            for (BlockPos p : placed) {
                check(helper, !level.getBlockState(p).is(goo), "flux goo left at " + p);
            }
            check(helper, player.getMainHandItem().getDamageValue() == placed.size(), "one durability per purified block, got " + player.getMainHandItem().getDamageValue());
            helper.succeed();
        });

        r.add("verify/collar_through_curios_and_real_damage", helper -> {
            ServerLevel level = helper.getLevel();
            Player wearer = helper.makeMockPlayer(GameType.SURVIVAL);
            initSlots(wearer);
            check(helper, CuriosBridge.equipNecklace(wearer, new ItemStack(FMItems.SUB_COLLAR.get())), "could not equip the collar");
            ItemStack collar = CuriosBridge.findEquipped(wearer, FMItems.SUB_COLLAR.get());
            wearer.setHealth(20.0F);
            wearer.hurtServer(level, level.damageSources().generic(), 4.0F);
            check(helper, wearer.getHealth() < 20.0F, "the damage did not land");
            collar = CuriosBridge.findEquipped(wearer, FMItems.SUB_COLLAR.get());
            int stored = TTAspects.PRIMALS.stream().mapToInt(a -> CollarItem.stored(CuriosBridge.findEquipped(wearer, FMItems.SUB_COLLAR.get())).amount(a)).sum();
            check(helper, stored == 1200, "4 damage should store 12 vis in the worn collar, stored " + stored + " centivis");
            // The Curios tick of the worn collar moves vis into the held wand.
            ItemStack wand = wand("thaumaturge:greatwood");
            wearer.setItemInHand(InteractionHand.MAIN_HAND, wand);
            wearer.tickCount = CollarItem.PULSE_TICKS;
            var curio = collar.getCapability(CuriosCapability.ITEM);
            check(helper, curio != null, "the collar has no Curios capability");
            curio.curioTick(new SlotContext("necklace", wearer, 0, false, true));
            int inWand = TTAspects.PRIMALS.stream().mapToInt(a -> WandBridge.vis(wearer.getMainHandItem(), a)).sum();
            check(helper, inWand > 0, "the curio tick did not charge the held wand");
            // Lust shards from players' blows in the Nether: 1/30, 4/30 with a riding crop.
            ServerLevel nether = nether(helper);
            nether.getChunk(0, 0);
            FakePlayer sub = FakePlayerFactory.get(nether, new GameProfile(UUID.randomUUID(), "fm_sub"));
            sub.setPos(0.5, 64, 0.5);
            initSlots(sub);
            check(helper, CuriosBridge.equipNecklace(sub, new ItemStack(FMItems.SUB_COLLAR.get())), "could not collar the fake player");
            Player dom = helper.makeMockPlayer(GameType.SURVIVAL);
            dom.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FMItems.RIDING_CROP.get()));
            List<ItemStack> shards = DropCapture.during(nether, () -> {
                for (int i = 0; i < 1500; i++) {
                    // A fake player never ticks, so it keeps its spawn invulnerability; the event is built the way
                    // LivingEntity#actuallyHurt builds it. The real damage path is covered above in the overworld.
                    DamageContainer container = new DamageContainer(nether.damageSources().playerAttack(dom), 1.0F);
                    container.captureInflictedDamage();
                    FMCombatEvents.onPain(new LivingDamageEvent.Post(sub, container));
                }
            });
            int fed = TTAspects.PRIMALS.stream().mapToInt(a -> CollarItem.stored(CuriosBridge.findEquipped(sub, FMItems.SUB_COLLAR.get())).amount(a)).sum();
            check(helper, fed > 0, "the blows never reached the collar handler");
            expect(helper, (int) shards.stream().filter(s -> s.is(FMItems.LUST_SHARD.get())).count(), 1500, 4.0 / 30, "lust shard from the collar with a riding crop");
            helper.succeed();
        });

        r.add("verify/wrath_cage_fed_through_tubes", 300, helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos jarPos = helper.absolutePos(new BlockPos(1, 1, 2));
            BlockPos tubePos = jarPos.above();
            BlockPos cagePos = tubePos.east();
            level.setBlockAndUpdate(jarPos, block("jar_normal").defaultBlockState());
            level.setBlockAndUpdate(tubePos, block("tube").defaultBlockState());
            level.setBlockAndUpdate(cagePos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(cagePos, FMBlocks.WRATH_CAGE.get().defaultBlockState());
            WrathCageBlockEntity cage = (WrathCageBlockEntity) level.getBlockEntity(cagePos);
            cage.swapCrystal(EntityType.PIG);
            cage.forceDelay(100000);
            var jar = EssentiaAccess.transport(level, jarPos, Direction.UP);
            check(helper, jar != null && jar.addEssentia(Aspects.resolve(level, BESTIA), 20, Direction.UP) > 0, "could not fill the jar");
            helper.succeedWhen(() -> {
                check(helper, cage.stored(BESTIA) > 0, "no bestia reached the cage through the tube yet");
                var readout = level.getCapability(EssentiaCapabilities.ASPECT_QUERY, cagePos, null);
                check(helper, readout != null && readout.queryAspects().amountOf(BESTIA, level.registryAccess()) == cage.stored(BESTIA), "goggles readout mismatch");
            });
        });

        r.addIsolated("verify/heretic_takes_the_umbral_bush_job", 6000, helper -> {
            ServerLevel level = helper.getLevel();
            for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 1, 4)))) {
                level.setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
            }
            net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, FMBlocks.UMBRAL_BUSH.get().defaultBlockState(), helper.absolutePos(new BlockPos(1, 2, 1)), 3);
            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 3));
            villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), net.minecraft.world.entity.npc.villager.VillagerProfession.NONE));
            helper.succeedWhen(() -> check(helper, villager.getVillagerData().profession().is(FMVillagers.HERETIC.getKey()),
                    "villager has not become a heretic yet: " + villager.getVillagerData().profession()));
        });

        r.add("verify/anvil_applies_fm_books", helper -> {
            Player smith = helper.makeMockPlayer(GameType.CREATIVE);
            Object[][] cases = {
                    {FMEnchantments.WRATH, Items.DIAMOND_SWORD}, {FMEnchantments.CAPITALIST, Items.IRON_SWORD}, {FMEnchantments.CONSUMING, Items.IRON_PICKAXE},
                    {FMEnchantments.EDUCATIONAL, Items.IRON_SWORD}, {FMEnchantments.CORRUPTING, Items.IRON_PICKAXE}, {FMEnchantments.IMPACT, FMItems.MORPH_PICKAXE.get()},
                    {FMEnchantments.VOIDTOUCHED, FMItems.MORPH_AXE.get()}, {FMEnchantments.FIERY_CORE, FMItems.MORPH_PICKAXE.get()},
                    {FMEnchantments.WRATH, FMItems.SKULL_AXE.get()}, {FMEnchantments.CONSUMING, FMItems.TAINT_SHOVEL.get()}};
            for (Object[] c : cases) {
                @SuppressWarnings("unchecked") ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) c[0];
                ItemStack result = anvil(helper, smith, new ItemStack((net.minecraft.world.item.Item) c[1]), book(helper, key));
                check(helper, EnchantmentHelper.getItemEnchantmentLevel(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), result) == 1,
                        key.identifier() + " book did not apply to " + c[1]);
            }
            check(helper, anvil(helper, smith, new ItemStack(Items.IRON_PICKAXE), book(helper, FMEnchantments.IMPACT)).isEmpty(), "impact must only go on morphic tools");
            helper.succeed();
        });

        r.add("verify/research_playthrough", helper -> {
            ServerLevel level = helper.getLevel();
            FakePlayer scholar = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "fm_scholar"));
            var entries = level.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
            List<Identifier> ours = entries.listElementIds().map(ResourceKey::identifier).filter(id -> id.getNamespace().equals(Forbidden.MODID)).sorted().toList();
            check(helper, ours.size() == 30, "expected 30 research entries, found " + ours.size());
            ResearchManager.applyAutoUnlock(scholar);
            Set<Identifier> done = new HashSet<>();
            for (Identifier id : ours) {
                study(helper, scholar, entries, id, done);
            }
            for (Identifier id : ours) {
                check(helper, KnowledgeAccess.of(scholar).isResearchComplete(id), id + " was not completed");
                for (IResearchStage stage : entries.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, id)).value().stages()) {
                    for (Identifier recipe : stage.recipes()) {
                        check(helper, level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipe)).isPresent(), id + " lists missing recipe " + recipe);
                    }
                }
            }
            helper.succeed();
        });

        r.add("verify/blink_upgrades_in_real_spells", helper -> {
            ServerLevel level = helper.getLevel();
            FakePlayer caster = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "fm_caster"));
            caster.setPos(helper.absolutePos(new BlockPos(0, 2, 0)).getCenter());
            ResourceKey<SpellPart> hellfire = ResourceKey.create(SpellPart.REGISTRY_KEY, Forbidden.id("hellfire"));
            ResourceKey<SpellPart> pandemonium = ResourceKey.create(SpellPart.REGISTRY_KEY, Forbidden.id("pandemonium"));
            ResourceKey<SpellPart> blink = ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "blink"));
            ResourceKey<SpellPart> touch = ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "touch"));
            // What the focal manipulator would accept: touch -> pandemonium -> blink -> hellfire.
            Spell combo = new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(touch).then(SpellNode.of(pandemonium).then(SpellNode.of(blink).then(SpellNode.of(hellfire))))));
            ResearchManager.complete(caster, Forbidden.id("blink_upgrades"));
            ResearchManager.complete(caster, Identifier.fromNamespaceAndPath("thaumaturge", "focus_blink"));
            ResearchManager.complete(caster, Identifier.fromNamespaceAndPath("thaumaturge", "base_auromancy"));
            SpellSummary summary = Spells.analyze(combo, null, level.registryAccess(), caster);
            check(helper, summary.problems().stream().noneMatch(p -> p.fatal()), "spell rejected: " + summary.problems().stream().map(p -> p.message().getString()).toList());

            Zombie burned = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
            float before = burned.getHealth();
            Spells.cast(caster, ItemStack.EMPTY, new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(hellfire))),
                    List.of(SpellTarget.entity(burned, Vec3.ZERO)), 1.0F);
            check(helper, burned.getHealth() < before && burned.isOnFire(), "hellfire should hurt and ignite its target");

            Zombie pulled = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
            Spells.cast(caster, ItemStack.EMPTY, new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(pandemonium))),
                    List.of(SpellTarget.entity(pulled, Vec3.ZERO)), 1.0F);
            check(helper, pulled.position().distanceTo(caster.position()) < 0.5, "pandemonium should bring the monster to the caster");
            Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 1));
            Vec3 pigAt = pig.position();
            Spells.cast(caster, ItemStack.EMPTY, new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(pandemonium))),
                    List.of(SpellTarget.entity(pig, Vec3.ZERO)), 1.0F);
            check(helper, pig.position().distanceTo(pigAt) < 0.01, "pandemonium only moves monsters");
            burned.discard();
            pulled.discard();
            helper.succeed();
        });

        r.add("verify/scribing_tools_on_the_research_table", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
            level.setBlockAndUpdate(pos, block("research_table").defaultBlockState());
            var table = (com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable) level.getBlockEntity(pos);
            check(helper, table != null, "research table has no block entity");
            table.items().set(0, ItemResource.of(new ItemStack(FMItems.CRYSTALWELL.get())), 1);
            check(helper, table.hasInkReady() && table.consumeInk(), "the table would not draw ink from the crystalwell");
            check(helper, table.items().getResource(0).toStack(1).getDamageValue() == 1, "crystalwell ink not spent");
            table.items().set(0, ItemResource.of(new ItemStack(FMItems.PRIMEWELL.get())), 1);
            for (int i = 0; i < 150; i++) {
                check(helper, table.consumeInk(), "primewell ran dry at note " + i);
            }
            check(helper, table.items().getResource(0).toStack(1).getDamageValue() == 0, "primewell took damage on the table");
            helper.succeed();
        });
    }

    /** Studies an entry the way a player would: parents first, then each stage after earning its knowledge. Returns its warp. */
    private static int study(GameTestHelper helper, ServerPlayer player, net.minecraft.core.HolderLookup.RegistryLookup<IResearchEntry> entries, Identifier id, Set<Identifier> done) {
        if (!done.add(id) || KnowledgeAccess.of(player).isResearchComplete(id)) {
            return 0;
        }
        IResearchEntry entry = entries.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, id)).value();
        int warp = 0;
        for (ResearchParent parent : entry.parents()) {
            if (parent.id().getNamespace().equals(Forbidden.MODID)) {
                warp += study(helper, player, entries, parent.id(), done);
            } else {
                check(helper, entries.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, parent.id())).isPresent(), id + " has unknown parent " + parent.id());
                ResearchManager.complete(player, parent.id());
            }
        }
        var knowledge = KnowledgeAccess.of(player);
        int warpBefore = WarpHelper.getActualWarp(player);
        if (!knowledge.isResearchKnown(id)) {
            check(helper, ResearchManager.unlockRequested(player, id), "could not start " + id);
        }
        for (int guard = 0; !knowledge.isResearchComplete(id) && guard <= entry.stages().size(); guard++) {
            int stage = Math.max(0, knowledge.researchStage(id));
            // What the research table hands out: a solved note per theory row, aspects from the pool for observations.
            int theoryOffset = 0;
            for (int i = 0; i < stage; i++) {
                theoryOffset += (int) entry.stages().get(i).requiredKnowledge().stream().filter(k -> k.type() == KnowledgeType.THEORY).count();
            }
            int row = 0;
            for (KnowledgeReward need : entry.stages().get(Math.min(stage, entry.stages().size() - 1)).requiredKnowledge()) {
                if (need.type() == KnowledgeType.THEORY) {
                    knowledge.addResearch(com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData.learnKey(id, theoryOffset + row++));
                } else {
                    for (var aspect : entry.noteAspects().entries()) {
                        com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess.grant(player, aspect.aspect(), aspect.amount() * Math.max(4, need.amount()) * 4);
                    }
                }
            }
            check(helper, ResearchManager.advanceStage(player, id), "could not advance " + id + " at stage " + stage);
        }
        check(helper, knowledge.isResearchComplete(id), id + " did not complete");
        int expected = entry.stages().stream().mapToInt(IResearchStage::warp).sum();
        int gained = WarpHelper.getActualWarp(player) - warpBefore;
        check(helper, gained == expected, id + " should give " + expected + " warp, gave " + gained);
        return warp + expected;
    }

    private static ItemStack anvil(GameTestHelper helper, Player player, ItemStack tool, ItemStack book) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(tool);
        menu.getSlot(1).set(book);
        menu.createResult();
        return menu.getSlot(2).getItem();
    }

    private static ItemStack book(GameTestHelper helper, ResourceKey<Enchantment> key) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), 1);
        book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private static net.minecraft.world.level.block.Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", path));
    }

    private static void initSlots(LivingEntity entity) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
            if (inv.getCurios().isEmpty()) {
                inv.reset();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ItemStack wand(String rod) {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", rod);
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        return wand;
    }
}
