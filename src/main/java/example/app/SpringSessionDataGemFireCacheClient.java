package example.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import example.support.NumberUtils;

/**
 * The SpringSessionDataGemFireCacheClient class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("unused")
public class SpringSessionDataGemFireCacheClient {

	public static void main(String[] args) {

		new SpringApplicationBuilder(SpringSessionDataGemFireCacheClient.class)
			.web(WebApplicationType.NONE)
			.build()
			.run(args);
	}

	@Autowired
	private GemFireOperationsSessionRepository sessionRepository;

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME)
	private Region<Object, Session> sessions;

	private Session load(Object sessionId) {
		return this.sessions.get(sessionId);
	}

	private Session newSession() {
		return this.sessionRepository.createSession();
	}

	private Session save(Session session) {
		this.sessionRepository.save(session);
		return session;
	}

	@PostConstruct
	public void postInit() {

		assertThat(this.sessionRepository).isNotNull();
		assertThat(this.sessions).isNotNull();
	}

	@Bean
	ApplicationRunner runner() {

		return args -> {

			Session expected = save(newSession());
			Session actual = load(expected.getId());

			assertThat(actual).isEqualTo(expected);

			System.err.printf("Expected [%1$s];%nAnd was [%2$s]%n", expected, actual);
		};
	}
}

@Profile("xml")
@EnableGemFireHttpSession
@ImportResource("client-cache.xml")
@SuppressWarnings("unused")
class GemFireCacheClientXmlConfiguration { }

@Profile("java")
@EnableGemFireHttpSession
@SuppressWarnings("unused")
class GemFireCacheClientJavaConfiguration {

	private static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";

	private static ConnectionEndpoint newConnectionEndpoint(String host, int port) {
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
			@Value("${gemfire.client.server.port:40404") int port) {

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

	@Bean(name = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME)
	@Profile("override-session-region")
	ClientRegionFactoryBean<Object, Session> sessionRegion(GemFireCache gemfireCache,
			@Qualifier("gemfirePool") Pool gemfirePool,
			@Qualifier("sessionRegionAttributes") RegionAttributes<Object, Session> sessionRegionAttributes) {

		System.err.printf("Overriding Spring Session Data GemFire's 'ClusteredSpringSessions' Region");

		ClientRegionFactoryBean<Object, Session> sessionRegion = new ClientRegionFactoryBean<>();

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
class GemFireCacheClientAnnotationConfiguration { }
