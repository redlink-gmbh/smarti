package io.redlink.smarti.services.importer;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ConnectionBackoffStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import io.redlink.smarti.webservice.pojo.RocketWebserviceConfig;

/**
 * Implements the Rocket.Chat MongoDB as data source for Smarti
 * 
 * @author Ruediger Kurz
 */
public class ConversationSourceRocketChatWebservice extends A_ConversationSource {
	
	RocketWebserviceConfig webserviceConfig;

	public ConversationSourceRocketChatWebservice(RocketWebserviceConfig config) {

		super(E_SourceType.RocketChatWebServie, config);
		webserviceConfig = config;
	}

	@Override
	public String exportConversations() throws Exception {
		
		return getMongoRequestExport();
	}

	/**
	 * 
	 * @param payload
	 * @return
	 */
	private String getMongoRequestExport() throws IOException, ClientProtocolException {

		HttpPost post = new HttpPost(webserviceConfig.getRocketChatEndpoint());
		post.addHeader("Content-Type", "application/json; charset=UTF-8");
		post.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		post.addHeader("Accept-Language", "en-US,en;q=0.8");
	
		StringEntity entity = new StringEntity(getSourceConfiguration().asJSON().toJSONString(), "UTF-8");
		
		entity.setContentType("application/json");
		post.setEntity(entity);
	
		try (CloseableHttpResponse response = httpClientBuilder.build().execute(post)) {
			if (200 == response.getStatusLine().getStatusCode()) {
				return IOUtils.toString(response.getEntity().getContent(),
						Charset.defaultCharset());
			}
		}
		return null;
	}
	
	
	/** A HTTP client to use the Smarti web service. */
	private final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
			.setRetryHandler((exception, executionCount, context) -> executionCount < 3)
			.setConnectionBackoffStrategy(new ConnectionBackoffStrategy() {
				@Override
				public boolean shouldBackoff(HttpResponse resp) {
					return false;
				}

				@Override
				public boolean shouldBackoff(Throwable t) {
					return t instanceof IOException;
				}
			}).setUserAgent("Smarti/0.0 Rocket.Chat-Endpoint/0.1");
}
