package kr.ieruminecraft.nonobadword.openai;

import kr.ieruminecraft.nonobadword.Nonobadword;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ToxicityAnalyzer {
    private static final String COMPLETION_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final String apiKey;
    private final String model;
    private final int contextMessages;
    private final double toxicityThreshold;
    private final Set<String> punishedMessages;
    private final YamlConfiguration langConfig;

    public ToxicityAnalyzer() {
        Nonobadword plugin = Nonobadword.getInstance();
        this.apiKey = plugin.getOpenAiApiKey();
        this.model = plugin.getConfig().getString("gpt-model", "gpt-4o-mini");
        this.contextMessages = plugin.getConfig().getInt("context-messages", 5);
        this.toxicityThreshold = plugin.getConfig().getDouble("toxicity-threshold", 0.7);
        this.punishedMessages = new HashSet<>();

        // 이전에 처벌된 메시지 로드
        File logFile = new File(plugin.getDataFolder(), "voice_logs.yml");
        if (logFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(logFile);
            if (config.contains("punished")) {
                punishedMessages.addAll(config.getStringList("punished"));
            }
        }
        File langFile = new File(plugin.getDataFolder(), "langs.yml");
        if (!langFile.exists()) {
            plugin.saveResource("langs.yml", false);
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void analyzeMessage(UUID playerUuid, String message) {

        List<String> recentMessages = getRecentMessages(contextMessages);
        recentMessages.add(message);

        JSONObject requestBody = new JSONObject()
                .put("model", model)
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "developer")
                                .put("content", "You are analyzing korean chat messages in a game." +
                                        "For the last message in the conversation, provide: " +
                                        "1. A toxicity score from 0 to 1 where 1 is extremely toxic " +
                                        "2. A brief reason in Korean for the score (1-2 sentence, must include which word was toxic) " +
                                        "ONLY response with following format: {score}|{reason}"))
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", String.join("\n", recentMessages))));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(COMPLETION_ENDPOINT)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return;

            JSONObject jsonResponse = new JSONObject(response.body().string());
            String airesponse = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
            String[] parts = airesponse.split("\\|");
            double toxicity = Double.parseDouble(parts[0].trim());
            String reason = parts[1].trim();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                String actionBarMsg = ChatColor.translateAlternateColorCodes('&',
                        String.format("&e%s &7(나쁜 정도: &c%.2f&7)", message, toxicity));
                player.sendActionBar(actionBarMsg);
            }
            Nonobadword.getInstance().getLogger().info(String.format(
                    "[ToxicityAnalyzer] Message analyzed - Player: %s, Toxicity: %.2f, Message: %s",
                    Bukkit.getPlayer(playerUuid).getName(),
                    toxicity,
                    message
            ));

            if (toxicity >= toxicityThreshold) {
                punishPlayer(playerUuid, message, toxicity, reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getRecentMessages(int count) {
        List<String> messages = new ArrayList<>();
        File logFile = new File(Nonobadword.getInstance().getDataFolder(), "voice_logs.yml");
        if (!logFile.exists()) return messages;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(logFile);
        if (!config.contains("logs")) return messages;

        List<Long> timestamps = new ArrayList<>();
        for (String key : config.getConfigurationSection("logs").getKeys(false)) {
            timestamps.add(Long.parseLong(key));
        }
        timestamps.sort(Collections.reverseOrder());

        for (int i = 0; i < Math.min(count, timestamps.size()); i++) {
            messages.add(config.getString("logs." + timestamps.get(i) + ".text"));
        }
        return messages;
    }

    private void punishPlayer(UUID playerUuid, String message, Double toxicity, String reason) {
        Bukkit.getScheduler().runTask(Nonobadword.getInstance(), () -> {
            String deathMsg = langConfig.getString("death-message")
                    .replace("{player}", Bukkit.getPlayer(playerUuid).getName())
                    .replace("{message}", message)
                    .replace("{toxicity}", String.format("%.1f", toxicity));

            String aiMsg = langConfig.getString("ai-reason-message")
                    .replace("{reason}", reason);

            deathMsg = ChatColor.translateAlternateColorCodes('&', deathMsg);
            aiMsg = ChatColor.translateAlternateColorCodes('&', aiMsg);

            Bukkit.broadcastMessage(deathMsg);
            Bukkit.broadcastMessage(aiMsg);
            Bukkit.getPlayer(playerUuid).setHealth(0);
        });
    }
}