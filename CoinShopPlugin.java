package com.mcforge.coinshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class CoinShopPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<Integer, String> menuSlots = new HashMap<>();
    private Inventory mainMenu;
    private String mainTitle;

    @Override public void onEnable() {
        saveDefaultConfig(); loadBalances();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String c : List.of("coinshop", "coin", "tags", "colors")) if (getCommand(c) != null) getCommand(c).setExecutor(this);
        buildMainMenu();
        getLogger().info("MCForgeCoinShop enabled");
    }
    @Override public void onDisable() { saveBalances(); }
    private File balanceFile() { return new File(getDataFolder(), "balances.yml"); }
    private void loadBalances() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(balanceFile());
        ConfigurationSection s = y.getConfigurationSection("balances"); if (s == null) return;
        for (String id : s.getKeys(false)) try { balances.put(UUID.fromString(id), s.getLong(id)); } catch (Exception ignored) {}
    }
    private void saveBalances() {
        YamlConfiguration y = new YamlConfiguration(); balances.forEach((id, n) -> y.set("balances." + id, n));
        try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); y.save(balanceFile()); } catch (Exception e) { getLogger().warning(e.getMessage()); }
    }
    private long balance(UUID id) { return balances.getOrDefault(id, 0L); }
    private void setBalance(UUID id, long amount) { balances.put(id, Math.max(0, amount)); saveBalances(); }
    private String cc(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    private ItemStack item(Material m, String name, List<String> lore) {
        ItemStack stack = new ItemStack(m); ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(cc(name));
        if (lore != null) meta.setLore(lore.stream().map(this::cc).toList()); stack.setItemMeta(meta); return stack;
    }
    private void buildMainMenu() {
        mainTitle = cc(getConfig().getString("shop-title", "&8MC Forge CoinShop"));
        mainMenu = Bukkit.createInventory(null, 27, mainTitle); menuSlots.clear();
        ConfigurationSection cats = getConfig().getConfigurationSection("categories"); if (cats == null) return;
        int[] slots = {10, 12, 14, 16, 22}; int idx = 0;
        for (String cat : cats.getKeys(false)) {
            if (idx >= slots.length) break;
            ConfigurationSection c = cats.getConfigurationSection(cat);
            Material mat = Material.matchMaterial(c.getString("icon", "CHEST")); if (mat == null) mat = Material.CHEST;
            int slot = slots[idx++]; mainMenu.setItem(slot, item(mat, c.getString("title", cat), List.of("&7Open category"))); menuSlots.put(slot, cat);
        }
    }
    private void openCategory(Player p, String category) {
        ConfigurationSection items = getConfig().getConfigurationSection("categories." + category + ".items");
        if (items == null) { p.sendMessage(cc("&cThis category has no items.")); return; }
        String title = cc(getConfig().getString("categories." + category + ".title", category));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        int slot = 0;
        for (String key : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(key);
            Material m = Material.matchMaterial(it.getString("material", "PAPER")); if (m == null) m = Material.PAPER;
            inv.setItem(slot++, item(m, it.getString("display", key), List.of("&ePrice: &6" + it.getLong("price") + " Coins", "&aClick to purchase", "&8ShopKey: " + category + "/" + key)));
            if (slot >= inv.getSize()) break;
        }
        p.openInventory(inv);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String c = command.getName().toLowerCase(Locale.ROOT);
        if (c.equals("coinshop")) { if (sender instanceof Player p) p.openInventory(mainMenu); else sender.sendMessage("Players only."); return true; }
        if (c.equals("tags") || c.equals("colors")) { if (sender instanceof Player p) openCategory(p, c); else sender.sendMessage("Players only."); return true; }
        if (c.equals("coin")) {
            if (args.length == 0) { sender.sendMessage(cc("&e/coin balance | give | take | set | check | giveall")); return true; }
            if (args[0].equalsIgnoreCase("balance") && sender instanceof Player p) { p.sendMessage(cc("&6Balance: &e" + balance(p.getUniqueId()) + " Coins")); return true; }
            if (!sender.isOp()) { sender.sendMessage(cc("&cOP only.")); return true; }
            if (args[0].equalsIgnoreCase("giveall") && args.length >= 2) { long n = parse(args[1]); if (n < 0) return invalid(sender); for (Player p : Bukkit.getOnlinePlayers()) setBalance(p.getUniqueId(), balance(p.getUniqueId()) + n); sender.sendMessage("Coins given to online players."); return true; }
            if (args.length < 3) { sender.sendMessage("/coin give|take|set|check <player> <amount>"); return true; }
            Player target = Bukkit.getPlayer(args[1]); if (target == null) { sender.sendMessage("Player must be online."); return true; }
            long n = parse(args[2]); if (n < 0) return invalid(sender);
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> setBalance(target.getUniqueId(), balance(target.getUniqueId()) + n);
                case "take" -> setBalance(target.getUniqueId(), balance(target.getUniqueId()) - n);
                case "set" -> setBalance(target.getUniqueId(), n);
                case "check" -> { sender.sendMessage(target.getName() + " balance: " + balance(target.getUniqueId())); return true; }
                default -> { sender.sendMessage("Unknown subcommand."); return true; }
            }
            sender.sendMessage("Updated " + target.getName() + " balance."); return true;
        }
        return true;
    }
    private boolean invalid(CommandSender s) { s.sendMessage("Invalid amount."); return true; }
    private long parse(String s) { try { return Long.parseLong(s); } catch (Exception e) { return -1; } }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(mainTitle)) { e.setCancelled(true); if (e.getWhoClicked() instanceof Player p) { String cat = menuSlots.get(e.getRawSlot()); if (cat != null) openCategory(p, cat); } return; }
        ConfigurationSection cats = getConfig().getConfigurationSection("categories"); if (cats == null) return;
        String matched = null;
        for (String cat : cats.getKeys(false)) if (e.getView().getTitle().equals(cc(getConfig().getString("categories." + cat + ".title", cat)))) { matched = cat; break; }
        if (matched == null) return;
        e.setCancelled(true); if (!(e.getWhoClicked() instanceof Player p) || e.getCurrentItem() == null) return;
        ItemMeta meta = e.getCurrentItem().getItemMeta(); if (meta == null || meta.getLore() == null) return;
        String marker = meta.getLore().stream().map(ChatColor::stripColor).filter(l -> l.startsWith("ShopKey: ")).findFirst().orElse(null); if (marker == null) return;
        String[] path = marker.substring(9).split("/", 2); if (path.length != 2) return;
        ConfigurationSection it = getConfig().getConfigurationSection("categories." + path[0] + ".items." + path[1]); if (it == null) return;
        long price = it.getLong("price"); if (balance(p.getUniqueId()) < price) { p.sendMessage(cc("&cNot enough Coins.")); return; }
        setBalance(p.getUniqueId(), balance(p.getUniqueId()) - price);
        for (String cmd : it.getStringList("commands")) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        p.sendMessage(cc("&aPurchase successful! &e-" + price + " Coins")); p.closeInventory();
    }
}
