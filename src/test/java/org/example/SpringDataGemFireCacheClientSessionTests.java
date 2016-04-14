package org.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.Region;

/**
 * The SpringDataGemFireCacheClientSessionTests class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataGemFireCacheClientSessionTests.GemFireCacheClientJavaConfiguration.class)
@SuppressWarnings("unused")
public class SpringDataGemFireCacheClientSessionTests extends AbstractGemFireCacheClientSessionTests {

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	private Region<Object, ExpiringSession> sessions;

	@Override
	protected Region<Object, ExpiringSession> getSessionRegion() {
		return sessions;
	}

	@Test
	public void sessionCreationAndAccessIsSuccessful() {
		ExpiringSession expected = save(touch(newSession()));

		assertThat(expected).isNotNull();
		assertThat(expected.isExpired()).isFalse();

		ExpiringSession actual = loadDirect(expected.getId());

		assertThat(actual).isEqualTo(expected);

		// Session timeout (i.e. GemFire's "ClusteredSpringSessions" Region expiration idle-timeout)
		// is set to 1 second
		waitOnConditionForDuration(() -> false, TimeUnit.SECONDS.toMillis(2));

		actual = load(actual.getId());

		assertThat(actual).isNull();

		actual = loadDirect(expected.getId());

		assertThat(actual).isNull();
	}

	@Configuration
	@EnableGemFireHttpSession
	public static class GemFireCacheClientJavaConfiguration {

		static final int GEMFIRE_CACHE_SERVER_PORT = 12480;
		static final int MAX_CONNECTIONS = 50;

		static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";
		static final String GEMFIRE_CACHE_SERVER_HOST = "localhost";

		String applicationName() {
			return SpringDataGemFireCacheClientSessionTests.class.getSimpleName();
		}

		String gemfireLogLevel() {
			return System.getProperty("gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
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
			gemfirePool.setMaxConnections(50);
			gemfirePool.setPingInterval(TimeUnit.SECONDS.toMillis(15));
			gemfirePool.setReadTimeout(intValue(TimeUnit.SECONDS.toMillis(20)));
			gemfirePool.setRetryAttempts(1);
			gemfirePool.setSubscriptionEnabled(true);
			gemfirePool.addServers(newConnectionEndpoint(GEMFIRE_CACHE_SERVER_HOST, GEMFIRE_CACHE_SERVER_PORT));

			return gemfirePool;
		}
	}

}
