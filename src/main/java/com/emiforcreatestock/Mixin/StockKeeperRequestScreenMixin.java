package com.emiforcreatestock.Mixin;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenMixin{
    @Invoker("sendIt")
    void emiforcreatestock$sendIt();
}
