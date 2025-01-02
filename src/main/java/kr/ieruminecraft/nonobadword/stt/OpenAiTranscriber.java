package kr.ieruminecraft.nonobadword.stt;

import kr.ieruminecraft.nonobadword.Nonobadword;
import okhttp3.*;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * WAV 파일 -> OpenAI Whisper API
 */
public class OpenAiTranscriber {

    private static final String TRANSCRIBE_ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";

    public static String transcribe(File wavFile) {
        String apiKey = Nonobadword.getInstance().getOpenAiApiKey();
        if (apiKey == null || apiKey.equalsIgnoreCase("NO_KEY_FOUND")) {
            Nonobadword.getInstance().getLogger().warning("[OpenAiTranscriber] API Key not set!");
            return "";
        }

        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        builder.addFormDataPart("model", "whisper-1");

        RequestBody fileBody = RequestBody.create(wavFile, MediaType.parse("audio/wav"));
        builder.addFormDataPart("file", wavFile.getName(), fileBody);
        builder.addFormDataPart("language", "ko");

        Request request = new Request.Builder()
                .url(TRANSCRIBE_ENDPOINT)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(builder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Nonobadword.getInstance().getLogger().warning("[OpenAiTranscriber] STT request failed: "
                        + response.code() + " / " + response.message());
                return "";
            }
            String body = response.body().string();
            JSONObject json = new JSONObject(body);
            String text = json.optString("text", "");
            Nonobadword.getInstance().getLogger().info(String.format(
                    "[STT] Voice transcribed - Timestamp: %s, Text: %s",
                    wavFile.getName().replace("voicechat_", "").replace(".wav", ""),
                    text
            ));
            return text;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
