package org.lareferencia.dashboard.app;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lareferencia.core.util.ConfigPathResolver;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// REMOVED: WebMvcConfigurerAdapter was deprecated in Spring 5.0 and removed in Spring 6.0

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
@ComponentScan(basePackages = { "org.lareferencia.dashboard", "org.lareferencia.app.dashboard",
                "org.lareferencia.core.service.validation", "org.lareferencia.core.repository.validation",
                "org.lareferencia.core.worker.validation" })
@EntityScan(basePackages = { "org.lareferencia.core.domain", "org.lareferencia.core.oabroker" })
@EnableJpaRepositories(basePackages = { "org.lareferencia.core.repository.jpa", "org.lareferencia.core.oabroker" })
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@EnableAutoConfiguration(exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration.class,
                org.springdoc.core.configuration.SpringDocDataRestConfiguration.class,
                org.springdoc.core.configuration.SpringDocHateoasConfiguration.class
})
@ImportResource({ "classpath*:application-context.xml" })
@Configuration
public class DashboardApplication {

        @Autowired
        private Environment enviroment;

        public static void main(String[] args) {
                // Export config directory as system property for XML context files
                // Must be set BEFORE Spring starts to be available for ${app.config.dir} in XML
                System.setProperty(ConfigPathResolver.CONFIG_DIR_PROPERTY, ConfigPathResolver.getConfigDir());

                SpringApplicationBuilder builder = new SpringApplicationBuilder()
                                .sources(DashboardApplication.class)
                                .listeners(new PropertiesDirectoryListener());

                builder.run(args);
        }

        /**
         * Listener that loads properties from
         * ${app.config.dir}/application.properties.d/*.properties
         */
        private static class PropertiesDirectoryListener
                        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

                @Override
                public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
                        Path dir = ConfigPathResolver.resolvePath("application.properties.d");

                        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                                System.out.println("[PropertiesLoader] Directory not found: " + dir);
                                return;
                        }

                        try (Stream<Path> stream = Files.list(dir)) {
                                ConfigurableEnvironment env = event.getEnvironment();

                                List<Path> propertyFiles = stream
                                                .filter(p -> p.toString().endsWith(".properties"))
                                                .sorted()
                                                .collect(Collectors.toList());

                                for (Path file : propertyFiles) {
                                        try {
                                                ResourcePropertySource source = new ResourcePropertySource(
                                                                "custom-" + file.getFileName().toString(),
                                                                new FileSystemResource(file.toFile()));
                                                env.getPropertySources().addLast(source);
                                                System.out.println("[PropertiesLoader] Loaded: " + file.getFileName());
                                        } catch (IOException e) {
                                                System.err.println("[PropertiesLoader] Failed to load: " + file + " - "
                                                                + e.getMessage());
                                        }
                                }

                        } catch (IOException e) {
                                System.err.println("[PropertiesLoader] Error listing directory: " + e.getMessage());
                        }
                }
        }

        /**
         * SpringDoc OpenAPI configuration (replacement for Springfox Swagger)
         * Swagger UI will be available at: http://localhost:8092/swagger-ui.html
         */
        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title(enviroment.getProperty("rest.lareferencia.metadata.title",
                                                                "LA Referencia Dashboard API"))
                                                .description(
                                                                enviroment.getProperty(
                                                                                "rest.lareferencia.metadata.description",
                                                                                "Dashboard REST API"))
                                                .version(enviroment.getProperty("rest.lareferencia.metadata.version",
                                                                "4.2.6"))
                                                .license(new License()
                                                                .name(enviroment.getProperty(
                                                                                "rest.lareferencia.metadata.license",
                                                                                "GPL-3.0"))
                                                                .url(enviroment.getProperty(
                                                                                "rest.lareferencia.metadata.licenseurl",
                                                                                "https://www.gnu.org/licenses/gpl-3.0.html"))));
        }

        /**
         * CORS configuration - Updated for Spring 6.0
         * Using WebMvcConfigurer interface directly instead of deprecated
         * WebMvcConfigurerAdapter
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

}
