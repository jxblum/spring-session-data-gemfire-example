package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.server.CacheServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

import example.support.NumberUtils;

/**
 * Test suite of test cases testing a GemFire cache client application using Spring Session backed by GemFire
 * for managing HttpSessions.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.session.Session
 * @see org.springframework.session.SessionRepository
 * @since 1.0.0
 */
public class GemFireCacheClientSessionTests extends AbstractGemFireCacheClientSessionTests {

	private static final int GEMFIRE_CACHE_SERVER_PORT = CacheServer.DEFAULT_PORT;

	private static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";
	private static final String GEMFIRE_CACHE_SERVER_HOST = "localhost";
	private static final String GEMFIRE_POOL_NAME = GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME;
	private static final String GEMFIRE_REGION_NAME = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME;

	private static ClientCache gemfireCache;

	private static SessionRepository<Session> sessionRepository;

	@BeforeClass
	public static void startGemFireCacheClient() throws Exception {

		gemfireCache = gemfireCache(applicationName(), logLevel());

		Pool gemfirePool = gemfirePool(GEMFIRE_POOL_NAME);

		Region<Object, Session> sessionRegion = sessionRegion(gemfireCache, gemfirePool, GEMFIRE_REGION_NAME);

		sessionRepository = sessionRepository(gemfireOperations(sessionRegion));
	}

	@AfterClass
	public static void stopGemFireCacheClient() {

		if (gemfireCache != null) {
			gemfireCache.close();
		}
	}

	private static String applicationName() {
		return GemFireCacheClientSessionTests.class.getSimpleName();
	}

	private static String logLevel() {
		return System.getProperty("gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	private static ClientCache gemfireCache(String name, String logLevel) {
		return new ClientCacheFactory().set("name", name).set("log-level", logLevel).create();
	}

	private static Pool gemfirePool(String poolName) {

		PoolFactory poolFactory = PoolManager.createFactory();

		poolFactory.setPingInterval(TimeUnit.SECONDS.toMillis(5));
		poolFactory.setReadTimeout(NumberUtils.intValue(TimeUnit.SECONDS.toMillis(20)));
		poolFactory.setRetryAttempts(1);
		poolFactory.setSubscriptionEnabled(true);
		poolFactory.setThreadLocalConnections(false);
		poolFactory.addServer(GEMFIRE_CACHE_SERVER_HOST, GEMFIRE_CACHE_SERVER_PORT);

		return poolFactory.create(poolName);
	}

	private static Region<Object, Session> sessionRegion(ClientCache gemfireCache, Pool gemfirePool, String name) {

		ClientRegionFactory<Object, Session> regionFactory =
			gemfireCache.createClientRegionFactory(ClientRegionShortcut.PROXY);

		regionFactory.setKeyConstraint(Object.class);
		regionFactory.setPoolName(gemfirePool.getName());
		regionFactory.setValueConstraint(Session.class);

		return regionFactory.create(name);
	}

	private static GemfireOperations gemfireOperations(Region<Object, Session> sessionRegion) {
		return new GemfireTemplate(sessionRegion);
	}

	private static SessionRepository<Session> sessionRepository(GemfireOperations gemfireOperations) throws Exception {

		GemFireOperationsSessionRepository sessionRepository =
			new GemFireOperationsSessionRepository(gemfireOperations);

		sessionRepository.afterPropertiesSet();

		return sessionRepository;
	}

	@Override
	protected Region<Object, Session> getSessionRegion() {
		return gemfireCache.getRegion(GEMFIRE_REGION_NAME);
	}

	@Override
	protected SessionRepository<Session> getSessionRepository() {
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

		Session expected = save(touch(newSession()));

		assertThat(expected).isNotNull();
		assertThat(expected.isExpired()).isFalse();

		Session actual = loadFromRegion(expected.getId());

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
