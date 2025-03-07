/*
 *       ____ _  _ ____ ___ ____ _  _ ____ ____ ____ ____ ___ _ _  _ ____
 *       |    |  | [__   |  |  | |\/| |    |__/ |__| |___  |  | |\ | | __
 *       |___ |__| ___]  |  |__| |  | |___ |  \ |  | |     |  | | \| |__]
 *
 *       CustomCrafting Recipe creation and management tool for Minecraft
 *                      Copyright (C) 2021  WolfyScript
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.wolfyscript.customcrafting.gui.cauldron;

import com.wolfyscript.utilities.bukkit.TagResolverUtil;
import java.util.Map;
import java.util.Optional;
import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.data.CCCache;
import me.wolfyscript.customcrafting.data.cache.CacheCauldronWorkstation;
import me.wolfyscript.customcrafting.data.persistent.CauldronBlockData;
import me.wolfyscript.customcrafting.gui.CCWindow;
import me.wolfyscript.customcrafting.gui.main_gui.ClusterMain;
import me.wolfyscript.customcrafting.listeners.customevents.CauldronPreCookEvent;
import me.wolfyscript.customcrafting.recipes.CustomRecipeCauldron;
import me.wolfyscript.customcrafting.recipes.RecipeType;
import me.wolfyscript.lib.net.kyori.adventure.text.Component;
import me.wolfyscript.lib.net.kyori.adventure.text.minimessage.tag.Tag;
import me.wolfyscript.lib.net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import me.wolfyscript.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import me.wolfyscript.utilities.api.inventory.gui.GuiCluster;
import me.wolfyscript.utilities.api.inventory.gui.GuiHandler;
import me.wolfyscript.utilities.api.inventory.gui.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.gui.button.CallbackButtonRender;
import me.wolfyscript.utilities.api.nms.inventory.GUIInventory;
import me.wolfyscript.utilities.util.inventory.ItemUtils;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CauldronWorkstationMenu extends CCWindow {

    protected static final int INGREDIENT_AMOUNT = 6;
    protected static final String RESULT = "result_slot";

    private static final String INDICATOR_LAVA = "indicator.lava";
    private static final String INDICATOR_WATER = "indicator.water";

    protected CauldronWorkstationMenu(GuiCluster<CCCache> cluster, CustomCrafting customCrafting) {
        super(cluster, CauldronWorkstationCluster.CAULDRON_MAIN.getKey(), 54, customCrafting);
        setForceSyncUpdate(true);
    }

    @Override
    public void onInit() {
        for (int i = 0; i < INGREDIENT_AMOUNT; i++) {
            int recipeSlot = i;
            getButtonBuilder().itemInput("crafting.slot_" + i).state(state -> state.icon(Material.AIR)
                    .action((cache, guiHandler, player, guiInventory, i1, event) -> false)
                    .postAction((cache, guiHandler, player, guiInventory, itemStack, i1, event) -> {
                        CacheCauldronWorkstation cacheCauldron = cache.getCauldronWorkstation();
                        cacheCauldron.setPreCookEvent(null);
                        cacheCauldron.getInput().set(recipeSlot, itemStack);
                        cacheCauldron.getBlock().flatMap(block -> cacheCauldron.getBlockData().flatMap(CauldronBlockData::getCauldronStatus)).ifPresent(status -> {
                            for (CustomRecipeCauldron recipeCauldron : customCrafting.getRegistries().getRecipes().getAvailable(RecipeType.CAULDRON, player)) {
                                if (recipeCauldron.checkRecipe(cacheCauldron.getInput(), status)) {
                                    CauldronPreCookEvent preCookEvent = new CauldronPreCookEvent(customCrafting, recipeCauldron, player, status.getBlock());
                                    if (!preCookEvent.isCancelled()) {
                                        //Cache event results
                                        cacheCauldron.setPreCookEvent(preCookEvent);
                                        player.playNote(player.getLocation(), Instrument.BELL, Note.sharp(2, Note.Tone.F));
                                    }
                                    return;
                                }
                            }
                        });
                    }).render((cache, guiHandler, player, guiInventory, itemStack, i1) -> {
                        CacheCauldronWorkstation cauldronWorkstation = cache.getCauldronWorkstation();
                        ItemStack stack = cauldronWorkstation.getInput().get(recipeSlot);
                        if (!ItemUtils.isAirOrNull(stack)) {
                            return CallbackButtonRender.UpdateResult.of(stack);
                        }
                        return CallbackButtonRender.UpdateResult.of(new ItemStack(Material.AIR));
                    })).register();
        }
        for (int resultSlot = 0; resultSlot < 4; resultSlot++) {
            int finalResultSlot = resultSlot;
            getButtonBuilder().itemInput("result_" + resultSlot).state(state -> state.icon(Material.AIR)
                    .action((cache, guiHandler, player, inventory, slot, event) -> false)
                    .postAction((cache, guiHandler, player, inventory, itemStack, i, event) -> {
                        cache.getCauldronWorkstation().getBlockData().ifPresent(cauldronBlockData -> cauldronBlockData.getResult()[finalResultSlot] = itemStack);
                    })
                    .render((cache, guiHandler, player, inventory, itemStack, slot) -> {
                        ItemStack result = cache.getCauldronWorkstation().getBlockData().map(cauldronBlockData -> cauldronBlockData.getResult()[finalResultSlot]).orElse(ItemUtils.AIR);
                        return CallbackButtonRender.UpdateResult.of(result);
                    })).register();
        }
        getButtonBuilder().toggle("start")
                .enabledState(state -> state.subKey("enabled").icon(Material.LIME_CONCRETE).action((cache, guiHandler, player, guiInventory, i, event) -> {
                    CacheCauldronWorkstation cauldronWorkstation = cache.getCauldronWorkstation();
                    cauldronWorkstation.getBlockData().ifPresent(cauldronBlockData -> {
                        if (cauldronBlockData.isResultEmpty()) {
                            cauldronBlockData.initNewRecipe(cauldronWorkstation);
                            cauldronWorkstation.setPreCookEvent(null);
                            if (cauldronBlockData.getRecipe().isPresent()) {
                                cauldronBlockData.getCauldronStatus().ifPresent(status -> {
                                    if (status.hasWater()) {
                                        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.1f, 1.75f);
                                        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.1f, 2.4f);
                                        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.1f, 1.2f);
                                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.1f, 0.5f);
                                    } else if (status.hasLava()) {
                                        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1f, 1f);
                                    } else {
                                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.1f, 0.25f);
                                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.2f, 0.5f);
                                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.6f, 1f);
                                    }
                                });
                                guiHandler.close();
                            }
                        }
                    });
                    return true;
                }))
                .disabledState(state -> state.subKey("disabled").icon(Material.GRAY_CONCRETE).action((cache, guiHandler, player, guiInventory, i, event) -> true))
                .defaultState(false).stateFunction((cache, guiHandler, player, guiInventory, i) -> {
                    CacheCauldronWorkstation cauldronWorkstation = cache.getCauldronWorkstation();
                    return cauldronWorkstation.getBlockData().map(CauldronBlockData::isResultEmpty).orElse(true) && cauldronWorkstation.getPreCookEvent().isPresent();
                }).register();
        getButtonBuilder().dummy("start_disabled").state(state -> state.icon(Material.GRAY_CONCRETE)).register();
        getButtonBuilder().dummy("cauldron_icon").state(s -> s.icon(Material.CAULDRON)).register();
        getButtonBuilder().dummy("signal_fire").state(s -> s.icon(Material.HAY_BLOCK)).register();

        getButtonBuilder().dummy(INDICATOR_LAVA).state(s -> s.icon(Material.ORANGE_STAINED_GLASS_PANE)
                .render((cache, guiHandler, player, guiInventory, itemStack, i) -> CallbackButtonRender.UpdateResult.of(Placeholder.parsed("level", String.valueOf(cache.getCauldronWorkstation().getBlockData().map(data -> data.getCauldronStatus().map(CauldronBlockData.CauldronStatus::getLevel).orElse(0)).orElse(0)))))).register();
        getButtonBuilder().dummy(INDICATOR_WATER).state(s -> s.icon(Material.BLUE_STAINED_GLASS_PANE)
                .render((cache, guiHandler, player, guiInventory, itemStack, i) -> CallbackButtonRender.UpdateResult.of(Placeholder.parsed("level", String.valueOf(cache.getCauldronWorkstation().getBlockData().map(data -> data.getCauldronStatus().map(CauldronBlockData.CauldronStatus::getLevel).orElse(0)).orElse(0)))))).register();
    }

    @Override
    public Component onUpdateTitle(Player player, @Nullable GUIInventory<CCCache> inventory, GuiHandler<CCCache> guiHandler) {
        CacheCauldronWorkstation cacheCauldronWorkstation = guiHandler.getCustomCache().getCauldronWorkstation();
        Optional<CauldronBlockData> optionalCauldronBlockData = cacheCauldronWorkstation.getBlockData();
        String menu = "main_menu";
        if (optionalCauldronBlockData.isPresent() && !optionalCauldronBlockData.get().isResultEmpty()) {
            menu = "result_menu";
        }
        String title = customCrafting.getConfigHandler().getConfig().getString("workstation.cauldron.gui." + menu + ".title", "<translate:inventories.cauldron.cauldron.default_title>");
        final TagResolver papiResolver = TagResolverUtil.papi(player);
        final TagResolver langResolver = TagResolver.resolver("translate", (args, context) -> Tag.selfClosingInserting(getChat().translated(args.popOr("The <translate> tag requires exactly one argument! The path to the language entry!").value(), papiResolver)));
        TagResolver recipeTypeTitle = Placeholder.component("title", getChat().getMiniMessage().deserialize(title, papiResolver, langResolver));
        return wolfyUtilities.getLanguageAPI().getComponent("inventories." + getNamespacedKey().getNamespace() + "." + getNamespacedKey().getKey() + ".gui_name", recipeTypeTitle, TagResolverUtil.papi(player));
    }

    @Override
    public void onUpdateAsync(GuiUpdate<CCCache> update) {
        //Prevent super class from rendering
    }

    @Override
    public void onUpdateSync(GuiUpdate<CCCache> event) {
        if (customCrafting.getConfigHandler().getConfig().isGUIDrawBackground()) {
            for (int i = 0; i < getSize(); i++) {
                event.setButton(i, ClusterMain.GLASS_GRAY);
            }
        }

        CCCache cache = event.getGuiHandler().getCustomCache();
        CacheCauldronWorkstation cacheCauldronWorkstation = cache.getCauldronWorkstation();
        Optional<CauldronBlockData> optionalCauldronBlockData = cacheCauldronWorkstation.getBlockData();

        if (optionalCauldronBlockData.isPresent()) {
            CauldronBlockData blockData = optionalCauldronBlockData.get();
            if (!blockData.isResultEmpty()) {
                event.setButton(21, "result_0");
                event.setButton(22, "result_1");
                event.setButton(30, "result_2");
                event.setButton(31, "result_3");
                return;
            }

            event.setButton(10, "crafting.slot_" + 3);
            event.setButton(12, "crafting.slot_" + 4);
            event.setButton(14, "crafting.slot_" + 5);

            event.setButton(20, "crafting.slot_" + 2);
            event.setButton(22, "crafting.slot_" + 1);
            event.setButton(30, "crafting.slot_" + 0);

            //event.setButton(31, "crafting.slot_" + 0);
            cacheCauldronWorkstation.getBlock().ifPresent(block -> {
                blockData.getCauldronStatus().ifPresent(status -> {
                    if (status.hasCampfire()) {
                        event.setItem(38, new ItemStack(Material.CAMPFIRE));
                    } else if (status.hasSoulCampfire()) {
                        event.setItem(38, new ItemStack(Material.SOUL_CAMPFIRE));
                    }
                    if (status.isSignalFire()) {
                        event.setButton(40, "signal_fire");
                    }
                    event.setButton(39, "cauldron_icon");

                    String levelItem = block.getType().equals(Material.LAVA_CAULDRON) ? INDICATOR_LAVA : INDICATOR_WATER;
                    for (int i = 0; i < 3; i++) {
                        if (i < status.getLevel() || status.hasLava()) {
                            event.setButton(45 - i * 9, levelItem);
                        } else {
                            event.setButton(45 - i * 9, ClusterMain.GLASS_WHITE);
                        }
                    }
                });
            });
            event.setButton(34, "start");
        }

    }

    @Override
    public boolean onClose(GuiHandler<CCCache> guiHandler, GUIInventory<CCCache> guiInventory, InventoryView transaction) {
        Player player = guiHandler.getPlayer();
        World world = player.getWorld();
        CCCache cache = guiHandler.getCustomCache();

        //Reset cache
        CacheCauldronWorkstation cacheCauldronWorkstation = cache.getCauldronWorkstation();
        cacheCauldronWorkstation.setPreCookEvent(null);
        cacheCauldronWorkstation.setBlockData(null);
        cacheCauldronWorkstation.setBlock(null);
        for (ItemStack itemStack : cacheCauldronWorkstation.getInput()) {
            if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
                Map<Integer, ItemStack> items = player.getInventory().addItem(itemStack);
                items.values().forEach(itemStack1 -> world.dropItemNaturally(player.getLocation(), itemStack1));
            }
        }
        cacheCauldronWorkstation.resetInput();
        return false;
    }

}
