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

@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {
    @ForMoreParameter(usingClass = StockRequestHandler.class,reason = "OrElse EMI gives bad recipe amount, and causes request count wrong (Problems caused by inability to divide evenly)")
    @Inject(method = "craftInteraction",at = @At(value = "INVOKE", target = "Ldev/emi/emi/runtime/EmiFavorite$Synthetic;getRecipe()Ldev/emi/emi/api/recipe/EmiRecipe;"))
    private static void setRecipe(EmiIngredient ingredient, Supplier<EmiRecipe> contextSupplier, EmiStackInteraction stack, Function<EmiBind, Boolean> function, CallbackInfoReturnable<Boolean> cir, @Local EmiFavorite.Synthetic syn) {
        ForMoreParameters.playerNeedCount = syn.total;
    }

    @ForMoreParameter(usingClass = StockRequestHandler.class)
    @Inject(method = "craftInteraction",at = @At(value = "RETURN"))
    private static void clearData(EmiIngredient ingredient, Supplier<EmiRecipe> contextSupplier, EmiStackInteraction stack, Function<EmiBind, Boolean> function, CallbackInfoReturnable<Boolean> cir) {
        ForMoreParameters.playerNeedCount = ForMoreParameters.playerNeedCountDefault;
    }
}
