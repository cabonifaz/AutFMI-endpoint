package org.app.autfmi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class.getName());

    public static String cargarArchivo(String linkArchivo) {
        try {
            logger.info(Constante.TXT_SEPARADOR);
            logger.info("Inicio Utilitarios - CargarArchivo");
            File archivo = new File(linkArchivo).getAbsoluteFile();
            logger.info("Consultando existencia de archivo...");
            if (archivo.exists()) {
                logger.info("El archivo existe");

                FileInputStream fileInputStream = new FileInputStream(archivo);
                byte[] byteArray = new byte[(int) archivo.length()];
                int bytesRead = fileInputStream.read(byteArray);
                fileInputStream.close();

                if (bytesRead != byteArray.length) {
                    logger.error("No se pudo leer completamente el archivo: " + linkArchivo);
                    return "";
                }

                String fileBase64 = Base64.getEncoder().encodeToString(byteArray);
                logger.info("Fin Utilitarios - CargarArchivo");
                logger.info(Constante.TXT_SEPARADOR);
                return fileBase64;
            } else {
                logger.info("El archivo no existe");
                logger.info("Fin Utilitarios - CargarArchivo");
                logger.info(Constante.TXT_SEPARADOR);
                return "";
            }
        } catch (IOException e) {
            logger.error("Error al cargar el archivo: " + e.getMessage());
            return "";
        }
    }

    public static void guardarArchivo(String archivoBase64, String ruta) {
        try {
            logger.info(Constante.TXT_SEPARADOR);
            logger.info("Inicio Utilitarios - GuardarArchivo");
            logger.info("Ruta asignada: " + ruta);

            byte[] fileBytes = Base64.getDecoder().decode(archivoBase64);

            // Obtener el directorio padre
            File repositorioDir = new File(ruta).getParentFile();

            logger.info("Verificando si el directorio ya existe...");

            // Si el directorio no existe, intentar crearlo
            // Comprobar si ya existe un archivo con el mismo nombre que el directorio
            if (repositorioDir.exists() && !repositorioDir.isDirectory()) {
                logger.error("El directorio no se puede crear porque existe un archivo con el mismo nombre: " + repositorioDir.getAbsolutePath());
                return;
            }

            if (!repositorioDir.getAbsoluteFile().exists()) {
                if (!repositorioDir.mkdirs()) {
                    logger.error("Error al crear el directorio: " + repositorioDir.getAbsolutePath());
                    return;
                }
            } else {
                logger.info("El directorio ya existe, se procede a guardar el archivo.");
            }

            File archivo = new File(ruta);

            // Guardar archivo
            try {
                Files.write(Paths.get(archivo.getAbsolutePath()), fileBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                logger.info("Archivo guardado exitosamente en la ruta: " + archivo.getAbsolutePath());
                logger.info("Fin Utilitarios - GuardarArchivo");
                logger.info(Constante.TXT_SEPARADOR);
            } catch (IOException e) {
                logger.error("Error al guardar el archivo: " + e.getMessage(), e);
            }

        } catch (IllegalArgumentException e) {
            logger.error("Cadena Base64 inválida: " + e.getMessage(), e);
        }

    }

    public static void eliminarArchivo(String archivoRutaPre) {
        try {
            logger.info(Constante.TXT_SEPARADOR);
            logger.info("Inicio Utilitarios - EliminarArchivo");
            logger.info("Ruta recibida: " + archivoRutaPre);

            // Obtiene la ruta absoluta y normalizada
            File archivo = new File(archivoRutaPre).getCanonicalFile();

            logger.info("Ruta normalizada del archivo: " + archivo.getAbsolutePath());

            if (!archivo.exists()) {
                logger.warn("El archivo no existe: " + archivo.getAbsolutePath());
                return;
            }

            if (archivo.delete()) {
                logger.info("Archivo eliminado exitosamente: " + archivo.getAbsolutePath());
            } else {
                logger.error("No se pudo eliminar el archivo: " + archivo.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Error al normalizar la ruta del archivo: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error al eliminar el archivo: " + e.getMessage());
        } finally {
            logger.info("Fin Utilitarios - EliminarArchivo");
            logger.info(Constante.TXT_SEPARADOR);
        }
    }
}
