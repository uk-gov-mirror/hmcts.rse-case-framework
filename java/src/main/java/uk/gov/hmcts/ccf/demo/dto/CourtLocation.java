package uk.gov.hmcts.ccf.demo.dto;

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema;
import lombok.Data;

@Data
public class CourtLocation {
    @JsonSchema(title = "Court name")
    private String applicantPreferredCourt;
}
