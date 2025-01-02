package kr.ieruminecraft.nonobadword.audio;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import kr.ieruminecraft.nonobadword.Nonobadword;

/**
 * Simple Voice Chat과 연동되는 "Addon" 클래스
 * VoicechatPlugin 구현체
 */
public class VoicechatAddon implements VoicechatPlugin {

    private VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        // SVC에서 내부 식별자로 사용
        return "onlynicewords";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi) {
            serverApi = (VoicechatServerApi) api;
            Nonobadword.getInstance().getLogger().info("[VoicechatAddon] SVC Server API initialized!");
        } else {
            Nonobadword.getInstance().getLogger().warning("[VoicechatAddon] Failed to initialize: not server API!");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // 마이크 패킷 이벤트
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        Nonobadword.getInstance().getLogger().info("[VoicechatAddon] MicrophonePacketEvent registered.");
    }

    /**
     * 플레이어가 마이크로 말할 때마다 호출
     */
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        // serverApi가 null이면 초기화가 안 된 상태
        if (serverApi == null) {
            return;
        }
        AudioHandler.getInstance().handleMicrophonePacket(serverApi, event);
    }
}
