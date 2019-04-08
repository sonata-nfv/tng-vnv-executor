/*
 * Copyright (c) 2015 SONATA-NFV, 2017 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * ALL RIGHTS RESERVED.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Neither the name of the SONATA-NFV, 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * This work has been performed in the framework of the SONATA project,
 * funded by the European Commission under Grant number 671517 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the SONATA
 * partner consortium (www.sonata-nfv.eu).
 *
 * This work has been performed in the framework of the 5GTANGO project,
 * funded by the European Commission under Grant number 761493 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the 5GTANGO
 * partner consortium (www.5gtango.eu).
 */

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
            logger.info("Test deleted: ${oldTestExecution.test_uuid}")
        }
    }
}
