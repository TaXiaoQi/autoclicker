package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.Config;
import com.example.autoclicker.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MuteFeature {

    private enum MuteState {
        UNMUTED,
        MANUAL_MUTED,
        AUTO_MUTED,
        MINIMIZED_MUTED
    }

    // 静音状态持久化文件：游戏被强制结束（任务管理器/启动器强杀）时，
    // 下次启动据此恢复主音量，避免音量永远卡在 0
    private static final Path MUTE_STATE_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("autoclicker_mute_state.txt");

    private static MuteFeature instance;

    private MuteState currentState = MuteState.UNMUTED;
    private Float savedMasterVolume = null; // 保存主音量
    // 静音建立时间戳：用于排除刚静音后其他模组/音频引擎写入音量造成的竞态误判
    private long mutedAtMillis = 0;

    // 联动静音请求来源（按功能分开记录，避免计数漂移）
    private boolean attackMuteRequested = false;
    private boolean placeMuteRequested = false;

    public MuteFeature() {
        instance = this;
    }

    public static MuteFeature getInstance() {
        return instance;
    }

    // ===================== 手动静音 =====================

    /**
     * 手动切换静音：总是基于当前主音量
     */
    public void toggleManualMute() {
        if (currentState == MuteState.MANUAL_MUTED) {
            // 退出手动静音：恢复保存的音量
            restoreMasterVolume();
            currentState = MuteState.UNMUTED;
        } else if (currentState == MuteState.UNMUTED) {
            // 当前未静音 → 进入手动静音
            muteAndRemember();
            currentState = MuteState.MANUAL_MUTED;
        } else {
            // 自动静音 / 最小化静音状态下按 F7：视为用户手动取消
            restoreMasterVolume();
            currentState = MuteState.UNMUTED;
            // 清除自动请求，避免状态残留；下次功能开关事件会重新请求
            attackMuteRequested = false;
            placeMuteRequested = false;
        }
    }

    // ===================== 自动静音（联动功能） =====================

    /**
     * 自动攻击请求联动静音
     */
    public void requestAttackMute() {
        attackMuteRequested = true;
        updateAutoMute();
    }

    /**
     * 自动攻击关闭，释放联动静音
     */
    public void releaseAttackMute() {
        attackMuteRequested = false;
        updateAutoMute();
    }

    /**
     * 自动放置请求联动静音
     */
    public void requestPlaceMute() {
        placeMuteRequested = true;
        updateAutoMute();
    }

    /**
     * 自动放置关闭，释放联动静音
     */
    public void releasePlaceMute() {
        placeMuteRequested = false;
        updateAutoMute();
    }

    /**
     * 根据当前配置同步联动静音（供 GUI 开关、超时自动关闭等所有配置变化入口调用）
     */
    public void syncFeatureMute() {
        Config config = ConfigManager.getConfig();
        attackMuteRequested = config.autoAttackEnabled && config.muteOnAutoAttack;
        placeMuteRequested = config.autoPlaceEnabled && config.muteOnAutoPlace;
        updateAutoMute();
    }

    private void updateAutoMute() {
        boolean wantAutoMute = attackMuteRequested || placeMuteRequested;

        if (currentState == MuteState.UNMUTED && wantAutoMute) {
            muteAndRemember();
            currentState = MuteState.AUTO_MUTED;
        } else if (currentState == MuteState.AUTO_MUTED && !wantAutoMute) {
            restoreMasterVolume();
            currentState = MuteState.UNMUTED;
        }
    }

    // ===================== 窗口最小化静音 =====================

    /**
     * 窗口最小化静音
     */
    public void updateMinimizedMute(boolean minimized) {
        if (currentState == MuteState.MANUAL_MUTED) return;

        // 已处于最小化静音时，无论开关是否被关闭都要能正确退出该状态，避免音量卡在 0
        if (currentState == MuteState.MINIMIZED_MUTED) {
            if (!minimized) {
                restoreMasterVolume();
                currentState = MuteState.UNMUTED;
            }
            return;
        }

        if (!ConfigManager.getConfig().muteWhenMinimized) return;

        if (minimized) {
            // 已处于自动静音（AUTO_MUTED）时音量为 0，不能重新采样，
            // 否则 savedMasterVolume 会被覆盖为 0，恢复时写回 0 导致音量永久丢失
            if (currentState != MuteState.AUTO_MUTED || savedMasterVolume == null) {
                muteAndRemember();
            }
            currentState = MuteState.MINIMIZED_MUTED;
        }
    }

    /**
     * 检查用户是否在静音期间手动调大了主音量（视为用户主动取消静音）
     * 应在游戏 tick 中定期调用
     */
    public void checkUserVolumeChange() {
        if (currentState == MuteState.UNMUTED) return;

        // 静音刚建立后 1 秒内的音量读数不可信（外部模组如 Dynamic FPS / 音频引擎可能正在写入），
        // 期间不判定用户主动取消，避免误清 savedMasterVolume 导致音量无法恢复
        if (System.currentTimeMillis() - mutedAtMillis < 1000) return;

        Options options = Minecraft.getInstance().options;
        double currentMaster = options.getSoundSourceVolume(SoundSource.MASTER);

        if (currentMaster > 0.0) {
            savedMasterVolume = null;
            attackMuteRequested = false;
            placeMuteRequested = false;
            currentState = MuteState.UNMUTED;
            clearMuteStateFile();
        }
    }

    // ===================== 强制恢复 / 启动恢复 =====================

    /**
     * 强制恢复音量（只恢复自动静音，不恢复手动静音）
     */
    public void forceRestore() {
        // 无论是否处于手动静音，都先清理联动静音请求，避免残留标志影响后续 updateAutoMute 判定
        attackMuteRequested = false;
        placeMuteRequested = false;

        if (currentState == MuteState.MANUAL_MUTED) {
            return;
        }

        if (savedMasterVolume != null) {
            restoreMasterVolume();
        }
        currentState = MuteState.UNMUTED;
    }

    /**
     * 完全强制恢复（包括手动静音）- 用于游戏正常退出
     */
    public void forceRestoreAll() {
        attackMuteRequested = false;
        placeMuteRequested = false;

        if (savedMasterVolume != null) {
            restoreMasterVolume();
        }
        currentState = MuteState.UNMUTED;
    }

    /**
     * 游戏启动时检查上次是否因强制结束未恢复音量，是则恢复。
     * 需在 Options 加载完成后调用（如 CLIENT_STARTED 事件）。
     */
    public void recoverSavedVolume() {
        if (!Files.exists(MUTE_STATE_PATH)) {
            return;
        }
        try {
            // Java 8 兼容：readString 是 Java 11 API，改用 readAllBytes
            String content = new String(Files.readAllBytes(MUTE_STATE_PATH), java.nio.charset.StandardCharsets.UTF_8);
            double volume = Double.parseDouble(content.trim());
            Options options = Minecraft.getInstance().options;
            // 音量写入方式各版本不同（1.19.2 及以下 setSoundCategoryVolume，1.19.3+ OptionInstance），走版本适配层
            com.example.autoclicker.tool.MasterVolume.set(options, volume);
            Main.LOGGER.info("已从上次异常退出中恢复主音量: {}", volume);
        } catch (Exception e) {
            Main.LOGGER.error("恢复主音量失败", e);
        } finally {
            clearMuteStateFile();
        }
    }

    // ===================== 内部工具 =====================

    /**
     * 记住当前音量并静音，同时把原音量持久化到磁盘
     */
    private void muteAndRemember() {
        Options options = Minecraft.getInstance().options;
        savedMasterVolume = options.getSoundSourceVolume(SoundSource.MASTER);
        com.example.autoclicker.tool.MasterVolume.set(options, 0.0);
        mutedAtMillis = System.currentTimeMillis();
        persistMuteState(savedMasterVolume.doubleValue());
    }

    private void persistMuteState(double volume) {
        try {
            // Java 8 兼容：writeString 是 Java 11 API，改用 write
            Files.write(MUTE_STATE_PATH, String.valueOf(volume).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            Main.LOGGER.error("静音状态持久化失败", e);
        }
    }

    private void restoreMasterVolume() {
        if (savedMasterVolume != null) {
            Options options = Minecraft.getInstance().options;
            com.example.autoclicker.tool.MasterVolume.set(options, savedMasterVolume.doubleValue());
            savedMasterVolume = null;
        }
        clearMuteStateFile();
    }

    private void clearMuteStateFile() {
        try {
            Files.deleteIfExists(MUTE_STATE_PATH);
        } catch (IOException e) {
            Main.LOGGER.error("清理静音状态文件失败", e);
        }
    }

    public boolean isManuallyMuted() {
        return currentState == MuteState.MANUAL_MUTED;
    }
}
