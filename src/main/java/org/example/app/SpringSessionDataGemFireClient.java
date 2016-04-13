package org.example.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import com.gemstone.gemfire.cache.Region;

/**
 * The SpringSessionDataGemFireClient class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("unused")
public class SpringSessionDataGemFireClient implements CommandLineRunner {

	public static void main(final String[] args) {
		SpringApplication springApplication = new SpringApplication(SpringSessionDataGemFireClient.class);
		springApplication.setWebEnvironment(false);
		springApplication.run(args);
	}

	@Autowired
	GemFireOperationsSessionRepository sessionRepository;

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	Region<Object, ExpiringSession> sessions;

	ExpiringSession save(ExpiringSession session) {
		sessionRepository.save(session);
		return session;
	}

	void setup(String... args) {
		assertThat(sessionRepository).isNotNull();
		assertThat(sessions).isNotNull();
	}

	@Override
	public void run(String... args) throws Exception {
		setup(args);

		ExpiringSession expected = save(sessionRepository.createSession());

		ExpiringSession actual = sessions.get(expected.getId());

		assertThat(actual).isEqualTo(expected);

		System.out.printf("Expected [%1$s];%nAnd was [%2$s]%n", expected, actual);
	}
}

@Configuration
@EnableGemFireHttpSession
@SuppressWarnings("unused")
class GemFireClientCacheConfiguration {

	static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";

	int intValue(Long value) {
		return value.intValue();
	}

	String applicationName() {
		return SpringSessionDataGemFireClient.class.getSimpleName().concat("Application");
	}

	String gemfireLogLevel() {
		return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	@Bean
	PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	Properties gemfireProperties() {
		Properties gemfireProperties = new Properties();
		gemfireProperties.setProperty("name", applicationName());
		gemfireProperties.setProperty("log-level", gemfireLogLevel());
		return gemfireProperties;
	}

	@Bean
	ClientCacheFactoryBean gemfireCache() {
		ClientCacheFactoryBean gemfireCache = new ClientCacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setProperties(gemfireProperties());

		return gemfireCache;
	}

	ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
	}

	@Bean
	PoolFactoryBean gemfirePool() {
		PoolFactoryBean gemfirePool = new PoolFactoryBean();

		gemfirePool.setFreeConnectionTimeout(intValue(TimeUnit.SECONDS.toMillis(5)));
		gemfirePool.setIdleTimeout(TimeUnit.MINUTES.toMillis(2));
		gemfirePool.setKeepAlive(false);
		gemfirePool.setMaxConnections(50);
		gemfirePool.setPingInterval(TimeUnit.SECONDS.toMillis(15));
		gemfirePool.setReadTimeout(intValue(TimeUnit.SECONDS.toMillis(20)));
		gemfirePool.setRetryAttempts(1);
		gemfirePool.setSubscriptionEnabled(true);
		gemfirePool.setServers(Collections.singleton(newConnectionEndpoint("localhost", 12480)));

		return gemfirePool;
	}
}
