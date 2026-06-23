package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceDynamoBlockEntity extends BlockEntity {
    private final GeneratorEnergyStorage energy =
            new GeneratorEnergyStorage(Config.ENERGY_CAPACITY.get(), Config.MAX_TRANSFER.get());
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public SourceDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ArsMekanica.SOURCE_DYNAMO_BE.get(), pos, state);
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
            var neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage handler = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
            if (handler == null) {
                continue;
            }
            int toSend = Math.min(Config.MAX_TRANSFER.get(), energy.getEnergyStored());
            int accepted = handler.receiveEnergy(toSend, true);
            if (accepted <= 0) {
                continue;
            }
            int sent = handler.receiveEnergy(accepted, false);
            if (sent > 0) {
                energy.extractEnergy(sent, false);
            }
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("arsmekanica_energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
