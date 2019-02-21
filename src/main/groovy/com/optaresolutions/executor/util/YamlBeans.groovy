package com.optaresolutions.executor.util

import com.optaresolutions.executor.model.DockerCompose
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.introspector.PropertyUtils
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer

import java.beans.IntrospectionException

@Configuration
class YamlBeans {

    @Bean
    @Qualifier("default")
    Yaml yaml() {
        return new Yaml()
    }

    @Bean
    @Qualifier("skipMissingProperties")
    Yaml yamlSkipMissingProperties() {
        def representer = new Representer()
        representer.propertyUtils.skipMissingProperties = true
        return new Yaml(representer)
    }

    @Bean
    @Qualifier("skipTags")
    Yaml yamlSkipTags() {
        def representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null
                }
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
                }
            }

            @Override
            protected Set<Property> getProperties( Class<? extends Object> type ) throws IntrospectionException {
                super.getProperties( type ).findAll { it.name != 'metaClass' }
            }

        }

        def options = new DumperOptions()
        options.indent = 2
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.lineBreak = DumperOptions.LineBreak.UNIX
        options.prettyFlow = true

        return new Yaml(representer, options)
    }
}
