package me.wolfyscript.customcrafting.recipes.blast_furnace;

import me.wolfyscript.customcrafting.configs.custom_configs.CustomConfig;
import me.wolfyscript.customcrafting.configs.custom_configs.blast_furnace.BlastingConfig;
import me.wolfyscript.customcrafting.configs.custom_configs.furnace.FurnaceConfig;
import me.wolfyscript.customcrafting.items.CustomItem;
import me.wolfyscript.customcrafting.recipes.CustomRecipe;
import me.wolfyscript.customcrafting.recipes.RecipePriority;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.RecipeChoice;

public class CustomBlastRecipe extends BlastingRecipe implements CustomRecipe {

    private RecipePriority recipePriority;
    private CustomItem result;
    private CustomItem source;
    private String id;
    private BlastingConfig config;

    public CustomBlastRecipe(BlastingConfig config) {
        super(new NamespacedKey(config.getFolder(), config.getName()), config.getResult(), new RecipeChoice.ExactChoice(config.getSource()), 0f, config.getCookingTime());
        this.id = config.getId();
        this.config = config;
        this.result = config.getResult();
        this.source = config.getSource();
        this.recipePriority = config.getPriority();
    }

    public CustomItem getSource() {
        return source;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public CustomItem getCustomResult() {
        return result;
    }

    @Override
    public RecipePriority getPriority() {
        return recipePriority;
    }

    @Override
    public void load() {

    }

    @Override
    public void save() {

    }

    @Override
    public CustomConfig getConfig() {
        return config;
    }
}
