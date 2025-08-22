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
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


@net.neoforged.fml.common.Mod(value = Mod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID,value = Dist.CLIENT)
public class ModClient {
    public static final String MODID = "displaysecondlanguage";
    private static final File mcHashResourceFIleDir;// get minecraft roaming file
    private static final Map<String,Map<String,String>> langsIndex;
    private static final Map<String,String> defaultLangs;
    private static Map<String,String> langs;
    private static final Gson gson = new Gson();

    static {
        mcHashResourceFIleDir = loadmcHashResourceFIle();
        langsIndex = loadlangsIndex();
        defaultLangs = loaddefaultLangs();
    }

    public ModClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
    
    private static File loadmcHashResourceFIle(){
        File dir = Minecraft.getInstance().gameDirectory;
        for (; dir != null && dir.exists(); dir = dir.getParentFile()) {

            // ".minecraft" before dir.getName().Because maybe dir.getName() is null,so use null.equals() can error
            if (".minecraft".equals(dir.getName())) {
                break;
            }
        }
        return new File(dir, "assets");
    }
    
    private static Map<String, Map<String, String>> loadlangsIndex() {
        // Version corresponding file query URL: https://zh.minecraft.wiki/w/%E6%95%A3%E5%88%97%E8%B5%84%E6%BA%90%E6%96%87%E4%BB%B6?variant=zh
        File indexFile = new File(mcHashResourceFIleDir, "indexes/17.json");
        Map<String, Map<String, Map<String, String>>> indexData = jsonToData(
                indexFile,
                new TypeToken<Map<String, Map<String, Map<String, String>>>>() {}
        );

        if (indexData != null && indexData.containsKey("objects")) {
            return gson.fromJson(
                    gson.toJson(indexData.get("objects")),
                    new TypeToken<Map<String, Map<String, String>>>() {}
            );
        }
        return null;
    }

    private static Map<String,String> loaddefaultLangs(){
        String error = null;
        Map<String,String> tempLangs = null;
        try (JarFile jar = new JarFile(new File(Minecraft.getInstance().gameDirectory, "1.21.1-NeoForge.jar"));){
            error = "jar.getJarEntry()";

            JarEntry entry = jar.getJarEntry("assets/minecraft/lang/en_us.json");

            if(entry != null && !entry.isDirectory()){
                error = "jar.getInputStream()";
                try(
                        InputStream inputStream = jar.getInputStream(entry);
                        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                ){
                    error = "InputStreamReader()";
                    tempLangs = gson.fromJson(reader,new TypeToken<Map<String, String>>() {});
                }
            }
        } catch (IOException e) {
            Mod.LOGGER.info("Fail to "+ error + e.getMessage());
        }
        return tempLangs;
    }

    public static Map<String, Map<String, String>> getLangsIndexes() {
        return langsIndex;
    }
    
    private static Map<String,String> loadSecondLanguage(String selectLang) {
        if(selectLang.equals("en_us"))  return defaultLangs;
        if(langsIndex == null)  return langs;
        if(langsIndex.get("minecraft/lang/" + selectLang + ".json")==null)  return langs;

        String hashValue = langsIndex.get("minecraft/lang/" + selectLang + ".json").get("hash");
        return jsonToData(
                new File(mcHashResourceFIleDir,"objects/" + hashValue.substring(0,2) + "/" + hashValue),
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

    private static <T> T jsonToData(String content, TypeToken<T> typeToken){
        if(content==null) return null;

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
        langs = loadSecondLanguage(Config.selectlang.get());
    }
}