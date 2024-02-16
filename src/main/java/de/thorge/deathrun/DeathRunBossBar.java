package de.thorge.deathrun;

import de.thorge.deathrun.events.PlayerEvents;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DeathRunBossBar {
    private final DeathRunWorld deathRunWorld;
    private final Map<Player, BossBar> bossBars = new HashMap<>();
    private boolean running = false;

    public DeathRunBossBar(DeathRunWorld deathRunWorld) {
        this.deathRunWorld = deathRunWorld;
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
        deathRunWorld.getPlayers().forEach(player -> {
            BossBar bossBar = bossBars.get(player);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            bossBars.remove(player);
        });
    }

    public void tick() {
        if (!running)
            return;

        deathRunWorld.getPlayers().forEach(player -> {
            BossBar bossBar = bossBars.get(player);
            //zeige in der Bossbar einen Pfeil in welche Richtung der Spieler laufen muss

            String title;
            //between -180 and 180
            float yaw = player.getLocation().getYaw();

            String[] arrows = {"↑", "←", "↓", "→"};
            int index;
            if (yaw > -135 && yaw < -45) {
                index = 0;
            } else if (yaw > -45 && yaw < 45) {
                index = 1;
            } else if (yaw > 45 && yaw < 135) {
                index = 2;
            } else {
                index = 3;
            }
            title = arrows[index];

            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID);
                bossBar.addPlayer(player);
                bossBars.put(player, bossBar);
            } else {
                bossBar.setTitle(title);
            }
        });

    }
}
