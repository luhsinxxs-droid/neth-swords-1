package com.diasmp.diaswords.managers;

import com.diasmp.diaswords.DiaSwords;
import com.diasmp.diaswords.SwordType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds and identifies custom ability sword ItemStacks.
 * Sword identity is tagged via PersistentDataContainer so it survives
 * renames, enchants, and inventory moves.
 */
public class SwordManager {

    private final DiaSwords plugin;

    public SwordManager(DiaSwords plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a fresh custom sword ItemStack for the given type, reading its
     * display name from config.
     */
    public ItemStack createSword(SwordType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        String rawName = plugin.getConfig().getString(type.getConfigKey() + ".display-name", type.name());
        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(rawName)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(triggerHint(type), NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(type.getKey(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        applyDefaultEnchants(item);
        return item;
    }

    /**
     * Applies the enchantments configured under general.default-enchants to a
     * freshly created sword. Unknown enchant names are skipped with a warning
     * logged, rather than failing the whole give.
     */
    private void applyDefaultEnchants(ItemStack item) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("general.default-enchants");
        if (section == null) return;

        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            String enchantName = entry.getKey();
            int level;
            try {
                level = Integer.parseInt(entry.getValue().toString());
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid enchant level for '" + enchantName + "' in default-enchants: "
                        + entry.getValue());
                continue;
            }
            if (level <= 0) continue;

            Enchantment enchantment = resolveEnchantment(enchantName);
            if (enchantment == null) {
                plugin.getLogger().warning("Unknown enchantment '" + enchantName + "' in default-enchants — skipping.");
                continue;
            }

            // addUnsafeEnchantment bypasses level caps and conflict checks, since
            // some configured levels (e.g. Sharpness V) exceed vanilla maximums.
            item.addUnsafeEnchantment(enchantment, level);
        }
    }

    /**
     * Resolves a config-friendly enchant name (e.g. "SHARPNESS", "sweeping_edge")
     * to a Bukkit Enchantment via the modern Registry lookup.
     */
    private Enchantment resolveEnchantment(String name) {
        NamespacedKey key = NamespacedKey.minecraft(name.trim().toLowerCase());
        return Registry.ENCHANTMENT.get(key);
    }

    /**
     * @return the SwordType tagged on this item, or null if it's not a DiaSwords item.
     */
    public SwordType getType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        for (SwordType type : SwordType.values()) {
            if (meta.getPersistentDataContainer().has(type.getKey(plugin), PersistentDataType.BYTE)) {
                return type;
            }
        }
        return null;
    }

    public boolean isCustomSword(ItemStack item) {
        return getType(item) != null;
    }

    /**
     * Human-readable activation hint shown in the sword's lore. Dash/Frost/Vortex/
     * Explosive require crouch + right-click; Lightning/Vampire are passive on-hit.
     */
    private String triggerHint(SwordType type) {
        return switch (type) {
            case DASH, FROST, VORTEX, EXPLOSIVE -> "Crouch + right-click to activate.";
            case LIGHTNING, VAMPIRE -> "Passive: triggers automatically on hit.";
        };
    }
}
