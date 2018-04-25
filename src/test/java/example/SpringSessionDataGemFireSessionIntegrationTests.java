package example;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.apache.geode.cache.Region;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * The {@link SpringSessionDataGemFireSessionIntegrationTests} class...
 *
 * @author John Blum
 * @see example.AbstractGemFireCacheClientSessionTests
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class SpringSessionDataGemFireSessionIntegrationTests extends AbstractGemFireCacheClientSessionTests {

	@SuppressWarnings("unused")
	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME)
	private Region<Object, Session> sessions;

	@Override
	protected Region<Object, Session> getSessionRegion() {
		return this.sessions;
	}

	@Test
	public void sessionCreationAndAccessIsSuccessful() {

		Session session = save(touch(newSession()));

		assertThat(session).isNotNull();
		assertThat(session.isExpired()).isFalse();

		session.setAttribute("attrOne", 1);
		session.setAttribute("attrTwo", 2);

		save(touch(session));

		Session loadedSession = load(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession.isExpired()).isFalse();
		assertThat(loadedSession).isNotSameAs(session);
		assertThat(loadedSession.getId()).isEqualTo(session.getId());
		assertThat(loadedSession.<Integer>getAttribute("attrOne")).isEqualTo(1);
		assertThat(loadedSession.<Integer>getAttribute("attrTwo")).isEqualTo(2);

		loadedSession.removeAttribute("attrTwo");

		assertThat(loadedSession.getAttributeNames()).doesNotContain("attrTwo");
		assertThat(loadedSession.getAttributeNames()).hasSize(1);

		save(touch(loadedSession));

		Session reloadedSession = load(loadedSession.getId());

		assertThat(reloadedSession).isNotNull();
		assertThat(reloadedSession.isExpired()).isFalse();
		assertThat(reloadedSession).isNotSameAs(loadedSession);
		assertThat(reloadedSession.getId()).isEqualTo(loadedSession.getId());
		assertThat(reloadedSession.getAttributeNames()).hasSize(1);
		assertThat(reloadedSession.getAttributeNames()).doesNotContain("attrTwo");
		assertThat(reloadedSession.<Integer>getAttribute("attrOne")).isEqualTo(1);
	}

	//@PeerCacheApplication
	@ClientCacheApplication(subscriptionEnabled = true)
	@EnableGemFireHttpSession(poolName = "DEFAULT")
	static class TestConfiguration { }

}
