package com.emiforcreatestock.Mixin;

import com.emiforcreatestock.StockRequestHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockCheckingBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(StockTickerBlockEntity.class)
public abstract class StockTickerBlockEntityMixin extends StockCheckingBlockEntity implements IHaveHoveringInformation {
    @Shadow protected List<ItemStack> categories;

    @Shadow protected List<List<BigItemStack>> lastClientsideStockSnapshot;

    public StockTickerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "receiveStockPacket",at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",ordinal = 2))
    public void initCraftCategories(List<BigItemStack> stacks, boolean endOfTransmission, CallbackInfo ci){
        for (int index = 0; index < categories.size(); index++) {
            ItemStack filter = categories.get(index);
            if(!filter.isEmpty() && StockRequestHandler.forAddress(filter.getHoverName().getString())){
                List<BigItemStack> inCategory = new ArrayList<>();
                FilterItemStack filterItemStack = FilterItemStack.of(filter);
                Iterator<BigItemStack> iterator = new ArrayList<>(stacks).iterator();

                while(iterator.hasNext()) {
                    BigItemStack bigStack = iterator.next();
                    if (filterItemStack.test(this.level, bigStack.stack)) {
                        inCategory.add(bigStack);
                        iterator.remove();
                    }
                }
                lastClientsideStockSnapshot.set(index, inCategory);
            }
        }
    }

    @ModifyExpressionValue(method = "receiveStockPacket",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    public boolean test(boolean original, @Local ItemStack filter){
        return original || StockRequestHandler.forAddress(filter.getHoverName().getString());
    }
}
