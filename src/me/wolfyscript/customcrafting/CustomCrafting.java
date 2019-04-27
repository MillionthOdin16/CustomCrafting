package me.wolfyscript.customcrafting;

import me.wolfyscript.customcrafting.commands.CommandCC;
import me.wolfyscript.customcrafting.data.Workbenches;
import me.wolfyscript.customcrafting.events.*;
import me.wolfyscript.customcrafting.data.PlayerCache;
import me.wolfyscript.customcrafting.handlers.ConfigHandler;
import me.wolfyscript.customcrafting.handlers.InventoryHandler;
import me.wolfyscript.customcrafting.handlers.RecipeHandler;
import me.wolfyscript.customcrafting.metrics.Metrics;
import me.wolfyscript.customcrafting.placeholderapi.PlaceHolder;
import me.wolfyscript.utilities.api.WolfyUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CustomCrafting extends JavaPlugin {

    private static Plugin instance;
    private static List<PlayerCache> playerCacheList = new ArrayList<>();
    private static WolfyUtilities api;
    private static ConfigHandler configHandler;
    private static InventoryHandler invHandler;
    private static RecipeHandler recipeHandler;
    private static Workbenches workbenches = null;

    public void onEnable() {
        instance = this;
        api = new WolfyUtilities(instance);
        api.setCHAT_PREFIX("§7[§6CC§7] ");
        api.setCONSOLE_PREFIX("§7[§3CC§7] ");

        System.out.println("  _____        __             _____         _____  _          ");
        System.out.println(" / ___/_ _____/ /____  __ _  / ___/______ _/ _/ /_(_)__  ___ _");
        System.out.println("/ /__/ // (_-< __/ _ \\/  ' \\/ /__/ __/ _ `/ _/ __/ / _ \\/ _ `/");
        System.out.println("\\___/\\_,_/___|__/\\___/_/_/_/\\___/_/  \\_,_/_/ \\__/_/_//_/\\_, / ");
        System.out.println("                                                       /___/ v" + instance.getDescription().getVersion()+"-beta");
        System.out.println(" ");
        System.out.println("------------------------------------------------------------------------");

        if(Bukkit.getPluginManager().getPlugin("WolfyUtilities") == null){
            System.out.println("WolfyUtilities is not installed!");
            System.out.println("You can download it here: ");
            System.out.println("    https://www.spigotmc.org/resources/wolfyutilities.64124/");
            System.out.println("------------------------------------------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        File mainConfig = new File(getDataFolder(), "Main-Config.yml");
        if (mainConfig.exists()) {
            System.out.println("Found old CustomCrafting data! renaming folder...");
            if (getDataFolder().renameTo(new File(getDataFolder().getParentFile(), "CustomCrafting_old"))) {
                System.out.println("Renamed to CustomCrafting_old!");
                System.out.println("Creating new folder");
            }
        }

        configHandler = new ConfigHandler(api);
        invHandler = new InventoryHandler(api);
        recipeHandler = new RecipeHandler(api);
        configHandler.load();


        System.out.println("------------------------------------------------------------------------");

        loadPlayerCache();

        getServer().getPluginManager().registerEvents(new PlayerEvent(), this);
        getServer().getPluginManager().registerEvents(new CraftEvents(api), this);
        getServer().getPluginManager().registerEvents(new BlockEvents(), this);
        getServer().getPluginManager().registerEvents(new FurnaceEvents(), this);
        getServer().getPluginManager().registerEvents(new WorkbenchContents(), this);
        CommandCC commandCC = new CommandCC();
        if(configHandler.getConfig().isCCenabled()){
            getServer().getPluginCommand("cc").setExecutor(commandCC);
            getServer().getPluginCommand("cc").setTabCompleter(commandCC);
        }
        getServer().getPluginCommand("customcrafting").setExecutor(commandCC);
        getServer().getPluginCommand("customcrafting").setTabCompleter(commandCC);

        invHandler.init();

        workbenches = new Workbenches(api);

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            api.sendConsoleMessage("$msg.startup.placeholder$");
            new PlaceHolder().register();
        }
        recipeHandler.loadConfigs();
        Thread updater = new Thread(this::checkUpdate);
        updater.start();

        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> configHandler.getConfig().getString("language")));
        metrics.addCustomChart(new Metrics.SimplePie("server_software", () -> {
            if(WolfyUtilities.hasSpigot()){
                return "Spigot";
            }else{
                return Bukkit.getServer().getName();
            }
        }));
        System.out.println("------------------------------------------------------------------------");
    }

    public void onDisable() {
        workbenches.endTask();
        workbenches.save();
        getRecipeHandler().onSave();
        savePlayerCache();
    }

    public void checkUpdate(){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(
                    "https://api.spigotmc.org/legacy/update.php?resource=55883").openConnection();
            String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            if(!version.isEmpty() && !version.equals(instance.getDescription().getVersion())){
                api.sendConsoleWarning("$msg.startup.outdated$");
            }
        } catch (Exception ex) {
            api.sendConsoleWarning("$msg.startup.update_check_fail$");
        }
    }

    public static ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public static Plugin getInst() {
        return instance;
    }

    public static WolfyUtilities getApi() {
        return api;
    }

    public static RecipeHandler getRecipeHandler() {
        return recipeHandler;
    }

    public static Workbenches getWorkbenches() {
        return workbenches;
    }

    public static boolean hasPlayerCache(Player player) {
        for (PlayerCache playerCache : playerCacheList) {
            if (playerCache.getUuid().equals(player.getUniqueId()))
                return true;
        }
        return false;
    }

    public static void renewPlayerCache(Player player){
        if(hasPlayerCache(player)){
            PlayerCache playerCache = getPlayerCache(player);
            playerCacheList.remove(playerCache);
        }
        playerCacheList.add(new PlayerCache(player.getUniqueId()));
    }

    public static PlayerCache getPlayerCache(Player player) {
        return getPlayerCache(player.getUniqueId());
    }

    public static PlayerCache getPlayerCache(UUID uuid) {
        for (PlayerCache playerCache : playerCacheList) {
            if (playerCache.getUuid().equals(uuid))
                return playerCache;
        }
        PlayerCache playerCache = new PlayerCache(uuid);
        playerCacheList.add(playerCache);
        return playerCache;
    }

    public static void savePlayerCache(){
        HashMap<UUID, HashMap<String, Object>> caches = new HashMap<>();
        for(PlayerCache playerCache : playerCacheList){
            caches.put(playerCache.getUuid(), playerCache.getStats());
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(CustomCrafting.getInst().getDataFolder() + File.separator + "playerstats.dat"));
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(fos);
            oos.writeObject(caches);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPlayerCache(){
        api.sendConsoleMessage("$msg.startup.playerstats$");
        File file = new File(CustomCrafting.getInst().getDataFolder() + File.separator + "playerstats.dat");
        if (file.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                BukkitObjectInputStream ois = new BukkitObjectInputStream(fis);
                try {
                    Object object = ois.readObject();
                    if (object instanceof HashMap) {
                        HashMap<UUID, HashMap<String, Object>> stats = (HashMap<UUID, HashMap<String, Object>>) object;
                        for(UUID uuid : stats.keySet()){
                            playerCacheList.add(new PlayerCache(uuid, stats.get(uuid)));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
