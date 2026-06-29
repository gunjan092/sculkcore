package dev.sculkcore.altar;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

public class AltarInstance {
    private final String altarTypeId;
    private final Location location;
    private ItemDisplay itemDisplay;
    private final List<TextDisplay> textDisplays = new ArrayList<>();

    public AltarInstance(String altarTypeId, Location location) {
        this.altarTypeId = altarTypeId;
        this.location = location;
    }

    public String getAltarTypeId() {
        return altarTypeId;
    }

    public Location getLocation() {
        return location;
    }

    public ItemDisplay getItemDisplay() {
        return itemDisplay;
    }

    public void setItemDisplay(ItemDisplay itemDisplay) {
        this.itemDisplay = itemDisplay;
    }

    public List<TextDisplay> getTextDisplays() {
        return textDisplays;
    }

    public void addTextDisplay(TextDisplay textDisplay) {
        this.textDisplays.add(textDisplay);
    }

    public void clearDisplays() {
        if (itemDisplay != null && !itemDisplay.isDead()) {
            itemDisplay.remove();
        }
        itemDisplay = null;
        for (TextDisplay textDisplay : textDisplays) {
            if (!textDisplay.isDead()) {
                textDisplay.remove();
            }
        }
        textDisplays.clear();
    }

    public void removeItemDisplayOnly() {
        if (itemDisplay != null && !itemDisplay.isDead()) {
            itemDisplay.remove();
        }
        itemDisplay = null;
    }
}
