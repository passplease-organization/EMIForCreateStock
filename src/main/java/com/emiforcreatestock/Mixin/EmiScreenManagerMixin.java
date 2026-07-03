package com.emiforcreatestock.Mixin;

import com.emiforcreatestock.ForMoreParameter;
import com.emiforcreatestock.ForMoreParameters;
import com.emiforcreatestock.StockRequestHandler;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;
import java.util.function.Supplier;

@Deprecated
//@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {
//    @ForMoreParameter(usingClass = StockRequestHandler.class,reason = "Watch if the craft method triggered by player")
//    @Inject(method = "craftInteraction",at = @At(value = "HEAD"))
//    private static void setRecipe(EmiIngredient ingredient, Supplier<EmiRecipe> contextSupplier, EmiStackInteraction stack, Function<EmiBind, Boolean> function, CallbackInfoReturnable<Boolean> cir) {
//        ForMoreParameters.playerClick = true;
//    }
//
//    @ForMoreParameter(usingClass = StockRequestHandler.class)
//    @Inject(method = "craftInteraction",at = @At(value = "RETURN"))
//    private static void clearData(EmiIngredient ingredient, Supplier<EmiRecipe> contextSupplier, EmiStackInteraction stack, Function<EmiBind, Boolean> function, CallbackInfoReturnable<Boolean> cir) {
//        ForMoreParameters.playerClick = false;
//    }
}
