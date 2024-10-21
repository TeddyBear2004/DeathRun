package de.thorge.deathrun.events;

import de.thorge.deathrun.DeathRunWorld;
import org.bukkit.damage.DamageEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class PlayerEvents implements Listener {
    private final List<DeathRunWorld> worlds;

    public PlayerEvents(List<DeathRunWorld> worlds) {
        this.worlds = worlds;
    }

    //disable nether and end portal
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
        }
    }

    //disable pvp
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    //cancel natural heal
    @EventHandler
    public void onPlayerRegen(EntityRegainHealthEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(BlockPlaceEvent event) {
        for (DeathRunWorld world : worlds) {
            if (world.isRunning() && world.getWorld().equals(event.getPlayer().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        for (DeathRunWorld world : worlds) {
            if (world.isRunning() && world.getWorld().equals(event.getPlayer().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(EntityDamageEvent event){
        for (DeathRunWorld world : worlds) {
            if (world.isRunning() && world.getWorld().equals(event.getEntity().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(PlayerDropItemEvent event){
        for (DeathRunWorld world : worlds) {
            if (world.isRunning() && world.getWorld().equals(event.getPlayer().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    //disable starvation
    @EventHandler
    public void on(EntityExhaustionEvent event) {
        for (DeathRunWorld world : worlds) {
            if (world.isRunning() && world.getWorld().equals(event.getEntity().getWorld())) {
                event.setCancelled(true);
            }
        }
    }


}
