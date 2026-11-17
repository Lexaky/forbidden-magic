package fox.spiteful.forbidden.content.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IAspectQuery;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.item.MobCrystalItem;
import fox.spiteful.forbidden.registry.FMBlockEntities;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Wrath Cage (FM {@code TileEntityWrathCage} + {@code WrathSpawnerLogic}): a spawner for the mob imprinted in its crystal.
 *
 * <p>Each round of spawns costs {@code fuelCost} essentia (default 5) and buys {@code fuelEfficiency} spawns (default 4).
 * Fuel is taken from the mob's own essentia first, then Ira, then Desidia; Desidia fuel adds 200 ticks to the delay. Up to
 * 64 of each is stored. A redstone signal pauses the cage. Spawn rules match FM: three tries per cycle, at most six of
 * the mob within range, 200-300 tick delay.
 *
 * <p>Essentia is pulled one point at a time from any adjacent essentia transport (jar, tube, ...). The cage also exposes
 * suction so Thaumaturge tubes route essentia to it; which essentia it asks for follows its mode (mob essentia, Ira,
 * Desidia), switched with the Diabolist Fork as in FM.
 */
public class WrathCageBlockEntity extends BlockEntity implements IEssentiaTransport, IAspectQuery {
    public static final int CAPACITY = 64;
    public static final ResourceKey<IAspect> IRA = ResourceKey.create(IAspect.REGISTRY_KEY, Forbidden.id("ira"));
    public static final ResourceKey<IAspect> DESIDIA = ResourceKey.create(IAspect.REGISTRY_KEY, Forbidden.id("desidia"));
    private static final int SPAWN_COUNT = 3;
    private static final int MAX_NEARBY = 6;
    private static final int SPAWN_RANGE = 4;
    private static final int MIN_DELAY = 200;
    private static final int MAX_DELAY = 300;
    private static final int SUCTION = 128;

    private @Nullable Identifier mob;
    private int wrath;
    private int sloth;
    private int special;
    private int mode;
    private int fuel;
    private boolean slothful;
    private int spawnDelay = 20;

    public WrathCageBlockEntity(BlockPos pos, BlockState state) {
        super(FMBlockEntities.WRATH_CAGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WrathCageBlockEntity cage) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!server.hasNeighborSignal(pos)) {
            cage.updateSpawner(server);
        }
        int cost = FMConfig.wrathCost();
        if (cost > 0 && cage.mob != null && !(cage.special >= CAPACITY || cage.wrath >= CAPACITY || cage.sloth >= CAPACITY)) {
            cage.drawEssentia(server, cost);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Mob
    // ----------------------------------------------------------------------------------------------------------------

    public Optional<EntityType<?>> mobType() {
        return mob == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(mob);
    }

    /** The essentia this cage's mob burns, or {@code null} without a mob. */
    public @Nullable ResourceKey<IAspect> mobAspect() {
        return mobType().flatMap(MobCrystalItem::aspectFor).orElse(null);
    }

    /** Installs the mob of an imprinted crystal and returns the crystal that was in the cage before (or empty). */
    public ItemStack swapCrystal(EntityType<?> type) {
        ItemStack previous = mobType().map(old -> MobCrystalItem.imprinted(FMItems.MOB_CRYSTAL.get(), old)).orElse(ItemStack.EMPTY);
        ResourceKey<IAspect> before = mobAspect();
        mob = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (before == null || !before.equals(mobAspect())) {
            special = 0; // FM checkAspect: stored mob essentia is lost when the essentia type changes
        }
        markUpdated();
        return previous;
    }

    public int cycleMode() {
        mode = (mode + 1) % 3;
        markUpdated();
        return mode;
    }

    public int mode() {
        return mode;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Spawning
    // ----------------------------------------------------------------------------------------------------------------

    void updateSpawner(ServerLevel level) {
        Optional<EntityType<?>> type = mobType();
        if (type.isEmpty()) {
            return;
        }
        int cost = FMConfig.wrathCost();
        if (cost > 0 && fuel <= 0) {
            if (special >= cost) {
                special -= cost;
                slothful = false;
                fuel = FMConfig.wrathEfficiency();
            } else if (wrath >= cost) {
                wrath -= cost;
                slothful = false;
                fuel = FMConfig.wrathEfficiency();
            } else if (sloth >= cost) {
                sloth -= cost;
                slothful = true;
                fuel = FMConfig.wrathEfficiency();
            }
            if (fuel > 0) {
                markUpdated();
            }
        }
        if (spawnDelay == -1) {
            resetDelay(level.getRandom());
        }
        if (fuel <= 0 && cost > 0) {
            return;
        }
        if (spawnDelay > 0) {
            spawnDelay--;
            return;
        }
        RandomSource rand = level.getRandom();
        BlockPos pos = getBlockPos();
        for (int i = 0; i < SPAWN_COUNT && (fuel > 0 || cost <= 0); i++) {
            Entity entity = type.get().create(level, EntitySpawnReason.SPAWNER);
            if (entity == null) {
                return;
            }
            AABB area = new AABB(pos).inflate(SPAWN_RANGE * 2, 4.0, SPAWN_RANGE * 2);
            int nearby = level.getEntities(entity.getType(), area, e -> true).size();
            if (nearby >= MAX_NEARBY) {
                entity.discard();
                resetDelay(rand);
                return;
            }
            double x = pos.getX() + (rand.nextDouble() - rand.nextDouble()) * SPAWN_RANGE;
            double y = pos.getY() + rand.nextInt(3) - 1;
            double z = pos.getZ() + (rand.nextDouble() - rand.nextDouble()) * SPAWN_RANGE;
            entity.snapTo(x, y, z, rand.nextFloat() * 360.0F, 0.0F);
            if (level.noCollision(entity) && !level.containsAnyLiquid(entity.getBoundingBox())) {
                if (entity instanceof Mob mobEntity) {
                    mobEntity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
                }
                level.addFreshEntityWithPassengers(entity);
                level.levelEvent(LevelEvent.PARTICLES_MOBBLOCK_SPAWN, pos, 0);
                if (entity instanceof Mob mobEntity) {
                    mobEntity.spawnAnim();
                    fuel--;
                }
                resetDelay(rand);
            } else {
                entity.discard();
            }
        }
        setChanged();
    }

    private void resetDelay(RandomSource rand) {
        spawnDelay = MIN_DELAY + rand.nextInt(MAX_DELAY - MIN_DELAY);
        if (slothful) {
            spawnDelay += 200;
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Essentia
    // ----------------------------------------------------------------------------------------------------------------

    void drawEssentia(ServerLevel level, int cost) {
        ResourceKey<IAspect> aspect = mobAspect();
        for (Direction dir : Direction.values()) {
            Direction face = dir.getOpposite();
            IEssentiaTransport source = EssentiaAccess.transport(level, getBlockPos().relative(dir), face);
            if (source == null || !source.canOutputTo(face) || source.getEssentiaAmount(face) <= 0) {
                continue;
            }
            Holder<IAspect> held = source.getEssentiaType(face);
            if (held == null) {
                continue;
            }
            if (special < CAPACITY && aspect != null && held.is(aspect) && source.takeEssentia(held, 1, face) == 1) {
                special++;
            } else if (wrath < CAPACITY && special < cost && held.is(IRA) && source.takeEssentia(held, 1, face) == 1) {
                wrath++;
            } else if (sloth < CAPACITY && special < cost && wrath < cost && held.is(DESIDIA) && source.takeEssentia(held, 1, face) == 1) {
                sloth++;
            } else {
                continue;
            }
            markUpdated();
            return;
        }
    }

    /** Adds essentia directly (used by tests and by sources that push). Returns the amount accepted. */
    public int fill(ResourceKey<IAspect> aspect, int amount) {
        if (FMConfig.wrathCost() <= 0 || mob == null || amount <= 0) {
            return 0;
        }
        int accepted;
        if (aspect.equals(mobAspect())) {
            accepted = Math.min(amount, CAPACITY - special);
            special += accepted;
        } else if (aspect.equals(IRA)) {
            accepted = Math.min(amount, CAPACITY - wrath);
            wrath += accepted;
        } else if (aspect.equals(DESIDIA)) {
            accepted = Math.min(amount, CAPACITY - sloth);
            sloth += accepted;
        } else {
            return 0;
        }
        if (accepted > 0) {
            markUpdated();
        }
        return accepted;
    }

    public int stored(ResourceKey<IAspect> aspect) {
        if (aspect.equals(IRA)) {
            return wrath;
        } else if (aspect.equals(DESIDIA)) {
            return sloth;
        }
        return aspect.equals(mobAspect()) ? special : 0;
    }

    public int fuel() {
        return fuel;
    }

    public int spawnDelay() {
        return spawnDelay;
    }

    /** Test hook: skip the current delay. */
    public void forceDelay(int ticks) {
        spawnDelay = ticks;
    }

    private @Nullable ResourceKey<IAspect> wantedAspect() {
        return switch (mode) {
            case 1 -> IRA;
            case 2 -> DESIDIA;
            default -> mobAspect();
        };
    }

    @Override
    public int addEssentia(Holder<IAspect> type, int count, Direction side) {
        return type.unwrapKey().map(key -> fill(key, count)).orElse(0);
    }

    @Override
    public int takeEssentia(Holder<IAspect> type, int count, Direction side) {
        return 0;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        return null;
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return 0;
    }

    @Override
    public void setSuction(@Nullable Holder<IAspect> type, int count) {}

    @Override
    public @Nullable Holder<IAspect> getSuctionType(@Nullable Direction side) {
        ResourceKey<IAspect> wanted = wantedAspect();
        return wanted == null || level == null ? null : Aspects.resolve(level, wanted);
    }

    @Override
    public int getSuctionAmount(@Nullable Direction side) {
        ResourceKey<IAspect> wanted = wantedAspect();
        return FMConfig.wrathCost() > 0 && wanted != null && stored(wanted) < CAPACITY ? SUCTION : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return false;
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return true;
    }

    @Override
    public boolean isConnectable(Direction side) {
        return true;
    }

    /** Goggles readout: the essentia of the current mode, as FM's {@code getAspects}. */
    @Override
    public AspectList queryAspects() {
        ResourceKey<IAspect> shown = wantedAspect();
        if (FMConfig.wrathCost() <= 0 || mob == null || shown == null || level == null) {
            return AspectList.EMPTY;
        }
        Holder<IAspect> holder = Aspects.resolve(level, shown);
        int amount = stored(shown);
        return holder == null || amount <= 0 ? AspectList.EMPTY : AspectList.of(new AspectInstance(holder, amount));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Persistence
    // ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("Mob", Identifier.CODEC, mob);
        output.putInt("Wrath", wrath);
        output.putInt("Sloth", sloth);
        output.putInt("Special", special);
        output.putInt("Mode", mode);
        output.putInt("Fuel", fuel);
        output.putBoolean("Slothful", slothful);
        output.putInt("Delay", spawnDelay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mob = input.read("Mob", Identifier.CODEC).orElse(null);
        wrath = input.getIntOr("Wrath", 0);
        sloth = input.getIntOr("Sloth", 0);
        special = input.getIntOr("Special", 0);
        mode = input.getIntOr("Mode", 0);
        fuel = input.getIntOr("Fuel", 0);
        slothful = input.getBooleanOr("Slothful", false);
        spawnDelay = input.getIntOr("Delay", 20);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // FM onBlockPreDestroy: the imprinted crystal pops out of a broken cage.
        if (level != null && !level.isClientSide()) {
            mobType().ifPresent(type -> {
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, MobCrystalItem.imprinted(FMItems.MOB_CRYSTAL.get(), type));
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            });
        }
        super.preRemoveSideEffects(pos, state);
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
