package com.rainsh.gtsgiantai.entity;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 骨骼渲染体：一个 ItemDisplay 实体承载一段骨骼的模型。
 * 默认使用可配置的 CustomModelData 物品（资源包模型）；无资源包时用物品图标占位。
 */
public final class BoneDisplay {

    private final BoneId bone;
    private final ItemDisplay display;

    public BoneDisplay(World world, Location anchor, BoneId bone, ItemStack baseItem, int customModelData) {
        this.bone = bone;
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        this.display = world.spawn(anchor, ItemDisplay.class);
        this.display.setItemStack(item);
        this.display.setBillboard(Display.Billboard.FIXED);
        this.display.setViewRange(96);
        this.display.setDisplayWidth(2);
        this.display.setDisplayHeight(2);
        this.display.setPersistent(true);
        // 让 Display 不受实体阴影/光照影响过大
        this.display.setBrightness(new Display.Brightness(15, 15));
        // 默认姿态
        this.display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new Quaternionf(),
                new Vector3f(1, 1, 1), new Quaternionf()));
    }

    /** 应用骨骼姿势（相对锚点局部变换） */
    public void applyPose(BonePose pose) {
        display.setTransformation(pose.toTransformation());
    }

    public BoneId getBone() {
        return bone;
    }

    public ItemDisplay getDisplay() {
        return display;
    }

    public void remove() {
        display.remove();
    }

    /** 是否在玩家视距内（用于LOD） */
    public boolean isNear(Player p) {
        Location a = display.getLocation();
        Location b = p.getLocation();
        if (!a.getWorld().equals(b.getWorld())) return false;
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz < 60 * 60;
    }

    public static ItemStack buildBaseItem(org.bukkit.Material material) {
        return new ItemStack(material != null ? material : org.bukkit.Material.PLAYER_HEAD);
    }

    public static void spawnWarning(List<BoneDisplay> displays, Player viewer) {
        // 预留：调试用
    }
}
