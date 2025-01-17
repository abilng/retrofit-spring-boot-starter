package in.abilng.springboot.retrofit.convertor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.abilng.springboot.retrofit.annotation.Json;
import in.abilng.springboot.retrofit.annotation.Xml;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb3.JaxbConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

class QualifiedTypeConverterFactoryTest {

    private final MockWebServer mockWebServer = new MockWebServer();

    private Retrofit retrofit;

    private interface TestApi {

        @Xml
        @POST("/test-xml")
        Call<XmlResponse> testXml(@Body @Xml XmlRequest request);

        @POST("/test-json")
        Call<JsonResponse> testJsonWithDefault(@Body JsonRequest request);

        @Json
        @POST("/test-json")
        Call<JsonResponse> testJson(@Body @Json JsonRequest request);
    }

    @Data
    private static class JsonResponse {

        @JsonProperty("name")
        public String name;
    }

    @Data
    @AllArgsConstructor
    private static class JsonRequest {

        @JsonProperty("id")
        public Integer id;
    }

    @Data
    @XmlRootElement(name = "PayLoad")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class XmlResponse {

        @XmlElement(name = "Name")
        public String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "Request-PayLoad")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class XmlRequest {

        @XmlElement(name = "ID")
        public Integer id;
    }

    @BeforeEach
    public void init() throws IOException {
        mockWebServer.start();
        OkHttpClient client = new OkHttpClient.Builder().build();

        QualifiedTypeConverterFactory converterFactory =
                new QualifiedTypeConverterFactory(
                        JacksonConverterFactory.create(), JaxbConverterFactory.create());

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(mockWebServer.url("/"))
                        .addConverterFactory(converterFactory)
                        .client(client)
                        .build();
    }

    @AfterEach
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testXml() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("<PayLoad><Name>Sample</Name></PayLoad>"));
        XmlResponse response =
                retrofit.create(TestApi.class).testXml(new XmlRequest(123)).execute().body();
        var request = mockWebServer.takeRequest();
        assertThat(
                request.getBody().readUtf8Line(),
                is("<?xml version=\"1.0\" ?><Request-PayLoad><ID>123</ID></Request-PayLoad>"));
        assertThat(response, notNullValue());
        assertThat(response.getName(), is("Sample"));
    }

    @Test
    public void testJson() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"name\": \"Jason\"}"));
        JsonResponse response =
                retrofit.create(TestApi.class).testJson(new JsonRequest(123)).execute().body();
        var request = mockWebServer.takeRequest();
        assertThat(request.getBody().readUtf8Line(), is("{\"id\":123}"));
        assertThat(response, notNullValue());
        assertThat(response.getName(), is("Jason"));
    }

    @Test
    public void testDefaultJson() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"name\": \"Jason\"}"));
        JsonResponse response =
                retrofit.create(TestApi.class).testJsonWithDefault(new JsonRequest(123)).execute().body();
        var request = mockWebServer.takeRequest();
        assertThat(request.getBody().readUtf8Line(), is("{\"id\":123}"));
        assertThat(response, notNullValue());
        assertThat(response.getName(), is("Jason"));
    }
}
