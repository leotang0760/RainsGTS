package com.rainsh.gtsgiantai.command;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.Mood;
import com.rainsh.gtsgiantai.manager.GiantManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /gts 管理指令：spawn / remove / list / state / scale / mood / forceaction /
 * reload / resourcepack / trace / help
 */
public final class GTSCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB = Arrays.asList(
            "spawn", "remove", "list", "state", "scale", "mood", "hp",
            "forceaction", "reload", "resourcepack", "trace", "help");

    private static final List<String> STATES = Arrays.asList("SLEEP", "IDLE", "ALERT", "CHASE");
    private static final List<String> MOODS = Arrays.asList("CALM", "PLAYFUL", "ANNOYED", "AGGRESSIVE");
    private static final List<String> ACTIONS = Arrays.asList(
            "idle", "sit", "sleep", "lie", "lean", "stomp", "punch",
            "hold", "cuddle", "whisper", "pat", "lift", "kiss", "nuzzle",
            "bounce", "carry", "lap",
            "observe", "block", "stretch");

    private final GTSGiantAI plugin;

    public GTSCommand(GTSGiantAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        GiantManager m = plugin.getGiantManager();

        switch (sub) {
            case "spawn": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("&e用法: /gts spawn <type> <scale> [x y z]"));
                    return true;
                }
                String type = args[1];
                double scale;
                try {
                    scale = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(plugin.msg("&c倍率必须是数字"));
                    return true;
                }
                Location loc = null;
                if (args.length >= 6) {
                    try {
                        double x = Double.parseDouble(args[3]);
                        double y = Double.parseDouble(args[4]);
                        double z = Double.parseDouble(args[5]);
                        if (sender instanceof Player p) {
                            loc = new Location(p.getWorld(), x, y, z);
                        } else if (Bukkit.getWorlds().size() > 0) {
                            loc = new Location(Bukkit.getWorlds().get(0), x, y, z);
                        }
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(plugin.msg("&c坐标必须是数字"));
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    loc = p.getLocation();
                } else {
                    sender.sendMessage(plugin.msg("&c控制台生成需指定坐标 x y z"));
                    return true;
                }
                GiantEntity g = m.spawnGiant(type, scale, loc);
                if (g == null) {
                    sender.sendMessage(plugin.msg("&c生成失败：可能已达该世界上限或世界无效"));
                } else {
                    sender.sendMessage(plugin.msg("&a已生成巨人 {giant}（倍率 x{scale}）ID: {id}")
                            .replace("{giant}", g.getDisplayName())
                            .replace("{scale}", String.valueOf(g.getScale()))
                            .replace("{id}", g.getId().toString().substring(0, 8)));
                }
                return true;
            }
            case "remove": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("&e用法: /gts remove <id>"));
                    return true;
                }
                GiantEntity target = findByPrefix(m, args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人 ID: {id}").replace("{id}", args[1]));
                    return true;
                }
                UUID id = target.getId();
                if (m.removeGiant(id)) {
                    sender.sendMessage(plugin.msg("&a已删除巨人 ID: {id}").replace("{id}", id.toString().substring(0, 8)));
                } else {
                    sender.sendMessage(plugin.msg("&c未找到巨人 ID: {id}").replace("{id}", args[1]));
                }
                return true;
            }
            case "list": {
                if (!checkAdmin(sender)) return true;
                List<GiantEntity> all = new ArrayList<>(m.allGiants());
                if (all.isEmpty()) {
                    sender.sendMessage(plugin.msg("&7当前世界没有巨人。"));
                    return true;
                }
                sender.sendMessage(plugin.msg("&d当前巨人列表:"));
                for (GiantEntity g : all) {
                    sender.sendMessage(plugin.msg("&7- &f{id} &7| {type} &7| x{scale} &7| {state} &7| {loc}")
                            .replace("{id}", g.getId().toString().substring(0, 8))
                            .replace("{type}", g.getTypeName())
                            .replace("{scale}", String.valueOf(g.getScale()))
                            .replace("{state}", g.getState().name())
                            .replace("{loc}", g.getAnchor() == null ? "?" :
                                    String.format("%.0f,%.0f,%.0f", g.getAnchor().x, g.getAnchor().y, g.getAnchor().z)));
                }
                return true;
            }
            case "hp": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("&e用法: /gts hp <id>"));
                    return true;
                }
                GiantEntity target = findByPrefix(m, args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人 ID: {id}").replace("{id}", args[1]));
                    return true;
                }
                if (!target.isBoss()) {
                    sender.sendMessage(plugin.msg("&7该巨人未启用Boss血量（boss.enabled=false 或未初始化）"));
                    return true;
                }
                sender.sendMessage(plugin.msg("&6{type} x{scale} | &c血量 {hp}/{max} &7| &e阶段 {phase}")
                        .replace("{type}", target.getTypeName())
                        .replace("{scale}", String.valueOf(target.getScale()))
                        .replace("{hp}", String.valueOf((int) target.getHp()))
                        .replace("{max}", String.valueOf((int) target.getMaxHp()))
                        .replace("{phase}", String.valueOf(target.getPhase())));
                return true;
            }
            case "state": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("&e用法: /gts state <id> <SLEEP|IDLE|ALERT|CHASE>"));
                    return true;
                }
                GiantEntity g = findByPrefix(m, args[1]);
                if (g == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人"));
                    return true;
                }
                g.setState(GiantState.fromString(args[2]));
                sender.sendMessage(plugin.msg("&a巨人 {id} 状态已切换为 {state}")
                        .replace("{id}", g.getId().toString().substring(0, 8))
                        .replace("{state}", args[2].toUpperCase()));
                return true;
            }
            case "scale": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("&e用法: /gts scale <id> <scale>"));
                    return true;
                }
                GiantEntity g = findByPrefix(m, args[1]);
                if (g == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人"));
                    return true;
                }
                try {
                    double s = Double.parseDouble(args[2]);
                    g.setScale(s);
                    sender.sendMessage(plugin.msg("&a巨人 {id} 倍率已修改为 x{scale}")
                            .replace("{id}", g.getId().toString().substring(0, 8))
                            .replace("{scale}", String.valueOf(s)));
                    m.saveAll();
                } catch (NumberFormatException ex) {
                    sender.sendMessage(plugin.msg("&c倍率必须是数字"));
                }
                return true;
            }
            case "mood": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("&e用法: /gts mood <id> <CALM|PLAYFUL|ANNOYED|AGGRESSIVE>"));
                    return true;
                }
                GiantEntity g = findByPrefix(m, args[1]);
                if (g == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人"));
                    return true;
                }
                g.setMood(Mood.fromString(args[2]));
                sender.sendMessage(plugin.msg("&a巨人 {id} 心情已设置为 {mood}")
                        .replace("{id}", g.getId().toString().substring(0, 8))
                        .replace("{mood}", args[2].toUpperCase()));
                return true;
            }
            case "forceaction": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("&e用法: /gts forceaction <id> <action>"));
                    return true;
                }
                GiantEntity g = findByPrefix(m, args[1]);
                if (g == null) {
                    sender.sendMessage(plugin.msg("&c未找到巨人"));
                    return true;
                }
                g.forceAction(args[2]);
                sender.sendMessage(plugin.msg("&a巨人 {id} 已强制触发动作 {action}")
                        .replace("{id}", g.getId().toString().substring(0, 8))
                        .replace("{action}", args[2]));
                return true;
            }
            case "reload": {
                if (!checkAdmin(sender)) return true;
                plugin.reloadAll();
                sender.sendMessage(plugin.msg("&a配置已重载。"));
                return true;
            }
            case "resourcepack": {
                if (!checkAdmin(sender)) return true;
                var rps = plugin.getResourcePackServer();
                if (rps == null || !rps.isReady()) {
                    String fail = rps == null ? "" : rps.getFailure();
                    sender.sendMessage(plugin.msg("&c资源包分发不可用" + (fail.isEmpty() ? "" : "：" + fail)));
                    sender.sendMessage(plugin.msg("&7请在 config.yml 的 resourcepack.url 填写外部直链，或修正 resourcepack.server 配置"));
                    return true;
                }
                if (args.length >= 2) {
                    Player p = Bukkit.getPlayerExact(args[1]);
                    if (p == null || !p.isOnline()) {
                        sender.sendMessage(plugin.msg("&c玩家不在线"));
                        return true;
                    }
                    rps.pushTo(p);
                    sender.sendMessage(plugin.msg("&a资源包已推送给 {player}").replace("{player}", p.getName()));
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        rps.pushTo(p);
                    }
                    sender.sendMessage(plugin.msg("&a资源包已推送给全部在线玩家"));
                }
                sender.sendMessage(plugin.msg("&7分发地址: " + rps.getResourcePackUrl()));
                return true;
            }
            case "trace": {
                if (!checkAdmin(sender)) return true;
                plugin.getConfig().set("debug.show_collision_boxes",
                        !plugin.getConfig().getBoolean("debug.show_collision_boxes"));
                plugin.saveConfig();
                boolean on = plugin.getConfig().getBoolean("debug.show_collision_boxes");
                sender.sendMessage(plugin.msg("&a碰撞盒可视化已" + (on ? "开启" : "关闭")));
                return true;
            }
            case "help":
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission("gtsgiantai.admin") || sender.isOp()) return true;
        sender.sendMessage(plugin.msg("&c你没有权限执行此操作。"));
        return false;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.msg("&e=== GTS GiantAI 指令 ==="));
        sender.sendMessage(plugin.msg("&e/gts spawn <type> <scale> [x y z] &7- 生成巨人"));
        sender.sendMessage(plugin.msg("&e/gts remove <id> &7- 删除巨人"));
        sender.sendMessage(plugin.msg("&e/gts list &7- 巨人列表"));
        sender.sendMessage(plugin.msg("&e/gts state <id> <SLEEP|IDLE|ALERT|CHASE> &7- 切换状态"));
        sender.sendMessage(plugin.msg("&e/gts scale <id> <scale> &7- 修改倍率"));
        sender.sendMessage(plugin.msg("&e/gts mood <id> <CALM|PLAYFUL|ANNOYED|AGGRESSIVE> &7- 设置心情"));
        sender.sendMessage(plugin.msg("&e/gts hp <id> &7- 查看Boss血量与阶段"));
        sender.sendMessage(plugin.msg("&e/gts forceaction <id> <action> &7- 强制动作"));
        sender.sendMessage(plugin.msg("&e/gts reload &7- 重载配置"));
        sender.sendMessage(plugin.msg("&e/gts resourcepack send [player] &7- 推送资源包"));
        sender.sendMessage(plugin.msg("&e/gts trace &7- 碰撞盒可视化开关"));
    }

    private UUID parseId(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** 支持8位短ID前缀匹配 */
    private GiantEntity findByPrefix(GiantManager m, String prefix) {
        if (prefix.length() == 8) {
            for (GiantEntity g : m.allGiants()) {
                if (g.getId().toString().startsWith(prefix)) return g;
            }
        }
        try {
            return m.getGiant(UUID.fromString(prefix));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUB) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("state") && args.length == 3) {
            for (String s : STATES) if (s.startsWith(args[2].toUpperCase())) out.add(s);
            return out;
        }
        if (sub.equals("mood") && args.length == 3) {
            for (String s : MOODS) if (s.startsWith(args[2].toUpperCase())) out.add(s);
            return out;
        }
        if (sub.equals("forceaction") && args.length == 3) {
            for (String s : ACTIONS) if (s.startsWith(args[2].toLowerCase())) out.add(s);
            return out;
        }
        if ((sub.equals("remove") || sub.equals("state") || sub.equals("scale")
                || sub.equals("mood") || sub.equals("forceaction")) && args.length == 2) {
            for (GiantEntity g : plugin.getGiantManager().allGiants()) {
                out.add(g.getId().toString().substring(0, 8));
            }
            return out;
        }
        return out;
    }
}
