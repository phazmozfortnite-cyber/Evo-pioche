package com.pioche.evolutive.pickaxe;

import com.pioche.evolutive.config.LevelDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Gère la création de la pioche évolutive et toute la lecture/écriture
 * de ses données de progression stockées dans son PersistentDataContainer.
 * La progression est donc liée à l'ITEM lui-même (pas au joueur).
 */
public final class PickaxeManager {

    private final PickaxeKeys keys;
    private final TreeMap<Integer, LevelDefinition> levels;

    public PickaxeManager(PickaxeKeys keys, TreeMap<Integer, LevelDefinition> levels) {
        this.keys = keys;
        this.levels = levels;
    }

    /** Remplace la table des niveaux (utilisé lors d'un /pioche reload). */
    public void updateLevels(TreeMap<Integer, LevelDefinition> newLevels) {
        this.levels.clear();
        this.levels.putAll(newLevels);
    }

    public TreeMap<Integer, LevelDefinition> getLevels() {
        return levels;
    }

    public LevelDefinition getFirstLevel() {
        return levels.firstEntry().getValue();
    }

    public LevelDefinition getDefinition(int level) {
        return levels.get(level);
    }

    /** Retourne le palier juste au-dessus du niveau donné, ou null si déjà au maximum. */
    public LevelDefinition getNextLevel(int currentLevel) {
        var entry = levels.higherEntry(currentLevel);
        return entry == null ? null : entry.getValue();
    }

    public boolean isMaxLevel(int currentLevel) {
        return getNextLevel(currentLevel) == null;
    }

    // ---------------------------------------------------------------
    //  Création
    // ---------------------------------------------------------------

    public ItemStack createPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(keys.marker, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(keys.level, PersistentDataType.INTEGER, getFirstLevel().level());
        meta.getPersistentDataContainer().set(keys.progress, PersistentDataType.INTEGER, 0);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        refresh(item);
        return item;
    }

    // ---------------------------------------------------------------
    //  Lecture / écriture des données PDC
    // ---------------------------------------------------------------

    public boolean isPickaxe(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        NamespacedKey k = keys.marker;
        return item.getItemMeta().getPersistentDataContainer().has(k, PersistentDataType.BYTE);
    }

    public int getLevel(ItemStack item) {
        Integer value = item.getItemMeta().getPersistentDataContainer().get(keys.level, PersistentDataType.INTEGER);
        return value != null ? value : getFirstLevel().level();
    }

    public int getProgress(ItemStack item) {
        Integer value = item.getItemMeta().getPersistentDataContainer().get(keys.progress, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public void setLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.level, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    public void setProgress(ItemStack item, int progress) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.progress, PersistentDataType.INTEGER, progress);
        item.setItemMeta(meta);
    }

    public void addProgress(ItemStack item, int amount) {
        setProgress(item, getProgress(item) + amount);
    }

    /**
     * Fait passer la pioche au palier suivant si un palier suivant existe.
     * Réinitialise la progression à 0. Ne fait rien si déjà au niveau max.
     * @return true si un level up a eu lieu.
     */
    public boolean levelUp(ItemStack item) {
        int current = getLevel(item);
        LevelDefinition next = getNextLevel(current);
        if (next == null) return false;

        setLevel(item, next.level());
        setProgress(item, 0);
        refresh(item);
        return true;
    }

    // ---------------------------------------------------------------
    //  Application visuelle + enchantements
    // ---------------------------------------------------------------

    /** Réapplique les enchantements et régénère la lore en fonction du niveau actuel stocké dans l'item. */
    public void refresh(ItemStack item) {
        int currentLevelNum = getLevel(item);
        LevelDefinition current = levels.get(currentLevelNum);
        if (current == null) return; // config incohérente, on ne touche à rien

        ItemMeta meta = item.getItemMeta();

        // --- Enchantements ---
        meta.removeEnchant(Enchantment.EFFICIENCY);
        meta.removeEnchant(Enchantment.FORTUNE);
        if (current.efficiency() > 0) {
            meta.addEnchant(Enchantment.EFFICIENCY, current.efficiency(), true);
        }
        if (current.fortune() > 0) {
            meta.addEnchant(Enchantment.FORTUNE, current.fortune(), true);
        }

        // --- Nom ---
        meta.displayName(Component.text("⛏ Pioche du Mineur", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        // --- Lore ---
        meta.lore(buildLore(current, getNextLevel(currentLevelNum), getProgress(item)));

        item.setItemMeta(meta);
    }

    private List<Component> buildLore(LevelDefinition current, LevelDefinition next, int progress) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Niveau " + current.level(), NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        lore.add(statLine("Minage", current.width() + "×" + current.height() + "×" + current.depth()));
        lore.add(statLine("Fortune", romanOrDash(current.fortune())));
        lore.add(statLine("Efficacité", romanOrDash(current.efficiency())));
        lore.add(statLine("Portée", "+" + current.reachBonus()));
        if (current.sellBonus() > 0) {
            lore.add(statLine("Bonus de vente", "+" + Math.round(current.sellBonus() * 100) + "%"));
        }

        lore.add(Component.empty());

        if (next != null && next.hasQuest()) {
            int required = next.questAmount();
            int done = Math.min(progress, required);
            lore.add(Component.text("➤ Prochain palier : Niveau " + next.level(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  " + next.questLabel() + " : " + done + " / " + required,
                            NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  [" + progressBar(done, required) + "]", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else if (next == null) {
            lore.add(Component.text("🏆 Pioche au niveau maximum !", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    private Component statLine(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private String progressBar(int done, int total) {
        int barLength = 20;
        int filled = total <= 0 ? 0 : (int) ((double) done / total * barLength);
        filled = Math.min(barLength, Math.max(0, filled));
        return "█".repeat(filled) + "░".repeat(barLength - filled);
    }

    private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

    private String romanOrDash(int value) {
        if (value <= 0) return "-";
        if (value < ROMAN.length) return ROMAN[value];
        return String.valueOf(value);
    }
}
