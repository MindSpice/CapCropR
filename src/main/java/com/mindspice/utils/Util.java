package com.mindspice.utils;

import com.mindspice.Main;
import javafx.scene.control.SplitPane;
import javafx.scene.control.skin.SplitPaneSkin;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Util {
    private static String lastOpened;

    public static String parseCaption(File file) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return IOUtils.toString(fis, StandardCharsets.UTF_8);

        } catch(IOException e) {
            return "";
        }
    }

    public static File openDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (lastOpened != null) {
            directoryChooser.setInitialDirectory(new File(lastOpened));
        }
        File dir = directoryChooser.showDialog(Main.stage);
        lastOpened = dir.getParent();
        return dir;
    }


    public static class SplitPassThrough extends SplitPaneSkin {
        public SplitPassThrough(SplitPane splitPane) {
            super(splitPane);
            consumeMouseEvents(false);
        }
    }
}
