package app.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")

class ResponseUtils {

    @Autowired
    ObjectMapper mapper

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
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map))
    }
}
