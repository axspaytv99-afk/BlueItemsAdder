package vn.bluetequila.blueitems;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.Permissible;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Thay :ten: bằng ký hiệu/glyph trong chat, tên item, menu và (cho admin) trong lệnh. */
public final class EmojiManager implements Listener {

    private record Emoji(String text, String font, String permission) {
        Component component() {
            Component c = Component.text(text);
            return font == null ? c : c.font(Key.key(font));
        }
    }

    private static final Pattern PATTERN = Pattern.compile(":([A-Za-z0-9_]+):");

    private final BlueItems plugin;
    private final Map<String, Emoji> emojis = new LinkedHashMap<>();
    private final TextReplacementConfig everyone;

    public EmojiManager(BlueItems plugin) {
        this.plugin = plugin;
        this.everyone = buildReplacer(null);
    }

    /** @param who người gõ (kiểm tra quyền từng emoji); null = cho phép tất cả */
    private TextReplacementConfig buildReplacer(Permissible who) {
        return TextReplacementConfig.builder()
                .match(PATTERN)
                .replacement((match, builder) -> {
                    Emoji e = emojis.get(match.group(1).toLowerCase(Locale.ROOT));
                    if (e == null) {
                        return builder; // không phải emoji -> giữ nguyên chữ
                    }
                    if (who != null && e.permission() != null && !who.hasPermission(e.permission())) {
                        return builder; // không có quyền -> giữ nguyên chữ
                    }
                    return e.component();
                })
                .build();
    }

    public void load(FileConfiguration cfg) {
        emojis.clear();
        ConfigurationSection root = cfg.getConfigurationSection("emojis");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            String text = null;
            String font = null;
            String permission = null;
            if (root.isConfigurationSection(key)) {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s != null) {
                    text = s.getString("text");
                    font = s.getString("font");
                    permission = s.getString("permission");
                }
            } else {
                text = root.getString(key);
            }
            if (text == null || text.isEmpty()) {
                continue;
            }
            emojis.put(key.toLowerCase(Locale.ROOT), new Emoji(text, font, permission));
        }
        plugin.getLogger().info("Đã tải " + emojis.size() + " emoji.");
    }

    /** Dùng cho tên item, menu... (không kiểm tra quyền). */
    public Component apply(Component component) {
        if (emojis.isEmpty()) {
            return component;
        }
        return component.replaceText(everyone);
    }

    /** Chuỗi thuần: thay :ten: bằng ký tự. Bỏ qua emoji dùng font riêng (không biểu diễn được bằng chuỗi). */
    public String expand(String input) {
        if (input.indexOf(':') < 0 || emojis.isEmpty()) {
            return input;
        }
        Matcher m = PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Emoji e = emojis.get(m.group(1).toLowerCase(Locale.ROOT));
            String replacement = (e != null && e.font() == null) ? e.text() : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Danh sách emoji -> glyph để hiển thị. */
    public Map<String, Component> all() {
        Map<String, Component> out = new LinkedHashMap<>();
        emojis.forEach((name, e) -> out.put(name, e.component()));
        return out;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (emojis.isEmpty() || !player.hasPermission("blueitems.emoji")) {
            return;
        }
        event.message(event.message().replaceText(buildReplacer(player)));
    }

    /** Admin gõ lệnh như: /lp group member meta setprefix 100 ":badge_member: " */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("blueitems.commands")) {
            event.setMessage(expand(event.getMessage()));
        }
    }

    /** Lệnh gõ trong console cũng được đổi. */
    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        event.setCommand(expand(event.getCommand()));
    }
}
