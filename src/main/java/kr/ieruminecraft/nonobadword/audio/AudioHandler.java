package kr.ieruminecraft.nonobadword.audio;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어별 오디오 버퍼를 관리.
 */
public class AudioHandler {

    private static AudioHandler instance;

    // 유저 UUID -> AudioBuffer
    private final Map<UUID, AudioBuffer> audioBuffers = new ConcurrentHashMap<>();

    // 침묵 감지 시간(밀리초). config.yml에서 읽은 값
    private long silenceThresholdMillis = 3000;

    private AudioHandler() {}

    public static AudioHandler getInstance() {
        if (instance == null) {
            instance = new AudioHandler();
        }
        return instance;
    }

    public void setSilenceThreshold(long millis) {
        silenceThresholdMillis = millis;
    }

    public void handleMicrophonePacket(VoicechatServerApi serverApi, MicrophonePacketEvent event) {
        UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
        byte[] opusData = event.getPacket().getOpusEncodedData();

        // 유저 버퍼 없으면 생성
        audioBuffers.putIfAbsent(playerUuid, new AudioBuffer(playerUuid));
        AudioBuffer buffer = audioBuffers.get(playerUuid);

        // 길이 0 → 말이 끝났다는 신호. 단, 바로 flushX, "n초 침묵" 체크는 별도
        if (opusData.length == 0) {
            return;
        }

        // 디코딩
        buffer.setLastSpokeTime(System.currentTimeMillis());
        OpusDecoder decoder = serverApi.createDecoder();
        try {
            short[] pcm = decoder.decode(opusData);
            buffer.appendPcm(pcm);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            decoder.close();
        }
    }

    /**
     * 1초에 한번씩 호출 -> n초간 말 없으면 AudioBuffer flush
     */
    public void checkSilentPlayers() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, AudioBuffer> entry : audioBuffers.entrySet()) {
            AudioBuffer buf = entry.getValue();
            long silentTime = now - buf.getLastSpokeTime();
            if (silentTime >= silenceThresholdMillis) {
                flushBuffer(entry.getKey());
            }
        }
    }

    private void flushBuffer(UUID playerUuid) {
        AudioBuffer buffer = audioBuffers.remove(playerUuid);
        if (buffer == null) {
            return;
        }
        buffer.processAndSendToSTTAsync();
    }
}
