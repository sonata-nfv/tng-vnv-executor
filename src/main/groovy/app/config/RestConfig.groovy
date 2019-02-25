package app.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
class RestConfig extends WebMvcConfigurerAdapter{

    @Bean
    @Qualifier("yaml")
    ObjectMapper objectMapperYaml() {
        def yamlFactory = new YAMLFactory()
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID, false)
                .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)

        ObjectMapper mapper = new ObjectMapper(yamlFactory)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        return mapper
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        return mapper
    }

    @Bean
    FilterRegistrationBean customCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource()
        CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues()
        source.registerCorsConfiguration("/**", config)
        config.addAllowedMethod(CorsConfiguration.ALL)
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source))
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE)
        return bean
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new YamlJackson2HttpMessageConverter())
    }

    final class YamlJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {
        YamlJackson2HttpMessageConverter() {
            super(objectMapperYaml(), MediaType.parseMediaType("application/yaml"))
        }
    }

    final class YamlJackson2ObjectMapperBuilder extends Jackson2ObjectMapperBuilder {
        YamlJackson2ObjectMapperBuilder() {

        }
    }
}
