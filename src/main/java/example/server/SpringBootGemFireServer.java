package example.server;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.server.CacheServer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.util.StringUtils;

import example.support.NumberUtils;

/**
 * The SpringBootGemFireServer class is a Spring Boot application that configures and bootstraps a GemFire Server
 * to store and manage HttpSessions with Spring Session.
 *
 * @author John Blum
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see com.gemstone.gemfire.cache.Cache
 * @since 1.0.0
 */
@SpringBootApplication
@EnableGemFireHttpSession(maxInactiveIntervalInSeconds = 1)
@SuppressWarnings("unused")
public class SpringBootGemFireServer {

	public static final int GEMFIRE_CACHE_SERVER_PORT = CacheServer.DEFAULT_PORT;

	protected static final String DEFAULT_GEMFIRE_LOG_LEVEL = "config";

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(SpringBootGemFireServer.class);
		springApplication.setWebEnvironment(false);
		springApplication.run(args);
	}

	@Bean
	static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	Properties gemfireProperties(
		@Value("${gemfire.locator.host-port:localhost[10334]}") String locatorHostPort,
		@Value("${gemfire.locators:}") String locators,
		@Value("${gemfire.manager.port:1099}") int managerPort)
	{
		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", applicationName());
		gemfireProperties.setProperty("mcast-port", "0");
		gemfireProperties.setProperty("log-level", logLevel());
		gemfireProperties.setProperty("jmx-manager", "true");
		gemfireProperties.setProperty("jmx-manager-port", String.valueOf(managerPort));
		gemfireProperties.setProperty("jmx-manager-start", "true");

		if (StringUtils.hasText(locators)) {
			gemfireProperties.setProperty("locators", locators);
		}
		else {
			gemfireProperties.setProperty("start-locator", locatorHostPort);
		}

		return gemfireProperties;
	}

	String applicationName() {
		return SpringBootGemFireServer.class.getSimpleName();
	}

	String logLevel() {
		return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
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
			@Value("${gemfire.cache.server.port:"+GEMFIRE_CACHE_SERVER_PORT+ "}") int port) {

		CacheServerFactoryBean gemfireCacheServer = new CacheServerFactoryBean();

		gemfireCacheServer.setAutoStartup(true);
		gemfireCacheServer.setCache(gemfireCache);
		gemfireCacheServer.setBindAddress(bindAddress);
		gemfireCacheServer.setHostNameForClients(hostnameForClients);
		gemfireCacheServer.setMaxTimeBetweenPings(NumberUtils.intValue(TimeUnit.MINUTES.toMillis(1)));
		gemfireCacheServer.setPort(port);

		return gemfireCacheServer;
	}
}
