package com.olyno.skriptmigrate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.olyno.skriptmigrate.commands.CmdMigrate;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;

public class SkriptMigrate extends JavaPlugin {

    public static SkriptMigrate instance;
	SkriptAddon addon;

	public void onEnable() {

        instance = this;
        addon = Skript.registerAddon(this);

        // Register Metrics
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () ->
            getConfig().getString("language", "en")));
        metrics.addCustomChart(new Metrics.SimplePie("skript_version", () ->
            Bukkit.getServer().getPluginManager().getPlugin("Skript").getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.SimplePie("Skord_version", () ->
            getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);
            if (javaVersion.startsWith("1.7")) {
                map.put("Java 1.7", entry);
            } else if (javaVersion.startsWith("1.8")) {
                map.put("Java 1.8", entry);
            } else if (javaVersion.startsWith("1.9")) {
                map.put("Java 1.9", entry);
            } else {
                map.put("Other", entry);
            }
            return map;
        }));

        File migrationDir = new File(getDataFolder(), "migrations");

        if (!migrationDir.exists()) {
            migrationDir.mkdirs();
        }

        getCommand("migrate").setExecutor(new CmdMigrate(migrationDir));

    }

    /**
     * Load the migration file of a plugin
     * @param plugin The plugin with a migration file.
     */
    public static void load(JavaPlugin plugin) {
        plugin.saveResource("migrations.yml", true);
        String pluginName = plugin.getName();
        Path migrationsFile = Paths.get("plugins/" + pluginName + "/migrations.yml");
        Path migrationsFileFinal = Paths.get("plugins/SkriptMigrate/migrations/" + pluginName.toLowerCase() + ".yml");
        try {
            Files.copy(migrationsFile, migrationsFileFinal);
        } catch (IOException ex) {
            if (ex instanceof FileAlreadyExistsException) {
                return;
            }
            ex.printStackTrace();
        }
    }

}
