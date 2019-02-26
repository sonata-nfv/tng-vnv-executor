import app.Application
import app.config.ApplicationConfig
import app.config.GeneralConfig
import app.database.TestExecution
import app.database.TestExecutionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [Application.class, GeneralConfig.class, ApplicationConfig.class, TestExecutionRepository.class])
@TestPropertySource("classpath:application-test.properties")
class DatabaseSpec extends Specification {

    @Autowired
    TestExecutionRepository testExecutionRepository

    static TestExecution testExecution

    def setup() {
        testExecution = new TestExecution()
        testExecution.id = UUID.randomUUID().toString()
        testExecution.state = TestExecution.TestState.STARTING
    }

    def "TestExecution inserted and found"() {

        when:
        testExecutionRepository.save(testExecution)
        def aux = testExecutionRepository.findById(testExecution.id).get()

        then:
        noExceptionThrown()
//        aux.created != null
//        aux.lasModifiedDate != null

        cleanup: testExecutionRepository.delete(aux)

    }

}
