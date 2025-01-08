package kr.ieruminecraft.nonobadword;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import kr.ieruminecraft.nonobadword.audio.AudioHandler;
import kr.ieruminecraft.nonobadword.audio.VoicechatAddon;
import kr.ieruminecraft.nonobadword.commands.NonobadwordCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 1) Bukkit/Spigot 플러그인 진입점
 * 2) Simple Voice Chat 애드온(VoicechatAddon)을 등록
 */
public class Nonobadword extends JavaPlugin {

    private NonobadwordCommand commandExecutor;

    private static Nonobadword instance;
    private boolean enabled;

    private String openAiApiKey;
    private long silenceThresholdMillis;
    private long minAudioLengthMs;
    private long maxBufferSizeMs;
    private String whisperPrompt;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("[Nonobadword] Plugin enabled.");

        // config.yml이 없으면 기본 파일 생성
        saveDefaultConfig();
        loadConfigValues();

        commandExecutor = new NonobadwordCommand(this);
        getCommand("nbw").setExecutor(commandExecutor);
        getCommand("nbw").setTabCompleter(commandExecutor);

        AudioHandler.getInstance().setSilenceThreshold(silenceThresholdMillis);

        BukkitVoicechatService service =
                getServer().getServicesManager().load(BukkitVoicechatService.class);
        try {
            VoicechatAddon addon = new VoicechatAddon();
            service.registerPlugin(addon);
            getLogger().info("[Nonobadword] VoicechatAddon registered with SVC.");
        } catch (NoClassDefFoundError e) {
            getLogger().warning("[Nonobadword] Simple Voice Chat not found or incompatible version!");
        }

        // 침묵 체크 스케줄러 (1초마다)
        getServer().getScheduler().runTaskTimer(
                this,
                () -> AudioHandler.getInstance().checkSilentPlayers(),
                20L,
                20L
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("[Nonobadword] Plugin disabled.");
    }

    public void loadConfigValues() {
        reloadConfig();
        this.enabled = getConfig().getBoolean("enabled", true); // 기본값은 true
        this.openAiApiKey = getConfig().getString("openai-api-key", "NO_KEY_FOUND");
        this.maxBufferSizeMs = getConfig().getLong("max-buffer-size-ms", 10000L);
        this.silenceThresholdMillis = getConfig().getLong("silence-threshold-ms", 3000L);
        this.minAudioLengthMs = getConfig().getLong("min-audio-length-ms", 1000L);
        this.whisperPrompt = getConfig().getString("whisper-prompt","상우,규민,이루,민재,민재야,이루야,규민아,상우야");
        getLogger().info("[Nonobadword] System enabled: " + enabled);
        getLogger().info("[Nonobadword] Loaded OpenAI API Key: "
                + (openAiApiKey.equals("NO_KEY_FOUND") ? "NOT_SET" : "****"));
        getLogger().info("[Nonobadword] Loaded Silence Threshold: " + silenceThresholdMillis + " ms");
    }

    // 다른 클래스가 config.yml 의 API Key를 참조할 수 있도록 Getter
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    public long getMinAudioLengthMs() {
        return minAudioLengthMs;
    }
    public static Nonobadword getInstance() {
        return instance;
    }
    public long getMaxBufferSizeMs() {
        return maxBufferSizeMs;
    }
    public boolean isSystemEnabled() {
        return enabled;
    }
    public String getWhisperPrompt() {
        return whisperPrompt;
    }
}
