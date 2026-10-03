package com.rainsh.gtsgiantai;

import com.rainsh.gtsgiantai.command.GTSCommand;
import com.rainsh.gtsgiantai.config.ConfigManager;
import com.rainsh.gtsgiantai.listener.PlayerListener;
import com.rainsh.gtsgiantai.manager.GiantManager;
import com.rainsh.gtsgiantai.permission.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * GTS GiantAI 主类
 * 重度GTS向 人型女巨人 行为树AI Paper插件（1.20.1+ / Folia兼容 / 零外部依赖）
 */
public final class GTSGiantAI extends JavaPlugin {

    private ConfigManager cfg;
    private PermissionManager permissions;
    private GiantManager giantManager;
    private com.rainsh.gtsgiantai.trace.TraceManager traceManager;
    private com.rainsh.gtsgiantai.village.VillageManager villageManager;
    private com.rainsh.gtsgiantai.resourcepack.ResourcePackServer resourcePackServer;
    private com.rainsh.gtsgiantai.dialogue.DialogueEngine dialogue;
    private FileConfiguration giantsData;
    private File giantsDataFile;
    private long globalTick = 0;

    /**
     * 改名迁移（v1.4.1）：旧版本插件目录 plugins/GTSGiantAI 存在而新目录 plugins/RainsGTS
     * 不存在时，把配置/对话/持久巨人数据一次性复制过去，避免服主数据丢失。
     */
    private void migrateLegacyData() {
        try {
            File legacy = new File(getDataFolder().getParentFile(), "GTSGiantAI");
            File current = getDataFolder();
            if (!legacy.isDirectory() || current.exists()) return;
            current.mkdirs();
            File[] files = legacy.listFiles();
            if (files == null) return;
            int n = 0;
            for (File f : files) {
                if (f.isFile()) {
                    java.nio.file.Files.copy(f.toPath(),
                            new File(current, f.getName()).toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    n++;
                }
            }
            getLogger().info("已从旧目录 plugins/GTSGiantAI 迁移 " + n + " 个文件到 " + current.getName() + "/");
        } catch (Throwable t) {
            getLogger().warning("数据迁移失败（不影响运行）: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() {
        migrateLegacyData(); // v1.4.1 改名 RainsGTS：一次性迁移旧 GTSGiantAI 数据目录
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("giants_data.yml");

        this.cfg = new ConfigManager(this);
        this.permissions = new PermissionManager(this);
        this.giantManager = new GiantManager(this);
        this.traceManager = new com.rainsh.gtsgiantai.trace.TraceManager(this);
        this.villageManager = new com.rainsh.gtsgiantai.village.VillageManager(this);
        loadGiantsData();

        // 注册指令
        GTSCommand cmd = new GTSCommand(this);
        PluginCommand c = getCommand("gtsgiantai");
        if (c != null) {
            c.setExecutor(cmd);
            c.setTabCompleter(cmd);
        }

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 内置资源包分发（玩家零操作自动加载模型）
        this.resourcePackServer = new com.rainsh.gtsgiantai.resourcepack.ResourcePackServer(this);
        resourcePackServer.start();

        // 对话引擎（巨人娘句子预设）
        this.dialogue = new com.rainsh.gtsgiantai.dialogue.DialogueEngine(this);

        // 加载持久巨人
        giantManager.loadAll();

        // 主循环（Folia兼容：GlobalRegionScheduler 在 Paper/Folia 均可用）
        getServer().getGlobalRegionScheduler().runAtFixedRate(this,
                task -> tick(), 1L, 1L);

        getLogger().info("GTS GiantAI 已启用 | Paper 1.20.1+ | Folia兼容 | LuckPerms接口: "
                + (permissions.isLuckPermsAvailable() ? "已接入" : "未检测到(自动降级)"));
    }

    @Override
    public void onDisable() {
        if (resourcePackServer != null) {
            resourcePackServer.stop();
        }
        if (giantManager != null) {
            giantManager.saveAll();
            // 持久化模式下，卸载时销毁实体，数据保留，重启恢复
            for (com.rainsh.gtsgiantai.entity.GiantEntity g : giantManager.allGiants()) {
                g.despawn();
            }
        }
        getLogger().info("GTS GiantAI 已卸载");
    }

    private void tick() {
        globalTick++;
        try {
            giantManager.tick(globalTick);
            traceManager.restoreTick();
            if (globalTick % 100 == 0) {
                traceManager.ancientFootprintTick(globalTick);
            }
            if (globalTick % 600 == 0) {
                giantManager.naturalSpawnTick(globalTick);
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "主循环异常", t);
        }
    }

    /** 重载全部配置 */
    public void reloadAll() {
        reloadConfig();
        cfg.reload();
        reloadMessages();
        if (dialogue != null) {
            dialogue.reload();
        }
        if (giantManager != null) {
            giantManager.saveAll();
        }
    }

    // ---------------- 消息 ----------------

    private FileConfiguration messages;
    private File messagesFile;

    private void reloadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream def = getResource("messages.yml");
        if (def != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(def, StandardCharsets.UTF_8)));
        }
    }

    private void saveResourceIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) saveResource(name, false);
    }

    /** 原始消息（已转颜色码） */
    public String msg(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** 从 messages.yml 读取并发送给玩家（支持占位符后续替换） */
    public void msg(Player p, String path) {
        String prefix = messages.getString("prefix", "&8[&dGTS&8] &r");
        String text = messages.getString(path, path);
        p.sendMessage(msg(prefix + text));
    }

    // ---------------- 数据文件 ----------------

    private void loadGiantsData() {
        giantsDataFile = new File(getDataFolder(), "giants_data.yml");
        if (!giantsDataFile.exists()) {
            saveResource("giants_data.yml", false);
        }
        giantsData = YamlConfiguration.loadConfiguration(giantsDataFile);
    }

    public FileConfiguration getGiantsData() {
        return giantsData;
    }

    public void saveGiantsData() {
        if (giantsData == null || giantsDataFile == null) return;
        try {
            giantsData.save(giantsDataFile);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "保存giants_data.yml失败", e);
        }
    }

    // ---------------- 访问器 ----------------

    public ConfigManager getCfg() {
        return cfg;
    }

    public PermissionManager getPermissions() {
        return permissions;
    }

    public GiantManager getGiantManager() {
        return giantManager;
    }

    public com.rainsh.gtsgiantai.trace.TraceManager getTraceManager() {
        return traceManager;
    }

    public com.rainsh.gtsgiantai.village.VillageManager getVillageManager() {
        return villageManager;
    }

    public com.rainsh.gtsgiantai.resourcepack.ResourcePackServer getResourcePackServer() {
        return resourcePackServer;
    }

    public com.rainsh.gtsgiantai.dialogue.DialogueEngine getDialogue() {
        return dialogue;
    }

    /** 取原生FileConfiguration（v1.1新配置读取） */
    public org.bukkit.configuration.file.FileConfiguration rawConfig() {
        return getConfig();
    }
}
