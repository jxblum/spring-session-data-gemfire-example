package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.client.PoolFactory;
import com.gemstone.gemfire.cache.client.PoolManager;
import com.gemstone.gemfire.cache.server.CacheServer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.GemfireConstants;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import example.support.NumberUtils;

/**
 * Test suite of test cases testing a GemFire cache client application using Spring Session backed by GemFire
 * for managing HttpSessions.
 *
 * @author John Blum
 * @see com.gemstone.gemfire.cache.client.ClientCache
 * @see org.springframework.session.ExpiringSession
 * @see org.springframework.session.SessionRepository
 * @since 1.0.0
 */
public class GemFireCacheClientSessionTests extends AbstractGemFireCacheClientSessionTests {

	protected static final int GEMFIRE_CACHE_SERVER_PORT = CacheServer.DEFAULT_PORT;

	protected static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";
	protected static final String GEMFIRE_CACHE_SERVER_HOST = "localhost";
	protected static final String GEMFIRE_POOL_NAME = GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME;
	protected static final String GEMFIRE_REGION_NAME =
		GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME;

	private static ClientCache gemfireCache;

	private static SessionRepository<ExpiringSession> sessionRepository;

	@BeforeClass
	public static void setupGemFireCacheClient() throws Exception {
		gemfireCache = gemfireCache(applicationName(), logLevel());

		Pool gemfirePool = gemfirePool(GEMFIRE_POOL_NAME);

		Region<Object, ExpiringSession> sessionRegion = sessionRegion(gemfireCache, gemfirePool, GEMFIRE_REGION_NAME);

		sessionRepository = sessionRepository(gemfireOperations(sessionRegion));
	}

	@AfterClass
	public static void shutdownGemFireCacheClient() {
		if (gemfireCache != null) {
			gemfireCache.close();
		}
	}

	static String applicationName() {
		return GemFireCacheClientSessionTests.class.getSimpleName();
	}

	static String logLevel() {
		return System.getProperty("gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	static ClientCache gemfireCache(String name, String logLevel) {
		return new ClientCacheFactory().set("name", name).set("log-level", logLevel).create();
	}

	static Pool gemfirePool(String poolName) {
		PoolFactory poolFactory = PoolManager.createFactory();

		poolFactory.setPingInterval(TimeUnit.SECONDS.toMillis(5));
		poolFactory.setReadTimeout(NumberUtils.intValue(TimeUnit.SECONDS.toMillis(20)));
		poolFactory.setRetryAttempts(1);
		poolFactory.setSubscriptionEnabled(true);
		poolFactory.setThreadLocalConnections(false);
		poolFactory.addServer(GEMFIRE_CACHE_SERVER_HOST, GEMFIRE_CACHE_SERVER_PORT);

		return poolFactory.create(poolName);
	}

	static Region<Object, ExpiringSession> sessionRegion(ClientCache gemfireCache, Pool gemfirePool, String name) {
		ClientRegionFactory<Object, ExpiringSession> regionFactory = gemfireCache.createClientRegionFactory(
			ClientRegionShortcut.PROXY);

		regionFactory.setKeyConstraint(Object.class);
		regionFactory.setPoolName(gemfirePool.getName());
		regionFactory.setValueConstraint(ExpiringSession.class);

		return regionFactory.create(name);
	}

	static GemfireOperations gemfireOperations(Region<Object, ExpiringSession> sessionRegion) {
		return new GemfireTemplate(sessionRegion);
	}

	static SessionRepository<ExpiringSession> sessionRepository(GemfireOperations gemfireOperations) throws Exception {
		GemFireOperationsSessionRepository sessionRepository = new GemFireOperationsSessionRepository(gemfireOperations);
		sessionRepository.afterPropertiesSet();
		return sessionRepository;
	}

	@Override
	protected Region<Object, ExpiringSession> getSessionRegion() {
		return gemfireCache.getRegion(GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME);
	}

	@Override
	protected SessionRepository<ExpiringSession> getSessionRepository() {
		assertThat(sessionRepository).isNotNull();
		return sessionRepository;
	}

	@Before
	public void setup() {
		assertThat(gemfireCache).isNotNull();
		assertThat(sessionRepository).isNotNull();
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
}
