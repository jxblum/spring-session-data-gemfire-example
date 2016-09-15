package example.webapp;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import example.server.SpringBootGemFireServer;
import example.support.NumberUtils;

/**
 * The SpringBootWebApplicationWithSpringSessionDataGemFireEnabled class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
@EnableGemFireHttpSession
@Controller
@SuppressWarnings("unused")
public class SpringBootWebApplicationWithSpringSessionDataGemFireEnabled {

	static final String DEFAULT_GEMFIRE_LOG_LEVEL = "config";
	static final String REQUEST_COUNT_SESSION_ATTRIBUTE_NAME = "requestCount";

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebApplicationWithSpringSessionDataGemFireEnabled.class, args);
	}

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
		return SpringBootWebApplicationWithSpringSessionDataGemFireEnabled.class.getSimpleName();
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

	@Bean
	PoolFactoryBean gemfirePool(
		@Value("${gemfire.client.server.host:localhost}") String host,
		@Value("${gemfire.client.server.port:"+ SpringBootGemFireServer.GEMFIRE_CACHE_SERVER_PORT+"}") int port)
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

	@RequestMapping(method = RequestMethod.GET, value="/")
	@ResponseBody
	public String hello() {
		return "<H1>Hello!</H1><br>Try the '/attributes' endpoint; it's more interesting, ;-)";
	}

	@RequestMapping("/attributes")
	@ResponseBody
	public Map attributes(HttpSession session,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "value", required = false) String value) {

		if (isSet(name) && isSet(value)) {
			session.setAttribute(name, value);
		}

		return attributes(updateRequestCount(session));
	}

	private boolean isSet(String value) {
		return StringUtils.hasText(value);
	}

	private Map<String, String> attributes(HttpSession session) {
		Map<String, String> attributes = new HashMap<>();

		for (String attributeName : toIterable(session.getAttributeNames())) {
			attributes.put(attributeName, String.valueOf(session.getAttribute(attributeName)));
		}

		return attributes;
	}

	private <T> Iterable<T> toIterable(Enumeration<T> enumeration) {
		return () -> new Iterator<T>() {
			@Override public boolean hasNext() {
				return enumeration.hasMoreElements();
			}

			@Override public T next() {
				return enumeration.nextElement();
			}
		};
	}

	@SuppressWarnings("all")
	private HttpSession updateRequestCount(HttpSession session) {
		synchronized (session) {
			session.setAttribute(REQUEST_COUNT_SESSION_ATTRIBUTE_NAME,
				nullSafeIncrement((Integer) session.getAttribute(REQUEST_COUNT_SESSION_ATTRIBUTE_NAME)));

			return session;
		}
	}

	private Integer nullSafeIncrement(Integer value) {
		return (value != null ? ++value : 1);
	}
}
