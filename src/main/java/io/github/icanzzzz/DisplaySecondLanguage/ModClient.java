package io.github.icanzzzz.DisplaySecondLanguage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


@net.neoforged.fml.common.Mod(value = Mod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID,value = Dist.CLIENT)
public class ModClient {
    public static final String MODID;
    private static final File mcassetsDir;// get minecraft roaming file
    private static final Map<String,Map<String,String>> langsIndex;
    private static final Map<String,String> defaultLangs;
    private static Map<String,String> langs;

    static {
        MODID = "displaysecondlanguage";

        //get assets path
        File dir = Minecraft.getInstance().gameDirectory;
        for (; dir != null && dir.exists(); dir = dir.getParentFile()) {

            // ".minecraft" before dir.getName().Because maybe dir.getName() is null,so use null.equals() can error
            if(".minecraft".equals(dir.getName())) {
                break;
            }
        }
        mcassetsDir = new File(dir, "assets");

        // initialize langsIndexes
        // get language indexes file
        String content="";
        try {
            // Index file corresponding version query website:https://zh.minecraft.wiki/w/%E6%95%A3%E5%88%97%E8%B5%84%E6%BA%90%E6%96%87%E4%BB%B6?variant=zh
            content = new String(
                    Files.readAllBytes(
                            new File(mcassetsDir, "indexes/17.json").toPath()
                    ),
                    StandardCharsets.UTF_8  // must specify utf-8.Because there may be error code.
            );
        } catch (IOException e) {
            Mod.LOGGER.info(e.getMessage());
        }

        if(content.length() <= 13) {
            Mod.LOGGER.info("index or indexList is empty");
            langsIndex = new HashMap<>();
        }
        else{
            langsIndex = new Gson().fromJson(
                    content.substring(12,content.length()-1),
                    new TypeToken<Map<String,Map<String,String>>>(){}.getType()
            );
        }

        // initialize defaultLangs
        InputStream inputStream = Mod.class.getResourceAsStream("/data/langs/en_us.json");  // get current version en_us.json
        if(inputStream == null) {
            Mod.LOGGER.info("not found default lang file");
            defaultLangs = new HashMap<>();
        }
        else{
            defaultLangs = new Gson().fromJson(new InputStreamReader(inputStream),new TypeToken<Map<String,String>>(){}.getType());
        }
        langs = defaultLangs;
    }

    public ModClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static Map<String, Map<String, String>> getLangsIndexes() {
        return langsIndex;
    }

    private static void loadSecondLanguage(String selectLang) {
        if(selectLang.equals("en_us"))  langs = defaultLangs;
        if(langsIndex.get("minecraft/lang/" + selectLang + ".json")==null)  return;

        String content="";
        String hashValue = langsIndex.get("minecraft/lang/" + selectLang + ".json").get("hash");
        // get language file
        try{
            content = new String(
                    Files.readAllBytes(
                            new File(
                                    mcassetsDir,
                                    "objects/" + hashValue.substring(0,2) + "/" + hashValue
                            ).toPath()
                    ),
                    StandardCharsets.UTF_8  // must specify utf-8.Because there may be error code.
            );
        } catch (IOException e) {
            Mod.LOGGER.info("Failed to read lang file:" + e.getMessage());
        }

        langs = new Gson().fromJson(
                content,
                new TypeToken<Map<String,String>>(){}.getType()
        );
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        String transString = langs.get(event.getItemStack().getItem().getDescriptionId());
        if(transString==null || transString.isEmpty()) return;

        event.getToolTip().add(
                1,
                Component.literal(transString)
        );
    }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent.Loading event) {
        if(event.getConfig().getSpec() != Config.SPEC) return;
        loadSecondLanguage(Config.selectlang.get());
    }

    @SubscribeEvent
    static void onConfigReload(ModConfigEvent.Reloading event) {
        if(event.getConfig().getSpec() != Config.SPEC) return;
        loadSecondLanguage(Config.selectlang.get());
    }
}