package app.config


import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class RestConfig {

//    @Value("${resttemplate.readtimeout}")
//    private int REST_TEMPLATE_READ_TIMEOUT
//
//    @Value("${resttemplate.connectiontimeout}")
//    private int REST_TEMPLATE_CONNECTION_TIMEOUT

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory())
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory()
//        factory.setReadTimeout(REST_TEMPLATE_READ_TIMEOUT)
//        factory.setConnectTimeout(REST_TEMPLATE_CONNECTION_TIMEOUT)

        return factory
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
}
