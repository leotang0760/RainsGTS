package com.rainsh.gtsgiantai.permission;

import com.rainsh.gtsgiantai.GTSGiantAI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 权限管理器：LuckPerms 原生接口（反射，无编译期依赖，可选）。
 * LP 不存在时自动退化为 Bukkit 权限节点；OP 拥有 admin 权限。
 */
public final class PermissionManager {

    private final GTSGiantAI plugin;
    private boolean luckPermsAvailable = false;

    public PermissionManager(GTSGiantAI plugin) {
        this.plugin = plugin;
        try {
            this.luckPermsAvailable = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        } catch (Throwable t) {
            this.luckPermsAvailable = false;
        }
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }

    /** 权限判定：LP > Bukkit；admin 节点对 OP 直接放行 */
    public boolean has(Player p, String node) {
        if (p == null) return false;
        if (node.equals("gtsgiantai.admin")) {
            if (p.isOp()) return true;
        }
        if (luckPermsAvailable) {
            try {
                return lpHas(p.getUniqueId(), node);
            } catch (Throwable t) {
                luckPermsAvailable = false; // LP异常后降级，不崩溃
            }
        }
        return p.hasPermission(node);
    }

    private boolean lpHas(UUID uuid, String node) throws Exception {
        Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
        Object api = providerClass.getMethod("get").invoke(null);
        Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
        Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
        if (user == null) return false;
        Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
        Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
        Object result = permissionData.getClass().getMethod("checkPermission", String.class)
                .invoke(permissionData, node);
        return (boolean) result.getClass().getMethod("asBoolean").invoke(result);
    }
}
