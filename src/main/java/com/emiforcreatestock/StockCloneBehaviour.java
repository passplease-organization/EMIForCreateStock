package com.emiforcreatestock;

import com.emiforcreatestock.Mixin.StockTickerBlockEntityMixin;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StockCloneBehaviour extends BlockEntityBehaviour implements ClipboardCloneable {
    public static final String KEY = "StockCloneBehaviour";

    public static final BehaviourType<StockCloneBehaviour> TYPE = new BehaviourType<>();

    private static final Map<FilterItem,Integer> FILTERS = new HashMap<>();

    static {
        List<Item> list = BuiltInRegistries.ITEM.stream().filter(FilterItem.class::isInstance).toList();
        list.forEach(item -> FILTERS.put((FilterItem) item,0));
    }

    public StockCloneBehaviour(SmartBlockEntity be) {
        super(be);
    }

    @Override
    public String getClipboardKey() {
        return KEY;
    }

    @Override
    public boolean writeToClipboard(HolderLookup.@NotNull Provider provider, CompoundTag tag, Direction direction) {
        CompoundTag nbt = new CompoundTag();
        getBlockEntity().saveAdditional(nbt,provider);
        tag.put(KEY, Objects.requireNonNull(nbt));
        return true;
    }

    @Override
    public boolean readFromClipboard(HolderLookup.@NotNull Provider provider, CompoundTag tag, Player player, Direction direction, boolean simulate) {
        if(legalBlockEntity() && tag.contains(KEY)) {
            List<ItemStack> stacks = NBTHelper.readItemList((ListTag) tag.getCompound(KEY).get("Categories"), provider);
            List<Item> items = stacks.stream().map(ItemStack::getItem).toList();
            Map<FilterItem, Integer> needFilters = getFilters();
            Inventory inventory = player.getInventory();
            needFilters.forEach((filterItem, integer) -> needFilters.replace(filterItem, Math.toIntExact(items.stream().filter(filterItem::equals).count())));
            if (checkEnoughFilterItem(player, inventory, needFilters)) {
                if(!simulate) {
                    getBlockEntity().loadCustomOnly(tag.getCompound(KEY), provider);
                    getBlockEntity().notifyUpdate();
                    removeFilterItem(player,inventory, needFilters);
                }
                return true;
            }
        }
        return false;
    }

    protected boolean legalBlockEntity(){
        StockTickerBlockEntityMixin entity = (StockTickerBlockEntityMixin) getBlockEntity();
        return entity.getActiveLinks() == 0 && entity.getLastClientsideStockSnapshot() == null && entity.getCategories().isEmpty();// newly placed
    }

    protected boolean checkEnoughFilterItem(Player player,Inventory inventory,Map<FilterItem, Integer> needFilters) {
        if(player.isCreative())
            return true;
        Map<FilterItem, Integer> existFilters = getFilters();
        existFilters.forEach((filterItem, integer) -> existFilters.replace(filterItem, inventory.countItem(filterItem)));
        boolean result = true;
        List<Integer> needCount = new ArrayList<>(needFilters.values());
        List<Integer> existCount = new ArrayList<>(existFilters.values());
        for (int index = 0; index < needCount.size(); index++) {
            result &= existCount.get(index) >= needCount.get(index);
        }
        return result;
    }

    protected void removeFilterItem(Player player,Inventory inventory, Map<FilterItem, Integer> items) {
        if(player.isCreative())
            return;
        items.forEach((filterItem, integer) -> {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if(stack.getItem() == filterItem) {
                    if (stack.getCount() >= integer) {
                        stack.shrink(integer);
                        return;
                    } else {
                        integer -= stack.getCount();
                        stack.setCount(0);
                    }
                }
            }
        });
        inventory.setChanged();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public StockTickerBlockEntity getBlockEntity() {
        return (StockTickerBlockEntity) blockEntity;
    }

    public static Map<FilterItem,Integer> getFilters() {
        return new HashMap<>(FILTERS);
    }
}
