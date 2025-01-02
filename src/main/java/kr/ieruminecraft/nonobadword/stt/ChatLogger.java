package kr.ieruminecraft.nonobadword.stt;

import kr.ieruminecraft.nonobadword.Nonobadword;
import kr.ieruminecraft.nonobadword.openai.ToxicityAnalyzer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 음성으로 변환된 텍스트를 YAML로 기록
 */
public class ChatLogger {

    private final ToxicityAnalyzer toxicityAnalyzer;
    private static ChatLogger instance;
    private final File file;
    private final YamlConfiguration config;

    private ChatLogger() {
        File dataFolder = Nonobadword.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        file = new File(dataFolder, "voice_logs.yml");
        config = YamlConfiguration.loadConfiguration(file);
        toxicityAnalyzer = new ToxicityAnalyzer();
    }

    public static ChatLogger getInstance() {
        if (instance == null) {
            instance = new ChatLogger();
        }
        return instance;
    }

    public void logText(UUID playerUuid, String text) {
        long now = System.currentTimeMillis();
        String path = "logs." + now;
        config.set(path + ".player", playerUuid.toString());
        config.set(path + ".text", text);
        try {
            config.save(file);
            toxicityAnalyzer.analyzeMessage(playerUuid, text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
