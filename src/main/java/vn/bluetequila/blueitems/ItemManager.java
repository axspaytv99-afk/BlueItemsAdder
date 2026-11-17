package vn.bluetequila.blueitems;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Đọc items.yml và tạo item custom. */
public final class ItemManager {

    private final BlueItems plugin;
    private final NamespacedKey idKey;
    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public ItemManager(BlueItems plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    public void load(FileConfiguration cfg) {
        items.clear();
        ConfigurationSection root = cfg.getConfigurationSection("items");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String key = id.toLowerCase(Locale.ROOT);
                ItemStack stack = create(s, null, key);
                if (stack != null) {
                    items.put(key, stack);
                }
            }
        }
        plugin.getLogger().info("Đã tải " + items.size() + " item custom.");
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(items.keySet());
    }

    /** Trả về bản sao của item custom, hoặc null nếu không có. */
    public ItemStack get(String id) {
        ItemStack stack = items.get(id.toLowerCase(Locale.ROOT));
        return stack == null ? null : stack.clone();
    }

    /** id item custom của một ItemStack (null nếu không phải item custom). */
    public String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean give(Player player, String id, int amount) {
        ItemStack proto = items.get(id.toLowerCase(Locale.ROOT));
        if (proto == null) {
            return false;
        }
        int left = Math.max(1, Math.min(amount, 2304));
        while (left > 0) {
            ItemStack stack = proto.clone();
            int n = Math.min(left, stack.getMaxStackSize());
            stack.setAmount(n);
            player.getInventory().addItem(stack).values().forEach(
                    rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            left -= n;
        }
        return true;
    }

    /**
     * Tạo item từ một section cấu hình. Dùng chung cho items.yml và các ô trong menu.
     *
     * @param viewer người xem (để thay placeholder), có thể null
     * @param id     id item custom để gắn thẻ, null nếu không phải item custom
     */
    public ItemStack create(ConfigurationSection s, OfflinePlayer viewer, String id) {
        String matName = s.getString("material", "PAPER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null || mat.isAir() || !mat.isItem()) {
            plugin.getLogger().warning("Material không hợp lệ '" + matName + "' tại " + s.getCurrentPath());
            return null;
        }

        int amount = Math.max(1, Math.min(s.getInt("amount", 1), mat.getMaxStackSize()));
        ItemStack stack = new ItemStack(mat, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        if (s.contains("name")) {
            meta.displayName(Text.item(s.getString("name", ""), viewer));
        }

        List<String> loreRaw = s.getStringList("lore");
        if (!loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                lore.add(Text.item(line, viewer));
            }
            meta.lore(lore);
        }

        String model = s.getString("model");
        if (model != null && !model.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(model.toLowerCase(Locale.ROOT));
            if (key != null) {
                meta.setItemModel(key);
            } else {
                plugin.getLogger().warning("model không hợp lệ '" + model + "' tại " + s.getCurrentPath());
            }
        }

        if (s.getBoolean("unbreakable")) {
            meta.setUnbreakable(true);
        }
        if (s.getBoolean("glow")) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (s.getBoolean("hide_tooltip")) {
            meta.setHideTooltip(true);
        }

        ConfigurationSection ench = s.getConfigurationSection("enchants");
        if (ench != null) {
            for (String name : ench.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString("minecraft:" + name.toLowerCase(Locale.ROOT));
                Enchantment e = key == null ? null : Registry.ENCHANTMENT.get(key);
                if (e == null) {
                    plugin.getLogger().warning("Enchant không tồn tại '" + name + "' tại " + s.getCurrentPath());
                    continue;
                }
                meta.addEnchant(e, Math.max(1, ench.getInt(name)), true);
            }
        }

        for (String flag : s.getStringList("flags")) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Flag không hợp lệ '" + flag + "' tại " + s.getCurrentPath());
            }
        }

        if (id != null) {
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
