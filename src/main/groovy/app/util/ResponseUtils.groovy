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

package app.util

import app.model.callback.Response
import app.model.resultsRepo.PostTestSuiteResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")
@Slf4j(value = "logger")
class ResponseUtils {

    @Autowired
    ObjectMapper mapper

    @Value('${CALLBACKS}')
    String CALLBACKS

    private static ResponseUtils instance = null

    private ResponseUtils() {}

    static ResponseUtils getInstance() {
        if(!instance) {
            instance = new ResponseUtils()
        }
        return instance
    }

    ResponseEntity getResponseEntity(HttpStatus status, String key, Object value) {
        def map = new HashMap<String, Object>()
        map.put(key, value)
        return getResponseEntity(status, map)
    }

    ResponseEntity getErrorResponseEntity(HttpStatus status, String message, Throwable stack) {
        def map = new HashMap<String, Object>()
        map.put("errorCode", status.value())
        map.put("message", message)
        map.put("stack", stack)
        return getResponseEntity(status, map)
    }

    ResponseEntity getResponseEntity(HttpStatus status, Map<String, Object> map) {
        return ResponseEntity.status(status).body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map))
    }

    void postCallback(String url, Object payload) {

        RestTemplate restTemplate = new RestTemplate()

        try {

            url = url.replaceAll("\\s","")
            logger.info("Callbacks ${CALLBACKS}. Sending callback to ${url}")

            URI uri = new URI(url)

            HttpHeaders headers = new HttpHeaders()
            headers.setContentType(MediaType.APPLICATION_JSON)

            HttpEntity<Response> request = new HttpEntity<>(payload, headers)

            def response = restTemplate.postForEntity(uri, request, String.class)

        } catch (Exception e) {
            logger.error(e.message)
        }
    }

    String postTestResult(String url, Object payload) {

        RestTemplate restTemplate = new RestTemplate()

        try {

            logger.info("Sending results to ${url}:")
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(payload);
            logger.info(json)

            URI uri = new URI(url)

            HttpHeaders headers = new HttpHeaders()
            headers.setContentType(MediaType.APPLICATION_JSON)

            HttpEntity<Response> request = new HttpEntity<>(payload, headers)

            ResponseEntity response = restTemplate.postForEntity(uri, request, PostTestSuiteResponse.class)

            return response.getBody().getUuid()

        } catch (Exception e) {
            logger.error(e)
        }
    }
}
