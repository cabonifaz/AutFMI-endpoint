package org.app.autfmi.model.request;
import lombok.Data;

@Data
public class InterviewUploadConfirmRequest {
    private Integer idInterview;
    private Integer idFileType;
    private String fileName;
    private String path;
}