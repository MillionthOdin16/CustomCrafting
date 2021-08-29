package me.wolfyscript.customcrafting.gui.item_creator.tabs;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.data.CCCache;
import me.wolfyscript.customcrafting.data.cache.items.Items;
import me.wolfyscript.customcrafting.data.cache.items.ItemsButtonAction;
import me.wolfyscript.customcrafting.gui.item_creator.ButtonOption;
import me.wolfyscript.customcrafting.gui.item_creator.MenuItemCreator;
import me.wolfyscript.customcrafting.utils.NamespacedKeyUtils;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import me.wolfyscript.utilities.api.inventory.gui.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.gui.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ChatInputButton;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.DummyButton;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ItemInputButton;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ToggleButton;
import me.wolfyscript.utilities.util.NamespacedKey;
import me.wolfyscript.utilities.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TabConsume extends ItemCreatorTab {

    public static final String KEY = "consume";

    public TabConsume() {
        super(new NamespacedKey(NamespacedKeyUtils.NAMESPACE, KEY));
    }

    @Override
    public void register(MenuItemCreator creator, WolfyUtilities api) {
        creator.registerButton(new ButtonOption(Material.ITEM_FRAME, this));
        creator.registerButton(new ChatInputButton<>(KEY + ".durability_cost.enabled", Material.DROPPER, (hashMap, cache, guiHandler, player, inventory, itemStack, slot, help) -> {
            hashMap.put("%VAR%", guiHandler.getCustomCache().getItems().getItem().getDurabilityCost());
            return itemStack;
        }, (guiHandler, player, s, strings) -> {
            try {
                int value = Integer.parseInt(s);
                guiHandler.getCustomCache().getItems().getItem().setDurabilityCost(value);
                creator.sendMessage(player, "consume.valid", new Pair<>("%VALUE%", String.valueOf(value)));
            } catch (NumberFormatException e) {
                creator.sendMessage(player, "consume.invalid", new Pair<>("%VALUE%", s));
                return true;
            }
            return false;
        }));
        creator.registerButton(new DummyButton<>(KEY + ".durability_cost.disabled", Material.DROPPER));

        creator.registerButton(new ToggleButton<>(KEY + ".consume_item", (cache, guiHandler, player, guiInventory, i) -> cache.getItems().getItem().isConsumed(), new ButtonState<>("consume.consume_item.enabled", Material.GREEN_CONCRETE, (ItemsButtonAction) (cache, items, guiHandler, player, inventory, i, event) -> {
            items.getItem().setConsumed(false);
            return true;
        }), new ButtonState<>(KEY + ".consume_item.disabled", Material.RED_CONCRETE, (ItemsButtonAction) (cache, items, guiHandler, player, inventory, i, event) -> {
            items.getItem().setConsumed(true);
            return true;
        })));

        creator.registerButton(new DummyButton<>(KEY + ".replacement.enabled", Material.GREEN_CONCRETE));
        creator.registerButton(new DummyButton<>(KEY + ".replacement.disabled", Material.RED_CONCRETE));

        creator.registerButton(new ItemInputButton<>(KEY + ".replacement", Material.AIR, (ItemsButtonAction) (cache, items, guiHandler, player, inventory, slot, event) -> {
            Bukkit.getScheduler().runTask(CustomCrafting.inst(), () -> {
                ItemStack replacement = inventory.getItem(slot);
                if (replacement != null) {
                    items.getItem().setReplacement(CustomItem.getReferenceByItemStack(replacement).getApiReference());
                } else {
                    items.getItem().setReplacement(null);
                }
            });
            return false;
        }, (hashMap, cache, guiHandler, player, inventory, itemStack, i, b) -> guiHandler.getCustomCache().getItems().getItem().hasReplacement() ? CustomItem.with(cache.getItems().getItem().getReplacement()).create() : new ItemStack(Material.AIR)));
    }

    @Override
    public void render(GuiUpdate<CCCache> update, CCCache cache, Items items, CustomItem customItem, ItemStack item) {
        update.setButton(31, "consume.consume_item");
        update.setButton(38, "consume.replacement");
        update.setButton(39, items.getItem().hasReplacement() ? "consume.replacement.enabled" : "consume.replacement.disabled");
        if (customItem.hasReplacement() || item.getMaxStackSize() > 1) {
            update.setButton(41, "consume.durability_cost.disabled");
        } else {
            update.setButton(41, "consume.durability_cost.enabled");
        }
    }
}
