package vn.bluetequila.blueitems;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;

/** Định dạng chat có huy hiệu rank (đọc từ chat.yml). Mặc định TẮT để không đụng plugin chat khác. */
public final class ChatFormatter implements Listener {

    private boolean enabled;
    private String format = "";
    private NamedTextColor messageColor;

    public void load(FileConfiguration cfg) {
        ConfigurationSection s = cfg.getConfigurationSection("chat");
        enabled = s != null && s.getBoolean("enabled", false);
        format = s == null ? "" : s.getString("format", "");
        String color = s == null ? null : s.getString("message_color");
        messageColor = color == null ? null : NamedTextColor.NAMES.value(color.toLowerCase(Locale.ROOT));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!enabled || format.isBlank()) {
            return;
        }
        Component head = Text.parse(format, event.getPlayer());
        NamedTextColor color = messageColor;
        event.renderer(ChatRenderer.viewerUnaware((source, name, message) ->
                head.append(color == null ? message : message.colorIfAbsent(color))));
    }
}
