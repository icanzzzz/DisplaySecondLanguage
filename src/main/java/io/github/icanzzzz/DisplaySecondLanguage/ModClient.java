package io.github.icanzzzz.DisplaySecondLanguage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
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

        Gson gson = new Gson();

        langsIndex = gson.fromJson(
                gson.toJson(jsonToData(
                        new File(mcassetsDir,"indexes/17.json"),
                        new TypeToken<Map<String,Map<String,Map<String,String>>>>(){}
                ).get("objects")),
                new TypeToken<Map<String,Map<String,String>>>(){}.getType()
        );

        // initialize defaultLangs
        InputStream inputStream = Mod.class.getResourceAsStream("/data/langs/en_us.json");  // get current version en_us.json
        if(inputStream == null) {
            Mod.LOGGER.info("not found default lang file");
            defaultLangs = new HashMap<>();
        }
        else{
            defaultLangs = gson.fromJson(new InputStreamReader(inputStream),new TypeToken<Map<String,String>>(){}.getType());
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
        if(langsIndex == null)  return;
        if(langsIndex.get("minecraft/lang/" + selectLang + ".json")==null)  return;

        String hashValue = langsIndex.get("minecraft/lang/" + selectLang + ".json").get("hash");
        langs = jsonToData(
                new File(mcassetsDir,"objects/" + hashValue.substring(0,2) + "/" + hashValue),
                new TypeToken<Map<String,String>>(){}
        );
    }

    /**
     * Internally, It is Google's Gson implementation
     *
     * @param file - The target json file.
     * @param typeToken - The type is you need type.
     * @return - The type of typeToken You entered.
     * @param <T>
     */
    private static <T> T jsonToData(File file, TypeToken<T> typeToken){
        String content="";
        try {
            content = new String(
                    Files.readAllBytes(file.toPath()),
                    StandardCharsets.UTF_8  // must specify utf-8.Because there may be error code.
            );
        } catch (IOException e) {
            Mod.LOGGER.info("Failed to read lang file:" + e.getMessage());
        }

        return new Gson().fromJson(content, typeToken);
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if(langs == null)   return;

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