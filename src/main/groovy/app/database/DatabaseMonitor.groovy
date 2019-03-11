package app.database

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Slf4j(value = "logger")
class DatabaseMonitor {

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Value('${DB.DELETION_INTERVAL}')
    String DELETION_INTERVAL


    @Scheduled(cron = "0 0 * * * * ")
    void deleteWeekLongTests() {

        def calendar = GregorianCalendar.getInstance(Locale.default) as Calendar
        calendar.lenient = true

        def interval = DELETION_INTERVAL.trim()
        def value = Integer.valueOf(interval.substring(0, interval.length()-1)) * (-1)
        def parameter
        switch(interval.substring(interval.length()-1)) {

            case "h": parameter = Calendar.HOUR_OF_DAY
                break
            case "d": parameter = Calendar.DAY_OF_YEAR
                break
            case "w": parameter = Calendar.WEEK_OF_YEAR
                break
            case "M": parameter = Calendar.MONTH
                break
            default: throw new IllegalArgumentException("The only time values accepted are: h/d/w/M (hour/day/week/Month)")
        }

        calendar.add(parameter, value)

        def oldTestExecutions = testExecutionRepository.findOldTestExecutions(calendar.time)
        logger.debug("Number of tests to delete: ${oldTestExecutions.size()}")

        for(oldTestExecution in oldTestExecutions) {
            testExecutionRepository.delete(oldTestExecution)
            logger.info("Test deleted: ${oldTestExecution.uuid}")
        }
    }
}
