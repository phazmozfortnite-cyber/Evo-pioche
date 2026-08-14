package com.pioche.evolutive.pickaxe;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Centralise les NamespacedKey utilisées pour stocker les données
 * de progression directement sur l'ItemStack de la pioche (PersistentDataContainer).
 */
public final class PickaxeKeys {

    public final NamespacedKey marker;   // marque l'item comme étant "notre" pioche
    public final NamespacedKey level;    // niveau actuel (1, 10, 20, ... 100)
    public final NamespacedKey progress; // progression de la quête en cours (nombre de blocs minés)

    public PickaxeKeys(Plugin plugin) {
        this.marker = new NamespacedKey(plugin, "pioche_evolutive");
        this.level = new NamespacedKey(plugin, "pioche_level");
        this.progress = new NamespacedKey(plugin, "pioche_progress");
    }
}
