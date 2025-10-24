package org.lareferencia.dashboard.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// REMOVED: WebMvcConfigurerAdapter was deprecated in Spring 5.0 and removed in Spring 6.0

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;


@SpringBootApplication
@ComponentScan(  basePackages={ "org.lareferencia.core.dashboard", "org.lareferencia.app.dashboard" } )
@EntityScan( basePackages= { "org.lareferencia.backend.domain", "org.lareferencia.core.oabroker" } )
@EnableJpaRepositories( basePackages={ "org.lareferencia.backend.repositories.jpa", "org.lareferencia.core.oabroker" } )
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@EnableAutoConfiguration( exclude = {
	    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
	    org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration.class,
	    org.springdoc.core.configuration.SpringDocDataRestConfiguration.class,
	    org.springdoc.core.configuration.SpringDocHateoasConfiguration.class
})
@ImportResource({"classpath*:application-context.xml"})
@Configuration
public class DashboardApplication {
	
	@Autowired
	private Environment enviroment;

	public static void main(String[] args) {
		SpringApplication.run(DashboardApplication.class, args);
	}
	
	/**
	 * SpringDoc OpenAPI configuration (replacement for Springfox Swagger)
	 * Swagger UI will be available at: http://localhost:8092/swagger-ui.html
	 */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(enviroment.getProperty("rest.lareferencia.metadata.title", "LA Referencia Dashboard API"))
                        .description(enviroment.getProperty("rest.lareferencia.metadata.description", "Dashboard REST API"))
                        .version(enviroment.getProperty("rest.lareferencia.metadata.version", "4.2.6"))
                        .license(new License()
                                .name(enviroment.getProperty("rest.lareferencia.metadata.license", "GPL-3.0"))
                                .url(enviroment.getProperty("rest.lareferencia.metadata.licenseurl", "https://www.gnu.org/licenses/gpl-3.0.html"))));
    }
    
    /**
     * CORS configuration - Updated for Spring 6.0
     * Using WebMvcConfigurer interface directly instead of deprecated WebMvcConfigurerAdapter
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }           
        };          
    }

    /** Bean para el repositorio Parquet de estadísticas de validación */
    @Bean
    public org.lareferencia.backend.repositories.parquet.ValidationStatParquetRepository validationStatParquetRepository() {
        return new org.lareferencia.backend.repositories.parquet.ValidationStatParquetRepository();
    }

    /** Bean para el helper de fingerprint de registros */
    @Bean
    public org.lareferencia.core.util.IRecordFingerprintHelper fingerprintHelper() {
        return new org.lareferencia.core.util.PrefixedRecordFingerprintHelper();
    }

    /** Bean para el servicio de estadísticas de validación Parquet */
    @Bean(name = "validationStatisticsParquetService")
    public org.lareferencia.backend.validation.IValidationStatisticsService validationStatisticsParquetService() {
        // Crear una instancia del servicio Parquet
        return new org.lareferencia.backend.validation.ValidationStatisticsParquetService();
    }


}
