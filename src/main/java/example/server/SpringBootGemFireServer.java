package example.server;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableLocator;
import org.springframework.data.gemfire.config.annotation.EnableManager;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;

/**
 * The {@link SpringBootGemFireServer} class is a Spring Boot application that configures and bootstraps
 * a GemFire Server to store and manage HttpSessions with Spring Session.
 *
 * @author John Blum
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.data.gemfire.config.annotation.CacheServerApplication
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @since 1.0.0
 */
@SpringBootApplication
@CacheServerApplication(name = "SpringBootGemFireServer")
@EnableLocator
@EnableManager(start = true)
@EnableGemFireHttpSession(maxInactiveIntervalInSeconds = 30)
@SuppressWarnings("unused")
public class SpringBootGemFireServer {

	public static void main(String[] args) {

		new SpringApplicationBuilder(SpringBootGemFireServer.class)
			.web(WebApplicationType.NONE)
			.build()
			.run(args);
	}
}
