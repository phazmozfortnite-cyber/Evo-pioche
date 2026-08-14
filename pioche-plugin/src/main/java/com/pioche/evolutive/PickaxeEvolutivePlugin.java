package com.pioche.evolutive;

import com.pioche.evolutive.commands.PiocheCommand;
import com.pioche.evolutive.config.LevelConfigLoader;
import com.pioche.evolutive.config.LevelDefinition;
import com.pioche.evolutive.listeners.MiningListener;
import com.pioche.evolutive.listeners.PickaxeEffectsTask;
import com.pioche.evolutive.pickaxe.PickaxeKeys;
import com.pioche.evolutive.pickaxe.PickaxeManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.TreeMap;

public final class PickaxeEvolutivePlugin extends JavaPlugin {

    private PickaxeManager pickaxeManager;
    private PickaxeEffectsTask effectsTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PickaxeKeys keys = new PickaxeKeys(this);
        TreeMap<Integer, LevelDefinition> levels = new LevelConfigLoader(getLogger()).load(getConfig());

        if (levels.isEmpty()) {
            getLogger().severe("[PiocheEvolutive] Aucun palier chargé depuis config.yml. Le plugin est désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.pickaxeManager = new PickaxeManager(keys, levels);

        getServer().getPluginManager().registerEvents(new MiningListener(pickaxeManager), this);

        this.effectsTask = new PickaxeEffectsTask(this, pickaxeManager);
        this.effectsTask.runTaskTimer(this, 0L, 20L); // toutes les secondes

        PiocheCommand commandExecutor = new PiocheCommand(this, pickaxeManager, this::reloadLevels);
        var pioche = getCommand("pioche");
        if (pioche != null) {
            pioche.setExecutor(commandExecutor);
        } else {
            getLogger().severe("[PiocheEvolutive] Commande 'pioche' introuvable : vérifie plugin.yml.");
        }

        getLogger().info("[PiocheEvolutive] Plugin activé avec " + levels.size() + " paliers.");
    }

    @Override
    public void onDisable() {
        if (effectsTask != null) {
            effectsTask.cancel();
        }
    }

    private void reloadLevels() {
        reloadConfig();
        TreeMap<Integer, LevelDefinition> levels = new LevelConfigLoader(getLogger()).load(getConfig());
        if (!levels.isEmpty()) {
            pickaxeManager.updateLevels(levels);
        } else {
            getLogger().warning("[PiocheEvolutive] Rechargement ignoré : la nouvelle config.yml ne contient aucun palier valide.");
        }
    }

    public PickaxeManager getPickaxeManager() {
        return pickaxeManager;
    }
}
