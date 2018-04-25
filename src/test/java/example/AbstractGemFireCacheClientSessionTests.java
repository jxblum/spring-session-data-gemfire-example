package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.apache.geode.cache.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

/**
 * The {@link AbstractGemFireCacheClientSessionTests} class is an abstract base class encapsulating core functionality
 * for writing integration tests using GemFire as the HttpSession implementation provider for Spring Session.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.springframework.session.Session
 * @see org.springframework.session.SessionRepository
 * @since 1.0.0
 */
@SuppressWarnings("all")
public abstract class AbstractGemFireCacheClientSessionTests {

	private final Object LOCK = new Object();

	@Autowired
	private SessionRepository<Session> sessionRepository;

	protected abstract Region<Object, Session> getSessionRegion();

	protected SessionRepository<Session> getSessionRepository() {
		assertThat(sessionRepository).isNotNull();
		return sessionRepository;
	}

	protected Session load(Object sessionId) {
		return getSessionRepository().findById(sessionId.toString());
	}

	protected Session loadFromRegion(Object sessionId) {
		return getSessionRegion().get(sessionId);
	}

	protected Session newSession() {
		return getSessionRepository().createSession();
	}

	protected Session save(Session session) {
		getSessionRepository().save(session);
		return session;
	}

	protected Session touch(Session session) {
		session.setLastAccessedTime(Instant.now());
		return session;
	}

	protected void waitOnConditionForDuration(Condition condition, long duration) {

		final long timeout = System.currentTimeMillis() + duration;

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

	protected interface Condition {
		boolean evaluate();
	}
}
