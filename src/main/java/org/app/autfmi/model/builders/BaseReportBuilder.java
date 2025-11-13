package org.app.autfmi.model.builders;

import java.util.ArrayList;
import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.util.PDFUtils;
import org.springframework.lang.NonNull;

public abstract class BaseReportBuilder<T> {

  protected final PDFUtils pdfUtils;
  protected final T report;
  @NonNull
  protected final GestorDTO gs;
  protected final List<FileDTO> files = new ArrayList<>();

  protected BaseReportBuilder(PDFUtils pdfUtils, T report, GestorDTO gs) {
    if (gs == null)
      throw new IllegalArgumentException("GestorDTO cannot be null");

    this.pdfUtils = pdfUtils;
    this.report = report;
    this.gs = gs;
  }

  public abstract List<FileDTO> build();

}
