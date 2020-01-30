package no.ccat.contract

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import testUtils.ApiTestContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")

class ConceptContractTest : ApiTestContainer() {

    @Nested
    inner class GetWithQuery{

    }

    @Nested
    inner class GetWithId{}

}