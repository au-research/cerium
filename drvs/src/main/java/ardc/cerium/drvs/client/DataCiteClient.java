package ardc.cerium.drvs.client;

import ardc.cerium.drvs.exception.DOINotFoundException;
import ardc.cerium.drvs.exception.DataCiteClientConfigurationException;
import ardc.cerium.drvs.exception.DataCiteClientException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

public class DataCiteClient {

	private final WebClient webClient;

	private final int connectionTimeoutMillies = 5000; // default is 30000

	private final int connectionReadWriteTimeoutSeconds = 10; // default is 100

	private final String dataCiteUrl;

	Logger logger = LoggerFactory.getLogger(DataCiteClient.class);

	/**
	 * @param dataCiteUrl the DataCite API url to harvest metadata from
	 * @throws DataCiteClientConfigurationException if DataCite's API url is not set
	 */
	public DataCiteClient(String dataCiteUrl) throws DataCiteClientConfigurationException {

		if (dataCiteUrl.trim().isEmpty()) {
			throw new DataCiteClientConfigurationException(
					DataCiteClientConfigurationException.Configuration.server_url);
		}
		this.dataCiteUrl = dataCiteUrl;

		// add custom timeout
		TcpClient tcpClient = TcpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillies)
				.doOnConnected(connection -> connection
						.addHandlerLast(new ReadTimeoutHandler(connectionReadWriteTimeoutSeconds))
						.addHandlerLast(new WriteTimeoutHandler(connectionReadWriteTimeoutSeconds)));

		this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
				.baseUrl(dataCiteUrl).build();
	}

	/**
	 * Get the latest registration metadata for a given IGSN record
	 * @param identifier the IGSN value
	 * @return the XML registration metadata as String
	 * @throws DataCiteClientException if response code is not 200
	 */
	public String getDOIMetadata(String identifier) throws Exception {
		logger.debug(String.format("Getting DataCite metadata for %s", identifier));
		String doiUrl = "://doi.org/";
		if(identifier.contains(doiUrl))
		{
			identifier = identifier.substring(identifier.indexOf(doiUrl) + doiUrl.length());
		}
		return doGetRequest(identifier);
	}

	/**
	 * executes a GET request and returns the body content
	 * @param service_url the url of the request
	 * @return the body content as String
	 * @throws Exception if the response code is not 200
	 */
	@Nullable
	private String doGetRequest(String service_url) throws DataCiteClientException, DOINotFoundException
		{
			try {
				ClientResponse response = this.webClient.get().uri(service_url)
						.header("Accept", "application/vnd.datacite.datacite+xml").exchange().block();
				if (response != null) {
					String content = response.bodyToMono(String.class).block();
					response.releaseBody();
					if (response.rawStatusCode() == 200) {
						return content;
					} else if (content != null) {
						throw new DOINotFoundException(dataCiteUrl, service_url, content);
					} else {
						throw new DataCiteClientException(dataCiteUrl, service_url,
								String.format("Empty response body received response code:%s", response.rawStatusCode()));
					}
				}else{
					throw new DataCiteClientException(dataCiteUrl, service_url, "No response received");
				}
			} catch (Exception e) {
				throw e;
			}
		}

}
