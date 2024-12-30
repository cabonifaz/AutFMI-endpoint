package org.app.autfmi.util;

import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
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

    public String getEmployeeHtmlTemplate() {
        String imageB64 = loadLogoPDF(1);
        return Constante.FORM_TEMPLATE_EMPLOYEE.replace("{{ImgB64}}", imageB64);
    }

    public String getSolicitudHtmlTemplate() {
        String imageB64 = loadLogoPDF(2);
        return Constante.FORM_TEMPLATE_EMPLOYEE.replace("{{ImgB64}}", imageB64);
    }

    public String replaceEntryRequestValues(String htmlTemplate, EmployeeEntryRequest request) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", request.getNombres())
                .replace("{{apellidos}}", request.getApellidos())
                .replace("{{unidad}}", request.getIdUnidad().toString())
                // INGRESO
                .replace("{{modalidad}}", request.getIdModalidad().toString())
                .replace("{{motivoIngreso}}", request.getIdMotivo().toString())
                .replace("{{cargo}}", request.getCargo())
                .replace("{{horarioTrabajo}}", request.getHorarioTrabajo())
                .replace("{{montoBaseIn}}", request.getMontoBase().toString())
                .replace("{{montoMovilidadIn}}", request.getMontoMovilidad().toString())
                .replace("{{montoTrimestralIn}}", request.getMontoTrimestral().toString())
                .replace("{{fechaInicioContrato}}", request.getFchInicioContrato())
                .replace("{{fechaTerminoContrato}}", request.getFchTerminoContrato())
                .replace("{{proyectoServicio}}", request.getProyectoServicio())
                .replace("{{objetoContrato}}", request.getObjetoContrato())
                // SUNAT
                .replace("{{declaradoSunat}}", request.getDeclararSunat() == 1 ? "Si" : "No")
                .replace("{{sedeDeclarar}}", request.getSedeDeclarar())
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "")
                .replace("{{montoMovilidadMov}}", "")
                .replace("{{montoTrimestralMov}}", "")
                .replace("{{puesto}}", "")
                .replace("{{area}}", "")
                .replace("{{jornada}}", "")
                .replace("{{fechaMovimiento}}", "")
                // CESE
                .replace("{{motivoCese}}", "")
                .replace("{{fechaCese}}", "")
                // FOOTER
                .replace("{{firmante}}", "");
        return htmlTemplate;
    }

    public String replaceMovementRequestValues(String htmlTemplate, EmployeeMovementRequest request) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", request.getNombres())
                .replace("{{apellidos}}", request.getApellidos())
                .replace("{{unidad}}", request.getIdUnidad().toString())
                // INGRESO
                .replace("{{modalidad}}", "")
                .replace("{{motivoIngreso}}", "")
                .replace("{{cargo}}", "")
                .replace("{{horarioTrabajo}}", "")
                .replace("{{montoBaseIn}}", "")
                .replace("{{montoMovilidadIn}}", "")
                .replace("{{montoTrimestralIn}}", "")
                .replace("{{fechaInicioContrato}}", "")
                .replace("{{fechaTerminoContrato}}", "")
                .replace("{{proyectoServicio}}", "")
                .replace("{{objetoContrato}}", "")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", request.getMontoBase().toString())
                .replace("{{montoMovilidadMov}}", request.getMontoMovilidad().toString())
                .replace("{{montoTrimestralMov}}", request.getMontoTrimestral().toString())
                .replace("{{puesto}}", request.getPuesto())
                .replace("{{area}}", request.getArea())
                .replace("{{jornada}}", request.getJornada())
                .replace("{{fechaMovimiento}}", request.getFchMovimiento())
                // CESE
                .replace("{{motivoCese}}", "")
                .replace("{{fechaCese}}", "")
                // FOOTER
                .replace("{{firmante}}", "");
        return htmlTemplate;
    }

    public String replaceOutRequestValues(String htmlTemplate, EmployeeContractEndRequest request) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", request.getNombres())
                .replace("{{apellidos}}", request.getApellidos())
                .replace("{{unidad}}", request.getIdUnidad().toString())
                // INGRESO
                .replace("{{modalidad}}", "")
                .replace("{{motivoIngreso}}", "")
                .replace("{{cargo}}", "")
                .replace("{{horarioTrabajo}}", "")
                .replace("{{montoBaseIn}}", "")
                .replace("{{montoMovilidadIn}}", "")
                .replace("{{montoTrimestralIn}}", "")
                .replace("{{fechaInicioContrato}}", "")
                .replace("{{fechaTerminoContrato}}", "")
                .replace("{{proyectoServicio}}", "")
                .replace("{{objetoContrato}}", "")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "")
                .replace("{{montoMovilidadMov}}", "")
                .replace("{{montoTrimestralMov}}", "")
                .replace("{{puesto}}", "")
                .replace("{{area}}", "")
                .replace("{{jornada}}", "")
                .replace("{{fechaMovimiento}}", "")
                // CESE
                .replace("{{motivoCese}}", request.getIdMotivo().toString())
                .replace("{{fechaCese}}", request.getFchCese())
                // FOOTER
                .replace("{{firmante}}", "");
        return htmlTemplate;
    }

}
