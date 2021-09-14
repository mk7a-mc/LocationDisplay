package com.mk7a.locationdisplay;

import co.aikar.commands.PaperCommandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;


public final class LocationDisplayPlugin extends JavaPlugin implements Listener {

    public final HashMap<UUID, BukkitTask> locationUpdateTasks = new HashMap<>();

    private final NamespacedKey playerDisplayStateStorageKey = new NamespacedKey(this, "state");
    private final DisplayState[] stateReverseLookup = new DisplayState[]{DisplayState.OFF, DisplayState.ACTIONBAR, DisplayState.BOSSBAR};
    private static final int TICK_UPDATE_PERIOD = 1;

    @Override
    public void onEnable() {

        PaperCommandManager cmdManager = new PaperCommandManager(this);
        cmdManager.registerCommand(new LocationCommand(this));
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        // Destroy boss bars
        locationUpdateTasks.forEach((uuid, task) -> {
            task.cancel();
            NamespacedKey barKey = new NamespacedKey(this, uuid.toString());
            KeyedBossBar bar = this.getServer().getBossBar(barKey);
            if (bar != null) {
                bar.removeAll();
                this.getServer().removeBossBar(barKey);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        DisplayState restoredState = getPlayerDisplayState(player);
        if (restoredState == DisplayState.BOSSBAR) {
            createBossBarLocationDisplay(player);
        } else if (restoredState == DisplayState.ACTIONBAR) {
            createActionBarLocationDisplay(player);
        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {

        if (locationUpdateTasks.containsKey(event.getPlayer().getUniqueId())) {
            // will also destroy action bar
            destroyBossBarLocationDisplay(event.getPlayer());
        }
    }


    public void createBossBarLocationDisplay(Player player) {
        // ensure any left over old bar is deleted
        this.destroyBossBarLocationDisplay(player);

        BossBar bar = createBossBar(player);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                bar.setTitle(getLocationString(player));
            }
        }.runTaskTimer(this, 0, TICK_UPDATE_PERIOD);
        locationUpdateTasks.put(player.getUniqueId(), task);
    }

    private BossBar createBossBar(Player player) {

        KeyedBossBar bar = this.getServer().createBossBar(new NamespacedKey(this, player.getUniqueId().toString()),
                "Initializing...", BarColor.WHITE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(1);

        return bar;
    }

    public void destroyBossBarLocationDisplay(Player player) {
        UUID uuid = player.getUniqueId();

        NamespacedKey barKey = new NamespacedKey(this, uuid.toString());
        KeyedBossBar bar = this.getServer().getBossBar(barKey);
        if (bar != null) {
            bar.removeAll();
            this.getServer().removeBossBar(barKey);
        }

        if (locationUpdateTasks.containsKey(uuid)) {
            locationUpdateTasks.get(uuid).cancel();
            locationUpdateTasks.remove(uuid);
        }

    }

    public void createActionBarLocationDisplay(Player player) {
        var task = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendActionBar(Component.text(getLocationString(player)));
            }
        }.runTaskTimer(this, 0, TICK_UPDATE_PERIOD);
        locationUpdateTasks.put(player.getUniqueId(), task);
    }

    public void destroyActionBarLocationDisplay(Player player) {
        var uuid = player.getUniqueId();
        if (locationUpdateTasks.containsKey(uuid)) {
            locationUpdateTasks.get(uuid).cancel();
            locationUpdateTasks.remove(uuid);
        }
    }

    public DisplayState getPlayerDisplayState(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (!container.has(playerDisplayStateStorageKey, PersistentDataType.BYTE)) {
            return DisplayState.OFF;
        }
        Byte encodedState = player.getPersistentDataContainer().get(playerDisplayStateStorageKey, PersistentDataType.BYTE);
        return stateReverseLookup[encodedState];
    }

    public void setPlayerDisplayState(Player player, DisplayState newState) {
        int data = newState == DisplayState.ACTIONBAR ? 0b01 : newState == DisplayState.BOSSBAR ? 0b10 : 0b0;
        player.getPersistentDataContainer().set(playerDisplayStateStorageKey, PersistentDataType.BYTE, (byte) data);
    }



    private String getLocationString(Player player) {

        Location loc = player.getLocation();
        int hour = (int) ((loc.getWorld().getTime() / 1000) + 6) % 24;
        String time = hour + "H";
        String location = String.format("%6s %6s %6s",
                "&7X:&f" + loc.getBlockX(),
                "&7Y:&f" + loc.getBlockY(),
                "&7Z:&f" + loc.getBlockZ());

        var raw = String.format("%10s  &7|&f %2s  &7|&f  %s", location, yawToDirection(loc.getYaw()), time);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }


    private String yawToDirection(float yaw) {

        if (yaw >= 148.75 || yaw <= -148.75) return "N";
        if (yaw < 148.75 && yaw > 106.25) return "NW";
        if (yaw <= 106.25 && yaw >= 63.75) return "W";
        if (yaw < 63.75 && yaw > 21.5) return "SW";
        if (yaw <= 21.5 && yaw >= -21.5) return "S";
        if (yaw > -63.75 && yaw < -21.5) return "SE";
        if (yaw >= -106.25 && yaw <= -63.75) return "E";
            // (yaw > -148.75 && yaw > 106.25)
        else return "NE";
    }



}

