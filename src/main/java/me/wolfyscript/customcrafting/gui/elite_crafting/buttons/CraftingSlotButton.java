package me.wolfyscript.customcrafting.gui.elite_crafting.buttons;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.data.TestCache;
import me.wolfyscript.customcrafting.data.cache.EliteWorkbench;
import me.wolfyscript.utilities.api.inventory.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.button.buttons.ItemInputButton;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CraftingSlotButton extends ItemInputButton {

    public CraftingSlotButton(int recipeSlot, CustomCrafting customCrafting) {
        super("crafting.slot_" + recipeSlot, new ButtonState("", Material.AIR, (guiHandler, player, inventory, slot, inventoryClickEvent) -> {
            TestCache cache = ((TestCache) guiHandler.getCustomCache());
            EliteWorkbench eliteWorkbench = cache.getEliteWorkbench();
            Bukkit.getScheduler().runTask(customCrafting, () -> {
                int gridSize = eliteWorkbench.getCurrentGridSize();
                int startSlot = (gridSize == 3 ? 2 : gridSize == 4 || gridSize == 5 ? 1 : 0);
                int itemSlot;
                for (int i = 0; i < gridSize * gridSize; i++) {
                    itemSlot = startSlot + i + (i / gridSize) * (9 - gridSize);
                    eliteWorkbench.getContents()[i] = inventory.getItem(itemSlot);
                }
                ItemStack result = customCrafting.getRecipeUtils().preCheckRecipe(eliteWorkbench.getContents(), player, false, inventory, true, eliteWorkbench != null && eliteWorkbench.getEliteWorkbenchData().isAdvancedRecipes());
                eliteWorkbench.setResult(result);
            });
            return false;
        }, (hashMap, guiHandler, player, itemStack, i, b) -> {
            TestCache cache = ((TestCache) guiHandler.getCustomCache());
            EliteWorkbench eliteWorkbench = cache.getEliteWorkbench();
            if (eliteWorkbench.getContents() != null) {
                ItemStack slotItem = eliteWorkbench.getContents()[recipeSlot];
                itemStack = slotItem == null ? new ItemStack(Material.AIR) : slotItem;
            }
            return itemStack;
        }));
    }
}
