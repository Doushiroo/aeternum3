package com.aeternum.systems.classes;

import com.aeternum.data.PlayerData;
import com.aeternum.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ClassSystem {

    public static final int MIN_LEVEL_TO_CHANGE_CLASS = 1;
    public static final long CLASS_CHANGE_COST_AU = 5_000L;

    /**
     * Attempt to set a player's class.
     * Validates karma, level, and cost requirements.
     */
    public static boolean chooseClass(ServerPlayer player, PlayerClass newClass) {
        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());

        // Level requirement
        if (data.getLevel() < MIN_LEVEL_TO_CHANGE_CLASS) {
            player.sendSystemMessage(Component.literal(
                "§cYou need to be at least level " + MIN_LEVEL_TO_CHANGE_CLASS + " to choose a class."));
            return false;
        }

        // Quest requirement
        if (newClass.requiresQuest()) {
            player.sendSystemMessage(Component.literal(
                "§cThe " + newClass.getDisplayName() + " class requires completing a special quest first."));
            return false;
        }

        // Karma requirement
        if (!newClass.isUnlockable(data.getKarma())) {
            if (newClass.getKarmaRequirement() > 0) {
                player.sendSystemMessage(Component.literal(
                    "§cYou need at least §e" + newClass.getKarmaRequirement() +
                    " karma §cto choose " + newClass.getDisplayName() + "."));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§cYou need karma of §e" + newClass.getKarmaRequirement() +
                    " §cor below to choose " + newClass.getDisplayName() + "."));
            }
            return false;
        }

        // Cost (only if changing from a non-wanderer class)
        if (!data.getPlayerClass().equals("WANDERER") &&
            !data.getPlayerClass().equals(newClass.name())) {
            if (!data.payFromWallet(CLASS_CHANGE_COST_AU)) {
                player.sendSystemMessage(Component.literal(
                    "§cClass change costs §e" + CLASS_CHANGE_COST_AU + " AU§c. Not enough funds."));
                return false;
            }
        }

        // Apply the class
        applyClass(player, data, newClass);
        return true;
    }

    private static void applyClass(ServerPlayer player, PlayerData data, PlayerClass newClass) {
        String oldClass = data.getPlayerClass();
        data.setPlayerClass(newClass.name());
        data.setClassLevel(1);
        data.addClassExperience(-data.getClassExperience()); // reset class xp

        // Apply stat bonuses
        double[] bonuses = newClass.getBaseStatBonuses();
        // bonuses: [maxHealth, maxEnergy, physAtk, magAtk, physDef, magDef]
        data.setMaxHealth(data.getMaxHealth() + bonuses[0]);
        data.setMaxEnergy(data.getMaxEnergy() + bonuses[1]);
        data.setPhysicalAttack(data.getPhysicalAttack() + bonuses[2]);
        data.setMagicAttack(data.getMagicAttack() + bonuses[3]);
        data.setPhysicalDefense(data.getPhysicalDefense() + bonuses[4]);
        data.setMagicDefense(data.getMagicDefense() + bonuses[5]);

        // Restore health
        data.setCurrentHealth(data.getMaxHealth());
        data.setCurrentEnergy(data.getMaxEnergy());

        // Notification
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l  ╔═══════════════════════════════╗"));
        player.sendSystemMessage(Component.literal("§6§l  ║   CLASS SELECTED!             ║"));
        player.sendSystemMessage(Component.literal("§e§l  ║   " + newClass.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7§l  ║   " + newClass.getDescription()));
        player.sendSystemMessage(Component.literal("§6§l  ╚═══════════════════════════════╝"));
        player.sendSystemMessage(Component.literal(""));

        if (bonuses[0] != 0)
            player.sendSystemMessage(Component.literal("§7  Max HP: §a+" + (int)bonuses[0]));
        if (bonuses[1] != 0)
            player.sendSystemMessage(Component.literal("§7  Max Energy: §b+" + (int)bonuses[1]));
        if (bonuses[2] != 0)
            player.sendSystemMessage(Component.literal("§7  Physical ATK: §c+" + (int)bonuses[2]));
        if (bonuses[3] != 0)
            player.sendSystemMessage(Component.literal("§7  Magic ATK: §d+" + (int)bonuses[3]));

        player.sendSystemMessage(Component.literal("§7Use §e/skills §7to see available skills for your class."));
    }

    /**
     * Returns a list of all classes the player can currently choose.
     */
    public static List<PlayerClass> getAvailableClasses(PlayerData data) {
        List<PlayerClass> available = new ArrayList<>();
        for (PlayerClass cls : PlayerClass.values()) {
            if (!cls.requiresQuest() && cls.isUnlockable(data.getKarma())) {
                available.add(cls);
            }
        }
        return available;
    }

    /**
     * Get the PlayerClass enum from the stored string.
     */
    public static PlayerClass getPlayerClass(PlayerData data) {
        try {
            return PlayerClass.valueOf(data.getPlayerClass());
        } catch (IllegalArgumentException e) {
            return PlayerClass.WANDERER;
        }
    }

    /**
     * Display class info to a player.
     */
    public static void showClassInfo(ServerPlayer player) {
        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        PlayerClass cls = getPlayerClass(data);

        player.sendSystemMessage(Component.literal("§6=== YOUR CLASS ==="));
        player.sendSystemMessage(Component.literal("§eClass: §f" + cls.getDisplayName() +
            " §7[" + cls.getType().name() + "]"));
        player.sendSystemMessage(Component.literal("§eClass Level: §f" + data.getClassLevel()));
        player.sendSystemMessage(Component.literal("§e" + cls.getDescription()));
        player.sendSystemMessage(Component.literal("§7Skills unlocked: §e" + data.getUnlockedSkills().size()));
        player.sendSystemMessage(Component.literal("§7Skill points available: §e" + data.getSkillPoints()));
    }
}
