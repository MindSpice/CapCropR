package com.mindspice;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageData {
    public String fileName;
    public File imageFile;
    public String caption = "";
    public File captionFile;

    public ImageData(String fileName, File imageFile) {
        this.fileName = fileName;
        this.imageFile = imageFile;
    }

    public BufferedImage getBufferedImage() {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            //FIXME throw dialog window
        }
        return null;
    }
}
