package de.thorge.deathrun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeathRunTimer {
    private final Path path;
    private final DeathRunWorld deathRunWorld;
    private final int maxtime;
    private int time;
    private boolean running;

    public DeathRunTimer(DeathRunWorld deathRunWorld,
                         int maxtime) {
        this.path = deathRunWorld.getWorld().getWorldFolder().toPath().resolve("timer.txt");
        this.deathRunWorld = deathRunWorld;
        this.maxtime = maxtime;
        this.time = this.load();
        this.running = false;
    }

    public void save() {
        try {
            Files.write(this.path, String.valueOf(this.time).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int load() {
        if (Files.exists(this.path)) {
            try {
                return Integer.parseInt(Files.readAllLines(this.path).get(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return maxtime;
        }
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean tick() {
        if (!this.running) {
            return false;
        }
        this.time--;
        print();

        if (this.time <= 0) {
            this.running = false;
            return true;
        }

        return false;
    }

    public void reset() {
        this.time = this.maxtime;
        this.running = false;
    }

    public void print() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld() == this.deathRunWorld.getWorld())
                onlinePlayer.sendActionBar(this.getTime());
        }
    }

    public Component getTime() {
        int hours = this.time / 72000;
        int minutes = (this.time % 72000) / 1200;
        int seconds = (this.time % 1200) / 20;

        return Component.text("Restzeit: ")
                .append(Component.text(String.format("%02d", hours)).color(NamedTextColor.GOLD))
                .append(Component.text(":").color(NamedTextColor.WHITE))
                .append(Component.text(String.format("%02d", minutes)).color(NamedTextColor.GOLD))
                .append(Component.text(":").color(NamedTextColor.WHITE))
                .append(Component.text(String.format("%02d", seconds)).color(NamedTextColor.GOLD));
    }
}
