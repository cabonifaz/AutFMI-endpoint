package org.app.autfmi.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

@Component
public class PDFUtils {

    public String loadLogoPDF(int idLogo) {
        try {
            String rutaLogo = "";
            if (idLogo == 1) {
                rutaLogo = "src/main/resources/assets/logo-fractal.png";
            } else {
                rutaLogo = "src/main/resources/assets/logo-fractal-2.png";
            }

            File file = new File(rutaLogo);

            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] imageBytes = fileInputStream.readAllBytes();

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            System.out.println("data:image/png;base64," + base64Image);

            fileInputStream.close();

            return  base64Image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
