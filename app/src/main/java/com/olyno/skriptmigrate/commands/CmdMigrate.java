package com.olyno.skriptmigrate.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.olyno.skriptmigrate.types.MigrationFile;
import com.olyno.skriptmigrate.types.MigrationStep;
import com.olyno.skriptmigrate.types.MigrationVersion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class CmdMigrate implements CommandExecutor {

    private File migrationsDir;
    private ArrayList<MigrationFile> migrationFiles = new ArrayList<MigrationFile>();

    public CmdMigrate(File migrationsDir) {
        this.migrationsDir = migrationsDir;
        reload();
        System.out.println(ChatColor.GREEN + "[Skript Migration] Migration files loaded with success!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("skriptmigration.migrate")) {
            if (args.length > 0 && args[0].contains("reload")) {
                reload(sender);
                return true;
            }
            if (migrationFiles.size() == 0) {
                sender.sendMessage(ChatColor.YELLOW + "[Skript Migration] No migration file found, skipped migration...");
                return true;
            }
            for (MigrationFile migrationFile : migrationFiles) {
                String addonVersion = Bukkit.getServer().getPluginManager().getPlugin(migrationFile.getAddonName()).getDescription().getVersion();
                // System.out.println("Addon version: " + addonVersion);
                if (!migrationFile.hasVersion(addonVersion)) {
                    sender.sendMessage(ChatColor.YELLOW + "[Skript Migration] Migration for " + migrationFile.getAddonName() + " " + addonVersion + " does not exist, skipped...");
                    continue;
                }
                List<MigrationStep> migrationSteps = migrationFile.getStepsFromVersion(addonVersion);
                if (migrationSteps.size() == 0) {
                    sender.sendMessage(ChatColor.YELLOW + "[Skript Migration] Migration for " + migrationFile.getAddonName() + " " + addonVersion + " has empty steps, skipped...");
                    continue;
                }
                try (Stream<Path> walk = Files.walk(Paths.get("plugins/Skript/scripts"))) {
                    walk.filter(Files::isRegularFile)
                        .filter(scriptPath -> scriptPath.toString().endsWith(".sk"))
                        .forEach(scriptPath -> {
                            try {
                                List<String> scriptLines = Files.readAllLines(scriptPath);
                                List<String> newScriptLines = scriptLines
                                    .stream()
                                    .map(line -> {
                                        for (MigrationStep migrationStep : migrationSteps) {
                                            line = line.replaceAll(migrationStep.getFind(), migrationStep.getResult());
                                        }
                                        return line;
                                    })
                                    .collect(Collectors.toList());
                                Files.write(scriptPath, newScriptLines, StandardOpenOption.TRUNCATE_EXISTING);
                            } catch (IOException ex) {
                                sender.sendMessage(ChatColor.RED + "[Skript Migration] Something wrong happened for the migration of " + migrationFile.getAddonName()
                                    + (sender instanceof Player ? " (Look in your server console)" : ":\n"));
                                ex.printStackTrace();
                            }
                        });
                    sender.sendMessage(ChatColor.GREEN + "[Skript Migration] Migration for " + migrationFile.getAddonName() + " has been a success!");
                } catch (IOException ex) {
                    sender.sendMessage(ChatColor.RED + "[Skript Migration] Something wrong happened for the migration of " + migrationFile.getAddonName()
                        + (sender instanceof Player ? " (Look in your server console)" : ":\n"));
                    ex.printStackTrace();
                }
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sk reload all");
            sender.sendMessage(ChatColor.GREEN + "[Skript Migration] Migrated with success!");
            return true;
        }
        return false;
    }

    public void reload(CommandSender sender) {
        reload();
        sender.sendMessage(ChatColor.GREEN + "[Skript Migration] Migrations reloaded with success!");
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        migrationFiles.clear();
        for (File migrationFile : migrationsDir.listFiles()) {
            try {
                String fileContent = String.join("\n", Files.readAllLines(migrationFile.toPath()));
                Yaml yaml = new Yaml();
                LinkedHashMap<String, Object> loadedMigration = (LinkedHashMap<String, Object>) yaml.load(fileContent);
                MigrationFile migration = new MigrationFile();
                migration.setAddonName((String) loadedMigration.get("addon_name"));
                migration.setAuthor((String) loadedMigration.get("author"));
                LinkedHashMap<String, Object> migrationSteps = (LinkedHashMap<String, Object>) loadedMigration.get("steps");
                for (Object stepVersionObject : migrationSteps.keySet()) {
                    String stepVersion = stepVersionObject.toString();
                    MigrationVersion migrationVersion = new MigrationVersion(stepVersion);
                    for (LinkedHashMap<String, Object> step : (List<LinkedHashMap<String, Object>>) migrationSteps.get(stepVersionObject)) {
                        MigrationStep migrationStep = new MigrationStep();
                        migrationStep.setFind((String) step.get("find"));
                        migrationStep.setResult((String) step.get("result"));
                        migrationVersion.addStep(migrationStep);
                    }
                    migration.addVersion(migrationVersion);
                }
                migrationFiles.add(migration);
            } catch (YAMLException | IOException ex) {
                System.out.println(ChatColor.RED + "[Skript Migration] Something bad happened when loading this migration file: " + migrationFile.toPath().toString());
                ex.printStackTrace();
            }
        }
    }
    
}
