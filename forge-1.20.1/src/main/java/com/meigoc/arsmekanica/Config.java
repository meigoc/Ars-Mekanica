package com.meigoc.arsmekanica;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue SOURCE_TO_FE;
    public static final ForgeConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue MAX_TRANSFER;
    public static final ForgeConfigSpec.IntValue SOURCE_PER_TICK;
    public static final ForgeConfigSpec.IntValue SOURCE_CAPACITY;
    public static final ForgeConfigSpec.IntValue PULL_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("source_dynamo");
        SOURCE_TO_FE = builder.comment("Forge Energy produced per 1 Source consumed")
                .defineInRange("sourceToFE", 16, 1, 1_000_000);
        SOURCE_PER_TICK = builder.comment("Maximum Source converted per tick")
                .defineInRange("sourcePerTick", 100, 1, 1_000_000);
        ENERGY_CAPACITY = builder.comment("Internal Forge Energy buffer")
                .defineInRange("energyCapacity", 64_000, 1_000, Integer.MAX_VALUE);
        MAX_TRANSFER = builder.comment("Maximum Forge Energy sent per side per tick")
                .defineInRange("maxTransfer", 2_000, 1, Integer.MAX_VALUE);
        SOURCE_CAPACITY = builder.comment("Internal Source buffer")
                .defineInRange("sourceCapacity", 10_000, 100, 10_000_000);
        PULL_RANGE = builder.comment("Block range to pull Source from nearby jars")
                .defineInRange("pullRange", 5, 1, 16);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }
}
