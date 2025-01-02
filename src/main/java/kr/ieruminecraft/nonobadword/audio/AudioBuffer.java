package kr.ieruminecraft.nonobadword.audio;

import kr.ieruminecraft.nonobadword.stt.ChatLogger;
import kr.ieruminecraft.nonobadword.Nonobadword;
import kr.ieruminecraft.nonobadword.stt.OpenAiTranscriber;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 한 유저의 PCM 데이터를 누적 & n초 침묵시 flush
 */
public class AudioBuffer {

    private final UUID playerUuid;
    private final ByteArrayOutputStream pcmData;
    private volatile long lastSpokeTime;

    public AudioBuffer(UUID uuid) {
        this.playerUuid = uuid;
        this.pcmData = new ByteArrayOutputStream();
        this.lastSpokeTime = System.currentTimeMillis();
    }

    public long getLastSpokeTime() {
        return lastSpokeTime;
    }

    public void setLastSpokeTime(long t) {
        this.lastSpokeTime = t;
    }

    // 16bit PCM(short[]) -> byte[] (little-endian)
    public void appendPcm(short[] pcm) {
        for (short s : pcm) {
            pcmData.write(s & 0xff);
            pcmData.write((s >> 8) & 0xff);
        }
        long audioLength = pcmData.size() / (48000 * 2 / 1000); // bytes to ms
        if (audioLength >= Nonobadword.getInstance().getMaxBufferSizeMs()) {
            processAndSendToSTTAsync();
            lastSpokeTime = System.currentTimeMillis();
            pcmData.reset();
        }
    }

    public void processAndSendToSTTAsync() {
        long audioLength = pcmData.size() / (48000 * 2 / 1000); // bytes to ms (48kHz, 16bit)
        if (audioLength < Nonobadword.getInstance().getMinAudioLengthMs()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(
                Nonobadword.getInstance(),
                () -> {
                    File wavFile = null;
                    try {
                        // 1) PCM -> WAV 파일
                        wavFile = pcmToWav();
                        // 2) OpenAI Whisper
                        String recognizedText = OpenAiTranscriber.transcribe(wavFile);
                        // 3) 액션바 & YML 로그
                        handleRecognizedText(recognizedText);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (wavFile != null && wavFile.exists()) {
                            wavFile.delete();
                        }
                    }
                }
        );
    }

    private File pcmToWav() throws IOException {
        byte[] rawPcm = pcmData.toByteArray();
        int totalAudioLen = rawPcm.length;
        int totalDataLen = totalAudioLen + 36;
        int sampleRate = 48000; // SVC 기본값
        int channels = 1;       // 모노
        int byteRate = sampleRate * channels * 2;

        File tempFile = File.createTempFile("voicechat_", ".wav");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // WAV header
            fos.write(new byte[]{'R', 'I', 'F', 'F'});
            writeInt(fos, totalDataLen);
            fos.write(new byte[]{'W', 'A', 'V', 'E'});
            // fmt chunk
            fos.write(new byte[]{'f', 'm', 't', ' '});
            writeInt(fos, 16);
            writeShort(fos, (short) 1);
            writeShort(fos, (short) channels);
            writeInt(fos, sampleRate);
            writeInt(fos, byteRate);
            writeShort(fos, (short) (channels * 2));
            writeShort(fos, (short) 16);
            // data chunk
            fos.write(new byte[]{'d', 'a', 't', 'a'});
            writeInt(fos, totalAudioLen);
            // PCM
            fos.write(rawPcm);
        }
        return tempFile;
    }

    private void writeInt(FileOutputStream fos, int val) throws IOException {
        fos.write(val & 0xff);
        fos.write((val >> 8) & 0xff);
        fos.write((val >> 16) & 0xff);
        fos.write((val >> 24) & 0xff);
    }
    private void writeShort(FileOutputStream fos, short val) throws IOException {
        fos.write(val & 0xff);
        fos.write((val >> 8) & 0xff);
    }

    private void handleRecognizedText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        ChatLogger.getInstance().logText(playerUuid, text);
    }
}
