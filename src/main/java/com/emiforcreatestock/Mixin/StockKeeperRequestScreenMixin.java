package com.emiforcreatestock.Mixin;

import com.emiforcreatestock.StockRequestHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.gui.ScreenWithStencils;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenMixin{
    @Invoker("sendIt")
    void emiforcreatestock$sendIt();

    @Mixin(StockKeeperRequestScreen.class)
    abstract class StockKeeperRequestScreenMixin_ extends AbstractSimiContainerScreen<StockKeeperRequestMenu> implements ScreenWithStencils {

        @Shadow public List<StockKeeperRequestScreen.CategoryEntry> categories;

        public StockKeeperRequestScreenMixin_(StockKeeperRequestMenu container, Inventory inv, Component title) {
            super(container, inv, title);
        }

        @Inject(method = "renderBg",at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z",ordinal = 3))
        public void renderCraftCategories(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci, @Local StockKeeperRequestScreen.CategoryEntry categoryEntry, @Local LocalRef<List<BigItemStack>> category){
            if(categoryEntry != null && StockRequestHandler.forAddress(((CategoryEntryMixin) categoryEntry).getName())) {
                category.set(new ArrayList<>());
            }
        }

        @ModifyExpressionValue(method = "refreshSearchResults",at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
        public boolean isAddressCategory(boolean original,@Local(name = "categoryIndex") int categoryIndex){
            return original || StockRequestHandler.forAddress(((CategoryEntryMixin)categories.get(categoryIndex)).getName());
        }
    }
}
