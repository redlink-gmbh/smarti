package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"json_file_url"
})
/**
 * @author Ruediger Kurz (ruediger.kurz@deutschebahn.com)
 * @since 03.08.2017
 */
public class RocketFileConfig extends A_SourceConfiguration {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RocketFileConfig [jsonFileURL=").append(jsonFileURL).append("]");
		return builder.toString();
	}

	@JsonProperty("json_file_url")
	private String jsonFileURL;

	public String getJsonFileURL() {
		return jsonFileURL;
	}

	public void setJsonFileURL(String jsonFileURL) {
		this.jsonFileURL = jsonFileURL;
	}
}
