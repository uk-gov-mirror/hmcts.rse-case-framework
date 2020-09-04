package uk.gov.hmcts.ccf.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccf.demo.api.ApiEventCreation;
import uk.gov.hmcts.ccf.demo.controller.WebController;
import uk.gov.hmcts.ccf.demo.dto.Company;
import uk.gov.hmcts.ccf.demo.dto.Organisation;
import uk.gov.hmcts.ccf.demo.enums.LocalAuthority;
import uk.gov.hmcts.ccf.demo.event.CreateClaim;

@Component
public class TestDataGenerator implements InitializingBean {

    @Autowired
    private WebController controller;

    @Value("${generate-data:false}")
    public String generate;

    @Override
    public void afterPropertiesSet() {
        if (!"true".equals(generate)) {
            return;
        }
        CreateClaim o = CreateClaim.builder()
        .claimantReference("666")
                .defendantReference("999")
                .claimant(new Company("Acme Ltd"))
                .defendant(new Organisation("Megacorp Inc"))
                .build();
        ApiEventCreation e = new ApiEventCreation("Create", new ObjectMapper().valueToTree(o));
        controller.createCase(e.getData());

        o = CreateClaim.builder()
                .claimantReference("1111")
                .defendantReference("33333")
                .claimant(new Company("Hooli"))
                .defendant(new Organisation("Wiki"))
                .build();
        e = new ApiEventCreation("Create", new ObjectMapper().valueToTree(o));
        controller.createCase(e.getData());
    }

}
