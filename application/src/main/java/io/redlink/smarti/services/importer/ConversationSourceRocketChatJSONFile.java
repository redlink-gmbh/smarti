package io.redlink.smarti.services.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import io.redlink.smarti.webservice.pojo.RocketFileConfig;

public class ConversationSourceRocketChatJSONFile extends A_ConversationSource {

	RocketFileConfig config;

	public ConversationSourceRocketChatJSONFile(RocketFileConfig config) {
		super(E_SourceType.RocketChatJSONFile, config);
		this.config = config;
	}

	@Override
	public String exportConversations() throws Exception {

		return readJsonFromUrl(config.getJsonFileURL());
	}

	public static String readJsonFromUrl(String url) throws IOException {

		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			return sb.toString();
		} finally {
			is.close();
		}
	}
}
