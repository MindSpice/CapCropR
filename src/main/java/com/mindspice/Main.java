package com.mindspice;


import com.mindspice.Settings.Settings;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;

public class Main extends Application {
    public static Stage stage;
    public static Scene scene;
    public static Scale scale;


    @Override
    public void start(javafx.stage.Stage stage) throws Exception {
        final int initWidth = 1920;
        final int initHeight = 1080;
        final Pane root = new Pane();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui.fxml"));
        Pane controller = loader.load();
        controller.setPrefWidth(initWidth);
        controller.setPrefHeight(initHeight);
        root.getChildren().add(controller);
        controller.setFocusTraversable(true);

        scale = new Scale(1, 1, 0, 0);
        scale.xProperty().bind(root.widthProperty().divide(initWidth));
        scale.yProperty().bind(root.heightProperty().divide(initHeight));
        root.getTransforms().add(scale);

        scene = new Scene(root, initWidth, initHeight);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setTitle("CapNProc");
        //Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
        //stage.getIcons().add(icon);


        Controller cont = (Controller)loader.getController();
        scene.setOnKeyPressed(cont::onKey);
        scene.setOnScroll(cont::onScroll);
        scene.setOnMouseClicked(cont::onMouse);
        cont.init();

        stage.show();
        //Listens for and rescales window on resize
        scene.rootProperty().addListener(new ChangeListener<Parent>() {
            @Override
            public void changed(ObservableValue<? extends Parent> arg0, Parent oldValue, Parent newValue) {
                scene.rootProperty().removeListener(this);
                scene.setRoot(root);
                ((Region) newValue).setPrefWidth(initWidth);
                ((Region) newValue).setPrefHeight(initHeight);
                root.getChildren().clear();
                root.getChildren().add(newValue);
                scene.rootProperty().addListener(this);
            }
        });
        stage.setFullScreen(Settings.get().fullscreen);
//        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
//            @Override
//            public void handle(WindowEvent event) {
//                if (!Util.confirm(Util.ErrorType.EXIT)) {
//                    event.consume();
//                }
//            }
//        });
//    }
    }

    public static void main(String[] args) throws InterruptedException {
        if (Settings.get().outputDirectories != null
                && !Settings.get().outputDirectories.isEmpty()) {
            State.saveDirectory = Settings.get().outputDirectories.get(0);
        }
        if (Settings.get().bindings != null) {
            for (var bind : Settings.get().bindings) {
                State.bindings.put(bind.key, bind);
            }
        }

        launch(args);
    }
}
