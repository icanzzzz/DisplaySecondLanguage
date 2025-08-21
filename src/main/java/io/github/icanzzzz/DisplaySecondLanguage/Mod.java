package io.github.icanzzzz.DisplaySecondLanguage;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@net.neoforged.fml.common.Mod(Mod.MODID)
public class Mod {
    public static final String MODID = "displaysecondlanguage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Mod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
