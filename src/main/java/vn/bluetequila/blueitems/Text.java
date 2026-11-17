package vn.bluetequila.blueitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Xử lý chữ: MiniMessage, mã màu &, placeholder, emoji. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern LEGACY = Pattern.compile("[&\u00a7]([0-9a-fA-Fk-oK-OrR])");
    private static Method papiMethod;

    private Text() {
    }

    /** Chuỗi cấu hình -> Component (có placeholder, mã màu, MiniMessage, emoji). */
    public static Component parse(String raw, OfflinePlayer player) {
        String text = legacyToMini(placeholders(player, raw));
        return BlueItems.get().emojis().apply(MM.deserialize(text));
    }

    /** Giống parse nhưng tắt chữ nghiêng mặc định (dùng cho tên/lore item). */
    public static Component item(String raw, OfflinePlayer player) {
        return parse(raw, player).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Thoát ký tự đặc biệt của MiniMessage cho văn bản do người dùng nhập. */
    public static String esc(String s) {
        return MM.escapeTags(s);
    }

    public static String placeholders(OfflinePlayer player, String text) {
        if (text == null) {
            return "";
        }
        // placeholder có sẵn, không cần PlaceholderAPI
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()));
        if (player == null) {
            return text;
        }
        String name = player.getName();
        text = text.replace("%player%", name == null ? "" : name);
        if (player instanceof Player online) {
            text = text.replace("%ping%", String.valueOf(online.getPing()));
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }
        try {
            if (papiMethod == null) {
                papiMethod = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                        .getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            }
            return (String) papiMethod.invoke(null, player, text);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return text;
        }
    }

    private static String legacyToMini(String in) {
        if (in.indexOf('&') < 0 && in.indexOf('\u00a7') < 0) {
            return in;
        }
        Matcher m = LEGACY.matcher(in);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            char code = Character.toLowerCase(m.group(1).charAt(0));
            m.appendReplacement(sb, Matcher.quoteReplacement("<" + legacyTag(code) + ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String legacyTag(char c) {
        return switch (c) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            default -> "reset";
        };
    }
}
