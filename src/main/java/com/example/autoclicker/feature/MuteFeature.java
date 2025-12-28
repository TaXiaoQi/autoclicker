package com.example.autoclicker.feature;

import com.example.autoclicker.config.ConfigManager;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class MuteFeature {
    private enum MuteReason { NONE, AUTO_FEATURE, MINIMIZED, MANUAL }
    private MuteReason currentReason = MuteReason.NONE;
    private Float savedVolume = null;
    private boolean isManuallyMuted = false;

    public void tick(Minecraft client) {
        // 检查是否应该静音
        boolean shouldMuteByFeature = shouldMuteByFeature();
        boolean shouldMuteByMinimize = shouldMuteByMinimize(client);

        // 应用静音逻辑
        if (shouldMuteByFeature || shouldMuteByMinimize) {
            if (currentReason == MuteReason.NONE && !isManuallyMuted) {
                setMuted(true, MuteReason.AUTO_FEATURE);
            }
        } else if (currentReason == MuteReason.AUTO_FEATURE && !isManuallyMuted) {
            setMuted(false, MuteReason.NONE);
        }
    }

    private boolean shouldMuteByFeature() {
        var config = ConfigManager.getConfig();
        boolean attackActive = config.autoAttackEnabled && config.muteOnAutoAttack;
        boolean placeActive = config.autoPlaceEnabled && config.muteOnAutoPlace;
        return attackActive || placeActive;
    }

    private boolean shouldMuteByMinimize(Minecraft client) {
        if (!ConfigManager.getConfig().muteWhenMinimized) {
            return false;
        }
        // 检查窗口是否最小化（简化实现）
        return !client.isWindowActive();
    }

    public void toggleManualMute() {
        isManuallyMuted = !isManuallyMuted;
        if (isManuallyMuted) {
            setMuted(true, MuteReason.MANUAL);
        } else {
            setMuted(false, MuteReason.NONE);
        }
    }

    public void forceRestore() {
        if (!isManuallyMuted) {
            setMuted(false, MuteReason.NONE);
        }
    }

    private void setMuted(boolean muted, MuteReason reason) {
        Options options = Minecraft.getInstance().options;

        if (muted) {
            if (currentReason == MuteReason.NONE) {
                // 保存当前音量
                savedVolume = options.masterVolume().get();
                // 设置音量为0（静音）
                options.masterVolume().set(0.0);
                options.save();
            }
            currentReason = reason;
        } else {
            if (savedVolume != null) {
                // 恢复之前保存的音量
                options.masterVolume().set(savedVolume.doubleValue());
                options.save();
                savedVolume = null;
            }
            currentReason = MuteReason.NONE;
        }
    }

    public boolean isManuallyMuted() {
        return isManuallyMuted;
    }

    public void updateMinimizedMute(boolean minimized) {
        // 窗口最小化静音逻辑
        if (minimized && ConfigManager.getConfig().muteWhenMinimized) {
            if (currentReason == MuteReason.NONE && !isManuallyMuted) {
                setMuted(true, MuteReason.MINIMIZED);
            }
        } else if (currentReason == MuteReason.MINIMIZED && !isManuallyMuted) {
            setMuted(false, MuteReason.NONE);
        }
    }

    public void setMutedByAutoFeature(boolean muted) {
        if (isManuallyMuted) return; // 手动静音优先级更高

        if (muted) {
            if (currentReason == MuteReason.NONE) {
                setMuted(true, MuteReason.AUTO_FEATURE);
            }
        } else if (currentReason == MuteReason.AUTO_FEATURE) {
            setMuted(false, MuteReason.NONE);
        }
    }

    public void updateAutoFeatureMute(boolean shouldMute) {
        setMutedByAutoFeature(shouldMute);
    }
}