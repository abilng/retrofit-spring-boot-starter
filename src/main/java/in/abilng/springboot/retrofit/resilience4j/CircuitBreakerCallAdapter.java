package in.abilng.springboot.retrofit.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Predicate;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Creates a Retrofit {@link CallAdapter.Factory} that decorates a Call to provide integration with
 * a {@link CircuitBreaker}.
 */
public final class CircuitBreakerCallAdapter extends CallAdapter.Factory {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Response> successResponse;

    private CircuitBreakerCallAdapter(
            final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse) {
        this.circuitBreaker = circuitBreaker;
        this.successResponse = successResponse;
    }

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls.
     *
     * @param circuitBreaker circuit breaker to use
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(final CircuitBreaker circuitBreaker) {
        return of(circuitBreaker, Response::isSuccessful);
    }

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls.
     *
     * @param circuitBreaker  circuit breaker to use
     * @param successResponse {@link Predicate} that determines whether the {@link Call} {@link
     *                        Response} should be considered successful
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(
            final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse) {
        return new CircuitBreakerCallAdapter(circuitBreaker, successResponse);
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        @SuppressWarnings("unchecked")
        CallAdapter<Object, Object> nextAdapter =
                (CallAdapter<Object, Object>) retrofit.nextCallAdapter(this, returnType, annotations);

        return new CallAdapter<Object, Object>() {
            @Override
            public Type responseType() {
                return nextAdapter.responseType();
            }

            @Override
            public Object adapt(Call<Object> call) {
                return nextAdapter.adapt(
                        RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, successResponse));
            }
        };
    }
}
