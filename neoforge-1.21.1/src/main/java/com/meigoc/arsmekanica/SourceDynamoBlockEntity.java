package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SourceDynamoBlockEntity extends BlockEntity {
    private final GeneratorEnergyStorage energy =
            new GeneratorEnergyStorage(Config.ENERGY_CAPACITY.get(), Config.MAX_TRANSFER.get());

    public SourceDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ArsMekanica.SOURCE_DYNAMO_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public void serverTick() {
        if (level == null) {
            return;
        }

        int perTick = Config.SOURCE_PER_TICK.get();
        int rate = Config.SOURCE_TO_FE.get();
        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();

        if (room < rate) {
            pushEnergy();
            return;
        }

        int stillNeed = Math.min(perTick, room / rate);

        for (var provider : SourceUtil.canTakeSource(worldPosition, level, Config.PULL_RANGE.get())) {
            ISourceTile jar = provider.getSource();
            int available = jar.getSource();
            if (available <= 0) {
                continue;
            }
            int drawn = Math.min(stillNeed, available);
            jar.removeSource(drawn);
            energy.generate(drawn * rate);
            stillNeed -= drawn;
            if (stillNeed <= 0) {
                break;
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
