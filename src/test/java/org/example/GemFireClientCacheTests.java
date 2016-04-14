package org.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

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

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.client.PoolFactory;
import com.gemstone.gemfire.cache.client.PoolManager;

/**
 * The GemFireClientCacheTests class...
 *
 * @author John Blum
 * @since 1.0.0
 */
public class GemFireClientCacheTests {

	protected static final int DEFAULT_CACHE_SERVER_PORT = 12480;
	protected static final int MAX_CONNECTIONS = 50;

	private static final Object LOCK = new Object();

	protected static final String DEFAULT_CACHE_SERVER_HOST = "localhost";
	protected static final String DEFAULT_GEMFIRE_LOG_LEVEL = "warning";
	protected static final String GEMFIRE_POOL_NAME = GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME;
	protected static final String GEMFIRE_REGION_NAME =
		GemFireHttpSessionConfiguration.DEFAULT_SPRING_SESSION_GEMFIRE_REGION_NAME;

	private static ClientCache gemfireCache;

	private static SessionRepository<ExpiringSession> sessionRepository;

	@BeforeClass
	public static void setupGemFireClient() {
		gemfireCache = gemfireCache(GemFireClientCacheTests.class.getSimpleName(), logLevel());

		Pool gemfirePool = gemfirePool(GEMFIRE_POOL_NAME);

		Region<Object, ExpiringSession> sessionRegion = sessionRegion(gemfireCache, gemfirePool, GEMFIRE_REGION_NAME);

		sessionRepository = sessionRepository(gemfireOperations(sessionRegion));
	}

	@AfterClass
	public static void shutdownGemFireClient() {
		if (gemfireCache != null) {
			gemfireCache.close();
		}
	}

	static int intValue(Number number) {
		return number.intValue();
	}

	static String logLevel() {
		return System.getProperty("gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	static ClientCache gemfireCache(String name, String logLevel) {
		return new ClientCacheFactory().set("name", name).set("log-level", logLevel).create();
	}

	static Pool gemfirePool(String poolName) {
		PoolFactory poolFactory = PoolManager.createFactory();

		poolFactory.setFreeConnectionTimeout(intValue(TimeUnit.SECONDS.toMillis(5)));
		poolFactory.setIdleTimeout(TimeUnit.MINUTES.toMillis(2));
		poolFactory.setMaxConnections(MAX_CONNECTIONS);
		poolFactory.setPingInterval(TimeUnit.SECONDS.toMillis(15));
		poolFactory.setPRSingleHopEnabled(true);
		poolFactory.setReadTimeout(intValue(TimeUnit.SECONDS.toMillis(20)));
		poolFactory.setRetryAttempts(1);
		poolFactory.setThreadLocalConnections(false);
		poolFactory.addServer(DEFAULT_CACHE_SERVER_HOST, DEFAULT_CACHE_SERVER_PORT);

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

	static SessionRepository<ExpiringSession> sessionRepository(GemfireOperations gemfireOperations) {
		return new GemFireOperationsSessionRepository(gemfireOperations);
	}

	ExpiringSession load(Object sessionId) {
		return sessionRepository.getSession(sessionId.toString());
	}

	ExpiringSession loadDirect(Object sessionId) {
		return gemfireCache.<Object, ExpiringSession>getRegion(GEMFIRE_REGION_NAME).get(sessionId);
	}

	ExpiringSession newSession() {
		return sessionRepository.createSession();
	}

	ExpiringSession save(ExpiringSession session) {
		sessionRepository.save(session);
		return session;
	}

	ExpiringSession touch(ExpiringSession session) {
		session.setLastAccessedTime(System.currentTimeMillis());
		return session;
	}

	void waitOnConditionForDuration(Condition condition, long duration) {
		final long timeout = (System.currentTimeMillis() + duration);

		boolean interrupted = false;

		try {
			// wait uninterrupted...
			while (!condition.evaluate() && System.currentTimeMillis() < timeout) {
				synchronized (LOCK) {
					try {
						TimeUnit.MILLISECONDS.timedWait(LOCK, 500);
					}
					catch (InterruptedException ignore) {
						interrupted = true;
					}
				}
			}
		}
		finally {
			// but, if we were interrupted, reset the interrupt!
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Before
	public void setup() {
		assertThat(gemfireCache).isNotNull();
		assertThat(sessionRepository).isNotNull();
	}

	@Test
	public void sessionCreationAndAccessIsSuccessful() {
		ExpiringSession expected = save(touch(newSession()));

		assertThat(expected).isNotNull();
		assertThat(expected.isExpired()).isFalse();

		ExpiringSession actual = loadDirect(expected.getId());

		assertThat(actual).isEqualTo(expected);

		waitOnConditionForDuration(() -> false, TimeUnit.SECONDS.toMillis(2));

		actual = load(actual.getId());

		assertThat(actual).isNull();

		actual = loadDirect(expected.getId());

		assertThat(actual).isNull();
	}

	interface Condition {
		boolean evaluate();
	}

}
