package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private static final String MENU_TITLE = "✦ Rift Scrolls ✦";
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private NamespacedKey scrollKey;
    private NamespacedKey typeKey;

    private enum Scroll {
        EMBER("Ember", Material.BLAZE_POWDER, "Scorch nearby foes in a burst of flame.", 12),
        FROST("Frost", Material.PRISMARINE_CRYSTALS, "Chill nearby foes and slow their advance.", 14),
        STORM("Storm", Material.AMETHYST_SHARD, "Blink forward in a crackle of lightning.", 10),
        VITAL("Vital", Material.GLOWSTONE_DUST, "Restore health and gain brief regeneration.", 18),
        VOID("Void", Material.ENDER_EYE, "Pull nearby foes toward your position.", 16);

        final String title;
        final Material material;
        final String description;
        final int cooldown;
        Scroll(String title, Material material, String description, int cooldown) {
            this.title = title; this.material = material; this.description = description; this.cooldown = cooldown;
        }
        static Scroll parse(String value) {
            try { return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { return null; }
        }
    }

    @Override public void onEnable() {
        scrollKey = new NamespacedKey(this, "rift_scroll");
        typeKey = new NamespacedKey(this, "scroll_type");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RiftScrolls enabled.");
    }

    @Override public void onDisable() { cooldowns.clear(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Use /scroll list or /scroll give <player> <type>."); return true; }
            openMenu(player); return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(Component.text("Available scrolls: Ember, Frost, Storm, Vital, Void", NamedTextColor.LIGHT_PURPLE)); return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length == 3) {
            if (!sender.hasPermission("riftscrolls.admin")) { sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED)); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            Scroll scroll = Scroll.parse(args[2]);
            if (target == null || scroll == null) { sender.sendMessage(Component.text("Player not found or invalid type. Use /scroll list.", NamedTextColor.RED)); return true; }
            target.getInventory().addItem(createScroll(scroll)).values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            target.sendMessage(Component.text("You received a " + scroll.title + " Scroll!", NamedTextColor.LIGHT_PURPLE));
            sender.sendMessage(Component.text("Gave " + scroll.title + " Scroll to " + target.getName() + ".", NamedTextColor.GREEN)); return true;
        }
        sender.sendMessage(Component.text("Usage: /scroll [list|give <player> <type>]", NamedTextColor.GRAY)); return true;
    }

    private void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(MENU_TITLE, NamedTextColor.DARK_PURPLE));
        Scroll[] values = Scroll.values();
        int[] slots = {10, 11, 12, 13, 14};
        for (int i = 0; i < values.length; i++) inventory.setItem(slots[i], createScroll(values[i]));
        player.openInventory(inventory);
    }

    private ItemStack createScroll(Scroll scroll) {
        ItemStack item = new ItemStack(scroll.material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✦ " + scroll.title + " Scroll ✦", NamedTextColor.LIGHT_PURPLE));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(scroll.description, NamedTextColor.GRAY));
        lore.add(Component.text("Right-click to unleash", NamedTextColor.GOLD));
        lore.add(Component.text("Cooldown: " + scroll.cooldown + " seconds", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(scrollKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, scroll.name());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text(MENU_TITLE, NamedTextColor.DARK_PURPLE))) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta != null) {
                    Scroll scroll = Scroll.parse(meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING));
                    if (scroll != null) { player.getInventory().addItem(createScroll(scroll)); player.sendMessage(Component.text("Scroll added to your inventory.", NamedTextColor.GREEN)); }
                }
            }
        }
    }

    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(scrollKey, PersistentDataType.BYTE)) return;
        Scroll scroll = Scroll.parse(meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING));
        if (scroll == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (ready > now) { player.sendMessage(Component.text("Ready in " + ((ready - now + 999) / 1000) + "s", NamedTextColor.GRAY)); return; }
        cooldowns.put(player.getUniqueId(), now + scroll.cooldown * 1000L);
        activate(player, scroll);
        if (item.getAmount() <= 1) player.getInventory().setItemInMainHand(null); else item.setAmount(item.getAmount() - 1);
    }

    private void activate(Player player, Scroll scroll) {
        var location = player.getLocation();
        switch (scroll) {
            case EMBER -> {
                player.getWorld().spawnParticle(Particle.FLAME, location, 45, 2.2, 1, 2.2, 0.04);
                player.getWorld().playSound(location, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                for (Entity entity : player.getNearbyEntities(4, 3, 4)) if (entity instanceof LivingEntity target && !target.equals(player)) { target.damage(5.0, player); target.setFireTicks(60); }
            }
            case FROST -> {
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 55, 3, 1.5, 3, 0.02);
                player.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1f, 0.7f);
                for (Entity entity : player.getNearbyEntities(5, 3, 5)) if (entity instanceof LivingEntity target && !target.equals(player)) { target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2)); target.damage(2.0, player); }
            }
            case STORM -> {
                Vector direction = location.getDirection().normalize().multiply(6);
                for (int i = 0; i < 6; i++) { var point = location.clone().add(direction.clone().multiply(i / 6.0)); if (!point.getBlock().isPassable() || !point.clone().add(0, 1, 0).getBlock().isPassable()) break; player.teleport(point); }
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 35, 0.7, 1, 0.7, 0.15);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
            }
            case VITAL -> {
                var attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double max = attribute == null ? 20.0 : attribute.getValue();
                player.setHealth(Math.min(max, player.getHealth() + 8));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 30, 1, 1, 1, 0.1);
                player.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
            case VOID -> {
                player.getWorld().spawnParticle(Particle.PORTAL, location, 70, 3, 1.5, 3, 0.2);
                player.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                for (Entity entity : player.getNearbyEntities(8, 5, 8)) if (entity instanceof LivingEntity target && !target.equals(player)) { Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.3); pull.setY(0.35); target.setVelocity(pull); }
            }
        }
        player.sendMessage(Component.text(scroll.title + " Scroll unleashed!", NamedTextColor.LIGHT_PURPLE));
    }
}
