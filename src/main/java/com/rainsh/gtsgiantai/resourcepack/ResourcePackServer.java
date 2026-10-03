package com.rainsh.gtsgiantai.resourcepack;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 内置资源包分发服务器：
 * 插件 jar 本身即合法资源包（内置 pack.mcmeta + assets/gtsgiantai 模型）。
 * 本类在插件内起一个极简 HTTP 服务，把 jar 文件作为 /gts-models.zip 提供给玩家，
 * 配合 setResourcePack 实现"玩家零操作自动加载模型"。
 * 零外部依赖（JDK 自带 com.sun.net.httpserver），只暴露一个固定路径。
 */
public final class ResourcePackServer {

    private final GTSGiantAI plugin;
    private HttpServer server;
    private byte[] jarBytes;
    private byte[] sha1;
    private String resolvedUrl = "";
    private String failure = "";

    public ResourcePackServer(GTSGiantAI plugin) {
        this.plugin = plugin;
    }

    /** 启动：读取配置 → 缓存 jar → 起 HTTP 服务。失败仅记录，不影响插件主体。 */
    public void start() {
        try {
            // 1. 缓存插件 jar 自身（即资源包 zip）
            File jarFile = new File(plugin.getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
                failure = "插件以非jar形态运行，内置分发不可用";
                plugin.getLogger().warning("[资源包] " + failure + "，请使用 config.yml resourcepack.url 直链");
                return;
            }
            jarBytes = Files.readAllBytes(jarFile.toPath());
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha1 = md.digest(jarBytes);

            // 2. 外部直链优先
            String cfgUrl = plugin.getConfig().getString("resourcepack.url", "");
            if (!cfgUrl.isEmpty()) {
                resolvedUrl = cfgUrl;
                plugin.getLogger().info("[资源包] 使用外部直链: " + cfgUrl);
                return;
            }

            // 3. 内置分发开关
            if (!plugin.getConfig().getBoolean("resourcepack.server.enabled", true)) {
                failure = "内置分发已在 config.yml 关闭（resourcepack.server.enabled=false）且未填 url";
                plugin.getLogger().warning("[资源包] " + failure);
                return;
            }

            // 4. 起内置 HTTP 服务
            String bind = plugin.getConfig().getString("resourcepack.server.bind", "0.0.0.0");
            int port = plugin.getConfig().getInt("resourcepack.server.port", 25564);
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
            server.createContext("/gts-models.zip", this::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            String host = plugin.getConfig().getString("resourcepack.server.publicHost", "");
            if (host.isEmpty()) {
                host = detectPublicIp();
                if (host == null) {
                    String sip = plugin.getServer().getIp();
                    host = (sip == null || sip.isEmpty()) ? "127.0.0.1" : sip;
                    plugin.getLogger().warning("[资源包] 未能自动探测公网IP，使用 " + host
                            + "。若玩家无法连接，请在 config.yml → resourcepack.server.publicHost 填写服务器公网IP/域名");
                }
            }
            resolvedUrl = "http://" + host + ":" + port + "/gts-models.zip";
            plugin.getLogger().info("[资源包] 内置分发已启动 " + bind + ":" + port
                    + " | 推送地址 " + resolvedUrl);
        } catch (Throwable t) {
            failure = "内置分发启动失败: " + t.getMessage();
            plugin.getLogger().log(Level.WARNING, "[资源包] " + failure, t);
        }
    }

    /** 处理 /gts-models.zip 下载（仅此一路径，防穿越） */
    private void handle(HttpExchange ex) throws IOException {
        URI uri = ex.getRequestURI();
        if (!"/gts-models.zip".equals(uri.getPath()) || jarBytes == null) {
            ex.sendResponseHeaders(404, -1);
            ex.close();
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "application/zip");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=gts-models.zip");
        ex.sendResponseHeaders(200, jarBytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(jarBytes);
        }
    }

    /** 尝试探测服务器公网IP（超时3秒，失败返回null） */
    private String detectPublicIp() {
        for (String api : new String[]{"https://api.ipify.org", "https://icanhazip.com"}) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(api).openConnection();
                c.setConnectTimeout(3000);
                c.setReadTimeout(3000);
                int code = c.getResponseCode();
                if (code == 200) {
                    byte[] b = c.getInputStream().readAllBytes();
                    String ip = new String(b, java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (!ip.isEmpty() && ip.matches("[\\d.]+")) return ip;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /** 对外推送URL（外部直链或内置HTTP地址），不可用返回空串 */
    public String getResourcePackUrl() {
        return resolvedUrl;
    }

    /** SHA-1 摘要（20字节，用于 setResourcePack 校验），不可用返回null */
    public byte[] getHash() {
        return sha1;
    }

    public String getFailure() {
        return failure;
    }

    public boolean isReady() {
        return !resolvedUrl.isEmpty();
    }

    /** 推送给单个玩家：url+sha1+prompt+force */
    public boolean pushTo(org.bukkit.entity.Player p) {
        if (!isReady()) return false;
        boolean force = plugin.getConfig().getBoolean("resourcepack.force", false);
        String prompt = plugin.getConfig().getString("resourcepack.prompt",
                "本服务器使用 GTS 巨人模型资源包，请点击接受以加载模型");
        if (sha1 != null) {
            p.setResourcePack(resolvedUrl, sha1, prompt, force);
        } else {
            p.setResourcePack(resolvedUrl);
        }
        return true;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
