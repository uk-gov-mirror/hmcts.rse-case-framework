package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;


import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString

public class SearchInputFieldsDefinition implements Serializable, CommonDCPModel {

    private String caseTypeId = null;
    private List<SearchInputField> fields = new ArrayList<>();
    private String displayContextParameter = null;


    @JsonProperty("case_type_id")
    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }


    @JsonProperty("fields")
    public List<SearchInputField> getFields() {
        return fields;
    }

    public void setFields(List<SearchInputField> fields) {
        this.fields = fields;
    }


    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

}
