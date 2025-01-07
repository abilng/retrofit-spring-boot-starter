package in.abilng.springboot.retrofit.interceptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;

class AuthorizationInterceptorTest {

    private MockWebServer mockWebServer = new MockWebServer();

    private OkHttpClient client;

    private Retrofit retrofit;
    private MockHttpServletRequest mockRequestInContext;

    private interface TestApi {
        @GET("/test")
        Call<String> test();

        @GET("/test")
        @Headers({"Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="})
        Call<String> testWithAuth();
    }

    @BeforeEach
    public void init() {
        client =
                new OkHttpClient.Builder().addInterceptor(AuthorizationInterceptor.getInstance()).build();

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(mockWebServer.url("/"))
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .client(client)
                        .build();

        mockRequestInContext = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequestInContext));
    }

    @AfterEach
    public void teardown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testInterceptWhenRequestContextHasHeader() throws Exception {
        mockRequestInContext.addHeader("authorization", "Bearer token");
        mockRequestInContext.addHeader("x-user-id", "uuid");
        mockRequestInContext.addHeader("x-some-header", "value");
        mockWebServer.enqueue(new MockResponse().setBody("Hello"));

        retrofit.create(TestApi.class).test().execute();

        var request = mockWebServer.takeRequest();

        assertThat(request.getHeader("authorization"), is("Bearer token"));
        assertThat(request.getHeader("x-user-id"), is("uuid"));
        assertThat(request.getHeader("x-some-header"), is(nullValue()));
    }

    @Test
    public void testInterceptWhenRequestAlreadyHasHeader() throws Exception {
        mockRequestInContext.addHeader("authorization", "Bearer token");
        mockWebServer.enqueue(new MockResponse().setBody("Hello"));

        retrofit.create(TestApi.class).testWithAuth().execute();

        var request = mockWebServer.takeRequest();

        assertThat(request.getHeader("authorization"), is("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="));
    }

    @Test
    public void testInterceptNoContext() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        mockWebServer.enqueue(new MockResponse().setBody("Hello"));

        retrofit.create(TestApi.class).test().execute();

        var request = mockWebServer.takeRequest();

        assertThat(request.getHeader("authorization"), is(nullValue()));
    }
}
