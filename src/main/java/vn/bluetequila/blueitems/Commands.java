package vn.bluetequila.blueitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class Commands implements TabExecutor {

    private final BlueItems plugin;

    public Commands(BlueItems plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender to, String text) {
        to.sendMessage(Text.parse(plugin.prefix() + text, null));
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("blueitems.admin")) {
            return true;
        }
        msg(sender, "<red>Bạn không có quyền dùng lệnh này.");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (admin(sender)) {
                    plugin.reloadAll();
                    msg(sender, "<green>Đã tải lại emoji, item và menu.");
                }
            }
            case "list" -> {
                if (admin(sender)) {
                    msg(sender, "<yellow>Item: <white>" + Text.esc(String.join(", ", plugin.items().ids())));
                    msg(sender, "<yellow>Menu: <white>" + Text.esc(String.join(", ", plugin.menus().ids())));
                }
            }
            case "give" -> give(sender, args);
            case "menu" -> menu(sender, args);
            case "emojis" -> emojis(sender);
            case "sidebar" -> sidebar(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        msg(sender, "<yellow>Lệnh BlueItems:");
        sender.sendMessage(Text.parse("<gray> /bi emojis <dark_gray>- xem danh sách emoji", null));
        sender.sendMessage(Text.parse("<gray> /bi menu [id] <dark_gray>- mở menu", null));
        sender.sendMessage(Text.parse("<gray> /bi sidebar <dark_gray>- ẩn/hiện bảng thông tin bên phải", null));
        if (sender.hasPermission("blueitems.admin")) {
            sender.sendMessage(Text.parse("<gray> /bi give [người chơi] [id] [số lượng] <dark_gray>- tặng item custom", null));
            sender.sendMessage(Text.parse("<gray> /bi list <dark_gray>- danh sách item và menu", null));
            sender.sendMessage(Text.parse("<gray> /bi reload <dark_gray>- tải lại cấu hình", null));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!admin(sender)) {
            return;
        }
        if (args.length < 3) {
            msg(sender, "<red>Cách dùng: /bi give [người chơi] [id] [số lượng]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg(sender, "<red>Không tìm thấy người chơi đang online: " + Text.esc(args[1]));
            return;
        }
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                msg(sender, "<red>Số lượng phải là số.");
                return;
            }
        }
        if (!plugin.items().give(target, args[2], amount)) {
            msg(sender, "<red>Không có item với id: " + Text.esc(args[2]));
            return;
        }
        msg(sender, "<green>Đã tặng <white>" + amount + "x " + Text.esc(args[2]) + "<green> cho <white>" + target.getName());
    }

    private void menu(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Cách dùng: /bi menu [id]");
            return;
        }
        Player target;
        if (args.length > 2) {
            if (!admin(sender)) {
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                msg(sender, "<red>Không tìm thấy người chơi đang online: " + Text.esc(args[2]));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            msg(sender, "<red>Console cần chỉ định người chơi: /bi menu [id] [người chơi]");
            return;
        }
        plugin.menus().open(target, args[1]);
    }

    private void sidebar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "<red>Lệnh này chỉ dùng trong game.");
            return;
        }
        if (!plugin.sidebar().enabled()) {
            msg(sender, "<red>Bảng thông tin bên phải đang tắt trên server.");
            return;
        }
        boolean visible = plugin.sidebar().toggle(player);
        msg(sender, visible ? "<green>Đã hiện bảng thông tin bên phải." : "<yellow>Đã ẩn bảng thông tin bên phải.");
    }

    private void emojis(CommandSender sender) {
        Map<String, Component> all = plugin.emojis().all();
        if (all.isEmpty()) {
            msg(sender, "<red>Chưa có emoji nào trong emojis.yml.");
            return;
        }
        msg(sender, "<yellow>Gõ <white>:tên:</white> trong chat để dùng emoji (Shift+click để chèn):");
        Component line = Component.empty();
        for (Map.Entry<String, Component> e : all.entrySet()) {
            Component entry = Component.text(":" + e.getKey() + ": ", NamedTextColor.GRAY)
                    .append(e.getValue().colorIfAbsent(NamedTextColor.WHITE))
                    .append(Component.text("   "))
                    .insertion(":" + e.getKey() + ":");
            line = line.append(entry);
        }
        sender.sendMessage(line);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("give", "list", "menu", "emojis", "sidebar", "reload"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give")) {
            if (args.length == 2) {
                return filter(onlineNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(plugin.items().ids(), args[2]);
            }
            if (args.length == 4) {
                return filter(List.of("1", "16", "64"), args[3]);
            }
        } else if (sub.equals("menu")) {
            if (args.length == 2) {
                return filter(plugin.menus().ids(), args[1]);
            }
            if (args.length == 3) {
                return filter(onlineNames(), args[2]);
            }
        }
        return List.of();
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
        return names;
    }

    private static List<String> filter(Collection<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted()
                .collect(Collectors.toList());
    }
}
