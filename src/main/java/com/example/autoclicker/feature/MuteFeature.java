package com.example.autoclicker.feature;

import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

public class MuteFeature {

    private enum MuteState {
        UNMUTED,
        MANUAL_MUTED,
        AUTO_MUTED,
        MINIMIZED_MUTED
    }

    private MuteState currentState = MuteState.UNMUTED;
    private Float savedMasterVolume = null; // 只保存主音量

    private int autoFeatureMuteCount = 0;

    /**
     * 手动切换静音：总是基于当前主音量
     */
    public void toggleManualMute() {
        Options options = Minecraft.getInstance().options;

        if (currentState == MuteState.MANUAL_MUTED) {
            // 退出手动静音：恢复保存的音量
            if (savedMasterVolume != null) {
                options.getSoundSourceOptionInstance(SoundSource.MASTER).set(savedMasterVolume.doubleValue());
                options.save();
            }
            savedMasterVolume = null;
            currentState = MuteState.UNMUTED;
        } else {
            // 不管当前是 AUTO 还是 MINIMIZED，都视为“用户要取消静音”
            // 但我们先检查：是否真的需要恢复？

            // 如果当前是静音状态（非 MANUAL），说明 savedMasterVolume 是原始音量
            if (currentState != MuteState.UNMUTED && savedMasterVolume != null) {
                // 恢复原始音量
                options.getSoundSourceOptionInstance(SoundSource.MASTER).set(savedMasterVolume.doubleValue());
                options.save();
                savedMasterVolume = null;
                currentState = MuteState.UNMUTED;
            } else {
                // 当前未静音（UNMUTED）→ 进入手动静音
                savedMasterVolume = (float) options.getSoundSourceVolume(SoundSource.MASTER);
                options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                options.save();
                currentState = MuteState.MANUAL_MUTED;
            }
        }
    }

    /**
     * 自动功能请求静音
     */
    public void requestAutoMute() {
        if (currentState == MuteState.MANUAL_MUTED) return;

        if (autoFeatureMuteCount == 0) {
            Options options = Minecraft.getInstance().options;
            if (currentState == MuteState.UNMUTED) {
                savedMasterVolume = (float) options.getSoundSourceVolume(SoundSource.MASTER);
                options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                options.save();
                currentState = MuteState.AUTO_MUTED;
            }
        }
        autoFeatureMuteCount++;
    }

    public void releaseAutoMute() {
        if (currentState == MuteState.MANUAL_MUTED) return;
        if (autoFeatureMuteCount <= 0) return;

        autoFeatureMuteCount--;
        if (autoFeatureMuteCount == 0 && currentState == MuteState.AUTO_MUTED) {
            restoreMasterVolume();
            currentState = MuteState.UNMUTED;
        }
    }

    /**
     * 窗口最小化静音
     */
    public void updateMinimizedMute(boolean minimized) {
        if (currentState == MuteState.MANUAL_MUTED) return;
        if (!ConfigManager.getConfig().muteWhenMinimized) return;

        Options options = Minecraft.getInstance().options;

        if (minimized && currentState == MuteState.UNMUTED) {
            savedMasterVolume = (float) options.getSoundSourceVolume(SoundSource.MASTER);
            options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            options.save();
            currentState = MuteState.MINIMIZED_MUTED;
        } else if (!minimized && currentState == MuteState.MINIMIZED_MUTED) {
            restoreMasterVolume();
            currentState = MuteState.UNMUTED;
        }
    }

    /**
     * 检查用户是否在静音期间修改了主音量
     * 应在游戏 tick 或渲染循环中定期调用（例如每帧）
     */
    public void checkUserVolumeChange() {
        if (currentState == MuteState.UNMUTED) return;

        Options options = Minecraft.getInstance().options;
        double currentMaster = options.getSoundSourceVolume(SoundSource.MASTER);

        // 如果当前主音量 > 0，说明用户手动调高了音量
        // 或者即使调低了但 ≠ 0，也视为干预（因为静音时应为 0）
        if (currentMaster > 0.0) {
            // 用户干预：退出所有静音，回到开放状态
            savedMasterVolume = null;
            currentState = MuteState.UNMUTED;
            // 注意：不恢复音量！因为用户已经设了他想要的值
        }
    }

    private void restoreMasterVolume() {
        if (savedMasterVolume != null) {
            Options options = Minecraft.getInstance().options;
            options.getSoundSourceOptionInstance(SoundSource.MASTER).set(savedMasterVolume.doubleValue());
            options.save();
            savedMasterVolume = null;
        }
    }

    public void forceRestore() {
        if (savedMasterVolume != null) {
            restoreMasterVolume();
        }
        currentState = MuteState.UNMUTED;
        autoFeatureMuteCount = 0;
    }

    public boolean isManuallyMuted() {
        return currentState == MuteState.MANUAL_MUTED;
    }

    public boolean isMuted() {
        return currentState != MuteState.UNMUTED;
    }
}