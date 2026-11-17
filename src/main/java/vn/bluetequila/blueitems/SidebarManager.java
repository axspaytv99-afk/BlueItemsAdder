package vn.bluetequila.blueitems;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bảng thông tin bên phải màn hình (sidebar), mỗi người một bảng riêng (đọc từ sidebar.yml). */
public final class SidebarManager implements Listener {

    private static final String OBJECTIVE = "bi_side";

    private final BlueItems plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Set<UUID> hidden = new HashSet<>();
    private BukkitTask task;
    private boolean enabled;
    private String title = "";
    private List<String> lines = List.of();

    public SidebarManager(BlueItems plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return enabled;
    }

    public void load(FileConfiguration cfg) {
        shutdown();
        ConfigurationSection s = cfg.getConfigurationSection("sidebar");
        enabled = s != null && s.getBoolean("enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Sidebar: tắt.");
            return;
        }
        title = s.getString("title", "");
        List<String> all = s.getStringList("lines");
        lines = new ArrayList<>(all.subList(0, Math.min(15, all.size())));
        long interval = Math.max(5, s.getInt("interval_ticks", 20));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hidden.contains(p.getUniqueId())) {
                create(p);
            }
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 1L, interval);
        plugin.getLogger().info("Sidebar: bật (" + lines.size() + " dòng).");
    }

    /** Dừng cập nhật và trả bảng điểm về mặc định cho mọi người. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (UUID id : boards.keySet()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.setScoreboard(manager.getMainScoreboard());
                }
            }
        }
        boards.clear();
    }

    /** Bật/tắt sidebar cho một người. Trả về true nếu giờ đang hiện. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (boards.containsKey(id)) {
            boards.remove(id);
            hidden.add(id);
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager != null) {
                player.setScoreboard(manager.getMainScoreboard());
            }
            return false;
        }
        hidden.remove(id);
        create(player);
        return true;
    }

    private static String entry(int index) {
        return "\u00a7" + Integer.toHexString(index) + "\u00a7r";
    }

    private void create(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        int n = lines.size();
        for (int i = 0; i < n; i++) {
            Team team = board.registerNewTeam("bi_l" + i);
            team.addEntry(entry(i));
            objective.getScore(entry(i)).setScore(n - i);
        }
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        update(player, board);
    }

    private void updateAll() {
        for (Map.Entry<UUID, Scoreboard> e : boards.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) {
                update(p, e.getValue());
            }
        }
    }

    private void update(Player player, Scoreboard board) {
        Objective objective = board.getObjective(OBJECTIVE);
        if (objective == null) {
            return;
        }
        objective.displayName(Text.parse(title, player));
        for (int i = 0; i < lines.size(); i++) {
            Team team = board.getTeam("bi_l" + i);
            if (team != null) {
                team.prefix(Text.parse(lines.get(i), player));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled && !hidden.contains(event.getPlayer().getUniqueId())) {
            create(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }
}
