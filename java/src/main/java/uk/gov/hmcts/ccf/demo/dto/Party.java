package uk.gov.hmcts.ccf.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uk.gov.hmcts.ccf.demo.enums.PartyType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="partyType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Individual.class, name = "Individual"),
        @JsonSubTypes.Type(value = Company.class, name = "Company"),
        @JsonSubTypes.Type(value = Organisation.class, name = "Organisation"),
        @JsonSubTypes.Type(value = SoleTrader.class, name = "SoleTrader")
})
@Data
public abstract class Party {
    private String address;
}
