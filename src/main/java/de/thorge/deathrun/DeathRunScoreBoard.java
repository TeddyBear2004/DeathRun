package de.thorge.deathrun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * This class is responsible for the Sidebar of the DeathRun game.
 * It shows the distance traveled by the top 10 player on the x-axis.
 */
public class DeathRunScoreBoard {
    private final Plugin plugin;
    private final DeathRunWorld deathRunWorld;
    private PersistentDataContainer dataContainer;
    private final Map<Player, Scoreboard> playerScoreboards = new HashMap<>();
    private boolean running = false;

    public DeathRunScoreBoard(Plugin plugin, DeathRunWorld deathRunWorld) {
        this.plugin = plugin;
        this.deathRunWorld = deathRunWorld;
        this.dataContainer = deathRunWorld.getWorld().getPersistentDataContainer();
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
        for (Player player : deathRunWorld.getPlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void reset(){
        playerScoreboards.clear();

        this.dataContainer = deathRunWorld.getWorld().getPersistentDataContainer();
    }

    public void tick() {

        if (!running) {
            return;
        }

        syncDistances();

        for (Player player : deathRunWorld.getPlayers()) {
            updateSidebar(player);
        }
    }

    private Scoreboard createScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.computeIfAbsent(player,
                k -> Bukkit.getScoreboardManager().getNewScoreboard());

        if (!scoreboard.equals(player.getScoreboard())) {
            player.setScoreboard(scoreboard);
        }
        return scoreboard;
    }

    private Objective getOrCreateDistanceObjective(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective("distance");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("distance", "dummy", ChatColor.GREEN + "Distance");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        return objective;
    }

    private List<Map.Entry<String, Double>> sortDistances(Map<String, Double> uuidDoubleMap) {
        List<Map.Entry<String, Double>> sortedDistances = new ArrayList<>(uuidDoubleMap.entrySet());
        sortedDistances.sort(Map.Entry.comparingByValue((o1, o2) -> (int) (o2 - o1)));
        return sortedDistances;
    }

    private void registerTeamsAndSetScores(Scoreboard scoreboard, Objective objective, List<Map.Entry<String, Double>> sortedDistances) {
        int count = 0;
        for (Map.Entry<String, Double> entry : sortedDistances) {
            String playerName = entry.getKey();
            double playerScore = entry.getValue();

            if (playerName == null)
                continue;
            Team team = scoreboard.getTeam(playerName);
            if (team == null)
                team = scoreboard.registerNewTeam(playerName);

            team.addEntry(playerName);

            Score playerScoreObj = objective.getScore(playerName);
            playerScoreObj.setScore((int) playerScore);

            setTeamPrefixAndSuffix(team, playerName, count);
            count++;
        }
    }

    private void setTeamPrefixAndSuffix(Team team, String playerName, int count) {
        Component rank;
        if (count == 0) {
            rank = Component.text("🥇 ").color(TextColor.color(0xFFD700));
        } else if (count == 1) {
            rank = Component.text("🥈 ").color(TextColor.color(0xC0C0C0));
        } else if (count == 2) {
            rank = Component.text("🥉 ").color(TextColor.color(0xCD7F32));
        } else {
            rank = Component.text((count + 1) + ". ");
        }
        team.prefix(rank);

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            team.suffix(
                    Component.text(" (" + (int) player.getHealth())
                            .append(Component.text("❤").color(TextColor.color(0xFF0000)))
                            .append(Component.text(")")));
        }
        team.suffix();
    }

    private void addOwnPlayerToSidebar(Player player, Team team, int ownPlayerRank) {
        double playerDistance = loadDistances().get(player.getName());

        // Set the own player's score in the Sidebar
        Score ownPlayerScore = team.getScoreboard().getObjective("distance").getScore(player.getName());
        ownPlayerScore.setScore((int) playerDistance);

        // Set the own player's rank prefix and health suffix
        Component rank;
        if (ownPlayerRank == 0) {
            rank = Component.text("🥇 ").color(TextColor.color(0xFFD700));
        } else if (ownPlayerRank == 1) {
            rank = Component.text("🥈 ").color(TextColor.color(0xC0C0C0));
        } else if (ownPlayerRank == 2) {
            rank = Component.text("🥉 ").color(TextColor.color(0xCD7F32));
        } else {
            rank = Component.text((ownPlayerRank + 1) + ". ");
        }
        team.prefix(rank);

        team.suffix(
                Component.text(" (" + (int) player.getHealth())
                        .append(Component.text("❤").color(TextColor.color(0xFF0000)))
                        .append(Component.text(")"))
        );
    }

    public void updateSidebar(Player player) {
        Scoreboard scoreboard = createScoreboard(player);

        Objective objective = getOrCreateDistanceObjective(scoreboard);

        Map<String, Double> uuidDoubleMap = loadDistances();
        List<Map.Entry<String, Double>> sortedDistances = sortDistances(uuidDoubleMap);

        registerTeamsAndSetScores(scoreboard, objective, sortedDistances);

        // Add own player to the sidebar
        Team ownPlayerTeam = scoreboard.getTeam(player.getName());
        if (ownPlayerTeam != null) {
            int ownPlayerRank = sortedDistances.indexOf(new AbstractMap.SimpleEntry<>(player.getName(), uuidDoubleMap.get(player.getName())));
            addOwnPlayerToSidebar(player, ownPlayerTeam, ownPlayerRank);
        }
    }


    public void syncDistances() {
        for (Player onlinePlayer : deathRunWorld.getPlayers()) {
            dataContainer.set(new NamespacedKey(plugin, "name." + onlinePlayer.getUniqueId()), PersistentDataType.STRING, onlinePlayer.getName());

            double distance = onlinePlayer.getLocation().distance(deathRunWorld.getWorld().getSpawnLocation());
            NamespacedKey namespacedKey = new NamespacedKey(plugin, "distance." + onlinePlayer.getUniqueId());

            //just set if bigger than the current value
            if (dataContainer.has(namespacedKey, PersistentDataType.DOUBLE)) {
                Double currentDistanceObj = dataContainer.get(namespacedKey, PersistentDataType.DOUBLE);
                if (currentDistanceObj != null) {
                    double currentDistance = currentDistanceObj;
                    if (distance > currentDistance) {
                        dataContainer.set(namespacedKey, PersistentDataType.DOUBLE, distance);
                    }
                }
            } else {
                //if not set yet
                dataContainer.set(namespacedKey, PersistentDataType.DOUBLE, distance);
            }
        }
    }

    public Map<String, Double> loadDistances() {
        Map<String, Double> distances = new HashMap<>();
        Set<NamespacedKey> keys = dataContainer.getKeys();

        keys.forEach(key -> {
            if (key.getKey().startsWith("distance.") && dataContainer.has(key, PersistentDataType.DOUBLE)) {
                Double distance = dataContainer.get(key, PersistentDataType.DOUBLE);
                UUID uuid = UUID.fromString(key.getKey().replace("distance.", ""));
                String name = dataContainer.get(new NamespacedKey(plugin, "name." + uuid), PersistentDataType.STRING);
                if (name == null)
                    name = Bukkit.getOfflinePlayer(uuid).getName();
                distances.put(name, distance);
            }
        });

        return distances;
    }
}
