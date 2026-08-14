package com.pioche.evolutive.config;

import org.bukkit.Material;

import java.util.Set;

/**
 * Représente un palier de la pioche évolutive (ex: niveau 50).
 * Immuable : chargé une fois depuis config.yml puis mis en cache.
 */
public record LevelDefinition(
        int level,
        int width,
        int height,
        int depth,
        int speedAmplifier,
        int fortune,
        int efficiency,
        int reachBonus,
        double sellBonus,
        boolean questIsAny,
        Set<Material> questMaterials, // ex: {COAL_ORE, DEEPSLATE_COAL_ORE}. Vide si pas de quête ou questIsAny.
        int questAmount
) {

    public boolean hasQuest() {
        return questIsAny || (questMaterials != null && !questMaterials.isEmpty());
    }

    /** Le bloc miné correspond-il à la cible de la quête de CE palier ? */
    public boolean matches(Material minedType) {
        if (!hasQuest()) return false;
        if (questIsAny) return true;
        return questMaterials.contains(minedType);
    }

    /** Nom lisible du matériau visé (pour la lore / les messages), ex: "Charbon". */
    public String questLabel() {
        if (questIsAny) return "N'importe quel bloc";
        if (questMaterials == null || questMaterials.isEmpty()) return "-";
        // On prend le "principal" (le premier de la liste, ordre d'insertion garanti par LinkedHashSet)
        Material main = questMaterials.iterator().next();
        String name = main.name().replace("DEEPSLATE_", "").replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
