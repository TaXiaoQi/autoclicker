package com.example.autoclicker.feature;

import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import java.util.EnumMap;
import java.util.Map;


public class MuteFeature {
    private static class VolumeState {
        private final Map<SoundSource, Float> volumes = new EnumMap<>(SoundSource.class);

        public void save(Options options) {
            for (SoundSource source : SoundSource.values()) {
                // 正确获取音量值
                volumes.put(source, options.getSoundSourceVolume(source));
            }
        }

        public void restore(Options options) {
            for (Map.Entry<SoundSource, Float> entry : volumes.entrySet()) {
                // 正确设置音量值 - 通过 OptionInstance
                options.getSoundSourceOptionInstance(entry.getKey()).set(entry.getValue().doubleValue());
            }
            options.save();
        }

        public boolean isEmpty() {
            return volumes.isEmpty();
        }
    }

    private enum MuteState {
        UNMUTED,       // 未静音
        MANUAL_MUTED,  // 手动静音
        AUTO_MUTED,    // 自动功能静音
        MINIMIZED_MUTED // 最小化静音
    }

    private MuteState currentState = MuteState.UNMUTED;
    private final VolumeState savedVolume = new VolumeState();

    private int autoFeatureMuteCount = 0;
    /**
     * 一键静音/恢复（手动触发）
     */
    public void toggleManualMute() {
        Options options = Minecraft.getInstance().options;

        if (currentState == MuteState.UNMUTED) {
            // 从非静音状态变为手动静音
            savedVolume.save(options);
            muteAllVolumes(options);
            currentState = MuteState.MANUAL_MUTED;
        } else if (currentState == MuteState.MANUAL_MUTED) {
            // 从手动静音恢复
            savedVolume.restore(options);
            savedVolume.volumes.clear();
            currentState = MuteState.UNMUTED;
        } else {
            // 从其他静音状态切换到手动静音
            VolumeState tempState = new VolumeState();

            if (currentState == MuteState.AUTO_MUTED || currentState == MuteState.MINIMIZED_MUTED) {
                // 1. 先恢复原来的音量
                savedVolume.restore(options);

                // 2. 保存当前音量（作为以后恢复的参考）
                tempState.save(options);

                // 3. 静音所有音量
                muteAllVolumes(options);

                // 4. 更新保存的音量
                savedVolume.volumes.clear();
                savedVolume.volumes.putAll(tempState.volumes);
            }

            currentState = MuteState.MANUAL_MUTED;
        }
    }

    /**
     * 静音所有音源
     */
    private void muteAllVolumes(Options options) {
        for (SoundSource source : SoundSource.values()) {
            // 正确设置音量为0
            options.getSoundSourceOptionInstance(source).set(0.0);
        }
        options.save();
    }

    /**
     * 根据配置更新自动功能静音状态
     */
    public void updateAutoFeatureMute(boolean shouldMute) {
        if (currentState == MuteState.MANUAL_MUTED) {
            return; // 手动静音优先级最高
        }

        Options options = Minecraft.getInstance().options;

        if (shouldMute && currentState == MuteState.UNMUTED) {
            // 需要静音：保存当前音量并静音
            savedVolume.save(options);
            muteAllVolumes(options);
            currentState = MuteState.AUTO_MUTED;
        } else if (!shouldMute && currentState == MuteState.AUTO_MUTED) {
            // 需要恢复：只有自动功能静音时才恢复
            savedVolume.restore(options);
            savedVolume.volumes.clear();
            currentState = MuteState.UNMUTED;
        }
    }

    /**
     * 更新最小化静音状态
     */
    public void updateMinimizedMute(boolean minimized) {
        var config = ConfigManager.getConfig();
        if (!config.muteWhenMinimized || currentState == MuteState.MANUAL_MUTED) {
            return;
        }

        Options options = Minecraft.getInstance().options;

        if (minimized && currentState == MuteState.UNMUTED) {
            // 最小化时静音
            savedVolume.save(options);
            muteAllVolumes(options);
            currentState = MuteState.MINIMIZED_MUTED;
        } else if (!minimized && currentState == MuteState.MINIMIZED_MUTED) {
            // 恢复窗口时恢复音量
            savedVolume.restore(options);
            savedVolume.volumes.clear();
            currentState = MuteState.UNMUTED;
        }
    }

    /**
     * 强制恢复所有音频（用于退出游戏等场景）
     */
    public void forceRestore() {
        if (currentState != MuteState.UNMUTED && !savedVolume.isEmpty()) {
            savedVolume.restore(Minecraft.getInstance().options);
            savedVolume.volumes.clear();
            currentState = MuteState.UNMUTED;
        }
    }

    /**
     * 检查是否处于手动静音状态
     */
    public boolean isManuallyMuted() {
        return currentState == MuteState.MANUAL_MUTED;
    }

    public void requestAutoMute() {
        if (autoFeatureMuteCount == 0) {
            updateAutoFeatureMute(true);
        }
        autoFeatureMuteCount++;
    }

    public void releaseAutoMute() {
        if (autoFeatureMuteCount > 0) {
            autoFeatureMuteCount--;
            if (autoFeatureMuteCount == 0) {
                updateAutoFeatureMute(false);
            }
        }
    }
}