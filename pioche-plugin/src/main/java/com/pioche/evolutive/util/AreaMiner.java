package com.pioche.evolutive.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcule la liste des blocs supplémentaires à casser autour du bloc miné,
 * en fonction de la taille de zone (largeur × hauteur × profondeur) du palier actuel
 * et de la direction dans laquelle regarde le joueur.
 *
 * On détermine l'axe principal regardé (X, Y ou Z) pour orienter le "plan" width×height,
 * et "depth" prolonge le minage dans cet axe (ex: 3x3x2 = un plan 3x3, sur 2 blocs de profondeur).
 */
public final class AreaMiner {

    private AreaMiner() {}

    public static List<Block> computeArea(Block origin, Vector lookDirection, int width, int height, int depth) {
        List<Block> blocks = new ArrayList<>();

        // Rien à faire si la config est 1x1x1 : juste le bloc d'origine (déjà géré par l'event vanilla)
        if (width <= 1 && height <= 1 && depth <= 1) {
            return blocks;
        }

        double absX = Math.abs(lookDirection.getX());
        double absY = Math.abs(lookDirection.getY());
        double absZ = Math.abs(lookDirection.getZ());

        // Axe principal = celui vers lequel le joueur regarde le plus fortement.
        // Cet axe devient l'axe de "profondeur" ; les deux autres forment le plan largeur/hauteur.
        Location originLoc = origin.getLocation();

        int halfWidth = (width - 1) / 2;
        int extraWidth = (width - 1) - halfWidth; // gère les tailles paires en étalant l'excédent d'un côté
        int halfHeight = (height - 1) / 2;
        int extraHeight = (height - 1) - halfHeight;

        if (absY >= absX && absY >= absZ) {
            // Le joueur regarde surtout vers le haut/bas -> le plan est horizontal (X/Z), profondeur en Y
            for (int d = 0; d < depth; d++) {
                int y = originLoc.getBlockY() + (lookDirection.getY() >= 0 ? d : -d);
                for (int dx = -halfWidth; dx <= extraWidth; dx++) {
                    for (int dz = -halfHeight; dz <= extraHeight; dz++) {
                        addIfNotOrigin(blocks, origin, originLoc.getBlockX() + dx, y, originLoc.getBlockZ() + dz);
                    }
                }
            }
        } else if (absX >= absZ) {
            // Le joueur regarde surtout vers X -> le plan est Y/Z, profondeur en X
            for (int d = 0; d < depth; d++) {
                int x = originLoc.getBlockX() + (lookDirection.getX() >= 0 ? d : -d);
                for (int dy = -halfHeight; dy <= extraHeight; dy++) {
                    for (int dz = -halfWidth; dz <= extraWidth; dz++) {
                        addIfNotOrigin(blocks, origin, x, originLoc.getBlockY() + dy, originLoc.getBlockZ() + dz);
                    }
                }
            }
        } else {
            // Le joueur regarde surtout vers Z -> le plan est X/Y, profondeur en Z
            for (int d = 0; d < depth; d++) {
                int z = originLoc.getBlockZ() + (lookDirection.getZ() >= 0 ? d : -d);
                for (int dy = -halfHeight; dy <= extraHeight; dy++) {
                    for (int dx = -halfWidth; dx <= extraWidth; dx++) {
                        addIfNotOrigin(blocks, origin, originLoc.getBlockX() + dx, originLoc.getBlockY() + dy, z);
                    }
                }
            }
        }

        return blocks;
    }

    private static void addIfNotOrigin(List<Block> list, Block origin, int x, int y, int z) {
        if (x == origin.getX() && y == origin.getY() && z == origin.getZ()) return; // déjà géré par l'event vanilla
        list.add(origin.getWorld().getBlockAt(x, y, z));
    }
}
