package com.meigoc.arsmekanica;

import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.documentation.entry.DocEntry;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class ArsMekanicaDocs {
    private static final String TEXT =
            "With Mekanism installed, Ars Nouveau's archwood is handled by the Precision Sawmill just like vanilla wood. "
            + "Logs, wood and stripped variants saw into archwood planks, and crafted archwood blocks break back down into planks.";

    public static void onAddEntries(ReloadDocumentationEvent.AddEntries event) {
        Item archwoodPlanks = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "archwood_planks"));
        ItemLike icon = archwoodPlanks == null ? ArsMekanica.SOURCE_DYNAMO_ITEM.get() : archwoodPlanks;

        DocEntry entry = new DocEntryBuilder(ArsMekanica.MODID, DocumentationRegistry.CRAFTING, "archwood_sawing")
                .withName("Archwood Sawing")
                .withIcon(icon)
                .withTextPage(TEXT)
                .build();

        DocumentationRegistry.registerEntry(DocumentationRegistry.CRAFTING, entry);
    }
}
