package com.example.autoclicker.tool;

import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

/**
 * 主音量读写适配：1.19.3+ 使用 OptionInstance 体系。
 */
public final class MasterVolume {

    private MasterVolume() {
    }

    public static void set(Options options, double volume) {
        options.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
        options.save();
    }
}
