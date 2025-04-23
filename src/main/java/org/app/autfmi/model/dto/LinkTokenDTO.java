package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.request.RequirementPostulantRequest;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkTokenDTO {
    private UserDTO userData;
    private List<RequirementPostulantRequest> lstrRequerimientos;
}
