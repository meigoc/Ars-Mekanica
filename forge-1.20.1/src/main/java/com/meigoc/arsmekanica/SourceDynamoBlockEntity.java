package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceDynamoBlockEntity extends AbstractSourceMachine {
    public static final int SOURCE_CAPACITY = 10000;
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int SOURCE_TO_FE = 16;
    public static final int PULL_PER_TICK = 100;
    public static final int CONVERT_SOURCE_PER_TICK = 100;
    public static final int MAX_EXTRACT = 2000;
    public static final int RANGE = 5;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(ENERGY_CAPACITY, MAX_EXTRACT);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public SourceDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ArsMekanica.SOURCE_DYNAMO_BE.get(), pos, state);
        setMaxSource(SOURCE_CAPACITY);
    }

    @Override
    public int getTransferRate() {
        return PULL_PER_TICK;
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
            var neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage handler = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
            if (handler == null || !handler.canReceive()) {
                continue;
            }
            int toSend = Math.min(MAX_EXTRACT, energy.getEnergyStored());
            int sent = handler.receiveEnergy(toSend, false);
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
