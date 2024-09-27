package com.starshooterstudios.fletcher;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class CustomGUI implements InventoryHolder {

    private final CustomInventoryType inventoryType;
    private final Inventory inventory;

    private CustomGUI(CustomInventoryType inventoryType, int size, Component title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.inventoryType = inventoryType;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public CustomInventoryType getInventoryType() {
        return inventoryType;
    }

    public enum CustomInventoryType{
        FLETCHING_TABLE
    }

    public static Inventory createInventory(CustomInventoryType inventoryType, int size, Component title) {
        return new CustomGUI(inventoryType, size, title).getInventory();
    }
}