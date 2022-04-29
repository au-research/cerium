package ardc.cerium.orcid;

import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class PublicOrcidClient {

    public static String PUBLIC_ORCID_API_V20 = "https://pub.orcid.org/v3.0";
    public static String PUBLIC_ORCID_API_V21 = "https://pub.orcid.org/v2.1";
    public static String PUBLIC_ORCID_API_V30 = "https://pub.orcid.org/v2.1";

    private String baseUrl;

    private OkHttpClient client;

    public PublicOrcidClient() {
        // v2.1 is the default
        this.baseUrl = PUBLIC_ORCID_API_V21;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public PublicOrcidClient(OkHttpClient client) {
        this.baseUrl = PUBLIC_ORCID_API_V21;
        this.client = client;
    }

    public OrcidRecord resolve(String orcid) {
        try {
            String json = resolveString(orcid, MetadataContentType.ORCID_JSON);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, OrcidRecord.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String resolveString(String orcid, MetadataContentType contentType) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        String url = String.format("%s/%s", this.baseUrl, orcid);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", contentType.getContentType())
                .build();
        Response response = client.newCall(request).execute();
        String data = response.body().string();
        return data;
    }
}
