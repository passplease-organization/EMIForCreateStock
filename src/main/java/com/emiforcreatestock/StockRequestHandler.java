package com.emiforcreatestock;

import com.emiforcreatestock.Mixin.StockKeeperRequestScreenMixin;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class StockRequestHandler implements StandardRecipeHandler<StockKeeperRequestMenu> {
    protected static final List<Slot> CRAFTING_SLOTS = List.of();

    @Override
    public List<Slot> getInputSources(StockKeeperRequestMenu menu) {
        return menu.slots;
    }

    @Override
    public List<Slot> getCraftingSlots(StockKeeperRequestMenu menu) {
        return CRAFTING_SLOTS;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return true;
    }

    // TODO 收藏合成表无效
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
        AbstractContainerScreen<StockKeeperRequestMenu> abstractContainerScreen = context.getScreen();
        if(abstractContainerScreen instanceof StockKeeperRequestScreen screen) {
            return enoughIngredients(recipe,screen, context.getAmount(),context.getInventory(),null);
        }
        return false;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
        AbstractContainerScreen<StockKeeperRequestMenu> abstractContainerScreen = context.getScreen();
        if(abstractContainerScreen instanceof StockKeeperRequestScreen screen){
            List<BigItemStack> stacks = new ArrayList<>();
            if(!enoughIngredients(recipe,screen, context.getAmount(), context.getInventory(),(ignore,amount,stack) -> {
                if(amount <= 0 || stack == null)
                    return;
                Optional<BigItemStack> optional = stacks.stream().filter(bigItemStack -> ItemStack.isSameItemSameComponents(bigItemStack.stack, stack.stack)).findFirst();
                if(optional.isPresent()){
                    optional.get().count += amount;
                }else stacks.add(new BigItemStack(stack.stack,Math.toIntExact(amount)));
            }))
                return false;
            moveItems(screen,stacks);
            return true;
        }
        return StandardRecipeHandler.super.craft(recipe, context);
    }

    protected boolean enoughIngredients(int requiredAmount,EmiPlayerInventory playerInventory,StockKeeperRequestScreen screen,EmiIngredient ingredient,@Nullable TriConsumer<EmiStack,Long,@Nullable BigItemStack> action,boolean findSome){
        if(ingredient.isEmpty())
            return true;
        for (EmiStack stack : ingredient.getEmiStacks()) {
            long amount = ingredient.getAmount() * requiredAmount;
            if (requiredAmount < Integer.MAX_VALUE && playerInventory.inventory.containsKey(stack)) {
                amount -= playerInventory.inventory.get(stack).getAmount();
                if (amount <= 0) {
                    if(action != null)
                        action.accept(stack, amount,null);
                    return true;
                }else if(findSome && action != null)
                    action.accept(stack, amount,null);
            }
            for(List<BigItemStack> items : screen.displayedItems){
                Optional<BigItemStack> optional = items.stream().filter(bigItemStack -> bigItemStack.stack.is(stack.getItemStack().getItem())).findFirst();
                if(optional.isPresent()){
                    BigItemStack bigItemStack = optional.get();
                    if(bigItemStack.count >= amount){
                        if(action != null)
                            action.accept(stack, amount,bigItemStack);
                        return true;
                    }else if(requiredAmount == Integer.MAX_VALUE){
                        if(action != null)// Max extract 9 * 64
                            action.accept(stack, Math.min(bigItemStack.stack.getMaxStackSize() * 9L,amount),bigItemStack);
                    }else {// No duplicate items in different List
                        if(findSome && action != null)
                            action.accept(stack, (long) bigItemStack.count,bigItemStack);
                        break;
                    }
                }
            }
        }
        return requiredAmount == Integer.MAX_VALUE;
    }

    protected boolean enoughIngredients(EmiRecipe recipe, StockKeeperRequestScreen screen, int craftTimes, EmiPlayerInventory playerInventory, @Nullable TriConsumer<EmiStack,Long,@Nullable BigItemStack> action){
        Map<EmiIngredient,Integer> foundIngredients = new HashMap<>();
        List<EmiStack> outputs = recipe.getOutputs();
        if(!outputs.isEmpty() && enoughIngredients(craftTimes,playerInventory,screen,outputs.getFirst(),(emiStack, amount, bigItemStack) -> {
            if(amount >= 0) {
                foundIngredients.put(outputs.getFirst(), Math.toIntExact(amount));
                if(action != null)
                    action.accept(emiStack, amount,bigItemStack);
            }
        },true))
            return true;
        for (EmiIngredient ingredient : recipe.getInputs()) {
            Optional<Map.Entry<EmiIngredient, Integer>> foundIngredient = foundIngredients.entrySet().stream()
                    .filter(entry -> EmiIngredient.areEqual(ingredient, entry.getKey()))
                    .findFirst();
            int count = Math.toIntExact(ingredient.getAmount() * craftTimes);
            boolean present = foundIngredient.isPresent();
            if(present) {
                count += foundIngredient.get().getValue();
            }
            if(!enoughIngredients(count,playerInventory,screen,ingredient,action,false))
                return false;
            foundIngredients.put(present ? foundIngredient.get().getKey() : ingredient,count);
        }
        return true;
    }

    protected void clearRecipe(@NotNull StockKeeperRequestScreen screen){
        screen.itemsToOrder.clear();
    }

    protected void sendRequest(@NotNull StockKeeperRequestScreen screen,@Nullable String address){
        if(!EMIForCreateStockConfig.sendIt())
            return;
        if(address != null)
            screen.addressBox.setValue(address);
        ((StockKeeperRequestScreenMixin)screen).emiforcreatestock$sendIt();
    }

    protected void moveItems(@NotNull StockKeeperRequestScreen screen,@NotNull List<BigItemStack> stacks){
        clearRecipe(screen);
        for(BigItemStack stack : stacks){
            if(screen.itemsToOrder.size() < 9)
                screen.itemsToOrder.add(stack);
            if(screen.itemsToOrder.size() >= 9)
                sendRequest(screen,null);
        }
        sendRequest(screen,null);
        if(!EMIForCreateStockConfig.sendIt()){
            screen.itemsToOrder.forEach(stack -> {
                for(List<BigItemStack> items : screen.displayedItems){
                    items.stream().filter(bigItemStack -> bigItemStack.stack.is(stack.stack.getItem())).findFirst()
                            .ifPresent(bigItemStack -> bigItemStack.count -= stack.count);
                }
            });
        }
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context, List<Widget> widgets, GuiGraphics draw) {
        // TODO Overwrite this method and draw myself
        if(context.getScreen() instanceof StockKeeperRequestScreen screen)
            StandardRecipeHandler.renderMissing(recipe,new PlayerInventoryAndStock(context.getInventory(),screen),widgets,draw);
        else StandardRecipeHandler.super.render(recipe,context,widgets,draw);
    }

    private class PlayerInventoryAndStock extends EmiPlayerInventory{
        public final @NotNull EmiPlayerInventory playerInventory;

        public final @NotNull StockKeeperRequestScreen screen;

        @Deprecated
        private PlayerInventoryAndStock(Player entity) throws IllegalAccessException {
            super(entity);
            throw new IllegalAccessException("Illegal access to PlayerInventoryAndStock constructor!");
        }

        public PlayerInventoryAndStock(@NotNull EmiPlayerInventory playerInventory,@NotNull StockKeeperRequestScreen screen) {
            super(List.of());
            this.inventory = playerInventory.inventory;
            this.playerInventory = playerInventory;
            this.screen = screen;
        }

        @Override
        public List<Boolean> getCraftAvailability(EmiRecipe recipe) {// Main function
            List<Boolean> list = new ArrayList<>();
            recipe.getInputs().forEach(ingredient -> list.add(StockRequestHandler.this.enoughIngredients(1,playerInventory,screen,ingredient,null,false)));
            return list;
        }

        @Override
        public Predicate<EmiRecipe> getPredicate() {
            return playerInventory.getPredicate();
        }

        @Override
        public List<EmiIngredient> getCraftables() {
            return playerInventory.getCraftables();
        }

        @Override
        public boolean canCraft(EmiRecipe recipe, long amount) {
            for(EmiIngredient ingredient : recipe.getInputs())
                if(!StockRequestHandler.this.enoughIngredients((int)amount,playerInventory,screen,ingredient,null,false))
                    return false;
            return true;
        }

        @Override
        public boolean isEqual(EmiPlayerInventory other) {
            if(other instanceof PlayerInventoryAndStock playerInventoryAndStock){
                return playerInventory.equals(playerInventoryAndStock.playerInventory) && screen.equals(playerInventoryAndStock.screen);
            }
            return false;
        }
    }
}
