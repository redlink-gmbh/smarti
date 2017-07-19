package io.redlink.smarti.webservice;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 18.07.17.
 */
public class RocketChatEndpointProxyTest {

    private LocalTestServer server = new LocalTestServer(null, null);

    @Before
    public void setUp() throws Exception {
        server.start();
        server.register("*", (httpRequest, httpResponse, httpContext) ->
                httpResponse.setEntity(new StringEntity("foobar")));
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void test() throws IOException {

        RocketChatEndpoint rocketChatEndpoint = new RocketChatEndpoint(
            server.getServiceAddress().getHostName(),
            server.getServiceAddress().getPort(),
            "http"
        );

        HttpGet get = new HttpGet("http://testhost.org:8983/test");

        try (CloseableHttpResponse response = rocketChatEndpoint.httpClientBuilder.build().execute(get)) {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            Assert.assertEquals("foobar", responseString);
        }

    }

}
