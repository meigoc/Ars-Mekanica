package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
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
    private final GeneratorEnergyStorage energy =
            new GeneratorEnergyStorage(Config.ENERGY_CAPACITY.get(), Config.MAX_TRANSFER.get());

    public SourceDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ArsMekanica.SOURCE_DYNAMO_BE.get(), pos, state);
    }

    @Override
    protected SourceStorage createDefaultStorage() {
        return new SourceStorage(Config.SOURCE_CAPACITY.get());
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public void serverTick() {
        if (level == null) {
            return;
        }

        int perTick = Config.SOURCE_PER_TICK.get();

        int need = Math.min(perTick, getMaxSource() - getSource());
        if (need > 0) {
            for (ISpecialSourceProvider provider : SourceUtil.canTakeSource(worldPosition, level, Config.PULL_RANGE.get())) {
                if (need <= 0) {
                    break;
                }
                need -= transferSource(provider.getSource(), this, need);
            }
        }

        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
        int rate = Config.SOURCE_TO_FE.get();
        if (getSource() > 0 && room >= rate) {
            int convertible = Math.min(perTick, Math.min(getSource(), room / rate));
            if (convertible > 0) {
                int before = getSource();
                removeSource(convertible);
                int consumed = before - getSource();
                energy.generate(consumed * rate);
            }
        }

        pushEnergy();
        setChanged();
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
            if (neighbor == null) {
                continue;
            }
            int toSend = Math.min(Config.MAX_TRANSFER.get(), energy.getEnergyStored());
            int accepted = neighbor.receiveEnergy(toSend, true);
            if (accepted <= 0) {
                continue;
            }
            int sent = neighbor.receiveEnergy(accepted, false);
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
