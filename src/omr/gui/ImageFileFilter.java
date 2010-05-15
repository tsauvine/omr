package omr.gui;

import java.io.File;

public class ImageFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        String extension = null;
        String fileName = file.getName();
        int index = fileName.lastIndexOf('.');
        if (index > 0 && index < fileName.length() - 1) {
            extension = fileName.substring(index + 1).toLowerCase();
        }
        
        if (extension == null) {
            return false;
        }
        
        if (extension.equals("tiff") ||
            extension.equals("tif") ||
            extension.equals("gif") ||
            extension.equals("jpeg") ||
            extension.equals("jpg") ||
            extension.equals("png") ||
            extension.equals("pdf")
            ) {
                return true;
        }

        return false;
    }

    public String getDescription() {
        return "Images";
    }

}
