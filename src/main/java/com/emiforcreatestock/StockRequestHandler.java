package com.emiforcreatestock;

import com.emiforcreatestock.Mixin.CategoryEntryMixin;
import com.emiforcreatestock.Mixin.StockKeeperRequestScreenMixin;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import dev.emi.emi.EmiUtil;
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
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class StockRequestHandler implements StandardRecipeHandler<StockKeeperRequestMenu> {
    protected static final List<Slot> CRAFTING_SLOTS = List.of();
    protected static final int MAX_DEPTH = 10;
    protected static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

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

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
        AbstractContainerScreen<StockKeeperRequestMenu> abstractContainerScreen = context.getScreen();
        if(abstractContainerScreen instanceof StockKeeperRequestScreen screen) {
            return enoughIngredients(recipe, screen, context.getAmount(), context.getInventory(), null);
        }
        return false;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
        AbstractContainerScreen<StockKeeperRequestMenu> abstractContainerScreen = context.getScreen();
        if(abstractContainerScreen instanceof StockKeeperRequestScreen screen){
            List<BigItemStack> stacks = new ArrayList<>();
            if(EMIForCreateStockConfig.craftableRecipe() && recipe.getBackingRecipe() != null && recipe.getBackingRecipe().value() instanceof CraftingRecipe craftingRecipe){
                EmiStack o = recipe.getOutputs().getFirst();
                // Fix 1: compute count as long to avoid int overflow
                long count = o.getAmount() * context.getAmount();
                count -= searchSingleStack(count, context.getInventory(), screen, null, o);
                if(count > 0){
                    CraftableBigItemStack cbis = new CraftableBigItemStack(o.getItemStack(), craftingRecipe);
                    screen.recipesToOrder.add(cbis);
                    // requestCraftable expects int; cap safely
                    screen.requestCraftable(cbis, count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
                }
            }
            // enoughIngredients now uses long craftTimes
            if(!enoughIngredients(recipe, screen, context.getAmount(), context.getInventory(), (ignore, amount, stack) -> {
                if(amount <= 0 || stack == null)
                    return;
                Optional<BigItemStack> optional = stacks.stream().filter(bigItemStack -> ItemStack.isSameItemSameComponents(bigItemStack.stack, stack.stack)).findFirst();
                if(optional.isPresent()){
                    optional.get().count += Math.toIntExact(amount);
                } else stacks.add(new BigItemStack(stack.stack, Math.toIntExact(amount)));
            }))
                return false;
            moveItems(recipe, screen, stacks);
            return true;
        }
        return StandardRecipeHandler.super.craft(recipe, context);
    }

    /**
     * Searches for a specific stack in player inventory and the stock screen.
     * @param requiredAmount how many items are needed. Use Long.MAX_VALUE to take as many as possible.
     * @return the number of items found (capped at requiredAmount unless Long.MAX_VALUE).
     */
    protected long searchSingleStack(long requiredAmount, EmiPlayerInventory playerInventory, StockKeeperRequestScreen screen, @Nullable TriConsumer<EmiStack, Long, @Nullable BigItemStack> action, EmiStack stack) {
        long amount = 0;
        // Only search inventory for non‑max requests (since max requests skip early return)
        if (requiredAmount < Long.MAX_VALUE && playerInventory.inventory.containsKey(stack)) {
            amount = playerInventory.inventory.get(stack).getAmount();
            if(amount >= requiredAmount)
                return requiredAmount;
        }
        for(List<BigItemStack> items : getItemSource(screen)){
            Optional<BigItemStack> optional = items.stream().filter(bigItemStack -> ItemStack.isSameItemSameComponents(bigItemStack.stack, stack.getItemStack())).findFirst();
            if(optional.isPresent()){
                BigItemStack bigItemStack = optional.get();
                if(requiredAmount == Long.MAX_VALUE){
                    // Max extract 9 * 64
                    amount = Math.min(bigItemStack.stack.getMaxStackSize() * 9L, bigItemStack.count);
                    if(action != null)
                        action.accept(stack, amount, bigItemStack);
                } else {
                    if(bigItemStack.count >= requiredAmount - amount){
                        if(action != null)
                            action.accept(stack, requiredAmount - amount, bigItemStack);
                        amount = requiredAmount;
                    } else { // No duplicate items in different lists
                        if(action != null)
                            action.accept(stack, (long) bigItemStack.count, bigItemStack);
                        amount += bigItemStack.count;
                    }
                }
            }
        }
        return amount; // already long, no cast needed
    }

    public static @NotNull List<List<BigItemStack>> getItemSource(StockKeeperRequestScreen screen){
        return screen.currentItemSource == null ? new ArrayList<>() : screen.currentItemSource;
    }

    /**
     * Checks whether enough ingredients are available and optionally collects the items.
     * If search too many times, between each search, the stuff will be counted twice which means in the end there might have "imaginary stuff" and return bad response.
     * But this problem has little influence if you have such huge items, as the most time stock will to be. And when after request all ingredients, the stack tree will tell you there is wrong.
     * @param craftTimes number of crafts to perform
     */
    protected boolean enoughIngredients(EmiRecipe recipe, StockKeeperRequestScreen screen, long craftTimes, EmiPlayerInventory playerInventory, @Nullable TriConsumer<EmiStack, Long, @Nullable BigItemStack> action){
        List<EmiStack> outputs = recipe.getOutputs();
        if(!outputs.isEmpty()) {
            EmiStack o = outputs.getFirst();
            // Compute required output items as long
            long totalOutputNeeded = o.getAmount() * craftTimes;
            long foundOutput = searchSingleStack(totalOutputNeeded, playerInventory, screen, action, o);
            long satisfiedCrafts = foundOutput / o.getAmount();
            if(craftTimes <= satisfiedCrafts)
                return true;
            craftTimes -= satisfiedCrafts;
        }
        for (EmiIngredient ingredient : recipe.getInputs()) {
            if(ingredient.isEmpty())
                continue;
            long totalRequired = ingredient.getAmount() * craftTimes;
            // Cap at Integer.MAX_VALUE and treat larger values as "take everything"
            long requiredAmount = totalRequired > Integer.MAX_VALUE ? Long.MAX_VALUE : totalRequired;
            if(ingredient.getEmiStacks().stream().noneMatch(stack -> {
                if(requiredAmount == Long.MAX_VALUE || searchSingleStack(requiredAmount, playerInventory, screen, null, stack) >= requiredAmount){
                    searchSingleStack(requiredAmount, playerInventory, screen, action, stack);
                    return true;
                }
                return false;
            })) {
                if(!ForMoreParameters.playerClick || DEPTH.get() >= MAX_DEPTH)
                    return false;
                DEPTH.set(DEPTH.get() + 1);
                EmiRecipe newRecipe = EmiUtil.getPreferredRecipe(ingredient, playerInventory, true);
                if(newRecipe == null || newRecipe.getOutputs().size() >= 50) {
                    // newRecipe.getOutputs().size() >= 50 is for refuse bad recipes, those come from unknown given by EMI
                    DEPTH.set(DEPTH.get() - 1);
                    return false;
                }
                int outputCount = EmiUtil.getOutputCount(newRecipe,ingredient);
                boolean back = outputCount > 0 && enoughIngredients(newRecipe, screen,requiredAmount / outputCount + Math.min(requiredAmount % outputCount,1),playerInventory, action);
                DEPTH.set(DEPTH.get() - 1);
                if(!back)
                    return false;
            }
        }
        return true;
    }

    protected void clearRecipe(@NotNull StockKeeperRequestScreen screen){
        screen.itemsToOrder.clear();
    }

    protected void sendRequest(@NotNull StockKeeperRequestScreen screen, @Nullable String address){
        String oldAddress = screen.addressBox.getValue();
        if(address != null)
            screen.addressBox.setValue(address);
        if(!EMIForCreateStockConfig.sendIt())
            return;
        ((StockKeeperRequestScreenMixin)screen).emiforcreatestock$sendIt();
        screen.addressBox.setValue(oldAddress);
    }

    protected void moveItems(EmiRecipe recipe, @NotNull StockKeeperRequestScreen screen, @NotNull List<BigItemStack> stacks){
        clearRecipe(screen);
        stacks.stream()
                .filter(stack -> recipe.getOutputs().stream().anyMatch(
                        output -> ItemStack.isSameItem(output.getItemStack(), stack.stack))
                )
                .forEach(
                        stack -> screen.itemsToOrder.add(stack)
                );
        if(!screen.itemsToOrder.isEmpty()) {
            stacks.removeAll(screen.itemsToOrder);
            sendRequest(screen, null);
        }
        for(BigItemStack stack : stacks){
            if(screen.itemsToOrder.size() < 9)
                screen.itemsToOrder.add(stack);
            if(screen.itemsToOrder.size() >= 9)
                sendRequest(screen, getAddress(recipe, screen));
        }
        sendRequest(screen, getAddress(recipe, screen));
        if(!EMIForCreateStockConfig.sendIt()){
            List<List<BigItemStack>> itemSource = getItemSource(screen);
            screen.itemsToOrder.forEach(stack -> {
                for(List<BigItemStack> items : itemSource){
                    items.stream().filter(bigItemStack -> bigItemStack.stack.is(stack.stack.getItem())).findFirst()
                            .ifPresent(bigItemStack -> bigItemStack.count -= stack.count);
                }
            });
        }
    }

    public @Nullable String getAddress(EmiRecipe recipe, StockKeeperRequestScreen screen){
        for(EmiStack output : recipe.getOutputs()){
            for (int ordinary = 0; ordinary < screen.categories.size(); ordinary++) {
                if(getItemSource(screen).get(ordinary).stream().anyMatch(stack -> {
                    if(stack.stack.getItem() instanceof FilterItem){
                        FilterItemStack filter = FilterItemStack.of(stack.stack);
                        return filter.test(screen.getMinecraft().level, output.getItemStack());
                    }
                    return false;
                })) {
                    return getAvailableAddress(((CategoryEntryMixin) screen.categories.get(ordinary)).getName());
                }
            }
        }
        return null;
    }

    public static @Nullable String getAvailableAddress(String categoryName){
        if(forAddress(categoryName))
            return categoryName.substring(1);
        else return null;
    }

    public static boolean forAddress(String categoryName){
        return categoryName.startsWith("#");
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context, List<Widget> widgets, GuiGraphics draw) {
        if(context.getScreen() instanceof StockKeeperRequestScreen screen)
            StandardRecipeHandler.renderMissing(recipe, new PlayerInventoryAndStock(context.getInventory(), screen), widgets, draw);
        else StandardRecipeHandler.super.render(recipe, context, widgets, draw);
    }

    private class PlayerInventoryAndStock extends EmiPlayerInventory {
        public final @NotNull EmiPlayerInventory playerInventory;
        public final @NotNull StockKeeperRequestScreen screen;

        @Deprecated
        private PlayerInventoryAndStock(Player entity) throws IllegalAccessException {
            super(entity);
            throw new IllegalAccessException("Illegal access to PlayerInventoryAndStock constructor!");
        }

        public PlayerInventoryAndStock(@NotNull EmiPlayerInventory playerInventory, @NotNull StockKeeperRequestScreen screen) {
            super(List.of());
            this.inventory = playerInventory.inventory;
            this.playerInventory = playerInventory;
            this.screen = screen;
        }

        @Override
        public List<Boolean> getCraftAvailability(EmiRecipe recipe) {
            Map<EmiStack, Integer> stacks = new HashMap<>();
            return recipe.getInputs().stream().map(emiIngredient -> {
                return emiIngredient.getEmiStacks().stream().anyMatch(stack -> searchSingleStack(stacks.getOrDefault(stack, 1), playerInventory, screen,
                        (emiStack, amount, bigItemStack) -> {
                            if(amount <= 0 || stack == null)
                                return;
                            stacks.compute(emiStack, (k, v) -> v == null ? 1 : v + 1);
                        }, stack) >= 1);
            }).toList();
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
            // Fix: use Long map to avoid int overflow
            Map<EmiStack, Long> stacks = new HashMap<>();
            return recipe.getInputs().stream().allMatch(emiIngredient -> {
                return emiIngredient.getEmiStacks().stream().anyMatch(stack -> searchSingleStack(stacks.getOrDefault(stack, amount), playerInventory, screen,
                        (emiStack, aLong, bigItemStack) -> {
                            if (aLong <= 0 || stack == null)
                                return;
                            stacks.compute(emiStack, (k, v) -> v == null ? amount : v + amount);
                        }, stack) >= 1);
            });
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