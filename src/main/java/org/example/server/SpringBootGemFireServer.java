package org.example.server;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;

import com.gemstone.gemfire.cache.Cache;

/**
 * The SpringBootGemFireServer class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@EnableGemFireHttpSession(maxInactiveIntervalInSeconds = 20)
@SuppressWarnings("unused")
public class SpringBootGemFireServer {

	protected static final String DEFAULT_GEMFIRE_LOG_LEVEL = "config";

	public static void main(final String[] args) {
		SpringApplication springApplication = new SpringApplication(SpringBootGemFireServer.class);
		springApplication.setWebEnvironment(false);
		springApplication.run(args);
	}

	String applicationName() {
		return SpringBootGemFireServer.class.getSimpleName();
	}

	String gemfireLogLevel() {
		return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	int intValue(Number number) {
		return number.intValue();
	}

	@Bean
	PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	// NOTE: the reason the port is a numerically typed value rather than a String, since Properties just take
	// a String value, is to ensure the port is actually a valid integer value
	@Bean
	Properties gemfireProperties(@Value("${gemfire.locator.host-and-port:localhost[11235]}") String locatorHostPort,
			@Value("${gemfire.locators:localhost[11235]}") String locators,
			@Value("${gemfire.manager.port:1199}") int managerPort) {

		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", applicationName());
		gemfireProperties.setProperty("mcast-port", "0");
		gemfireProperties.setProperty("log-level", gemfireLogLevel());
		gemfireProperties.setProperty("locators", locators);
		gemfireProperties.setProperty("jmx-manager", "true");
		gemfireProperties.setProperty("jmx-manager-port", String.valueOf(managerPort));
		gemfireProperties.setProperty("jmx-manager-start", "true");
		gemfireProperties.setProperty("start-locator", locatorHostPort);

		return gemfireProperties;
	}

	@Bean
	CacheFactoryBean gemfireCache(@Qualifier("gemfireProperties") Properties gemfireProperties) {
		CacheFactoryBean gemfireCache = new CacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setProperties(gemfireProperties);

		return gemfireCache;
	}

	@Bean
	CacheServerFactoryBean gemfireCacheServer(Cache gemfireCache,
			@Value("${gemfire.cache.server.bind-address:localhost}") String bindAddress,
			@Value("${gemfire.cache.server.hostname-for-clients:localhost}") String hostnameForClients,
			@Value("${gemfire.cache.server.port:12480}") int port) {

		CacheServerFactoryBean gemfireCacheServer = new CacheServerFactoryBean();

		gemfireCacheServer.setAutoStartup(true);
		gemfireCacheServer.setCache(gemfireCache);
		gemfireCacheServer.setBindAddress(bindAddress);
		gemfireCacheServer.setHostNameForClients(hostnameForClients);
		gemfireCacheServer.setMaxTimeBetweenPings(intValue(TimeUnit.MINUTES.toMillis(1)));
		gemfireCacheServer.setNotifyBySubscription(true);
		gemfireCacheServer.setPort(port);

		return gemfireCacheServer;
	}

}
