package io.github.icanzzzz.DisplaySecondLanguage;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder()
            .comment("General settings")
            .push("general");

    public static ModConfigSpec.ConfigValue<String> selectlang = BUILDER
            .comment("current language")
            .define("language","en_us");
    static final ModConfigSpec SPEC = BUILDER.build();
}