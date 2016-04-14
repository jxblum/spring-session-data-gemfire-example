package org.example.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.GemfireConstants;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;

/**
 * The SpringSessionDataGemFireCacheClient class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("unused")
public class SpringSessionDataGemFireCacheClient implements CommandLineRunner {

	public static void main(final String[] args) {
		SpringApplication springApplication = new SpringApplication(SpringSessionDataGemFireCacheClient.class);
		springApplication.setWebEnvironment(false);
		springApplication.run(args);
	}

	@Autowired
	GemFireOperationsSessionRepository sessionRepository;

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	Region<Object, ExpiringSession> sessions;

	ExpiringSession newSession() {
		return sessionRepository.createSession();
	}

	ExpiringSession load(Object sessionId) {
		return sessions.get(sessionId);
	}

	ExpiringSession save(ExpiringSession session) {
		sessionRepository.save(session);
		return session;
	}

	@PostConstruct
	public void postInit() {
		assertThat(sessionRepository).isNotNull();
		assertThat(sessions).isNotNull();
	}

	@Override
	public void run(String... args) throws Exception {
		ExpiringSession expected = save(newSession());
		ExpiringSession actual = load(expected.getId());

		assertThat(actual).isEqualTo(expected);

		System.out.printf("Expected [%1$s];%nAnd was [%2$s]%n", expected, actual);
	}
}

@Configuration
@ImportResource("client-cache.xml")
@EnableGemFireHttpSession
@Profile("xml")
@SuppressWarnings("unused")
class GemFireCacheClientXmlConfiguration {
}

@Configuration
@Profile("java")
@EnableGemFireHttpSession
@SuppressWarnings("unused")
class GemFireCacheClientJavaConfiguration {

	static final int GEMFIRE_CACHE_SERVER_PORT = 12480;
	static final int MAX_CONNECTIONS = 50;

	static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";
	static final String GEMFIRE_CACHE_SERVER_HOST = "localhost";

	String applicationName() {
		return SpringSessionDataGemFireCacheClient.class.getSimpleName().concat("Application");
	}

	String gemfireLogLevel() {
		return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	int intValue(Long value) {
		return value.intValue();
	}

	ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
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

	@Bean
	PoolFactoryBean gemfirePool() {
		PoolFactoryBean gemfirePool = new PoolFactoryBean();

		gemfirePool.setFreeConnectionTimeout(intValue(TimeUnit.SECONDS.toMillis(5)));
		gemfirePool.setIdleTimeout(TimeUnit.MINUTES.toMillis(2));
		gemfirePool.setKeepAlive(false);
		gemfirePool.setMaxConnections(MAX_CONNECTIONS);
		gemfirePool.setPingInterval(TimeUnit.SECONDS.toMillis(15));
		gemfirePool.setReadTimeout(intValue(TimeUnit.SECONDS.toMillis(20)));
		gemfirePool.setRetryAttempts(1);
		gemfirePool.setSubscriptionEnabled(true);
		gemfirePool.addServers(newConnectionEndpoint(GEMFIRE_CACHE_SERVER_HOST, GEMFIRE_CACHE_SERVER_PORT));

		return gemfirePool;
	}

	@Bean(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	@Profile("overridden-session-region")
	ClientRegionFactoryBean<Object, ExpiringSession> sessionRegion(GemFireCache gemfireCache,
		RegionAttributes<Object, ExpiringSession> sessionRegionAttributes)
	{
		System.err.printf("Overriding Spring Session Data GemFire's 'ClusteredSpringSessions' Region");

		ClientRegionFactoryBean<Object, ExpiringSession> sessionRegion = new ClientRegionFactoryBean<>();

		sessionRegion.setAttributes(sessionRegionAttributes);
		sessionRegion.setCache(gemfireCache);
		sessionRegion.setClose(false);
		sessionRegion.setPoolName(GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME);
		sessionRegion.setShortcut(ClientRegionShortcut.PROXY);

		return sessionRegion;
	}

}
