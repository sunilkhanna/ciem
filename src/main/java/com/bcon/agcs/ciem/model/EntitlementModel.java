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
public class EntitlementModel {
    private String resourceId;
    private String resourceName;
    private String specialAttr;
    private List<EntitlementModel> action;

    @Override
    public boolean equals(Object object) {
        return resourceId.equals(((EntitlementModel) object).getResourceId());
    }
}
