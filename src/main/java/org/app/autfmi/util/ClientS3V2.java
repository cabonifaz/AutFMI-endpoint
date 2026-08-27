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
import java.time.Duration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cliente S3 (SDK v2).
 *
 * <p>
 * Homologado con {@code com.bdt.bancotalentosbackend.util.ClientS3V2} de Banco
 * de Talentos: ambas clases son el mismo código salvo el paquete, para que la
 * subida de archivos de los dos backends se comporte igual.
 *
 * <p>
 * Respecto a la versión anterior de FMI se incorpora el blindaje que BDT ya
 * tenía en su antigua clase {@code S3Utils} y que aquí sólo existía, disperso y
 * duplicado, dentro de {@code RequirementService}: {@link #validateKey(String)},
 * {@link #resolveContentType(String)}, {@link #extractExtension(String)} y
 * {@link #sanitizeFileName(String, String)}. Ese endurecimiento existe por
 * fallos reales — ver {@code NOTA_SUBIDA_S3_BDT.md} en la raíz.
 */
@Component
public class ClientS3V2 {

  private final Logger logger = LoggerFactory.getLogger(ClientS3V2.class);

  private S3Client s3Client;
  private String bucketName;
  private S3Presigner presigner;

  @PostConstruct
  public void init() {
    this.bucketName = System.getenv("AWS_BUCKET");
    Region region = resolveRegion();

    this.presigner = S3Presigner.builder()
        .region(region)
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build();

    this.s3Client = S3Client.builder()
        .region(region)
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build();
  }

  /**
   * Resuelve la región avisando en voz alta cuando la variable falta, en vez de
   * caer al valor por defecto en silencio.
   *
   * <p>
   * OJO: NO se hace {@code Region.of(regionName)} con el valor crudo: si {@code AWS_REGION} no está definida eso lanza dentro del
   * {@code @PostConstruct} y el contexto de Spring no arranca. Con el respaldo,
   * el backend levanta y el fallo se ve en el log y en el PUT, no en el arranque.
   */
  private Region resolveRegion() {
    String regionName = System.getenv("AWS_REGION");
    if (bucketName == null || bucketName.trim().isEmpty()) {
      logger.error("La variable de entorno AWS_BUCKET no está configurada: no se podrá firmar ninguna URL.");
    }
    if (regionName == null || regionName.trim().isEmpty()) {
      logger.error("La variable de entorno AWS_REGION no está configurada; se usa us-east-1. "
          + "Si el bucket vive en otra región, la firma se calcula sobre otro endpoint y el PUT fallará.");
      return Region.US_EAST_1;
    }
    return Region.of(regionName.trim());
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
   * @return true si se eliminó (o si la ruta venía vacía), false si hubo error.
   */
  public boolean delete(String path) {
    String key = path == null ? null : path.trim();
    if (key == null || key.isEmpty()) {
      return true;
    }
    try {
      logger.info("Eliminando objeto de S3: {}", key);
      DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build();
      s3Client.deleteObject(deleteRequest);
      return true;
    } catch (Exception e) {
      logger.error("Error al eliminar objeto de S3: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Verifica si un objeto existe en el bucket.
   *
   * @param path La ruta (key) del archivo.
   * @return true si existe, false en caso contrario.
   */
  public boolean exists(String path) {
    return headObject(path) != null;
  }

  /**
   * Obtiene la metadata (HEAD) de un objeto en S3, o null si no existe.
   * Permite verificar existencia y leer tamaño/tipo en una sola llamada.
   */
  public HeadObjectResponse headObject(String path) {
    // Se recorta igual que en validateKey: si no, una ruta con espacio final se
    // firmaría con una key y se comprobaría con otra.
    String key = path == null ? null : path.trim();
    if (key == null || key.isEmpty()) {
      return null;
    }
    try {
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build();
      return s3Client.headObject(headObjectRequest);
    } catch (NoSuchKeyException e) {
      return null;
    } catch (S3Exception e) {
      // Un 403 aquí NO significa que el objeto falte: es que el rol IAM no tiene
      // s3:GetObject sobre esa key. Se registra el código para poder distinguirlo.
      logger.error("Error al obtener metadata en S3 (status {}): {}",
          e.statusCode(), e.awsErrorDetails().errorMessage());
      return null;
    }
  }

  /**
   * URL GET pre-firmada que fuerza la descarga como adjunto (Content-Disposition:
   * attachment), evitando que el navegador renderice el contenido en línea.
   */
  public String generatePresignedDownloadUrl(String path, String fileName, int minutes) {
    String key = validateKey(path);
    if (key == null) {
      return "";
    }

    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
          .build();

      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(minutes))
          .getObjectRequest(getObjectRequest)
          .build();

      return presigner.presignGetObject(presignRequest).url().toString();

    } catch (Exception e) {
      logger.error("Error al generar URL firmada de descarga: {}", e.getMessage());
      return "";
    }
  }

  // Genera una URL pre-firmada para acceder a un objeto en S3 por un tiempo limitado.

  public String generatePresignedUrl(String path, int minutes) {
    String key = validateKey(path);
    if (key == null) {
      return "";
    }

    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build();

      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(minutes))
          .getObjectRequest(getObjectRequest)
          .build();

      PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

      return presignedRequest.url().toString();

    } catch (Exception e) {
      logger.error("Error al generar URL firmada: {}", e.getMessage());
      return "";
    }
  }

  /**
   * URL GET pre-firmada para ver el objeto EN LÍNEA en el navegador (visor de
   * PDF/imagen) en vez de forzar la descarga.
   *
   * <p>
   * Sobrescribe las cabeceras de respuesta para que el objeto se sirva con
   * {@code Content-Disposition: inline} y un {@code Content-Type} derivado de la
   * extensión. Así se ve en línea incluso para objetos guardados en S3 con un
   * tipo genérico (por ejemplo los heredados de la época base64, subidos como
   * {@code application/octet-stream}).
   *
   * @param path    La key del objeto en S3.
   * @param minutes Expiración en minutos.
   * @return La URL firmada, o cadena vacía si hay error.
   */
  public String generatePresignedInlineUrl(String path, int minutes) {
    String key = validateKey(path);
    if (key == null) {
      return "";
    }

    try {
      String fileName = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
      String contentType = resolveContentType(fileName);

      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .responseContentType(contentType)
          .responseContentDisposition("inline; filename=\"" + fileName + "\"")
          .build();

      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(minutes))
          .getObjectRequest(getObjectRequest)
          .build();

      PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
      return presignedRequest.url().toString();

    } catch (Exception e) {
      logger.error("Error al generar URL firmada inline: {}", e.getMessage());
      return "";
    }
  }

  // Genera una URL pre-firmada para subir un archivo al AWS por tiempo limitado.

  public String generatePresignedUploadUrl(
      String path,
      String contentType,
      int minutes) {
    String key = validateKey(path);
    if (key == null) {
      return "";
    }

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          // OJO: el content-type entra en X-Amz-SignedHeaders (la lista de
          // cabeceras que el firmante ignora es sólo connection, x-amzn-trace-id,
          // user-agent, expect y transfer-encoding). El cliente debe mandar
          // exactamente este valor o S3 responde SignatureDoesNotMatch.
          .contentType(contentType)
          .build();

      PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(minutes))
          .putObjectRequest(putObjectRequest)
          .build();

      PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

      logger.info("URL firmada de carga generada exitosamente para: {}", key);
      return presignedRequest.url().toString();

    } catch (Exception e) {
      logger.error("Error al generar URL firmada de carga: {}", e.getMessage());
      return "";
    }
  }

  /**
   * Comprueba que una key se puede firmar de verdad, y la devuelve recortada.
   *
   * <p>
   * Las rutas no siempre las construye este backend: en el reemplazo de CV y en
   * las descargas vienen de la BD, donde conviven filas heredadas de la época
   * base64. Lo que se rechaza aquí:
   *
   * <ul>
   * <li>El marcador {@code [ID]} sin sustituir. Es el fallo real de
   * {@code addOrUpdateTalent}: concatena la constante de carpeta sin reemplazar
   * el marcador, así que la key puede acabar como
   * {@code repositorio/talento/[ID]/foto.png}. La URL firmada sale bien formada
   * (el SDK codifica los corchetes) pero apunta a un objeto que no existe.</li>
   * <li>Rutas de sistema de archivos o URLs completas ({@code C:\...},
   * {@code https://...}), que no son keys de S3.</li>
   * <li>Saltos de línea y caracteres de control, que sí romperían la petición.</li>
   * <li>La barra inicial, que en S3 crea un nivel de carpeta con nombre vacío.</li>
   * </ul>
   *
   * @param path La key candidata del objeto en S3.
   * @return La key recortada, o {@code null} si no se puede firmar.
   */
  private String validateKey(String path) {
    if (path == null) {
      return null;
    }
    String key = path.trim();
    if (key.isEmpty()) {
      return null;
    }
    if (bucketName == null || bucketName.trim().isEmpty()) {
      logger.error("AWS_BUCKET no está configurada: no se puede firmar la ruta {}", key);
      return null;
    }
    if (key.contains("[ID]")) {
      logger.error("Ruta S3 con el marcador [ID] sin sustituir: {}", key);
      return null;
    }
    if (key.startsWith("/") || key.contains("://") || key.contains("\\")) {
      logger.error("Ruta S3 inválida (no es una key relativa del bucket): {}", key);
      return null;
    }
    for (int i = 0; i < key.length(); i++) {
      if (key.charAt(i) < 0x20 || key.charAt(i) == 0x7F) {
        logger.error("Ruta S3 con caracteres de control en la posición {}: {}", i, key);
        return null;
      }
    }
    return key;
  }

  /**
   * Resuelve el MIME type a partir de la extensión del nombre de archivo.
   *
   * <p>
   * Se usa tanto para la vista en línea (GET) como para decidir con qué
   * content-type se firma una URL de carga. El navegador deja {@code File.type}
   * vacío para las extensiones que no conoce, y firmar con un content-type vacío
   * produce una firma que el PUT nunca puede reproducir, así que el servidor
   * siempre resuelve aquí un valor concreto.
   *
   * @param fileName El nombre de archivo (puede incluir extensión).
   * @return El MIME type, o {@code application/octet-stream} si no se reconoce.
   */
  public static String resolveContentType(String fileName) {
    String lower = fileName == null ? "" : fileName.toLowerCase();
    if (lower.endsWith(".pdf"))
      return "application/pdf";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
      return "image/jpeg";
    if (lower.endsWith(".png"))
      return "image/png";
    if (lower.endsWith(".gif"))
      return "image/gif";
    if (lower.endsWith(".webp"))
      return "image/webp";
    if (lower.endsWith(".doc"))
      return "application/msword";
    if (lower.endsWith(".docx"))
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (lower.endsWith(".xls"))
      return "application/vnd.ms-excel";
    if (lower.endsWith(".xlsx"))
      return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    if (lower.endsWith(".zip"))
      return "application/zip";
    return "application/octet-stream";
  }

  /**
   * Extensión en minúsculas y SIN el punto, descartando antes cualquier ruta
   * incrustada en el nombre.
   *
   * <p>
   * Misma implementación que tenía {@code RequirementService}, donde protege la
   * subida de archivos de postulante.
   *
   * @param name El nombre de archivo, posiblemente con ruta.
   * @return La extensión sin el punto, o cadena vacía si no tiene.
   */
  public static String extractExtension(String name) {
    if (name == null) {
      return "";
    }
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    String base = slash >= 0 ? name.substring(slash + 1) : name;
    int dot = base.lastIndexOf('.');
    return dot >= 0 ? base.substring(dot + 1).toLowerCase() : "";
  }

  /**
   * Sanea el nombre de archivo: descarta cualquier ruta, restringe a caracteres
   * seguros [A-Za-z0-9._-], limita la longitud y conserva la extensión.
   *
   * <p>
   * Sin esto, un nombre con tildes, espacios, paréntesis o {@code ../} acababa
   * literal en la key de S3: la URL firmada arrastraba esos caracteres
   * percent-encoded, y el objeto podía escribirse fuera de su propia carpeta.
   *
   * @param originalFilename El nombre tal cual lo mandó el cliente.
   * @param extension        La extensión sin el punto (ver
   *                         {@link #extractExtension(String)}).
   * @return Un nombre de archivo seguro.
   */
  public static String sanitizeFileName(String originalFilename, String extension) {
    if (originalFilename == null) {
      return extension == null || extension.isEmpty() ? "archivo" : "archivo." + extension;
    }
    int slash = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
    String base = slash >= 0 ? originalFilename.substring(slash + 1) : originalFilename;
    int dot = base.lastIndexOf('.');
    String namePart = dot >= 0 ? base.substring(0, dot) : base;

    namePart = namePart.replaceAll("[^A-Za-z0-9._-]", "_");
    if (namePart.isEmpty()) {
      namePart = "archivo";
    }
    if (namePart.length() > 80) {
      namePart = namePart.substring(0, 80);
    }
    return extension == null || extension.isEmpty() ? namePart : namePart + "." + extension;
  }
}
