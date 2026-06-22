package com.meigoc.arsmekanica;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SOURCE_TO_FE;
    public static final ModConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue MAX_TRANSFER;
    public static final ModConfigSpec.IntValue SOURCE_PER_TICK;
    public static final ModConfigSpec.IntValue SOURCE_CAPACITY;
    public static final ModConfigSpec.IntValue PULL_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("source_dynamo");
        SOURCE_TO_FE = builder.comment("Forge Energy produced per 1 Source consumed")
                .defineInRange("sourceToFE", 16, 1, 1_000_000);
        SOURCE_PER_TICK = builder.comment("Source drawn and converted per tick (controls drain speed and FE output)")
                .defineInRange("sourcePerTick", 20, 1, 1_000_000);
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
