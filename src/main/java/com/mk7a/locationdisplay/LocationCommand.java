package com.mk7a.locationdisplay;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("locationdisplay|ld")
@Description("Toggle location display")
public class LocationCommand extends BaseCommand {

    private final LocationDisplayPlugin plugin;

    public LocationCommand(LocationDisplayPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @HelpCommand
    public void onDefault(Player player) {
        player.sendMessage("/locationdisplay|ld <bossbar|b/actionbar|a>");
    }

    @Subcommand("bossbar|b")
    @CommandPermission("locationdisplay.bossbar")
    public void bossbarToggle(Player player) {
        DisplayState currentState = plugin.getPlayerDisplayState(player);
        if (currentState == DisplayState.BOSSBAR) {
            plugin.destroyBossBarLocationDisplay(player);
            plugin.setPlayerDisplayState(player, DisplayState.OFF);
            sendStateUpdateMsg(player, "OFF");
        } else {
            if (currentState == DisplayState.ACTIONBAR) {
                plugin.destroyActionBarLocationDisplay(player);
            }
            plugin.createBossBarLocationDisplay(player);
            plugin.setPlayerDisplayState(player, DisplayState.BOSSBAR);
            sendStateUpdateMsg(player, "BOSSBAR");
        }
    }

    @Subcommand("actionbar|a")
    @CommandPermission("locationdisplay.actionbar")
    public void actionbarToggle(Player player) {
        DisplayState currentState = plugin.getPlayerDisplayState(player);
        if (currentState == DisplayState.ACTIONBAR) {
            plugin.destroyActionBarLocationDisplay(player);
            plugin.setPlayerDisplayState(player, DisplayState.OFF);
            sendStateUpdateMsg(player, "OFF");
        } else {
            if (currentState == DisplayState.BOSSBAR) {
                plugin.destroyBossBarLocationDisplay(player);
            }
            plugin.createActionBarLocationDisplay(player);
            plugin.setPlayerDisplayState(player, DisplayState.ACTIONBAR);
            sendStateUpdateMsg(player, "ACTIONBAR");
        }
    }


    private void sendStateUpdateMsg(Player player, String newState) {
        player.sendMessage(ChatColor.GRAY + "Location display set to: " + ChatColor.GOLD + newState);
    }


}
