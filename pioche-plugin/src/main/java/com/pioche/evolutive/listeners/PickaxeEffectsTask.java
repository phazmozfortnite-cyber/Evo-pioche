package com.pioche.evolutive.listeners;

import com.pioche.evolutive.config.LevelDefinition;
import com.pioche.evolutive.pickaxe.PickaxeManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

/**
 * Applique périodiquement :
 *  - un effet de Hâte (vitesse de minage) dont l'amplificateur dépend du palier actuel
 *  - un bonus de portée de bloc (Attribute) dépendant du palier actuel
 * aux joueurs qui tiennent la pioche évolutive en main.
 *
 * NOTE : le nom exact de la constante Attribute pour la portée d'interaction avec les blocs
 * a changé plusieurs fois dans l'API Bukkit/Paper (BLOCK_INTERACTION_RANGE / PLAYER_BLOCK_INTERACTION_RANGE
 * selon les versions). On tente plusieurs noms connus et on désactive proprement cette fonctionnalité
 * si aucun ne correspond, plutôt que de crasher au démarrage.
 */
public final class PickaxeEffectsTask extends BukkitRunnable {

    private static final NamespacedKeyHolder MODIFIER_ID = new NamespacedKeyHolder();
    private static final String[] CANDIDATE_ATTRIBUTE_NAMES = {
            "PLAYER_BLOCK_INTERACTION_RANGE",
            "BLOCK_INTERACTION_RANGE",
            "GENERIC_BLOCK_INTERACTION_RANGE"
    };

    private final Plugin plugin;
    private final PickaxeManager pickaxeManager;
    private final Logger logger;
    private Attribute reachAttribute; // null si introuvable sur cette version
    private boolean warnedOnce = false;

    public PickaxeEffectsTask(Plugin plugin, PickaxeManager pickaxeManager) {
        this.plugin = plugin;
        this.pickaxeManager = pickaxeManager;
        this.logger = plugin.getLogger();
        this.reachAttribute = resolveReachAttribute();
    }

    private Attribute resolveReachAttribute() {
        for (String name : CANDIDATE_ATTRIBUTE_NAMES) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // essaie le nom suivant
            }
        }
        return null;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            boolean holdingPickaxe = pickaxeManager.isPickaxe(mainHand);

            applyHaste(player, holdingPickaxe, mainHand);
            applyReach(player, holdingPickaxe, mainHand);
        }
    }

    private void applyHaste(Player player, boolean holdingPickaxe, ItemStack item) {
        if (!holdingPickaxe) {
            if (player.hasPotionEffect(PotionEffectType.FAST_DIGGING) && isOurHaste(player)) {
                player.removePotionEffect(PotionEffectType.FAST_DIGGING);
            }
            return;
        }

        int level = pickaxeManager.getLevel(item);
        LevelDefinition def = pickaxeManager.getDefinition(level);
        if (def == null || def.speedAmplifier() <= 0) return;

        // Durée un peu plus longue que l'intervalle de la tâche pour éviter le clignotement
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,
                60, def.speedAmplifier() - 1, false, false, false));
    }

    private boolean isOurHaste(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.FAST_DIGGING);
        return effect != null && effect.getDuration() <= 60; // heuristique simple : nos effets ont une durée courte
    }

    private void applyReach(Player player, boolean holdingPickaxe, ItemStack item) {
        if (reachAttribute == null) {
            if (!warnedOnce) {
                logger.warning("[PiocheEvolutive] Aucun attribut de portée de bloc trouvé sur cette version du serveur : "
                        + "le bonus de Reach ne sera pas appliqué (les autres fonctionnalités restent actives).");
                warnedOnce = true;
            }
            return;
        }

        AttributeInstance instance = player.getAttribute(reachAttribute);
        if (instance == null) return;

        instance.getModifiers().stream()
                .filter(m -> m.getKey() != null && m.getKey().equals(MODIFIER_ID.key(plugin)))
                .forEach(instance::removeModifier);

        if (!holdingPickaxe) return;

        int level = pickaxeManager.getLevel(item);
        LevelDefinition def = pickaxeManager.getDefinition(level);
        if (def == null || def.reachBonus() <= 0) return;

        AttributeModifier modifier = new AttributeModifier(
                MODIFIER_ID.key(plugin),
                def.reachBonus(),
                AttributeModifier.Operation.ADD_NUMBER
        );
        instance.addModifier(modifier);
    }

    /** Petit helper pour créer/mettre en cache la NamespacedKey utilisée comme identifiant de modifier. */
    private static final class NamespacedKeyHolder {
        private org.bukkit.NamespacedKey cached;

        org.bukkit.NamespacedKey key(Plugin plugin) {
            if (cached == null) {
                cached = new org.bukkit.NamespacedKey(plugin, "pioche_evolutive_reach");
            }
            return cached;
        }
    }
}
