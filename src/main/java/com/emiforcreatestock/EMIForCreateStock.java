package com.emiforcreatestock;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = EMIForCreateStock.MODID,dist = Dist.CLIENT)
public class EMIForCreateStock {
    public static final String MODID = "emiforcreatestock";

    public EMIForCreateStock(IEventBus modEventBus, ModContainer modContainer) {
        EMIForCreateStockConfig.register(modContainer);
    }
}
