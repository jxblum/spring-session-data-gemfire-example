package example.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.client.Pool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import example.server.SpringBootGemFireServer;
import example.support.NumberUtils;

/**
 * The SpringSessionDataGemFireCacheClient class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("unused")
public class SpringSessionDataGemFireCacheClient implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(SpringSessionDataGemFireCacheClient.class);
		springApplication.setWebEnvironment(false);
		springApplication.run(args);
	}

	@Autowired
	GemFireOperationsSessionRepository sessionRepository;

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	Region<Object, ExpiringSession> sessions;

	ExpiringSession load(Object sessionId) {
		return sessions.get(sessionId);
	}

	ExpiringSession newSession() {
		return sessionRepository.createSession();
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

		System.err.printf("Expected [%1$s];%nAnd was [%2$s]%n", expected, actual);
	}
}

@Profile("xml")
@EnableGemFireHttpSession
@ImportResource("client-cache.xml")
@SuppressWarnings("unused")
class GemFireCacheClientXmlConfiguration {
}

@Profile("java")
@EnableGemFireHttpSession
@SuppressWarnings("unused")
class GemFireCacheClientJavaConfiguration {

	static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";

	static ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
	}

	@Bean
	static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	Properties gemfireProperties() {
		Properties gemfireProperties = new Properties();
		gemfireProperties.setProperty("name", applicationName());
		gemfireProperties.setProperty("log-level", logLevel());
		return gemfireProperties;
	}

	String applicationName() {
		return SpringSessionDataGemFireCacheClient.class.getSimpleName().concat("Application");
	}

	String logLevel() {
		return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	@Bean
	ClientCacheFactoryBean gemfireCache() {
		ClientCacheFactoryBean gemfireCache = new ClientCacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setProperties(gemfireProperties());

		return gemfireCache;
	}

	@Bean(name = GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME)
	PoolFactoryBean gemfirePool(
		@Value("${gemfire.client.server.host:localhost}") String host,
		@Value("${gemfire.client.server.port:"+ SpringBootGemFireServer.GEMFIRE_CACHE_SERVER_PORT+"}") int port
	)
	{
		PoolFactoryBean gemfirePool = new PoolFactoryBean();

		gemfirePool.setKeepAlive(false);
		gemfirePool.setPingInterval(TimeUnit.SECONDS.toMillis(5));
		gemfirePool.setReadTimeout(NumberUtils.intValue(TimeUnit.SECONDS.toMillis(20)));
		gemfirePool.setRetryAttempts(1);
		gemfirePool.setSubscriptionEnabled(true);
		gemfirePool.setThreadLocalConnections(false);
		gemfirePool.addServers(newConnectionEndpoint(host, port));

		return gemfirePool;
	}

	@Bean(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	@Profile("override-session-region")
	ClientRegionFactoryBean<Object, ExpiringSession> sessionRegion(GemFireCache gemfireCache,
		@Qualifier("gemfirePool") Pool gemfirePool,
		@Qualifier("sessionRegionAttributes") RegionAttributes<Object, ExpiringSession> sessionRegionAttributes)
	{
		System.err.printf("Overriding Spring Session Data GemFire's 'ClusteredSpringSessions' Region");

		ClientRegionFactoryBean<Object, ExpiringSession> sessionRegion = new ClientRegionFactoryBean<>();

		sessionRegion.setAttributes(sessionRegionAttributes);
		sessionRegion.setCache(gemfireCache);
		sessionRegion.setClose(false);
		sessionRegion.setPoolName(gemfirePool.getName());
		sessionRegion.setShortcut(ClientRegionShortcut.PROXY);

		return sessionRegion;
	}
}

@Profile("annotation")
@EnableGemFireHttpSession(poolName = "DEFAULT")
@ClientCacheApplication(subscriptionEnabled = true)
@SuppressWarnings("unused")
class GemFireCacheClientAnnotationConfiguration {
}
