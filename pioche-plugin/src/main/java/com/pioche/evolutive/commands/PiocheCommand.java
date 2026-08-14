package com.pioche.evolutive.commands;

import com.pioche.evolutive.config.LevelDefinition;
import com.pioche.evolutive.pickaxe.PickaxeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class PiocheCommand implements CommandExecutor {

    private final Plugin plugin;
    private final PickaxeManager pickaxeManager;
    private final Runnable configReloader;

    public PiocheCommand(Plugin plugin, PickaxeManager pickaxeManager, Runnable configReloader) {
        this.plugin = plugin;
        this.pickaxeManager = pickaxeManager;
        this.configReloader = configReloader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage : /pioche <give|info|level|reload>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "info" -> handleInfo(sender);
            case "level" -> handleLevel(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pioche.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Joueur introuvable : " + args[1], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Précise un joueur : /pioche give <joueur>", NamedTextColor.RED));
            return;
        }

        ItemStack pickaxe = pickaxeManager.createPickaxe();
        target.getInventory().addItem(pickaxe);
        sender.sendMessage(Component.text("Pioche évolutive donnée à " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!pickaxeManager.isPickaxe(item)) {
            sender.sendMessage(Component.text("Tu ne tiens pas la pioche évolutive en main.", NamedTextColor.RED));
            return;
        }

        int level = pickaxeManager.getLevel(item);
        int progress = pickaxeManager.getProgress(item);
        LevelDefinition current = pickaxeManager.getDefinition(level);
        LevelDefinition next = pickaxeManager.getNextLevel(level);

        sender.sendMessage(Component.text("=== Pioche évolutive ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Niveau actuel : " + level, NamedTextColor.YELLOW));
        if (current != null) {
            sender.sendMessage(Component.text("Zone de minage : " + current.width() + "x" + current.height() + "x" + current.depth(), NamedTextColor.GRAY));
        }
        if (next != null && next.hasQuest()) {
            sender.sendMessage(Component.text("Prochain palier (" + next.level() + ") : "
                    + next.questLabel() + " " + progress + "/" + next.questAmount(), NamedTextColor.AQUA));
        } else if (next == null) {
            sender.sendMessage(Component.text("Niveau maximum atteint !", NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void handleLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pioche.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /pioche level <joueur> <niveau>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable : " + args[1], NamedTextColor.RED));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Niveau invalide.", NamedTextColor.RED));
            return;
        }

        if (pickaxeManager.getDefinition(level) == null) {
            sender.sendMessage(Component.text("Ce niveau n'existe pas dans la config (paliers valides : "
                    + pickaxeManager.getLevels().keySet() + ").", NamedTextColor.RED));
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (!pickaxeManager.isPickaxe(item)) {
            sender.sendMessage(Component.text(target.getName() + " ne tient pas la pioche évolutive en main.", NamedTextColor.RED));
            return;
        }

        pickaxeManager.setLevel(item, level);
        pickaxeManager.setProgress(item, 0);
        pickaxeManager.refresh(item);
        target.getInventory().setItemInMainHand(item);

        sender.sendMessage(Component.text("Niveau de " + target.getName() + " mis à " + level + ".", NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("pioche.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return;
        }
        configReloader.run();
        sender.sendMessage(Component.text("Configuration de la pioche évolutive rechargée.", NamedTextColor.GREEN));
    }
}
