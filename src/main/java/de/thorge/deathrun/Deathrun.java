package de.thorge.deathrun;

import de.thorge.deathrun.events.PlayerEvents;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Deathrun extends JavaPlugin {
    private List<DeathRunWorld> deathRunWorlds;

    public DeathRunWorld loadDeathRunWorld(ConfigurationSection configurationSection, int maxtime) {
        String name = configurationSection.getString("name");
        String biome = configurationSection.getString("biome");
        Biome biome1 = biome != null && !biome.isBlank() ? Biome.valueOf(biome) : null;

        return new DeathRunWorld(this, biome1, name, maxtime, configurationSection.getInt("width"));
    }

    private List<BukkitTask> tasks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        ConfigurationSection worlds = getConfig().getConfigurationSection("worlds");
        if (worlds == null) {
            getLogger().warning("No worlds configured");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        int maxtime = getConfig().getInt("maxtime") * 20 * 60;

        deathRunWorlds = worlds.getKeys(false).stream()
                .map(key -> getConfig().getConfigurationSection("worlds." + key))
                .filter(Objects::nonNull)
                .map((ConfigurationSection configurationSection) -> loadDeathRunWorld(configurationSection, maxtime))
                .toList();

        getCommand("load").setExecutor((commandSender, command, s, strings) -> {
            String name;
            if (strings.length == 0) {
                name = null;
            } else {
                name = strings[0];
            }

            for (DeathRunWorld deathRunWorld : deathRunWorlds) {
                if (name == null || deathRunWorld.getWorld().getName().equals(name)) {
                    deathRunWorld.load();
                    break;
                }
            }

            return true;
        });

        getCommand("start").setExecutor((commandSender, command, s, strings) -> {
            for (DeathRunWorld deathRunWorld : deathRunWorlds)
                deathRunWorld.start();

            return true;
        });

        getCommand("stop").setExecutor((commandSender, command, s, strings) -> {
            for (DeathRunWorld deathRunWorld : deathRunWorlds)
                deathRunWorld.stop();

            return true;
        });

        getCommand("spectate").setExecutor((commandSender, command, s, strings) -> {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                for (DeathRunWorld deathRunWorld : deathRunWorlds) {
                    if (deathRunWorld.getPlayers().contains(player)) {
                        deathRunWorld.spectate(player);
                        break;
                    }
                }
            }
            return true;
        });

        getCommand("reset").setExecutor((commandSender, command, s, strings) -> {
            for (DeathRunWorld deathRunWorld : deathRunWorlds)
                deathRunWorld.reset();

            return true;
        });

        tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (DeathRunWorld deathRunWorld : deathRunWorlds) {
                deathRunWorld.tick();
            }
        }, 1, 1));
    }

    @Override
    public void onDisable() {
        deathRunWorlds.forEach(deathRunWorld -> {
            deathRunWorld.stop();
            deathRunWorld.getTimer().save();
        });
        tasks.forEach(BukkitTask::cancel);
    }
}
