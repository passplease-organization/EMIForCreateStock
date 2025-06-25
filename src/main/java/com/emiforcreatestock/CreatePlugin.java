package com.emiforcreatestock;

import com.simibubi.create.AllMenuTypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class CreatePlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(AllMenuTypes.STOCK_KEEPER_REQUEST.get(),new StockRequestHandler());
    }
}
