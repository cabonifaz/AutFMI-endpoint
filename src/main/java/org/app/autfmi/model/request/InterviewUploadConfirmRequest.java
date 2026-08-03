package org.app.autfmi.model.request;
import lombok.Data;

@Data
public class InterviewUploadConfirmRequest {
    private Integer idInterview;
    private Integer idFileType;
    private String fileName;
    private String path;
    // Solo para el ICS: si es true, se envía el correo de entrevista con el ICS adjunto.
    private Boolean notify;
    private String notificationType;
}