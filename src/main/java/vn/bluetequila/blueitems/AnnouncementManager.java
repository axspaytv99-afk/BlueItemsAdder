package vn.bluetequila.blueitems;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/** Thông báo tự động xoay vòng trong chat (đọc từ announcements.yml). */
public final class AnnouncementManager {

    private final BlueItems plugin;
    private BukkitTask task;
    private int index;
    private String prefix = "";
    private List<String> messages = List.of();

    public AnnouncementManager(BlueItems plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration cfg) {
        stop();
        ConfigurationSection s = cfg.getConfigurationSection("announcements");
        if (s == null || !s.getBoolean("enabled", true)) {
            plugin.getLogger().info("Thông báo tự động: tắt.");
            return;
        }
        messages = s.getStringList("messages");
        if (messages.isEmpty()) {
            return;
        }
        prefix = s.getString("prefix", "");
        long period = Math.max(10L, s.getLong("interval_seconds", 300L)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcast, period, period);
        plugin.getLogger().info("Thông báo tự động: bật (" + messages.size() + " tin).");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcast() {
        String message = messages.get(index % messages.size());
        index++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(Text.parse(prefix + message, p));
        }
    }
}
