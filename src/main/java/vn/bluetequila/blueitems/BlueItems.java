package vn.bluetequila.blueitems;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class BlueItems extends JavaPlugin {

    private static final String[] RESOURCES = {
            "emojis.yml", "items.yml", "menus.yml",
            "tab.yml", "sidebar.yml", "announcements.yml", "chat.yml"
    };

    private static BlueItems instance;
    private EmojiManager emojis;
    private ItemManager items;
    private MenuManager menus;
    private TabManager tab;
    private SidebarManager sidebar;
    private AnnouncementManager announcements;
    private ChatFormatter chat;

    public static BlueItems get() {
        return instance;
    }

    public EmojiManager emojis() {
        return emojis;
    }

    public ItemManager items() {
        return items;
    }

    public MenuManager menus() {
        return menus;
    }

    public SidebarManager sidebar() {
        return sidebar;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        for (String file : RESOURCES) {
            if (!new File(getDataFolder(), file).exists()) {
                saveResource(file, false);
            }
        }

        emojis = new EmojiManager(this);
        items = new ItemManager(this);
        menus = new MenuManager(this);
        tab = new TabManager(this);
        sidebar = new SidebarManager(this);
        announcements = new AnnouncementManager(this);
        chat = new ChatFormatter();

        getServer().getPluginManager().registerEvents(emojis, this);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(tab, this);
        getServer().getPluginManager().registerEvents(sidebar, this);
        getServer().getPluginManager().registerEvents(chat, this);

        Commands handler = new Commands(this);
        PluginCommand command = Objects.requireNonNull(getCommand("blueitems"));
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        reloadAll();
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.unregisterCommands();
        }
        if (tab != null) {
            tab.shutdown();
        }
        if (sidebar != null) {
            sidebar.shutdown();
        }
        if (announcements != null) {
            announcements.stop();
        }
    }

    /** Tải lại toàn bộ cấu hình. Thứ tự quan trọng: emoji -> item -> menu -> phần còn lại. */
    public void reloadAll() {
        reloadConfig();
        emojis.load(yaml("emojis.yml"));
        items.load(yaml("items.yml"));
        menus.load(yaml("menus.yml"));
        tab.load(yaml("tab.yml"));
        sidebar.load(yaml("sidebar.yml"));
        announcements.load(yaml("announcements.yml"));
        chat.load(yaml("chat.yml"));
    }

    public String prefix() {
        return getConfig().getString("prefix", "");
    }

    private YamlConfiguration yaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }
}
