package com.starshooterstudios.fletcher;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FletchingTableFunctionality extends JavaPlugin implements Listener {
    private NamespacedKey noTakeKey;
    private NamespacedKey tippedIdentifierKey;
    private NamespacedKey headKey;
    private NamespacedKey hiltKey;
    private NamespacedKey featherKey;
    private NamespacedKey forceKey;
    private NamespacedKey arrowDataKey;
    private NamespacedKey powerKey;
    private ItemStack empty;
    private ItemStack emptyInvalidTip;
    private ItemStack errorIcon;
    private ItemStack tippedIdentifierEmpty;
    private ItemStack head;
    private ItemStack potion;
    private ItemStack stick;
    private ItemStack feather;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initialize(this);
    }

    public void initialize(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        noTakeKey = new NamespacedKey(plugin, "no-take-slot");
        tippedIdentifierKey = new NamespacedKey(plugin, "tipped-identifier");
        headKey = new NamespacedKey(plugin, "head-id");
        hiltKey = new NamespacedKey(plugin, "hilt-id");
        featherKey = new NamespacedKey(plugin, "feather-count");
        arrowDataKey = new NamespacedKey(plugin, "arrow-data");
        powerKey = new NamespacedKey(plugin, "arrow-power");

        forceKey = new NamespacedKey(plugin, "arrow-force");

        empty = makeSlot(1);
        errorIcon = makeSlot(5);
        emptyInvalidTip = makeSlot(1);
        ItemMeta eMeta = emptyInvalidTip.getItemMeta();
        eMeta.setHideTooltip(false);
        eMeta.displayName(Component.text("Flint is required for a Tipped Arrow").decoration(TextDecoration.ITALIC, false));
        emptyInvalidTip.setItemMeta(eMeta);
        tippedIdentifierEmpty = makeSlot(1);
        ItemMeta meta = tippedIdentifierEmpty.getItemMeta();
        meta.getPersistentDataContainer().set(tippedIdentifierKey, PersistentDataType.BOOLEAN, true);
        tippedIdentifierEmpty.setItemMeta(meta);
        head = makeSlot(1);
        potion = makeSlot(2);
        stick = makeSlot(3);
        feather = makeSlot(4);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getType().equals(Material.FLETCHING_TABLE)) return;
        if (!event.getAction().isRightClick()) return;
        if (event.getPlayer().isSneaking()) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR
                    || event.getPlayer().getInventory().getItemInOffHand().getType() != Material.AIR) return;
        }
        event.setCancelled(true);
        event.getPlayer().swingMainHand();
        Inventory inventory = CustomGUI.createInventory(
                CustomGUI.CustomInventoryType.FLETCHING_TABLE,
                27,
                Component.text().append(Component.text("\uF000\uE000\uF001").color(NamedTextColor.WHITE).font(Key.key("supernova:fletching_table"))).append(Component.text("Fletching Table").font(Key.key("minecraft:default"))).build()
        );

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, empty);
        }

        inventory.setItem(1, head);
        inventory.setItem(3, potion);
        inventory.setItem(10, stick);
        inventory.setItem(19, feather);
        inventory.setItem(20, feather);
        inventory.setItem(21, feather);

        event.getPlayer().openInventory(inventory);
    }

    @SuppressWarnings("UnstableApiUsage")
    public ItemStack makeSlot(int num) {
        ItemStack slot = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = slot.getItemMeta();
        meta.setHideTooltip(true);
        CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        comp.setStrings(Collections.singletonList(String.valueOf(num)));
        meta.setCustomModelDataComponent(comp);
        meta.getPersistentDataContainer().set(noTakeKey, PersistentDataType.BOOLEAN, true);
        slot.setItemMeta(meta);
        return slot;
    }

    public boolean isArrow(Material material) {
        return material.equals(Material.ARROW) || material.equals(Material.TIPPED_ARROW);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getPersistentDataContainer().has(noTakeKey)) event.setCancelled(true);
        if (event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof CustomGUI gui) {
            if (gui.getInventoryType().equals(CustomGUI.CustomInventoryType.FLETCHING_TABLE)) {
                if (gui.getInventory().equals(event.getClickedInventory()) && event.getSlot() == 16) {
                    if (event.getClick().isShiftClick()) {
                        event.setCancelled(true);
                        if (event.getCurrentItem() != null && !event.getCurrentItem().getPersistentDataContainer().has(noTakeKey)) {
                            int it = event.getCurrentItem().getAmount();
                            for (ItemStack i : event.getWhoClicked().getInventory().addItem(event.getCurrentItem()).values()) {
                                if (i.getAmount() == it) return;
                                ((CraftPlayer) event.getWhoClicked()).getHandle().drop(CraftItemStack.asNMSCopy(i), true);
                            }
                            event.setCurrentItem(null);
                        }
                    }
                    if (event.getClick().equals(ClickType.DROP)) {
                        event.setCancelled(true);
                        if (event.getCurrentItem() != null) {
                            ItemStack item = event.getCurrentItem();
                            ((CraftPlayer) event.getWhoClicked()).getHandle().drop(CraftItemStack.asNMSCopy(item), true);
                            event.setCurrentItem(null);
                        }
                    }
                    if (!event.getCursor().getType().equals(Material.AIR)) {
                        ItemStack currentItem = event.getCurrentItem();
                        if (currentItem != null && event.getCursor().getType().equals(currentItem.getType()) && event.getCursor().getItemMeta().equals(currentItem.getItemMeta())) {
                            if (currentItem.getAmount() + event.getCursor().getAmount() < event.getCursor().getMaxStackSize()) {
                                event.getCursor().setAmount(event.getCursor().getAmount() + currentItem.getAmount());
                                event.setCurrentItem(null);
                            }
                        }
                        event.setCancelled(true);
                    } else if (event.getClick().isRightClick() && !event.getClick().isShiftClick()) {
                        event.setCancelled(true);
                        event.getWhoClicked().setItemOnCursor(event.getCurrentItem());
                        event.setCurrentItem(null);
                    }
                }
                if (event.getClick().isShiftClick()) {
                    if (event.getCurrentItem() != null) {
                        if (isArrow(event.getCurrentItem().getType()) && !gui.getInventory().equals(event.getClickedInventory())) event.setCancelled(true);
                    }
                }

                if (event.getClick().equals(ClickType.DOUBLE_CLICK)) {
                    ItemStack cursor = event.getCursor();
                    if (isArrow(cursor.getType())) {
                        event.setCancelled(true);
                        int i = 64 - cursor.getAmount();
                        for (ItemStack item : event.getWhoClicked().getInventory()) {
                            if (item == null) continue;
                            if (!item.getType().equals(cursor.getType())) continue;
                            if (item.getItemMeta().equals(cursor.getItemMeta())) {
                                i -= item.getAmount();
                                cursor.setAmount(Math.min(cursor.getMaxStackSize(), cursor.getAmount() + item.getAmount()));
                                if (i < 0) {
                                    item.setAmount(Math.abs(i));
                                } else {
                                    item.setAmount(0);
                                }
                                if (i <= 0) break;
                            }
                        }
                    }
                }

                checkSlot(event, gui, 1, List.of(Material.FLINT, Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.GLOWSTONE_DUST, Material.ECHO_SHARD, Material.ENDER_PEARL, Material.SLIME_BALL), empty);
                checkSlot(event, gui, 3, List.of(Material.SPLASH_POTION), potion);
                checkSlot(event, gui, 10, List.of(Material.STICK, Material.BREEZE_ROD, Material.BLAZE_ROD), stick);
                checkSlot(event, gui, 19, List.of(Material.FEATHER), feather);
                checkSlot(event, gui, 20, List.of(Material.FEATHER), feather);
                checkSlot(event, gui, 21, List.of(Material.FEATHER), feather);

                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    int f = Math.min(isFilled(gui, 1), isFilled(gui, 10));

                    int feathers = 0;
                    if (f > 0) {
                        int f19 = isFilled(gui, 19);
                        if (f19 > 0) {
                            feathers++;
                            f = Math.min(f, f19);
                            int f20 = isFilled(gui, 20);
                            if (f20 > 0) {
                                feathers++;
                                f = Math.min(f, f20);
                            }
                            int f21 = isFilled(gui, 21);
                            if (f21 > 0) {
                                feathers++;
                                f = Math.min(f, f21);
                            }
                        }
                    }

                    ItemStack output = gui.getInventory().getItem(16);
                    if (output == null || output.getType().equals(Material.AIR)) {
                        reduceSlot(gui, 1, empty, f);
                        ItemStack i = gui.getInventory().getItem(0);
                        if (i != null && i.getPersistentDataContainer().has(tippedIdentifierKey)) reduceSlot(gui, 3, potion, f);
                        reduceSlot(gui, 10, stick, f);
                        reduceSlot(gui, 19, feather, f);
                        reduceSlot(gui, 20, feather, f);
                        reduceSlot(gui, 21, feather, f);

                        gui.getInventory().setItem(16, empty);

                        f = Math.min(isFilled(gui, 1), isFilled(gui, 10));

                        feathers = 0;
                        if (f > 0) {
                            int f19 = isFilled(gui, 19);
                            if (f19 > 0) {
                                feathers++;
                                f = Math.min(f, f19);
                                int f20 = isFilled(gui, 20);
                                if (f20 > 0) {
                                    feathers++;
                                    f = Math.min(f, f20);
                                }
                                int f21 = isFilled(gui, 21);
                                if (f21 > 0) {
                                    feathers++;
                                    f = Math.min(f, f21);
                                }
                            }
                        }
                    }

                    ItemStack item = gui.getInventory().getItem(1);
                    boolean isTipped = item != null && isFilled(gui, 3) != 0;
                    if (isTipped && !item.getType().equals(Material.FLINT) && !item.getType().equals(Material.LIME_STAINED_GLASS_PANE)) {
                        gui.getInventory().setItem(16, empty);

                        gui.getInventory().setItem(14, emptyInvalidTip);
                        gui.getInventory().setItem(13, emptyInvalidTip);
                        gui.getInventory().setItem(26, errorIcon);
                        return;
                    }

                    gui.getInventory().setItem(14, empty);
                    gui.getInventory().setItem(13, empty);
                    gui.getInventory().setItem(26, empty);


                    if (feathers == 0) {
                        gui.getInventory().setItem(16, empty);
                        return;
                    }

                    if (isTipped) {
                        gui.getInventory().setItem(0, tippedIdentifierEmpty);
                    } else gui.getInventory().setItem(0, empty);

                    ItemStack potion = gui.getInventory().getItem(3);

                    int headID;
                    int hiltID;

                    ItemStack head = gui.getInventory().getItem(1);
                    ItemStack hilt = gui.getInventory().getItem(10);
                    if (head == null) head = new ItemStack(Material.AIR);
                    if (hilt == null) hilt = new ItemStack(Material.AIR);
                    headID = switch (head.getType()) {
                        default -> 1;
                        case IRON_NUGGET -> 2;
                        case GOLD_NUGGET -> 3;
                        case GLOWSTONE_DUST -> 4;
                        case ECHO_SHARD -> 5;
                        case ENDER_PEARL -> 6;
                        case SLIME_BALL -> 7;
                    };
                    hiltID = switch (hilt.getType()) {
                        default -> 1;
                        case BREEZE_ROD -> 2;
                        case BLAZE_ROD -> 3;
                    };


                    ItemStack arrow = makeArrow(headID, hiltID, feathers, f, isTipped ? potion : null);

                    gui.getInventory().setItem(16, arrow);
                });
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public ItemStack makeArrow(int headID, int hiltID, int feathers, int amount, @Nullable ItemStack potion) {
        boolean isTipped = potion != null;

        ItemStack arrow = new ItemStack(isTipped ? Material.TIPPED_ARROW : Material.ARROW, amount);

        ItemMeta arrowMeta = arrow.getItemMeta();

        PersistentDataContainer container = arrowMeta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();


        String hiltName;
        String headName;

        headName = switch (headID) {
            default -> "Flint ";
            case 2 -> "Weighted ";
            case 3 -> "Gilded ";
            case 4 -> "Spectral ";
            case 5 -> "Sonic ";
            case 6 -> "Teleportation ";
            case 7 -> "Bouncy ";
        };
        hiltName = switch (hiltID) {
            case 2 -> "Breezing ";
            case 3 -> "Blazing ";
            default -> "";
        };


        container.set(headKey, PersistentDataType.INTEGER, headID);
        container.set(hiltKey, PersistentDataType.INTEGER, hiltID);
        container.set(featherKey, PersistentDataType.INTEGER, feathers);

        arrowMeta.getPersistentDataContainer().set(arrowDataKey, PersistentDataType.TAG_CONTAINER, container);

        arrowMeta.lore(List.of(Component.text("Accuracy: %s".formatted(feathers)).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));

        if (arrowMeta instanceof PotionMeta meta) {
            if (potion != null && potion.getItemMeta() instanceof PotionMeta splashPotionMeta) {
                if (splashPotionMeta.hasBasePotionType()) {
                    meta.setBasePotionType(splashPotionMeta.getBasePotionType());
                }
                meta.displayName(Component.text(hiltName + "Arrow of %s".formatted(getPotionName(splashPotionMeta))).decoration(TextDecoration.ITALIC, false));
                meta.setColor(splashPotionMeta.getColor());
                for (PotionEffect effect : splashPotionMeta.getCustomEffects()) meta.addCustomEffect(effect, false);
            }
            CustomModelDataComponent comp = arrowMeta.getCustomModelDataComponent();
            comp.setStrings(Collections.singletonList(String.valueOf(hiltID)));
            arrowMeta.setCustomModelDataComponent(comp);
        } else {
            String cmd = "%s%s".formatted(headID, hiltID);
            CustomModelDataComponent comp = arrowMeta.getCustomModelDataComponent();
            comp.setStrings(Collections.singletonList(cmd));
            arrowMeta.setCustomModelDataComponent(comp);
            arrowMeta.displayName(Component.text(hiltName + headName + "Arrow").decoration(TextDecoration.ITALIC, false));
        }

        arrow.setItemMeta(arrowMeta);

        return arrow;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (List.of(Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW).contains(event.getRecipe().getResult().getType())) {
            event.getInventory().setResult(null);
        }
    }

    private final NamespacedKey blindnessPotionKey = new NamespacedKey("illusioners", "blindness_potion");

    public @NotNull String getPotionName(@NotNull PotionMeta meta) {
        if (meta.getPersistentDataContainer().has(blindnessPotionKey)) return "Blindness";
        PotionType type = meta.getBasePotionType();
        if (type == null) return "Extraction";
        return switch (type) {
            case THICK -> "Thickness";
            case LUCK -> "Luck";
            case WATER -> "Splashing";
            case OOZING -> "Oozing";
            case POISON, LONG_POISON, STRONG_POISON -> "Poison";
            case AWKWARD -> "Awkwardness";
            case HARMING, STRONG_HARMING -> "Harming";
            case STRONG_HEALING, HEALING -> "Healing";
            case STRONG_LEAPING, LONG_LEAPING, LEAPING -> "Leaping";
            case MUNDANE -> "Mundaneness";
            case WEAVING -> "Weaving";
            case INFESTED -> "Infestation";
            case STRONG_SLOWNESS, LONG_SLOWNESS, SLOWNESS -> "Slowness";
            case STRONG_STRENGTH, LONG_STRENGTH, STRENGTH -> "Strength";
            case LONG_WEAKNESS, WEAKNESS -> "Weakness";
            case STRONG_SWIFTNESS, SWIFTNESS, LONG_SWIFTNESS -> "Swiftness";
            case LONG_INVISIBILITY, INVISIBILITY -> "Invisibility";
            case NIGHT_VISION, LONG_NIGHT_VISION -> "Night Vision";
            case REGENERATION, LONG_REGENERATION, STRONG_REGENERATION -> "Regeneration";
            case SLOW_FALLING, LONG_SLOW_FALLING -> "Slow Falling";
            case WIND_CHARGED -> "Wind Charging";
            case TURTLE_MASTER, LONG_TURTLE_MASTER, STRONG_TURTLE_MASTER -> "the Turtle Master";
            case FIRE_RESISTANCE, LONG_FIRE_RESISTANCE -> "Fire Resistance";
            case WATER_BREATHING, LONG_WATER_BREATHING -> "Water Breathing";
        };
    }

    public int isFilled(CustomGUI gui, int index) {
        ItemStack item = gui.getInventory().getItem(index);
        if (item == null || item.getItemMeta() == null) return 0;
        return item.getPersistentDataContainer().has(noTakeKey) ? 0 : item.getAmount();
    }

    public void reduceSlot(CustomGUI gui, int index, ItemStack emptySlotItem, int amount) {
        ItemStack item = gui.getInventory().getItem(index);
        if (item != null) {
            item.setAmount(Math.max(0, item.getAmount() - amount));
            gui.getInventory().setItem(index, item);
        }
        if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) {
            gui.getInventory().setItem(index, emptySlotItem);
        }
    }

    public void checkSlot(InventoryClickEvent event, CustomGUI gui, int index, List<Material> materials, ItemStack emptySlotItem) {
        if (gui.getInventory().equals(event.getClickedInventory()) && event.getSlot() == index && !event.getCursor().getType().equals(Material.AIR)) {
            if (!materials.contains(event.getCursor().getType())) event.setCancelled(true);
        }

        if (index == event.getSlot() && event.getCurrentItem() != null && gui.getInventory().equals(event.getClickedInventory())) {
            if (event.getClick().equals(ClickType.SWAP_OFFHAND)) {
                ItemStack offhand = event.getWhoClicked().getInventory().getItemInOffHand();
                if (materials.contains(offhand.getType())) {
                    if (event.getCurrentItem().getPersistentDataContainer().has(noTakeKey)) {
                        event.setCurrentItem(offhand);
                        event.getWhoClicked().getInventory().setItemInOffHand(null);
                    }
                } else if (!offhand.getType().equals(Material.AIR)) {
                    event.setCancelled(true);
                }
            }

            if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                ItemStack item = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (item != null) {
                    if (materials.contains(item.getType())) {
                        if (event.getCurrentItem().getPersistentDataContainer().has(noTakeKey)) {
                            event.setCurrentItem(item);
                            event.getWhoClicked().getInventory().setItem(event.getHotbarButton(), null);
                        }
                    } else if (!item.getType().equals(Material.AIR)) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            ItemStack i = gui.getInventory().getItem(index);
            if (i != null && event.getClick().isShiftClick() && event.getClickedInventory() != null) {
                if (i.getPersistentDataContainer().has(noTakeKey)) {
                    if (!event.getClickedInventory().equals(gui.getInventory()) && event.getCurrentItem() != null) {
                        if (materials.contains(event.getCurrentItem().getType())) {
                            gui.getInventory().setItem(index, event.getCurrentItem());
                            event.setCurrentItem(null);
                        }
                    }
                }
            }
            if (index == event.getSlot() && event.getCurrentItem() != null && event.getCurrentItem().getPersistentDataContainer().has(noTakeKey)) {
                ItemStack ci = event.getCursor().clone();
                if (materials.contains(ci.getType())) {
                    if (!event.getClick().isShiftClick()) {
                        if (event.getClick().isRightClick()) {
                            ci.setAmount(1);
                            event.setCurrentItem(ci);
                            ci.setAmount(event.getCursor().getAmount() - 1);
                            event.getWhoClicked().setItemOnCursor(ci);
                        } else {
                            event.getWhoClicked().setItemOnCursor(null);
                            event.setCurrentItem(ci);
                        }
                    }
                }
            }

            ItemStack item = gui.getInventory().getItem(index);
            if (item == null || item.getType().equals(Material.AIR)) {
                gui.getInventory().setItem(index, emptySlotItem);
            }
        });
    }

    private final Set<Integer> slots = Set.of(1, 3, 10, 19, 20, 21);

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CustomGUI gui) {
            if (gui.getInventoryType().equals(CustomGUI.CustomInventoryType.FLETCHING_TABLE)) {
                for (int i : slots) {
                    ItemStack item = gui.getInventory().getItem(i);
                    if (item == null || item.getItemMeta() == null || item.getPersistentDataContainer().has(noTakeKey)) continue;
                    for (ItemStack it : event.getPlayer().getInventory().addItem(item).values()) {
                        ((CraftPlayer) event.getPlayer()).getHandle().drop(CraftItemStack.asNMSCopy(it), true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (List.of(Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW).contains(event.getEntity().getItemStack().getType())) {
            if (!event.getEntity().getItemStack().getPersistentDataContainer().has(arrowDataKey)) {
                switch (event.getEntity().getItemStack().getType()) {
                    case ARROW -> event.getEntity().setItemStack(makeArrow(1, 1, 3, event.getEntity().getItemStack().getAmount(), null));
                    case SPECTRAL_ARROW -> event.getEntity().setItemStack(makeArrow(4, 1, random.nextInt(1, 4), event.getEntity().getItemStack().getAmount(), null));
                    case TIPPED_ARROW -> event.getEntity().setItemStack(makeArrow(1, 1, 3, event.getEntity().getItemStack().getAmount(), event.getEntity().getItemStack()));
                }
            }
        }
    }

    private final Random random = new Random();

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (event.getRecipe().getResult().getType().equals(Material.ARROW)) {
            ItemStack arrow = makeArrow(1, 1, random.nextInt(1, 3), 16, null);
            MerchantRecipe r = event.getRecipe();
            event.setRecipe(new MerchantRecipe(
                    arrow,
                    r.getUses(),
                    r.getMaxUses(),
                    r.hasExperienceReward(),
                    r.getVillagerExperience(),
                    r.getPriceMultiplier(),
                    r.getDemand(),
                    r.getSpecialPrice(),
                    r.shouldIgnoreDiscounts())
            );
        } else if (event.getRecipe().getResult().getType().equals(Material.TIPPED_ARROW)) {
            ItemStack arrow = makeArrow(random.nextInt(2, 4), random.nextInt(1, 4), 3, 16, null);

            MerchantRecipe r = event.getRecipe();
            event.setRecipe(new MerchantRecipe(
                    arrow,
                    r.getUses(),
                    r.getMaxUses(),
                    r.hasExperienceReward(),
                    r.getVillagerExperience(),
                    r.getPriceMultiplier(),
                    r.getDemand(),
                    r.getSpecialPrice(),
                    r.shouldIgnoreDiscounts())
            );
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        ItemStack item = event.getConsumable();
        if (item == null || item.getItemMeta() == null) return;
        PersistentDataContainer container = item.getPersistentDataContainer().get(arrowDataKey, PersistentDataType.TAG_CONTAINER);
        if (container == null) return;
        int feathers = container.getOrDefault(featherKey, PersistentDataType.INTEGER, 2);
        int hilt = container.getOrDefault(hiltKey, PersistentDataType.INTEGER, 1);
        int head = container.getOrDefault(headKey, PersistentDataType.INTEGER, 1);
        container.set(forceKey, PersistentDataType.FLOAT, event.getForce());
        if (event.getBow() != null) container.set(powerKey, PersistentDataType.INTEGER, event.getBow().getEnchantmentLevel(Enchantment.POWER));

        float force = event.getForce();
        if (hilt == 3) force *= 1.5f;
        if (head == 2) force /= 2;
        else if (head == 3) force /= 1.5f;

        if (hilt == 2) event.getProjectile().setGravity(false);

        ((AbstractProjectile) event.getProjectile()).getHandle().shootFromRotation(((CraftEntity) event.getEntity()).getHandle(), event.getEntity().getPitch(), event.getEntity().getYaw(), 0, force, 3 - feathers);
        event.getProjectile().getPersistentDataContainer().set(arrowDataKey, PersistentDataType.TAG_CONTAINER, container);
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        for (World world : Bukkit.getWorlds()) {
            for (AbstractArrow arrow : world.getEntitiesByClass(AbstractArrow.class)) {
                if (arrow.isInBlock()) continue;
                if (!arrow.hasGravity()) arrow.setVelocity(arrow.getVelocity().add(new Vector(0, -0.025, 0)));
                PersistentDataContainer container = arrow.getPersistentDataContainer().get(arrowDataKey, PersistentDataType.TAG_CONTAINER);
                if (container == null) continue;
                int i = container.getOrDefault(hiltKey, PersistentDataType.INTEGER, 0);
                if (i == 0 || i == 1) continue;
                Vec3 vec3d = ((CraftAbstractArrow) arrow).getHandle().getDeltaMovement();
                double d1 = vec3d.x;
                double d2 = vec3d.y;
                double d3 = vec3d.z;
                arrow.getWorld().spawnParticle(i == 3 ? Particle.SMALL_FLAME : Particle.WHITE_SMOKE, arrow.getLocation().clone().add(d1 * i/4, d2 * i/4, d3 * i/4), 1, 0.1, 0.1, 0.1, 0.04);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            if (arrow.getShooter() instanceof Entity) return;
            ItemStack item = arrow.getItemStack();
            PersistentDataContainer container = item.getPersistentDataContainer().get(arrowDataKey, PersistentDataType.TAG_CONTAINER);
            if (container == null) return;
            int feathers = container.getOrDefault(featherKey, PersistentDataType.INTEGER, 2);
            int hilt = container.getOrDefault(hiltKey, PersistentDataType.INTEGER, 1);
            int head = container.getOrDefault(headKey, PersistentDataType.INTEGER, 1);
            container.set(forceKey, PersistentDataType.FLOAT, 1.1f);

            float force = 1.1f;
            if (hilt == 3) force *= 1.5f;
            if (head == 2) force /= 2;
            else if (head == 3) force /= 1.5f;

            if (hilt == 2) event.getEntity().setGravity(false);

            ((AbstractProjectile) event.getEntity()).getHandle().shoot(event.getEntity().getVelocity().getX(), event.getEntity().getVelocity().getY(), event.getEntity().getVelocity().getZ(), force, 3 - feathers);
            event.getEntity().getPersistentDataContainer().set(arrowDataKey, PersistentDataType.TAG_CONTAINER, container);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void doKnockback(AbstractArrow a, LivingEntity t, DamageSource source) {
        net.minecraft.world.entity.LivingEntity target = ((CraftLivingEntity) t).getHandle();
        net.minecraft.world.entity.projectile.AbstractArrow arrow = ((CraftAbstractArrow) a).getHandle();
        float f = 0;
        if (arrow.firedFromWeapon != null) {
            ServerLevel world = ((CraftWorld) arrow.getBukkitEntity().getWorld()).getHandle();

            if (world != null) {

                f = EnchantmentHelper.modifyKnockback(world, arrow.firedFromWeapon, target, ((CraftDamageSource) source).getHandle(), 0.0F);
            }
        }

        if (f > 0.0D) {
            double d1 = Math.max(0.0D, 1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 vec3d = arrow.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(f * 0.6D * d1);

            if (vec3d.lengthSqr() > 0.0D) {
                target.push(vec3d.x, 0.1D, vec3d.z, arrow);
            }
        }
    }

    @EventHandler
    @SuppressWarnings("UnstableApiUsage")
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (event.getHitEntity() instanceof LivingEntity entity) {
            List<PotionEffect> effects = new ArrayList<>(arrow.getCustomEffects());
            if (arrow.getBasePotionType() != null) effects.addAll(arrow.getBasePotionType().getPotionEffects());
            for (PotionEffect effect : effects) {
                entity.addPotionEffect(effect.withDuration(effect.getDuration() / 8));
            }
        }

        PersistentDataContainer container = event.getEntity().getPersistentDataContainer().get(arrowDataKey, PersistentDataType.TAG_CONTAINER);
        if (container == null) return;
        int head = container.getOrDefault(headKey, PersistentDataType.INTEGER, 1);

        float damage = container.getOrDefault(forceKey, PersistentDataType.FLOAT, 0f)*2;

        if (arrow.isCritical()) damage *= 1.5f;
        int power = container.getOrDefault(powerKey, PersistentDataType.INTEGER, 0);
        if (power > 0) damage += (power+1) * 0.25f;

        DamageSource.Builder builder = DamageSource.builder(DamageType.ARROW);
        if (event.getEntity().getShooter() instanceof LivingEntity entity) builder = builder.withDirectEntity(event.getEntity()).withCausingEntity(entity);
        DamageSource source = builder.build();
        boolean shouldRemove = false;

        if (event.getHitEntity() instanceof LivingEntity entity) doKnockback(arrow, entity, source);
        switch (head) {
            case 2 -> damage *= 2;
            case 3 -> {
                damage *= 1.5f;
                shouldRemove = true;
            }
            case 4 -> {
                if (event.getHitEntity() instanceof LivingEntity entity) entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 9600, 0, false, false, false));
            }
            case 5 -> {
                event.getEntity().getWorld().spawnParticle(Particle.SONIC_BOOM, event.getEntity().getLocation(), 1);
                event.getEntity().getWorld().playSound(event.getEntity(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1, 1);

                for (Entity entity : event.getEntity().getNearbyEntities(3, 3, 3)) {
                    if (!entity.equals(event.getHitEntity())) {
                        net.minecraft.world.entity.Entity en = ((CraftEntity) entity).getHandle();
                        en.hurtServer(en.level().getMinecraftWorld(), ((CraftDamageSource)source).getHandle(), (float) (damage / Math.max(1, entity.getLocation().distance(event.getEntity().getLocation()) * 2)));
                        if (event.getEntity().getFireTicks() > 0) entity.setFireTicks(100);
                    }
                }
                for (Entity entity : event.getEntity().getNearbyEntities(64, 64, 64)) {
                    if (entity instanceof Warden warden) {
                        warden.setDisturbanceLocation(event.getEntity().getLocation());
                        int i = 0;
                        while (warden.getEntityAngryAt() != null) {
                            warden.clearAnger(warden.getEntityAngryAt());
                            i++;
                            if (i > 40) break;
                        }
                        warden.setTarget(null);
                        warden.setAnger(event.getEntity(), 150);
                    }
                }
            }
            case 6 -> {
                if (event.getEntity().getShooter() instanceof LivingEntity entity) {
                    Location loc = event.getEntity().getLocation().clone();
                    loc.setYaw(entity.getYaw());
                    loc.setPitch(entity.getPitch());
                    entity.teleport(loc);
                    entity.damage(damage, DamageSource.builder(DamageType.FALL).build());
                    shouldRemove = true;
                }
            }
            case 7 -> {
                if (event.getHitEntity() instanceof LivingEntity entity) {
                    entity.knockback(damage/2.5, -Mth.sin(((CraftEntity)event.getEntity()).getHandle().getYRot() * 0.017453292F), -Mth.cos(((CraftEntity)event.getEntity()).getHandle().getYRot() * 0.017453292F));
                    entity.playHurtAnimation(0);
                    ((CraftLivingEntity) entity).getHandle().animateHurt(0);
                }
                damage = 0;
            }
        }

        if (event.getHitEntity() != null) {
            net.minecraft.world.entity.Entity en = ((CraftEntity) event.getHitEntity()).getHandle();
            en.hurtServer(en.level().getMinecraftWorld(), ((CraftDamageSource)source).getHandle(), damage);
            if (event.getEntity().getFireTicks() > 0) event.getHitEntity().setFireTicks(100);
        }

        if (shouldRemove && !event.getEntity().isDead()) {
            ItemStack item;
            if (event.getEntity() instanceof Arrow a) {
                item = a.getItemStack();
            } else item = new ItemStack(Material.GOLD_NUGGET);
            event.getEntity().getWorld().spawnParticle(Particle.ITEM, event.getEntity().getLocation(), 20, 0, 0, 0, 0.1, item);
            event.getEntity().remove();
        }

        if (event.getHitEntity() != null) {
            if (shouldRemove) return;
            event.setCancelled(true);
            event.getEntity().remove();
            if (event.getHitEntity() instanceof LivingEntity entity) entity.setArrowsInBody(entity.getArrowsInBody() + 1, true);
        } else {
            container.set(forceKey, PersistentDataType.FLOAT, 0.25f);
            event.getEntity().getPersistentDataContainer().set(arrowDataKey, PersistentDataType.TAG_CONTAINER, container);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws ExecutionException, InterruptedException {
        if (!getConfig().getBoolean("enable-resource-pack")) return;
        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create("https://github.com/cometcake575/Fletcher/raw/refs/heads/main/FletcherPack.zip"))
                .computeHashAndBuild().get();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> event.getPlayer().sendResourcePacks(packInfo), 5);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof BlockInventoryHolder) {
            for (int i = 0; i < event.getInventory().getSize(); i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item == null || item.getItemMeta() == null) continue;
                if (!item.getPersistentDataContainer().has(arrowDataKey)) {
                    switch (item.getType()) {
                        case ARROW -> event.getInventory().setItem(i, makeArrow(1, 1, 3, item.getAmount(), null));
                        case SPECTRAL_ARROW ->
                                event.getInventory().setItem(i, makeArrow(4, 1, random.nextInt(1, 4), item.getAmount(), null));
                        case TIPPED_ARROW ->
                                event.getInventory().setItem(i, makeArrow(1, 1, 3, item.getAmount(), item));
                    }
                }
            }
        }
    }
}
