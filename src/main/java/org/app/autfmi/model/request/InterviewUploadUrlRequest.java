package org.app.autfmi.model.request;
import lombok.Data;

@Data
public class InterviewUploadUrlRequest {
    private Integer idInterview;
    private Integer idFileType;
    private String fileName;
    private String contentType;
    private Long size;
}