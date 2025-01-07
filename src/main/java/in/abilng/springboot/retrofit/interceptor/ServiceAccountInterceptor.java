package in.abilng.springboot.retrofit.interceptor;

import in.abilng.springboot.retrofit.annotation.ServiceAccount;
import in.abilng.springboot.retrofit.config.KeyCloakProperties;
import in.abilng.springboot.retrofit.keycloak.KeyClockClient;
import in.abilng.springboot.retrofit.keycloak.Token;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Invocation;

/**
 * okHttp Interceptor to add service auth Headers.
 */
public class ServiceAccountInterceptor implements Interceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";
    private final KeyCloakProperties properties;
    private final ConcurrentMap<String, Token> cache;
    private final KeyClockClient keyClockClient;

    /**
     * Instantiates a new Service account interceptor.
     *
     * @param properties the properties
     */
    public ServiceAccountInterceptor(KeyCloakProperties properties) {
        this.properties = properties;
        this.cache = new ConcurrentHashMap<>();
        this.keyClockClient = new KeyClockClient(properties.getBaseUrl());
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final Optional<ServiceAccount> serviceAccountOption =
                Optional.ofNullable(request.tag(Invocation.class))
                        .map(Invocation::method)
                        .map(method -> method.getAnnotation(ServiceAccount.class));

        final Request.Builder newRequestBuilder = request.newBuilder();
        serviceAccountOption.ifPresent(
                serviceAccount -> {
                    var token = getBearerToken(serviceAccount.value(), serviceAccount.scopes());
                    newRequestBuilder.addHeader(AUTHORIZATION_HEADER, token);
                });
        return chain.proceed(newRequestBuilder.build());
    }

    private String getBearerToken(String accountName, String... scopes) {
        final String key = getKey(accountName, scopes);
        cache.computeIfAbsent(key, (ignored) -> getAccessToken(accountName, scopes));
        final Token token = cache.get(key);
        if (token.expireTime().isBefore(LocalDateTime.now())) {
            cache.remove(key, token);
            return getBearerToken(accountName, scopes);
        } else {
            return BEARER + token.accessToken();
        }
    }

    private Token getAccessToken(String accountName, String... scopes) {
        final KeyCloakProperties.ServiceAccountCredentials accountCredentials =
                properties.getCredentials().get(accountName);

        if (accountCredentials == null) {
            throw new IllegalStateException(
                    "Key-clock config for %s is not found".formatted(accountName));
        }
        return keyClockClient.getToken(
                properties.getRealm(),
                accountCredentials.getClientId(),
                accountCredentials.getClientSecret(),
                scopes);
    }

    private String getKey(String accountName, String... scopes) {
        if (scopes == null || scopes.length == 0) {
            return accountName;
        } else {
            return accountName + "#" + String.join("#", scopes);
        }
    }
}
