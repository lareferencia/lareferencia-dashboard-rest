package org.lareferencia.dashboard.app;

import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * SpringDoc OpenAPI configuration to handle special cases
 */
@Configuration
public class SpringDocConfig {

    /**
     * Customizer to fix Pageable parameter documentation
     * Prevents SpringDoc from trying to introspect JPA entities when documenting Pageable
     */
    @Bean
    public ParameterCustomizer parameterCustomizer() {
        return (parameter, methodParameter) -> {
            // Fix for Pageable parameters - ensure examples are simple strings
            if (parameter.getSchema() != null && parameter.getExample() != null) {
                // If the example is complex, simplify it
                if (parameter.getExample().toString().contains("[")) {
                    parameter.setExample(null);
                }
            }
            
            // Ensure sort parameter has a simple example
            if ("sort".equals(parameter.getName())) {
                parameter.setExample("id,desc");
                if (parameter.getSchema() == null) {
                    parameter.setSchema(new StringSchema());
                }
            }
            
            return parameter;
        };
    }
}
