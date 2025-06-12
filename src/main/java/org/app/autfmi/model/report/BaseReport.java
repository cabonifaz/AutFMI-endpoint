package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseReport implements IReport{
    private BaseResponse response;
}
