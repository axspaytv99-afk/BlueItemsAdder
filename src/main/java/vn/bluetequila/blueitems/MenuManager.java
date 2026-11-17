package vn.bluetequila.blueitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Menu GUI đọc từ menus.yml. */
public final class MenuManager implements Listener {

    public record MenuItem(ConfigurationSection section, List<String> actions,
                           List<String> leftActions, List<String> rightActions) {
    }

    public record Menu(String id, String title, String permission, int rows,
                       ConfigurationSection filler, Map<Integer, MenuItem> slots) {
    }

    private static final class MenuHolder implements InventoryHolder {
        private final Menu menu;
        private Inventory inventory;

        private MenuHolder(Menu menu) {
            this.menu = menu;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final class MenuCommand extends Command {
        private MenuCommand(String name) {
            super(name);
            setDescription("Mở menu");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dùng trong game.");
                return true;
            }
            String id = commandToMenu.get(getName());
            if (id == null) {
                player.sendMessage(Text.parse(plugin.prefix() + "<red>Menu này không còn tồn tại.", player));
                return true;
            }
            open(player, id);
            return true;
        }
    }

    private final BlueItems plugin;
    private final Map<String, Menu> menus = new LinkedHashMap<>();
    private final Map<String, String> commandToMenu = new HashMap<>();
    private final Map<String, MenuCommand> registered = new HashMap<>();

    public MenuManager(BlueItems plugin) {
        this.plugin = plugin;
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(menus.keySet());
    }

    public void load(FileConfiguration cfg) {
        menus.clear();
        commandToMenu.clear();
        ConfigurationSection root = cfg.getConfigurationSection("menus");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String key = id.toLowerCase(Locale.ROOT);
                int rows = Math.max(1, Math.min(6, s.getInt("rows", 3)));

                Map<Integer, MenuItem> slots = new HashMap<>();
                ConfigurationSection items = s.getConfigurationSection("items");
                if (items != null) {
                    for (String itemKey : items.getKeys(false)) {
                        ConfigurationSection is = items.getConfigurationSection(itemKey);
                        if (is == null) {
                            continue;
                        }
                        MenuItem mi = new MenuItem(is, is.getStringList("actions"),
                                is.getStringList("left_actions"), is.getStringList("right_actions"));
                        List<Integer> list = new ArrayList<>();
                        if (is.isList("slots")) {
                            list.addAll(is.getIntegerList("slots"));
                        }
                        if (is.contains("slot")) {
                            list.add(is.getInt("slot"));
                        }
                        for (int slot : list) {
                            if (slot >= 0 && slot < rows * 9) {
                                slots.put(slot, mi);
                            } else {
                                plugin.getLogger().warning("Slot " + slot + " ngoài menu '" + id + "' (item " + itemKey + ")");
                            }
                        }
                    }
                }

                menus.put(key, new Menu(key, s.getString("title", id), s.getString("permission"),
                        rows, s.getConfigurationSection("filler"), slots));

                String cmd = s.getString("open_command");
                if (cmd != null && !cmd.isBlank()) {
                    registerCommand(cmd.toLowerCase(Locale.ROOT).replace("/", "").trim(), key);
                }
            }
        }
        plugin.getLogger().info("Đã tải " + menus.size() + " menu.");
    }

    private void registerCommand(String label, String menuId) {
        commandToMenu.put(label, menuId);
        if (registered.containsKey(label)) {
            return;
        }
        MenuCommand cmd = new MenuCommand(label);
        registered.put(label, cmd);
        plugin.getServer().getCommandMap().register("blueitems", cmd);
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
    }

    public void unregisterCommands() {
        CommandMap map = plugin.getServer().getCommandMap();
        for (MenuCommand cmd : registered.values()) {
            cmd.unregister(map);
            map.getKnownCommands().values().removeIf(c -> c == cmd);
        }
        registered.clear();
    }

    public boolean open(Player player, String id) {
        Menu menu = menus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) {
            player.sendMessage(Text.parse(plugin.prefix() + "<red>Không có menu: " + Text.esc(id), player));
            return false;
        }
        if (menu.permission() != null && !player.hasPermission(menu.permission())) {
            player.sendMessage(Text.parse(plugin.prefix() + "<red>Bạn không có quyền mở menu này.", player));
            return false;
        }

        MenuHolder holder = new MenuHolder(menu);
        Inventory inv = Bukkit.createInventory(holder, menu.rows() * 9, Text.parse(menu.title(), player));
        holder.inventory = inv;

        if (menu.filler() != null) {
            ItemStack filler = plugin.items().create(menu.filler(), player, null);
            if (filler != null) {
                for (int i = 0; i < inv.getSize(); i++) {
                    inv.setItem(i, filler.clone());
                }
            }
        }
        for (Map.Entry<Integer, MenuItem> e : menu.slots().entrySet()) {
            inv.setItem(e.getKey(), buildItem(e.getValue(), player));
        }
        player.openInventory(inv);
        return true;
    }

    private ItemStack buildItem(MenuItem mi, Player player) {
        ConfigurationSection s = mi.section();
        String ref = s.getString("item");
        if (ref != null) {
            ItemStack base = plugin.items().get(ref);
            if (base == null) {
                plugin.getLogger().warning("Menu tham chiếu item không tồn tại: " + ref);
                return new ItemStack(Material.BARRIER);
            }
            if (s.contains("amount")) {
                base.setAmount(Math.max(1, Math.min(s.getInt("amount"), base.getMaxStackSize())));
            }
            return base;
        }
        ItemStack stack = plugin.items().create(s, player, null);
        return stack == null ? new ItemStack(Material.BARRIER) : stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        MenuItem mi = holder.menu.slots().get(e.getSlot());
        if (mi == null) {
            return;
        }
        List<String> actions = new ArrayList<>(mi.actions());
        if (e.getClick().isLeftClick()) {
            actions.addAll(mi.leftActions());
        } else if (e.getClick().isRightClick()) {
            actions.addAll(mi.rightActions());
        }
        if (actions.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> run(player, actions));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }

    private void run(Player player, List<String> actions) {
        for (String rawLine : actions) {
            String line = rawLine.trim();
            String tag = "";
            String arg = line;
            if (line.startsWith("[")) {
                int end = line.indexOf(']');
                if (end > 0) {
                    tag = line.substring(1, end).toLowerCase(Locale.ROOT);
                    arg = line.substring(end + 1).trim();
                }
            }
            String text = Text.placeholders(player, arg);
            switch (tag) {
                case "player" -> player.performCommand(stripSlash(text));
                case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(text));
                case "message" -> player.sendMessage(Text.parse(arg, player));
                case "close" -> player.closeInventory();
                case "open" -> open(player, text);
                case "sound" -> player.playSound(player.getLocation(), text, 1f, 1f);
                case "give" -> {
                    String[] parts = text.split("\\s+");
                    int amount = 1;
                    if (parts.length > 1) {
                        try {
                            amount = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            // giữ mặc định 1
                        }
                    }
                    plugin.items().give(player, parts[0], amount);
                }
                default -> plugin.getLogger().warning("Hành động không hợp lệ: " + rawLine);
            }
        }
    }

    private static String stripSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}
