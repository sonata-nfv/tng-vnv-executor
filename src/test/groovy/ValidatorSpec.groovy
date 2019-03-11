import app.model.test.Condition
import app.util.Validator
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

class ValidatorSpec extends Specification {

    @Autowired
    Validator validator

    static File txtFile
    static File jsonFile
    static def ERROR_UNKNOWN_OPTION = "Validation FAILED. Unknown option"

    def setupSpec() {
        jsonFile = new File('/test-validator.json')
        txtFile = new File('/test-validator.json')
    }

    def "Json eq"() {

        setup:
        Condition parser = new Condition()
        parser.setType("json")
        parser.setCondition("=")
        parser.setFind("error_rate")
        parser.setValue("0.04")
        parser.setFile("file")
        parser.setName("name")
        parser.setVerdict("pass")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json eq2"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("==")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json neq"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("!=")
        parser.setFind("error_rate")
        parser.setValue("0.05")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json gt"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition(">")
        parser.setFind("error_rate")
        parser.setValue("0.03")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()

    }

    def "Json get"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition(">=")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()

    }

    def "Json lt"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<")
        parser.setFind("error_rate")
        parser.setValue("0.05")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json let"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<=")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json unknown condition"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<>")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then:
        def ex = thrown(Exception.class)
        ex.message == ERROR_UNKNOWN_OPTION

    }

    def "Text present"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("OK")
        parser.setCondition("present")

        when: validator.validateConditions(parser, txtFile)
        then: noExceptionThrown()
    }

    def "Text not present"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("KO")
        parser.setCondition("not present")

        when: validator.validateConditions(parser, txtFile)
        then: noExceptionThrown()
    }

    def "Text unknown condition"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("KK")
        parser.setCondition("present")

        when: validator.validateConditions(parser, txtFile)
        then:
        def ex = thrown(Exception.class)
        ex.message == ERROR_UNKNOWN_OPTION
    }
}