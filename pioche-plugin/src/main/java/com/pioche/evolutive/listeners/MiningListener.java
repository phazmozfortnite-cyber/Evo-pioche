package com.pioche.evolutive.listeners;

import com.pioche.evolutive.config.LevelDefinition;
import com.pioche.evolutive.pickaxe.PickaxeManager;
import com.pioche.evolutive.util.AreaMiner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MiningListener implements Listener {

    private final PickaxeManager pickaxeManager;

    // Blocs qu'on est en train de casser manuellement, pour éviter de redéclencher cet event dessus.
    private final Set<Block> currentlyBreaking = new HashSet<>();

    public MiningListener(PickaxeManager pickaxeManager) {
        this.pickaxeManager = pickaxeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!pickaxeManager.isPickaxe(tool)) return;
        if (currentlyBreaking.contains(event.getBlock())) return;

        int currentLevel = pickaxeManager.getLevel(tool);
        LevelDefinition currentDef = pickaxeManager.getDefinition(currentLevel);
        if (currentDef == null) return;

        LevelDefinition nextDef = pickaxeManager.getNextLevel(currentLevel);

        // 1) Le bloc cassé "normalement" par l'event compte pour la quête
        int gained = 0;
        if (nextDef != null && nextDef.matches(event.getBlock().getType())) {
            gained++;
        }

        // 2) Minage en zone (AoE) pour les blocs additionnels autour de l'origine
        List<Block> extra = AreaMiner.computeArea(
                event.getBlock(),
                player.getEyeLocation().getDirection(),
                currentDef.width(), currentDef.height(), currentDef.depth()
        );

        for (Block block : extra) {
            if (block.getType() == Material.AIR || block.getType().isAir()) continue;
            if (block.getType() == Material.BEDROCK) continue;

            BlockBreakEvent synthetic = new BlockBreakEvent(block, player);
            currentlyBreaking.add(block);
            try {
                Bukkit.getPluginManager().callEvent(synthetic);
                if (synthetic.isCancelled()) continue; // respecte les protections (WorldGuard, claims, etc.)

                Material minedType = block.getType();
                boolean drop = !player.getGameMode().name().equals("CREATIVE");
                if (drop) {
                    block.breakNaturally(tool);
                } else {
                    block.setType(Material.AIR);
                }

                if (nextDef != null && nextDef.matches(minedType)) {
                    gained++;
                }
            } finally {
                currentlyBreaking.remove(block);
            }
        }

        if (gained <= 0) return;

        pickaxeManager.addProgress(tool, gained);

        int required = nextDef != null ? nextDef.questAmount() : 0;
        int progress = pickaxeManager.getProgress(tool);

        if (nextDef != null && progress >= required) {
            boolean leveledUp = pickaxeManager.levelUp(tool);
            if (leveledUp) {
                announceLevelUp(player, nextDef);
            }
        } else {
            pickaxeManager.refresh(tool);
        }

        player.getInventory().setItemInMainHand(tool);
    }

    private void announceLevelUp(Player player, LevelDefinition newLevel) {
        player.sendMessage(Component.text("✦ Ta pioche est passée au niveau " + newLevel.level() + " ! ✦",
                NamedTextColor.GOLD, TextDecoration.BOLD));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
    }
}
