package com.yourname.godspears;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class GodSpearsPlugin extends JavaPlugin implements Listener {

    private final NamespacedKey spearKey = new NamespacedKey(this, "spear_type");
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> activeBloodSpears = new HashSet<>();
    private final Set<UUID> globalSpearTracker = new HashSet<>(); 

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        registerRecipes();
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String spear = getHeldSpearType(p);
                if (spear == null) continue;

                switch (spear) {
                    case "emerald" -> p.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 40, 0, false, false));
                    case "ender" -> {
                        if (p.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 4, false, false));
                        }
                    }
                }
            }
        }, 0L, 20L);
    }

    private String getHeldSpearType(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main.hasItemMeta()) {
            return main.getItemMeta().getPersistentDataContainer().get(spearKey, PersistentDataType.STRING);
        }
        return null;
    }

    private void registerRecipes() {
        createSpearRecipe("emerald", Material.EMERALD_BLOCK, " Emerald Spear", "§a");
        createSpearRecipe("saturation", Material.GOLD_BLOCK, " Saturation Spear", "§e");
        createSpearRecipe("speed", Material.DIAMOND_BLOCK, " Speed Spear", "§b");
        createSpearRecipe("sculk", Material.SCULK_CATALYST, " Sculk Spear", "§1");
        createSpearRecipe("blood", Material.REDSTONE_BLOCK, " Blood Spear", "§c");
        createSpearRecipe("void", Material.PACKED_ICE, " Void Spear", "§3");
        createSpearRecipe("heart", Material.NETHER_WART_BLOCK, " Heart Spear", "§d");
        createSpearRecipe("wither", Material.WITHER_SKELETON_SKULL, " Wither Spear", "§8");
        createSpearRecipe("ender", Material.DRAGON_EGG, " Ender Spear", "§0");
    }

    private void createSpearRecipe(String id, Material core, String name, String color) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + name);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(spearKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, id + "_spear"), item);
        recipe.shape("  C", " S ", "S  ");
        recipe.setIngredient('C', core);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        ItemStack result = e.getRecipe().getResult();
        if (!result.hasItemMeta()) return;
        String type = result.getItemMeta().getPersistentDataContainer().get(spearKey, PersistentDataType.STRING);
        
        if (type != null) {
            if (globalSpearTracker.contains(UUID.nameUUIDFromBytes(type.getBytes()))) {
                e.setCancelled(true);
                e.getWhoClicked().sendMessage("§cThis unique artifact already exists in this universe.");
            } else {
                globalSpearTracker.add(UUID.nameUUIDFromBytes(type.getBytes()));
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!e.isSneaking()) return;
        String spear = getHeldSpearType(p);
        if (spear == null) return;

        long now = System.currentTimeMillis();
        if (cooldowns.getOrDefault(p.getUniqueId(), 0L) > now) {
            p.sendMessage("§cYour weapon's soul energy is recharging.");
            return;
        }

        boolean castSuccessful = executeAbility(p, spear);
        if (castSuccessful) {
            cooldowns.put(p.getUniqueId(), now + 15000); 
        }
    }

    private boolean executeAbility(Player p, String type) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection();

        switch (type) {
            case "speed" -> {
                p.setVelocity(dir.multiply(2.2).setY(0.4));
                p.getWorld().spawnParticle(Particle.CLOUD, loc, 15, 0.2, 0.2, 0.2, 0.1);
                p.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, 1f, 1.5f);
                return true;
            }
            case "sculk" -> {
                Warden warden = (Warden) p.getWorld().spawnEntity(loc.add(dir.multiply(3)), EntityType.WARDEN);
                warden.setCustomName("§1" + p.getName() + "'s Stalker");
                Bukkit.getScheduler().runTaskLater(this, warden::remove, 1200L); 
                return true;
            }
            case "blood" -> {
                activeBloodSpears.add(p.getUniqueId());
                p.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
                Bukkit.getScheduler().runTaskLater(this, () -> activeBloodSpears.remove(p.getUniqueId()), 1200L);
                return true;
            }
            case "void" -> {
                p.getNearbyEntities(12, 12, 12).stream()
                        .filter(entity -> entity instanceof LivingEntity && entity != p)
                        .forEach(entity -> {
                            LivingEntity le = (LivingEntity) entity;
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, false));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false));
                        });
                p.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                return true;
            }
            case "heart" -> {
                RayTraceResult ray = p.getWorld().rayTraceEntities(p.getEyeLocation(), dir, 30, entity -> entity != p);
                if (ray != null && ray.getHitEntity() instanceof LivingEntity target) {
                    target.damage(10.0, p); 
                    p.getWorld().spawnParticle(Particle.HEART, target.getLocation(), 5);
                }
                return true;
            }
            case "wither" -> {
                for (int i = 0; i < 3; i++) {
                    WitherSkull skull = p.launchProjectile(WitherSkull.class);
                    skull.setVelocity(dir.multiply(1.5));
                }
                return true;
            }
            case "ender" -> {
                Location targetLoc = loc.add(dir.multiply(50));
                Block b = targetLoc.getBlock();
                if (!b.getType().isSolid()) {
                    p.teleport(targetLoc);
                    p.getWorld().playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    return true;
                }
                p.sendMessage("§cDestination mass blocked execution.");
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager) {
            if (activeBloodSpears.contains(damager.getUniqueId())) {
                e.setDamage(e.getDamage() * 1.5); 
                damager.getWorld().spawnParticle(Particle.CRIT, e.getEntity().getLocation(), 10);
            }
        }
    }

    @EventHandler
    public void onItemDestroy(EntityDamageEvent e) {
        if (e.getEntityType() == EntityType.ITEM) {
            Item itemEntity = (Item) e.getEntity();
            ItemStack stack = itemEntity.getItemStack();
            if (stack.hasItemMeta() && stack.getItemMeta().getPersistentDataContainer().has(spearKey, PersistentDataType.STRING)) {
                e.setCancelled(true);
                e.getEntity().setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if ("saturation".equals(getHeldSpearType(p))) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                p.setFoodLevel(20);
                p.setSaturation(20f);
            }, 1L);
        }
    }
}
