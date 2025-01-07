package in.abilng.springboot.retrofit.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpStatus;

/**
 * The client for interacting with Keycloak service. To be used for service auth.
 */
@Slf4j
public class KeyClockClient {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC = "Basic ";
    private static final String TOKEN_LOGIN_URL = "%s/realms/%s/protocol/openid-connect/token";
    private static final int BUFFER_IN_SEC = 30;
    private static final String SPACE = " ";
    private final String baseUrl;
    private final OkHttpClient client;

    /**
     * Instantiates a new Key clock client.
     *
     * @param baseUrl the base url
     */
    public KeyClockClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder().build();
    }

    /**
     * Gets token.
     *
     * @param realm    the realm
     * @param userName the username
     * @param password the password
     * @param scopes   the scopes
     * @return the token
     */
    public Token getToken(String realm, String userName, String password, String... scopes) {

        log.info("Getting token of {} with {} optional scopes", userName, scopes);
        final FormBody.Builder requestBodyBuilder =
                new FormBody.Builder().add("grant_type", "client_credentials");

        Optional.ofNullable(scopes)
                .filter(scopeArray -> scopeArray.length > 0)
                .map(scopeArray -> String.join(SPACE, scopeArray))
                .ifPresent(scopesParam -> requestBodyBuilder.add("scope", scopesParam));

        Request request =
                new Request.Builder()
                        .url(TOKEN_LOGIN_URL.formatted(baseUrl, realm))
                        .addHeader(AUTHORIZATION_HEADER, getBasicAuthenticationHeader(userName, password))
                        .post(requestBodyBuilder.build())
                        .build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            @Cleanup
            final InputStream body =
                    Optional.ofNullable(response.body())
                            .map(ResponseBody::byteStream)
                            .orElse(InputStream.nullInputStream());

            if (response.code() == HttpStatus.OK.value()) {

                final Map<String, String> tokenResponse =
                        objectMapper.readValue(body, new TypeReference<>() {});
                return new Token(
                        tokenResponse.get("access_token"),
                        LocalDateTime.now()
                                .plusSeconds(Integer.parseInt(tokenResponse.get("expires_in")) - BUFFER_IN_SEC));
            } else {
                final String errorBody = new String(body.readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Auth Error:: " + errorBody);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return BASIC
                + Base64.getEncoder().encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The type Token response. Received from getToken call to Keycloak client.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class TokenResponse {
        private String accessToken;
        private Integer expiresIn;
    }
}
