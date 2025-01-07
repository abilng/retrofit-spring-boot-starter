package in.abilng.springboot.retrofit.utils;

import java.lang.annotation.Annotation;
import java.util.Optional;
import okhttp3.Request;
import retrofit2.Invocation;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * The Observation utils.
 */
public class ObservationUtils {

    /**
     * Gets uri in Annotation from request.
     *
     * @param request the request
     * @return the uri
     */
    public static String getUri(Request request) {
        return Optional.<String>empty()
                .or(() -> getAnnotation(request, GET.class).map(GET::value))
                .or(() -> getAnnotation(request, POST.class).map(POST::value))
                .or(() -> getAnnotation(request, PUT.class).map(PUT::value))
                .or(() -> getAnnotation(request, PATCH.class).map(PATCH::value))
                .or(() -> getAnnotation(request, DELETE.class).map(DELETE::value))
                .orElse("none");
    }

    private static <T extends Annotation> Optional<T> getAnnotation(
            Request request, Class<T> annotationClass) {
        return Optional.ofNullable(request.tag(Invocation.class))
                .map(Invocation::method)
                .map(method -> method.getAnnotation(annotationClass));
    }
}
