package com.emiforcreatestock.Mixin;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StockKeeperRequestScreen.CategoryEntry.class)
public interface CategoryEntryMixin {
    @Accessor("name")
    String getName();
}
