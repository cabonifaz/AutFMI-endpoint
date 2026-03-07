package org.app.autfmi.util;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ClientS3V2 {

  private final Logger logger = LoggerFactory.getLogger(ClientS3V2.class);

  private S3Client s3Client;
  private String bucketName;

  @PostConstruct
  public void init() {
    String regionName = System.getenv("AWS_REGION");
    this.bucketName = System.getenv("AWS_BUCKET");

    if (regionName == null || bucketName == null) {
      logger.error("Las variables de entorno AWS_REGION o AWS_BUCKET no están configuradas.");
    }

    this.s3Client = S3Client.builder()
        .region(Region.of(regionName != null ? regionName : "us-east-1"))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build();
  }

  /**
   * Sube un archivo a S3 usando un MultipartFile.
   * 
   * @param file El archivo multipart recibido.
   * @param path La ruta (key) donde se guardará en el bucket.
   * @return El path (key) del archivo guardado.
   * @throws IOException Si hay error al leer el stream del archivo.
   */
  public String upload(MultipartFile file, String path) throws IOException {
    this.logger.info("Uploading MultipartFile to S3: {}", path);
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .contentType(file.getContentType())
        .build();

    s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    return path;
  }

  /**
   * Sube un archivo a S3 usando un objeto File local.
   * 
   * @param file El archivo local.
   * @param path La ruta (key) donde se guardará en el bucket.
   * @return El path (key) del archivo guardado.
   */
  public String upload(File file, String path) {
    this.logger.info("Subiendo archivo local a S3: {}", path);
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    s3Client.putObject(putRequest, RequestBody.fromFile(file));
    return path;
  }

  /**
   * Sube contenido desde un InputStream.
   * 
   * @param inputStream El flujo de datos.
   * @param size        El tamaño del contenido.
   * @param contentType El tipo de contenido (mime type).
   * @param path        La ruta (key) en S3.
   * @return El path (key) del archivo guardado.
   */
  public String upload(InputStream inputStream, long size, String contentType, String path) {
    logger.info("Uploading InputStream to S3: {}", path);
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .contentType(contentType)
        .build();

    this.s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, size));
    return path;
  }

  /**
   * Descarga un archivo de S3 como un arreglo de bytes.
   * 
   * @param path La ruta (key) del archivo en S3.
   * @return El contenido del archivo en bytes.
   */
  public byte[] download(String path) {
    logger.info("Descargando archivo desde S3: {}", path);
    GetObjectRequest getRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
    return objectBytes.asByteArray();
  }

  /**
   * Obtiene un InputStream del archivo en S3.
   * 
   * @param path La ruta (key) del archivo en S3.
   * @return InputStream del contenido.
   */
  public InputStream downloadStream(String path) {
    logger.info("Obteniendo stream de S3: {}", path);
    GetObjectRequest getRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    return s3Client.getObject(getRequest);
  }

  /**
   * Elimina un objeto de S3.
   * 
   * @param path La ruta (key) del archivo.
   */
  public void delete(String path) {
    logger.info("Eliminando objeto de S3: {}", path);
    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    s3Client.deleteObject(deleteRequest);
  }

  /**
   * Verifica si un objeto existe en el bucket.
   * 
   * @param path La ruta (key) del archivo.
   * @return true si existe, false en caso contrario.
   */
  public boolean exists(String path) {
    try {
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(bucketName)
          .key(path)
          .build();
      s3Client.headObject(headObjectRequest);
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      logger.error("Error al verificar existencia en S3: {}", e.awsErrorDetails().errorMessage());
      return false;
    }
  }
}
