package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.capability.SourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SourceDynamoBlockEntity extends AbstractSourceMachine {
    public static final int SOURCE_CAPACITY = 10000;
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int SOURCE_TO_FE = 16;
    public static final int PULL_PER_TICK = 100;
    public static final int CONVERT_SOURCE_PER_TICK = 100;
    public static final int MAX_EXTRACT = 2000;
    public static final int RANGE = 5;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(ENERGY_CAPACITY, MAX_EXTRACT);

    public SourceDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ArsMekanica.SOURCE_DYNAMO_BE.get(), pos, state);
    }

    @Override
    protected SourceStorage createDefaultStorage() {
        return new SourceStorage(SOURCE_CAPACITY);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public void serverTick() {
        if (level == null) {
            return;
        }

        if (getSource() < getMaxSource()) {
            int need = Math.min(PULL_PER_TICK, getMaxSource() - getSource());
            if (need > 0 && SourceUtil.takeSource(worldPosition, level, RANGE, need) != null) {
                addSource(need);
            }
        }

        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (getSource() > 0 && room > 0) {
            int convertible = Math.min(CONVERT_SOURCE_PER_TICK, Math.min(getSource(), room / SOURCE_TO_FE));
            if (convertible > 0) {
                removeSource(convertible);
                energy.generate(convertible * SOURCE_TO_FE);
            }
        }

        pushEnergy();
    }

    private void pushEnergy() {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (energy.getEnergyStored() <= 0) {
                break;
            }
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }
            int toSend = Math.min(MAX_EXTRACT, energy.getEnergyStored());
            int sent = neighbor.receiveEnergy(toSend, false);
            if (sent > 0) {
                energy.extractEnergy(sent, false);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("arsmekanica_energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy.setStored(tag.getInt("arsmekanica_energy"));
    }

    private static class GeneratorEnergyStorage extends EnergyStorage {
        GeneratorEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }

        void generate(int amount) {
            this.energy = Math.min(this.capacity, this.energy + amount);
        }

        void setStored(int amount) {
            this.energy = Math.max(0, Math.min(this.capacity, amount));
        }
    }
}
