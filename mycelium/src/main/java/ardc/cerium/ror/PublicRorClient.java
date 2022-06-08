package ardc.cerium.ror;

import ardc.cerium.ror.schema.ror.json.RorRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class PublicRorClient {

    public static String PUBLIC_ROR_API_V1 = "https://api.ror.org/organizations";

    private String baseUrl;

    private OkHttpClient client;

    public PublicRorClient() {
        this.baseUrl = PUBLIC_ROR_API_V1;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public PublicRorClient(OkHttpClient client) {
        this.baseUrl = PUBLIC_ROR_API_V1;
        this.client = client;
    }

    public RorRecord resolve(String ror) {
        try {
            String json = resolveString(ror, MetadataContentType.ROR_JSON);
            if(isJSONValid(json)) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, RorRecord.class);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String resolveString(String ror, MetadataContentType contentType) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        String url = String.format("%s/%s", this.baseUrl, ror);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", contentType.getContentType())
                .build();
        Response response = client.newCall(request).execute();
        String data = response.body().string();
        return data;
    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
