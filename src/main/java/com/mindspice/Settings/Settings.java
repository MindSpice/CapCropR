package com.mindspice.Settings;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


import org.imgscalr.Scalr;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class Settings {
    private static Settings instance;
    public int cropWidth = 512;
    public int cropHeight = 512;
    public int cropIncDecScrollW = 8;
    public int cropIncDecScrollH = 8;
    public Scalr.Method editorQuality;
    public Scalr.Method previewQuality;
    public Scalr.Method saveQuality;
    public int[] cropOutlineColor = new int[]{255,0,0};
    public int cropLineWidth = 4;
    public boolean overWriteByDefault = false;
    public boolean fullscreen = false;
    public boolean confirmDialog = false;
    public boolean alwaysSaveFlipped = false;
    public List<File> outputDirectories;
    public List<Bind> bindings;

    public enum Binding {
        SAVE,
        SAVE_CONT,
        SAVE_CAP,
        SAVE_FLIPPED,
        PREVIEW,
        CROP_ALTER,
        CROP_RESIZE,
        FORWARD,
        BACK,
    }

    public static Settings get(){
        if (instance == null) {
            loadConfig();
        }
        return instance;
    }

    private static void loadConfig() {
        var yaml = new ObjectMapper(new YAMLFactory());
        yaml.enable((DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        var userDir = System.getProperty("user.dir") + File.separator;
        var configFile = new File((userDir + "config.yaml"));

        try (var fr = new FileReader(configFile)) {
            instance = yaml.readValue(fr, Settings.class);
        } catch (IOException e) {
            // Add dialog
            throw new RuntimeException(e);
        }
    }


    // Used when building to gen default config, set to read only, users can just copy it if needed
    private static void writeEmptyConfig() {
        var yaml = new ObjectMapper(new YAMLFactory());
        var userDir = System.getProperty("user.dir") + File.separator;
        var defaultConfig = new File(userDir + "default.yaml");

        try (final FileWriter fw = new FileWriter(defaultConfig)) {
            fw.write(yaml.writeValueAsString(instance));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
