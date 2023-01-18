package com.mindspice;

import com.mindspice.Settings.Settings;
import com.mindspice.utils.Util;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import static com.mindspice.utils.Util.parseCaption;

public class Controller {
    @FXML
    public Pane image_pane;
    @FXML
    public ImageView crop_view;
    @FXML
    public ImageView image_view;
    @FXML
    public TextField img_dir;
    @FXML
    public TextField save_dir;
    @FXML
    public TextArea caption_const;
    @FXML
    public CheckBox first_word_is_prefix;
    @FXML
    public TextField prefix_field;
    @FXML
    public TextField crop_width;
    @FXML
    public TextField crop_height;
    @FXML
    public TextArea caption_box;
    @FXML
    // Should be extended, doesn't matter much in practice for basic use.
    public ComboBox save_to;
    @FXML
    public TextField curr_index;
    @FXML
    public TextField goto_index;
    @FXML
    public TextArea console_box;

    private int currImageIndex;
    private Image currImageScaled;
    private BufferedImage currImageRaw;

    private double mouseX;
    private double mouseY;

    private Rectangle cropRect;
    private double imageScaleW;
    private double imageScaleH;
    private final int VIEW_FIT_W = 1360;
    private final int VIEW_FIT_H = 1040;

    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void init() {
        image_view.setPreserveRatio(true);
        crop_width.setText(String.valueOf(Settings.get().cropWidth));
        crop_height.setText(String.valueOf(Settings.get().cropHeight));
        if (State.saveDirectory != null) {
            save_dir.setText(State.saveDirectory.getAbsolutePath());
        }
        save_to.setItems(FXCollections.observableArrayList(FXCollections.observableArrayList(Settings.get().outputDirectories)));
        save_to.getSelectionModel().select(0);
    }


    @FXML
    public void onKey(KeyEvent e) {
        if (State.bindings.containsKey(e.getCode())) {
            var binding = State.bindings.get(e.getCode());
            if (binding.modifier != KeyCode.UNDEFINED) {
                var modifier = binding.modifier;
                if (modifier == KeyCode.CONTROL && !e.isControlDown()
                        || modifier == KeyCode.ALT && !e.isAltDown()) {
                    return;
                }
            }
            switch (binding.binding) {
                case SAVE -> saveImageAndCaption(binding.valueX, false, Settings.get().alwaysSaveFlipped);
                case SAVE_CONT -> {
                    saveImageAndCaption(binding.valueX, false, Settings.get().alwaysSaveFlipped);
                    goForward(null);
                }
                case SAVE_CAP -> saveCaption(null);
                case PREVIEW -> preview();
                case CROP_ALTER -> adjustCropSize(binding.valueX, binding.valueY, false);
                case CROP_RESIZE -> adjustCropSize(binding.valueX, binding.valueY, true);
                case FORWARD -> goForward(null);
                case BACK -> goBack(null);
            }
        }
        e.consume();
    }

    @FXML
    public void onMouse(MouseEvent e) {
        if (currImageScaled == null) {
            e.consume();
            return;
        }
        switch(e.getButton()) {
            case MIDDLE -> {
                var xy = getXYFromCenter(e, cropRect);
                cropRect.setX(xy[0]);
                cropRect.setY(xy[1]);
                e.consume();
                return;
            }
            case BACK -> {
                goBack(null);
                e.consume();
                return;

            }
            case FORWARD -> {
                goForward(null);
                e.consume();
                return;
            }
        }

        Rectangle rect = new Rectangle();
        rect.setMouseTransparent(true);
        var rgb = Settings.get().cropOutlineColor;
        rect.setStroke(Color.rgb(rgb[0],rgb[1],rgb[2]));
        rect.setStrokeWidth(Settings.get().cropLineWidth);
        rect.setFill(Color.TRANSPARENT);

        switch (e.getButton()) {
            case PRIMARY -> {
                if (e.isShiftDown() || e.isSecondaryButtonDown()) {
                    var xy = getXYFromCenter(e, cropRect);
                    cropRect.setX(xy[0]);
                    cropRect.setY(xy[1]);
                    e.consume();
                    return;
                }
                if (e.isControlDown()) {
                    rect.setWidth(Settings.get().cropWidth * imageScaleW * 1.5);
                    rect.setHeight(Settings.get().cropHeight * imageScaleH * 1.5);
                } else {
                    rect.setWidth(Settings.get().cropWidth * imageScaleW);
                    rect.setHeight(Settings.get().cropHeight * imageScaleH);
                }
            }
            case SECONDARY -> {
                if (e.isControlDown()) {
                    rect.setWidth(Settings.get().cropWidth * imageScaleW * 2.5);
                    rect.setHeight(Settings.get().cropHeight * imageScaleH * 2.5);
                } else {
                    rect.setWidth(Settings.get().cropWidth * imageScaleW * 2);
                    rect.setHeight(Settings.get().cropHeight * imageScaleH * 2);
                }
            }
        }

        var xy = getXYFromCenter(e, rect);
        rect.setX(xy[0]);
        rect.setY(xy[1]);
        if (cropRect != null) {
            image_pane.getChildren().remove(cropRect);
        }
        image_pane.getChildren().add(rect);
        cropRect = rect;
        e.consume();
    }

    public void onScroll(ScrollEvent e){;
        if (e.getDeltaY() < 0) {
            adjustCropSize(Settings.get().cropIncDecScrollW, Settings.get().cropIncDecScrollH,  false);
        } else if(e.getDeltaY() > 0) {
            adjustCropSize( -(Settings.get().cropIncDecScrollW), -(Settings.get().cropIncDecScrollH),  false);
        }
    }


    private void adjustCropSize(double w, double h, boolean isResize) {
        if (cropRect == null) {
            return;
        }
        cropRect.setWidth(isResize ? w : cropRect.getWidth() + w);
        cropRect.setHeight(isResize ? h : cropRect.getHeight() + h);
        cropRect.setX(mouseX - (cropRect.getWidth() / 2));
        cropRect.setY(mouseY - (cropRect.getHeight() / 2));
    }


    private int[] getXYFromCenter(MouseEvent e, Rectangle rect) {
        double winScaleW = Main.scale.getX();
        double winScaleH = Main.scale.getY();
        mouseX = (e.getSceneX() / winScaleW) - (20 * winScaleW); // offsets from sides, and window scaling calc
        mouseY = (e.getSceneY() / winScaleH) - (20 * winScaleH);
        return new int[]{(int) (mouseX - (rect.getWidth() / 2)), (int) (mouseY - (rect.getHeight() / 2))};
    }


    public void setCropWH(ActionEvent actionEvent) {
        Settings.get().cropWidth = NumberUtils.isParsable(crop_width.getText())
                ? Integer.parseInt(crop_width.getText())
                : Settings.get().cropWidth;
        Settings.get().cropHeight = NumberUtils.isParsable(crop_height.getText())
                ? Integer.parseInt(crop_height.getText())
                : Settings.get().cropHeight;

        crop_width.setText(String.valueOf(Settings.get().cropWidth));
        crop_height.setText(String.valueOf(Settings.get().cropHeight));
    }


    @FXML
    private void preview() {
        //TODO
    }

    @FXML
    public void saveAndCont(ActionEvent actionEvent) {
        saveImageAndCaption(-1, true, false);
        image_pane.getChildren().remove(cropRect);
    }

    @FXML
    public void saveAndStay(ActionEvent actionEvent) {
        saveImageAndCaption(-1, false,false);
    }

    public void saveFlipped(ActionEvent actionEvent) {
        saveImageAndCaption(-1, false, true);
    }

    @FXML
    public void saveCaption(ActionEvent actionEvent) {
        var data = State.images.get(currImageIndex);
        try {
            var dir = data.imageFile.getCanonicalFile().getParent();
            var name = FilenameUtils.getBaseName(data.imageFile.toString());
            saveCaptionFile(name, new File(dir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        if (currImageIndex != 0 && !State.images.isEmpty()) {
            var data = State.images.get(--currImageIndex);
            curr_index.setText(String.valueOf(currImageIndex));
            displayImage(data.getBufferedImage());
            setCaption(data);
        }
    }

    @FXML
    public void goForward(ActionEvent actionEvent) {
        if (currImageIndex != State.images.size() - 1 && !State.images.isEmpty()) {
            var data = State.images.get(++currImageIndex);
            curr_index.setText(String.valueOf(currImageIndex));
            displayImage(data.getBufferedImage());
            setCaption(data);
        }
    }

    @FXML
    public void gotoIndex(ActionEvent actionEvent) {
        if (!NumberUtils.isParsable(goto_index.getText())) {
            return;
        }
        var index = Integer.parseInt(goto_index.getText());
        if (index >= 0 && index <= State.images.size() - 1) {
            var data = State.images.get(index);
            curr_index.setText(String.valueOf(index));
            displayImage(data.getBufferedImage());
            setCaption(data);
        }
    }


    public void setCaption(ImageData data) {
        caption_box.clear();
        caption_box.setText(caption_const.getText() + data.caption);
    }


    public void openSaveDir(ActionEvent actionEvent) {
        var dir = Util.openDirectory();
        State.saveDirectory = dir;
        //State.outputDirectories.add(dir);
        save_dir.setText(String.valueOf(dir));
        Settings.get().outputDirectories.add(dir);
        save_to.setItems(FXCollections.observableArrayList(FXCollections.observableArrayList(Settings.get().outputDirectories)));
        save_to.getSelectionModel().select(Settings.get().outputDirectories.size() -1);
        try {
            save_dir.setText(dir.getCanonicalPath());
        } catch (IOException e) {
            //todo dialog warning
        }
    }


    public void saveToChanged(ActionEvent actionEvent) {
        var index = save_to.getSelectionModel().getSelectedIndex();
        if (index == -1) return;
        var dir = Settings.get().outputDirectories.get(index);
        State.saveDirectory = dir;
        save_dir.setText(dir.toString());
    }


    @FXML
    public void openImageDir(ActionEvent actionEvent) {
        if (State.images.isEmpty()) {
            //FIXME Hackish scene doesn't exists until controller, neither can reference each other
            Main.scene.setOnKeyPressed(this::onKey);
        }
        //todo set crop width box here

        try {
            var dir = Util.openDirectory();
            var fileList = dir.listFiles();
            if (fileList == null) {
                return;
            }
            State.images.clear();
            var captions = new ArrayList<String[]>();
            Arrays.sort(fileList);

            for (var file : fileList) {
                var ext = FilenameUtils.getExtension(file.getName());
                var name = FilenameUtils.getBaseName(file.getName());

                if (ext.equals("jpg") || ext.equals("png") || ext.equals("jpeg")) {
                    State.images.add(new ImageData(name, file));
                } else if (ext.equals("txt")) {
                    if (State.images.get(State.images.size() - 1).fileName.equals(name)) {
                        State.images.get(State.images.size() - 1).caption = parseCaption(file);
                        State.images.get(State.images.size() - 1).captionFile = file;
                    } else {
                        captions.add(new String[]{name, parseCaption(file), file.getCanonicalPath()});
                    }
                }
            }

            for (var cap : captions) {
                for (var image : State.images) {
                    if (image.fileName.equals(cap[0])) {
                        image.caption = cap[1];
                        image.captionFile = new File(cap[2]);
                    }
                }
            }
            if (!State.images.isEmpty()) {
                curr_index.setText(String.valueOf(0));
                currImageIndex = 0;
                displayImage(State.images.get(0).getBufferedImage());
                caption_box.clear();
                caption_box.setText(State.images.get(0).caption);
                img_dir.setText(dir.getCanonicalPath());
            }
        } catch (IOException ignored) {
            //FIXME add dialog warning
        }
    }

    private void displayImage(BufferedImage bImage) {
        if (bImage == null) {
            return;
        }
        currImageRaw = bImage;
        image_pane.getChildren().remove(cropRect);
        var sValue = bImage.getHeight() > bImage.getWidth()? VIEW_FIT_W : VIEW_FIT_H;
        BufferedImage scaledImg = Scalr.resize(
                bImage,
                Settings.get().editorQuality,
                VIEW_FIT_H, //Hack to fix bad ImageView scaling
                VIEW_FIT_H);

        currImageScaled = SwingFXUtils.toFXImage(scaledImg, null);
        image_view.setFitWidth(currImageScaled.getWidth());
        image_view.setFitHeight(currImageScaled.getHeight());
        image_view.setImage(currImageScaled);

        imageScaleH = image_view.getImage().getHeight() / bImage.getHeight();
        imageScaleW = image_view.getImage().getWidth() / bImage.getWidth();
    }


    private void saveImageAndCaption(int dirIndex, boolean gotoNext, boolean saveFlipped) {
        if (cropRect == null) {
            //todo throw dialog
            return;
        }

        try {
            String prefix = getPrefix();
            String name;
            var dir = dirIndex == -1 ? State.saveDirectory : Settings.get().outputDirectories.get(dirIndex);
            if (!dir.exists()) {
                // FIXME throw dialog
            }
            name = prefix + currImageIndex;
            var imageFile = new File(dir, name + ".png");
            if (imageFile.isFile() && !Settings.get().overWriteByDefault) {
                for (int i = 0; i < 1000; ++i) {
                    name = prefix + currImageIndex + "copy(" + i + ")";
                    imageFile = new File(dir, name + ".png");
                    if (!imageFile.exists()) {
                        break;
                    }
                }
            }

            var wScaleUp = currImageRaw.getWidth() / image_view.getImage().getWidth();
            var hScaleUp = currImageRaw.getHeight() / image_view.getImage().getHeight();
            var fullRect = new Rectangle();

            fullRect.setWidth(cropRect.getWidth() * wScaleUp);
            fullRect.setHeight(cropRect.getWidth() * hScaleUp);
            fullRect.setX(cropRect.getX() * wScaleUp);
            fullRect.setY(cropRect.getY() * hScaleUp);

            var croppedImage = Scalr.crop(
                    currImageRaw,
                    (int) fullRect.getX(),
                    (int) fullRect.getY(),
                    (int) fullRect.getWidth(),
                    (int) fullRect.getHeight());

            if (croppedImage.getWidth() != Settings.get().cropWidth || croppedImage.getHeight() != Settings.get().cropHeight) {
                croppedImage = Scalr.resize(
                        croppedImage,
                        Settings.get().saveQuality,
                        Settings.get().cropWidth,
                        Settings.get().cropHeight);
            }

            ImageIO.write(croppedImage, "PNG", imageFile);
            saveCaptionFile(name, dir);
            updateConsole("Saved Image:" + imageFile);

            if (saveFlipped || Settings.get().alwaysSaveFlipped) {
                var flippedFile = new File( dir,name + "-f.png");
                ImageIO.write(getFlippedImage(croppedImage), "PNG", flippedFile);
                saveCaptionFile(name + "-f", dir);
                updateConsole("Saved Flipped Image:" + flippedFile);
            }


            if (gotoNext) {
                goForward(null);
            }
        } catch (IOException e) {
            //TODO dialogs
        } catch (IllegalArgumentException e) {

        }
    }


    private void saveCaptionFile(String name, File dir) {
        var capFile = new File(dir, name + ".txt");
        try {
            Files.write(capFile.toPath(), (caption_const.getText() + caption_box.getText()).getBytes());
            updateConsole("Saved Caption:" + capFile);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO dialog
        }
    }

    private String getPrefix() {
        var prefix = "";
        if (first_word_is_prefix.isSelected()) {
            if (!caption_const.getText().isEmpty()) {
                prefix = caption_const.getText().split("[\\s,]+")[0];
            } else {
                prefix = caption_box.getText().split("[\\s,]+")[0];
            }
        } else {
            prefix = prefix_field.getText();
        }
        return prefix;
    }

    private BufferedImage getFlippedImage(BufferedImage bImage) {
        BufferedImage flipped = new BufferedImage(bImage.getWidth(), bImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = flipped.createGraphics();
        gg.drawImage(bImage, bImage.getHeight(), 0, -bImage.getWidth(), bImage.getHeight(), null);
        gg.dispose();
        return flipped;
    }

    private void updateConsole(String string){
        console_box.appendText(string + "\n");
    }

}
