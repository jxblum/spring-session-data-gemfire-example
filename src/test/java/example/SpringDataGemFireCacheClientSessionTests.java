package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.gemstone.gemfire.cache.Region;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import example.support.NumberUtils;

/**
 * Test suite of test cases testing a Spring Data GemFire cache client application using Spring Session
 * backed by GemFire to manage HttpSessions.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.session.ExpiringSession
 * @see org.springframework.session.SessionRepository
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SpringDataGemFireCacheClientSessionTests extends AbstractGemFireCacheClientSessionTests {

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME)
	private Region<Object, ExpiringSession> sessions;

	@Override
	protected Region<Object, ExpiringSession> getSessionRegion() {
		return sessions;
	}

	@Test
	public void sessionCreationAccessAndExpirationIsSuccessful() {
		ExpiringSession expected = save(touch(newSession()));

		assertThat(expected).isNotNull();
		assertThat(expected.isExpired()).isFalse();

		ExpiringSession actual = loadFromRegion(expected.getId());

		assertThat(actual).isNotNull();
		assertThat(actual.isExpired()).isFalse();
		assertThat(actual).isEqualTo(expected);

		// Session timeout (i.e. GemFire's "ClusteredSpringSessions" Region expiration idle-timeout)
		// is set to 1 second
		waitOnConditionForDuration(() -> false, TimeUnit.SECONDS.toMillis(2));

		actual = load(actual.getId());

		assertThat(actual).isNull();

		actual = loadFromRegion(expected.getId());

		assertThat(actual).isNull();
	}

	@EnableGemFireHttpSession
	static class TestConfiguration {

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
			return SpringDataGemFireCacheClientSessionTests.class.getSimpleName();
		}

		String logLevel() {
			return System.getProperty("gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
		}

		@Bean
		ClientCacheFactoryBean gemfireCache() {
			ClientCacheFactoryBean gemfireCache = new ClientCacheFactoryBean();

			gemfireCache.setClose(true);
			gemfireCache.setProperties(gemfireProperties());

			return gemfireCache;
		}

		@Bean
		PoolFactoryBean gemfirePool(
				@Value("${gemfire.client.server.host:localhost}") String host,
		    	@Value("${gemfire.client.server.port:40404}") int port ) {

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
	}
}
