package com.mindspice;

import com.mindspice.Settings.Bind;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public class State {
    public static File imageDirectory;
    public static File saveDirectory;
    public static List<ImageData> images = new ArrayList<>();
    public static EnumMap<KeyCode, Bind> bindings = new EnumMap<>(KeyCode.class);
}
