
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

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.swagger.annotations.Api;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@SpringBootApplication
@ComponentScan(  basePackages={ "org.lareferencia.core.dashboard", "org.lareferencia.app.dashboard" } )
@EntityScan( basePackages= { "org.lareferencia.backend.domain", "org.lareferencia.core.oabroker" } )
@EnableJpaRepositories( basePackages={ "org.lareferencia.backend.repositories.jpa", "org.lareferencia.core.oabroker" } ) 
@EnableAutoConfiguration( exclude = {
	    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@EnableSwagger2
@ImportResource({"classpath*:application-context.xml"})
@Configuration
public class DashboardApplication {
	
	@Autowired
	private Environment enviroment;

	public static void main(String[] args) {
		SpringApplication.run(DashboardApplication.class, args);
	}
	
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(enviroment.getProperty("rest.lareferencia.spec"))
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                //.paths(PathSelectors.any())
                .build().apiInfo(metadata());
    }

    private ApiInfo metadata() {
        return new ApiInfoBuilder()
                .title(enviroment.getProperty("rest.lareferencia.metadata.title"))
                .description(enviroment.getProperty("rest.lareferencia.metadata.description"))
                .version(enviroment.getProperty("rest.lareferencia.metadata.version"))
                .license(enviroment.getProperty("rest.lareferencia.metadata.license"))
                .licenseUrl(enviroment.getProperty("rest.lareferencia.metadata.licenseurl"))
                .build();
    }
    
    @SuppressWarnings("deprecation")
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
        	
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
    public org.lareferencia.backend.services.validation.IValidationStatisticsService validationStatisticsParquetService() {
        // Crear una instancia del servicio Parquet
        return new org.lareferencia.backend.services.parquet.ValidationStatisticsParquetService();
    }


}
