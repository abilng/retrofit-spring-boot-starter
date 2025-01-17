package in.abilng.springboot.retrofit.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.*;

class ObservationUtilsTest {

    private Retrofit retrofit;
    private OkHttpClient client;

    private interface TestApi {

        @POST("/test-post")
        Call<String> post();

        @GET("/test-get")
        Call<String> get();

        @PATCH("/test-patch")
        Call<String> patch();

        @DELETE("/test-delete")
        Call<String> delete();

        @DELETE("/test-put")
        Call<String> put();
    }

    @BeforeEach
    public void init() throws IOException {

        client = new OkHttpClient.Builder().build();

        retrofit =
                new Retrofit.Builder()
                        .baseUrl("https://example.com")
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .client(client)
                        .build();
    }

    @Test
    public void testGetUri() throws Exception {
        Request request = retrofit.create(TestApi.class).delete().request();
        assertThat(ObservationUtils.getUri(request), is("/test-delete"));

        request = retrofit.create(TestApi.class).get().request();
        assertThat(ObservationUtils.getUri(request), is("/test-get"));

        request = retrofit.create(TestApi.class).patch().request();
        assertThat(ObservationUtils.getUri(request), is("/test-patch"));

        request = retrofit.create(TestApi.class).post().request();
        assertThat(ObservationUtils.getUri(request), is("/test-post"));

        request = retrofit.create(TestApi.class).put().request();
        assertThat(ObservationUtils.getUri(request), is("/test-put"));

        request = new Request.Builder().url("https://example.com/test-url").get().build();
        assertThat(ObservationUtils.getUri(request), is("unknown"));
    }
}
