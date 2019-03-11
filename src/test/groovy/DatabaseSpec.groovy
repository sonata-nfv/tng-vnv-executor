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

    def setupSpec() {
        testExecution = new TestExecution()
        testExecution.uuid = UUID.randomUUID().toString()
        testExecution.state = TestExecution.TestState.STARTING
    }

    def "TestExecution inserted and found"() {

        when:
        testExecutionRepository.save(testExecution)
        def aux = testExecutionRepository.findById(testExecution.uuid).get()

        then:
        noExceptionThrown()
        aux.created != null
        aux.lasModifiedDate != null
        aux.state == TestExecution.TestState.STARTING

    }

    def "TestExecution updating"() {

        setup: testExecution.state = TestExecution.TestState.CANCELLED
        when:
        testExecutionRepository.save(testExecution)
        def aux = testExecutionRepository.findById(testExecution.uuid).get()

        then:
        noExceptionThrown()
        aux.state == TestExecution.TestState.CANCELLED
        aux.created != null
        aux.lasModifiedDate != null
    }

    def "TestExecution deleted"() {

        when:
        testExecutionRepository.delete(testExecution)
        testExecutionRepository.findById(testExecution.uuid).get()

        then: thrown(NoSuchElementException.class)
    }

}
