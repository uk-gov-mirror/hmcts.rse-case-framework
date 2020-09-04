package uk.gov.hmcts.ccf.demo

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import org.jooq.JSONB
import org.jooq.generated.tables.records.CasesRecord
import org.jooq.generated.tables.records.UnspecCasesRecord
import org.jooq.impl.DefaultDSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional;
import spock.lang.Specification
import uk.gov.hmcts.ccf.demo.api.ApiEventCreation
import uk.gov.hmcts.ccf.demo.dto.Company
import uk.gov.hmcts.ccf.demo.event.CreateClaim
import uk.gov.hmcts.ccf.demo.model.UnspecCase
import uk.gov.hmcts.ccf.demo.repository.CaseRepository

import static org.jooq.generated.Tables.CASES
import static org.jooq.generated.Tables.UNSPEC_CASES;

@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class CaseRepositorySpecification extends Specification {

    @Autowired
    CaseRepository repository

    @Autowired
    DefaultDSLContext create;


    def "Saves and loads the case"() {
        given:
        CasesRecord parent = create.newRecord(CASES);
        parent.setDescription("Never going to happen");
        parent.store();

        UnspecCase c = new UnspecCase(parent.getCaseId(), new Company(), new Company())
        c.setNotes(Lists.asList("Bar"))
        c.setName("Foo")
        repository.save(c)
        def loaded = repository.load(c.id)

        expect:
        loaded.name == "Foo"
        loaded.notes.size() == 1
    }

    def "Updates a case"() {
        given:
        CasesRecord parent = create.newRecord(CASES);
        parent.setDescription("Never going to happen");
        parent.store();

        UnspecCase c = new UnspecCase(parent.getCaseId(), new Company(), new Company())
        c.setNotes(Lists.asList("Bar"))
        c.setName("Foo")
        repository.save(c)
        def loaded = repository.load(c.id)
        loaded.getNotes().add("Bar")
        repository.save(loaded)
        loaded = repository.load(c.id)

        expect:
        loaded.name == "Foo"
        loaded.notes.size() == 2
    }


    def "A claim must have defendant and claimant"() {
        when:
        UnspecCase u = new UnspecCase(1, null, null)
        repository.save(u)

        then:
        thrown NullPointerException
    }
}
