/*
 *    __  __ _        _____ _                
 *   |  \/  (_)_ __  |_   _(_)_ __ ___   ___ 
 *   | |\/| | | '__|   | | | | '_ ` _ \ / _ \
 *   | |  | | | |      | | | | | | | | |  __/
 *   |_|  |_|_|_|      |_| |_|_| |_| |_|\___|
 *                                           
 *   Project: Mir Time | Author: Ars3nBar
 */
package ru.mirtime.playtime;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MirTimePlaytime extends JavaPlugin implements CommandExecutor, Listener {

    private File dataFile;
    private YamlConfiguration db;
    private final Map<Integer, String> slotToReward = new HashMap<>();
    private final Set<String> notifiedCache = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "claimed.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        db = YamlConfiguration.loadConfiguration(dataFile);

        if (getCommand("playtime") != null) getCommand("playtime").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                ConfigurationSection sec = getConfig().getConfigurationSection("rewards");
                if (sec == null) return;
                String prefix = getConfig().getString("prefix", "&#0055ff&l⚡ &#00E5FF&lMIR-TIME &8» ");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    long playedMins = getPlayedMinutes(p);
                    List<String> claimed = db.getStringList(p.getUniqueId().toString());
                    for (String key : sec.getKeys(false)) {
                        if (claimed.contains(key)) continue;
                        long req = sec.getLong(key + ".minutes", 10);
                        String cacheId = p.getUniqueId() + ":" + key;
                        if (playedMins >= req && notifiedCache.add(cacheId)) {
                            TextComponent msg = new TextComponent(color(prefix + "&fВам доступна награда за онлайн: " + sec.getString(key + ".name", key) + " "));
                            TextComponent btn = new TextComponent(color("&#00FF66&l[🎁 ЗАБРАТЬ]"));
                            btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/playtime"));
                            p.spigot().sendMessage(msg, btn);
                            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 600L, 600L);

        Bukkit.getConsoleSender().sendMessage(color("&#0055ff   __  __ _        _____ _                "));
        Bukkit.getConsoleSender().sendMessage(color("&#0055ff  |  \\/  (_)_ __  |_   _(_)_ __ ___   ___ "));
        Bukkit.getConsoleSender().sendMessage(color("&#00E5FF  | |\\/| | | '__|   | | | | '_ ` _ \\ / _ \\"));
        Bukkit.getConsoleSender().sendMessage(color("&#00E5FF  | |  | | | |      | | | | | | | | |  __/"));
        Bukkit.getConsoleSender().sendMessage(color("&#00FF66  |_|  |_|_|_|      |_| |_|_| |_| |_|\\___|"));
        Bukkit.getConsoleSender().sendMessage(color("&#00E5FF  MirTimePlaytime v1.0.0 успешно запущен!"));
    }

    private void saveDb() {
        try { db.save(dataFile); } catch (IOException ignored) {}
    }

    private long getPlayedMinutes(Player p) {
        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return ticks / 1200L;
    }

    private String formatMinutes(long totalMins) {
        long hours = totalMins / 60;
        long mins = totalMins % 60;
        if (hours > 0) return hours + " ч. " + mins + " мин.";
        return mins + " мин.";
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length > 0 && List.of(args).get(0).equalsIgnoreCase("reload") && p.hasPermission("op")) {
            reloadConfig();
            p.sendMessage(color(getConfig().getString("prefix") + "&#00FF66Конфиг MirTimePlaytime перезагружен!"));
            return true;
        }
        openMenu(p);
        return true;
    }

    private void openMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l⚡ &#00E5FF&lНАГРАДЫ ЗА ВРЕМЯ В ИГРЕ"));
        ItemStack blue = item(Material.BLUE_STAINED_GLASS_PANE, "&#0055ff▪", null);
        ItemStack cyan = item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&#00E5FF▫", null);
        for (int s : new int[]{0,1,7,8,9,17,18,26,27,35,36,37,43,44}) inv.setItem(s, blue);
        for (int s : new int[]{2,3,5,6,38,39,41,42}) inv.setItem(s, cyan);

        long playedMins = getPlayedMinutes(p);
        List<String> claimed = db.getStringList(p.getUniqueId().toString());

        inv.setItem(4, item(Material.CLOCK, "&#00E5FF&lВАШ ОБЩИЙ ОНЛАЙН", List.of(
                " &#0055ff● &fНаиграно всего: &#00FF66" + formatMinutes(playedMins),
                " &#0055ff● &fПолучено наград: &#00E5FF" + claimed.size() + " шт.",
                "",
                "&7Играйте на сервере, чтобы открывать новые этапы!"
        )));

        slotToReward.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("rewards");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                int slot = sec.getInt(key + ".slot", 10);
                long reqMins = sec.getLong(key + ".minutes", 10);
                boolean isClaimed = claimed.contains(key);
                boolean isReady = !isClaimed && playedMins >= reqMins;

                Material mat = isClaimed ? Material.GRAY_DYE : Material.matchMaterial(sec.getString(key + ".material", "GOLD_INGOT"));
                if (mat == null) mat = Material.GOLD_INGOT;

                List<String> lore = new ArrayList<>(sec.getStringList(key + ".lore"));
                lore.add("");
                lore.add("&fПрогресс: &#00E5FF" + formatMinutes(Math.min(playedMins, reqMins)) + " &8/ &#00FF66" + formatMinutes(reqMins));
                if (isClaimed) {
                    lore.add("&7[✔ НАГРАДА УЖЕ ПОЛУЧЕНА]");
                } else if (isReady) {
                    lore.add("&#00FF66&l[🎁 НАЖМИТЕ, ЧТОБЫ ЗАБРАТЬ!]");
                } else {
                    lore.add("&e[⏳ ОСТАЛОСЬ: " + formatMinutes(reqMins - playedMins) + "]");
                }

                inv.setItem(slot, item(mat, sec.getString(key + ".name", key), lore));
                slotToReward.put(slot, key);
            }
        }

        inv.setItem(40, item(Material.BARRIER, "&c&l[✖ Закрыть меню]", null));
        p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String clean = ChatColor.stripColor(e.getView().getTitle());
        if (!clean.contains("НАГРАДЫ ЗА ВРЕМЯ")) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == 40) { p.closeInventory(); return; }

        String key = slotToReward.get(slot);
        if (key == null) return;

        long playedMins = getPlayedMinutes(p);
        long reqMins = getConfig().getLong("rewards." + key + ".minutes", 10);
        List<String> claimed = new ArrayList<>(db.getStringList(p.getUniqueId().toString()));

        if (claimed.contains(key) || playedMins < reqMins) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        claimed.add(key);
        db.set(p.getUniqueId().toString(), claimed);
        saveDb();

        for (String cmd : getConfig().getStringList("rewards." + key + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }

        String prefix = color(getConfig().getString("prefix", "&#0055ff&l⚡ &#00E5FF&lMIR-TIME &8» "));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.sendMessage(prefix + color("&#00FF66Вы получили награду: " + getConfig().getString("rewards." + key + ".name", key) + "&#00FF66!"));
        openMenu(p);
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> list = new ArrayList<>();
                for (String s : lore) list.add(color(s));
                meta.setLore(list);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            is.setItemMeta(meta);
        }
        return is;
    }

    public static String color(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            matcher.appendReplacement(buffer, rep.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
