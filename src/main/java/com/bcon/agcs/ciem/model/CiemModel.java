package com.bcon.agcs.ciem.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CiemModel {
    private String userId;
    private String userName;
    private String specialAttr;
    private List<EntitlementModel> role;
    private List<EntitlementModel> appRole;
    private List<EntitlementModel> group;
    private List<EntitlementModel> policy;
}
