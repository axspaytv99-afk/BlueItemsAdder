package vn.bluetequila.blueitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/** Bảng tab: header, footer và tên hiển thị của từng người (đọc từ tab.yml). */
public final class TabManager implements Listener {

    private final BlueItems plugin;
    private BukkitTask task;
    private boolean enabled;
    private List<String> header = List.of();
    private List<String> footer = List.of();
    private String nameFormat = "";

    public TabManager(BlueItems plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration cfg) {
        shutdown();
        ConfigurationSection s = cfg.getConfigurationSection("tab");
        enabled = s != null && s.getBoolean("enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Tab list: tắt.");
            return;
        }
        header = s.getStringList("header");
        footer = s.getStringList("footer");
        nameFormat = s.getString("name_format", "");
        long interval = Math.max(5, s.getInt("interval_ticks", 40));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1L, interval);
        plugin.getLogger().info("Tab list: bật (cập nhật mỗi " + interval + " tick).");
    }

    /** Dừng cập nhật và trả bảng tab về mặc định. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playerListName(null);
                p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    public void refresh(Player p) {
        if (!header.isEmpty() || !footer.isEmpty()) {
            p.sendPlayerListHeaderAndFooter(join(header, p), join(footer, p));
        }
        if (!nameFormat.isBlank()) {
            p.playerListName(Text.parse(nameFormat, p));
        }
    }

    private Component join(List<String> lines, Player p) {
        List<Component> parts = new ArrayList<>();
        for (String line : lines) {
            parts.add(Text.parse(line, p));
        }
        return Component.join(JoinConfiguration.newlines(), parts);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        }, 2L);
    }
}
