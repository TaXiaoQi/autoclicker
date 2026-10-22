package com.example.autoclicker.tool;

import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

/**
 * 主音量读写适配：1.19.2 及之前无 OptionInstance，使用 setSoundCategoryVolume。
 */
public final class MasterVolume {

    private MasterVolume() {
    }

    public static void set(Options options, double volume) {
        options.setSoundCategoryVolume(SoundSource.MASTER, (float) volume);
        options.save();
    }
}
