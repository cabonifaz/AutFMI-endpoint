package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

import org.app.autfmi.model.response.BaseResponse;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeFullHistoryDTO extends BaseResponse {

    private Integer talentId;

    // DATOS PERSONALES

    private String names;
    private String lastname;
    private String surname;
    private String email;
    private String documentNumber;
    private String description;

    // HISTORIAL

    private List<ContractDTO> contracts;
    private List<MovementDTO> movements;
    private List<EquipmentRequestDTO> equipmentRequests;
    private List<TerminationDTO> terminations;

    public EmployeeFullHistoryDTO(Integer messageId, String message) {
        super(messageId, message);
    }

    // CLASES INTERNAS

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractDTO {
        private Integer contractId;
        private String talentName;
        private Integer contractTypeId;
        private String contractType;
        private String contractObject;
        private Integer areaId;
        private String area;
        private String client;
        private String rqCode;
        private String rqTitle;
        private String startDate;
        private String endDate;
        private String baseAmount;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementDTO {
        private String movementDate;
        private String reason;
        private String previousArea;
        private String position;
        private String movementType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentRequestDTO {
        private Integer requestId;
        private String equipmentType;
        private String brand;
        private String requestDate;
        private String deliveryDate;
        private String mobileAssigned;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminationDTO {
        private Integer terminationId;
        private String terminationDate;
        private String terminationReason;
        private String client;
        private Integer requirementId;
        private String requirementTitle;
        private String requirementCode;
        private Integer idContract;
    }
}
