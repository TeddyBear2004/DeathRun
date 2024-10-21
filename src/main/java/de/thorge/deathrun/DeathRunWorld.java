package de.thorge.deathrun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeathRunWorld {
    private World world;
    private final DeathRunTimer timer;
    private final DeathRunBossBar bossBar;
    private final DeathRunScoreBoard scoreBoard;
    private final Map<Player, WorldBorder> worldBorders = new HashMap<>();
    private final Biome biome;
    private final String worldName;
    private final int borderSize;

    public DeathRunWorld(Plugin plugin, @Nullable Biome biome, String worldName, int maxtime, int borderSize) {
        this.biome = biome;
        this.worldName = worldName;
        this.borderSize = borderSize;
        //generate world
        generateWorld(biome, worldName);
        this.timer = new DeathRunTimer(this, maxtime);
        this.bossBar = new DeathRunBossBar(this);
        this.scoreBoard = new DeathRunScoreBoard(plugin, this);
    }

    private void generateWorld(@Nullable Biome biome, String worldName) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        if (biome != null) {
            BiomeProvider biomeProvider = new BiomeProvider() {
                @Override
                public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int i, int i1, int i2) {
                    return biome;
                }

                @Override
                public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
                    return List.of(biome);
                }
            };

            worldCreator.biomeProvider(biomeProvider);
        }
        this.world = worldCreator.createWorld();
    }

    public List<Player> getPlayers() {
        List<Player> players = this.world.getPlayers();
        players.removeIf(player -> player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE);
        return players;
    }

    public void load() {
        Location spawnLocation = this.world.getSpawnLocation().clone();
        spawnLocation.setYaw(-90);
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getEnderChest().clear();

            WorldBorder worldBorder = Bukkit.createWorldBorder();
            worldBorder.setSize(borderSize);
            worldBorder.setCenter(spawnLocation);

            player.setWorldBorder(worldBorder);

            worldBorders.put(player, worldBorder);
        });

    }

    private boolean running = false;

    public boolean isRunning() {
        return running;
    }

    public void start() {
        running = true;

        this.timer.start();
        this.bossBar.start();
        this.scoreBoard.start();
    }

    public void stop() {
        running = true;

        this.timer.stop();
        this.bossBar.stop();
        this.scoreBoard.stop();


    }

    public void tick() {
        this.bossBar.tick();
        this.scoreBoard.tick();

        if (this.timer.tick()) {
            this.stop();


            List<Map.Entry<String, Double>> sortedDistances = new ArrayList<>(scoreBoard.loadDistances().entrySet());
            sortedDistances.sort(Map.Entry.comparingByValue((o1, o2) -> (int) (o2 - o1)));

            Component message = Component.text("Top 10 Distanzen:");
            for (int i = 0; i < Math.min(10, sortedDistances.size()); i++) {
                Map.Entry<String, Double> entry = sortedDistances.get(i);
                TextColor color = i == 0 ? TextColor.color(0x00FF00) : TextColor.color(0xFFFFFF);

                message = message
                        .append(
                                Component.text("\n" + (i + 1) + ". " + entry.getKey() + ": " + entry.getValue().intValue() + "m").color(color));
            }

            Bukkit.broadcast(message);

        } else {
            if (!running)
                return;

            getPlayers().forEach(player -> {
                WorldBorder worldBorder = worldBorders.get(player);
                if (worldBorder == null)
                    return;
                Location location = world.getSpawnLocation().clone();

                if (player.getLocation().getX() > location.getX())
                    location.setX(player.getX());
                worldBorder.setCenter(location);
            });
        }
    }

    public World getWorld() {
        return world;
    }

    public DeathRunTimer getTimer() {
        return timer;
    }

    public void spectate(Player player) {
        player.teleport(world.getSpawnLocation());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public void reset() {
        this.bossBar.stop();
        this.scoreBoard.stop();

        File worldFolder = world.getWorldFolder();

        for (Player player : getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        Bukkit.unloadWorld(world, false);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        deleteWorld(worldFolder);

        generateWorld(biome, worldName);
        this.scoreBoard.reset();
        this.timer.reset();
    }

    private void deleteWorld(File worldFolder) {
        File[] files = worldFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteWorld(file);
                } else {
                    file.delete();
                }
            }
        }
        worldFolder.delete();
    }
}
