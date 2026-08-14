package com.pioche.evolutive.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Lit la section "levels" de config.yml et construit une TreeMap triée par niveau,
 * ce qui permet de récupérer facilement "le palier suivant" à partir du niveau courant.
 */
public final class LevelConfigLoader {

    private final Logger logger;

    public LevelConfigLoader(Logger logger) {
        this.logger = logger;
    }

    public TreeMap<Integer, LevelDefinition> load(FileConfiguration config) {
        TreeMap<Integer, LevelDefinition> result = new TreeMap<>();

        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection == null) {
            logger.severe("[PiocheEvolutive] Section 'levels' introuvable dans config.yml !");
            return result;
        }

        for (String key : levelsSection.getKeys(false)) {
            int levelNumber;
            try {
                levelNumber = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                logger.warning("[PiocheEvolutive] Clé de niveau invalide dans config.yml : " + key);
                continue;
            }

            ConfigurationSection section = levelsSection.getConfigurationSection(key);
            if (section == null) continue;

            ConfigurationSection mining = section.getConfigurationSection("mining");
            int width = mining != null ? mining.getInt("width", 1) : 1;
            int height = mining != null ? mining.getInt("height", 1) : 1;
            int depth = mining != null ? mining.getInt("depth", 1) : 1;

            int speed = section.getInt("speed", 0);
            int fortune = section.getInt("fortune", 0);
            int efficiency = section.getInt("efficiency", 0);
            int reachBonus = section.getInt("reach-bonus", 0);
            double sellBonus = section.getDouble("sell-bonus", 0.0);

            boolean questIsAny = false;
            Set<Material> questMaterials = new LinkedHashSet<>();
            int questAmount = 0;

            ConfigurationSection quest = section.getConfigurationSection("quest");
            if (quest != null) {
                String materialName = quest.getString("material", "");
                questAmount = quest.getInt("amount", 0);

                if ("ANY".equalsIgnoreCase(materialName)) {
                    questIsAny = true;
                } else if (!materialName.isBlank()) {
                    Material base = Material.matchMaterial(materialName);
                    if (base == null) {
                        logger.warning("[PiocheEvolutive] Matériau inconnu au niveau " + levelNumber + " : " + materialName);
                    } else {
                        questMaterials.addAll(expandOreVariants(base));
                    }
                }
            }

            result.put(levelNumber, new LevelDefinition(
                    levelNumber, width, height, depth,
                    speed, fortune, efficiency, reachBonus, sellBonus,
                    questIsAny, questMaterials, questAmount
            ));
        }

        return result;
    }

    /**
     * Ajoute automatiquement la variante "deepslate" d'un minerai quand elle existe,
     * pour que casser du minerai de fer en deepslate compte aussi bien que la version normale.
     */
    private Set<Material> expandOreVariants(Material base) {
        Set<Material> set = new LinkedHashSet<>();
        set.add(base);
        String deepslateName = "DEEPSLATE_" + base.name();
        Material deepslate = Material.matchMaterial(deepslateName);
        if (deepslate != null) {
            set.add(deepslate);
        }
        return set;
    }
}
