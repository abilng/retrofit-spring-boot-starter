package in.abilng.springboot.retrofit.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * okHttp Interceptor to pass through auth Headers.
 */
public class AuthorizationInterceptor implements Interceptor {
    private static final List<String> PASS_THROUGH_HEADERS = List.of("Authorization");

    @NonNull
    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final Request.Builder newRequestBuilder = request.newBuilder();
        PASS_THROUGH_HEADERS.stream()
                .filter(header -> Optional.ofNullable(request.header(header)).isEmpty())
                .forEach(header -> setHeader(header, newRequestBuilder));
        return chain.proceed(newRequestBuilder.build());
    }

    private static void setHeader(String header, Request.Builder newRequestBuilder) {
        getRequestHeader(header).ifPresent(value -> newRequestBuilder.addHeader(header, value));
    }

    private static Optional<String> getRequestHeader(String key) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            return Optional.ofNullable(request.getHeader(key));
        } else {
            return Optional.empty();
        }
    }

    /**
     * SingletonHolder for AuthorizationInterceptor.
     */
    private static class SingletonHolder {
        /**
         * static Singleton instance.
         */
        public static final AuthorizationInterceptor instance = new AuthorizationInterceptor();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static AuthorizationInterceptor getInstance() {
        return SingletonHolder.instance;
    }
}
