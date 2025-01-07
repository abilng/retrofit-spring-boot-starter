package in.abilng.springboot.retrofit.resilience4j;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import in.abilng.springboot.retrofit.resilience4j.internal.DelegateCall;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.StopWatch;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Decorates a Retrofit {@link Call} to inform a {@link CircuitBreaker} when an exception is thrown.
 * All exceptions are marked as errors or responses not matching the supplied predicate.  For
 * example:
 * <br/>
 * <code>
 * RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, Response::isSuccessful);
 * </code>
 */
public interface RetrofitCircuitBreaker {

    /**
     * Decorate {@link Call}s allow {@link CircuitBreaker} functionality.
     *
     * @param <T>             Response type of call
     * @param circuitBreaker  {@link CircuitBreaker} to apply
     * @param call            Call to decorate
     * @param responseSuccess determines whether the response should be considered an expected response
     * @return Original Call decorated with CircuitBreaker
     */
    static <T> Call<T> decorateCall(
            final CircuitBreaker circuitBreaker,
            final Call<T> call,
            final Predicate<Response> responseSuccess) {
        return new CircuitBreakingCall<>(call, circuitBreaker, responseSuccess);
    }

    /**
     * The Circuit breaking call.
     *
     * @param <T> The resposne type parameter
     */
    class CircuitBreakingCall<T> extends DelegateCall<T> {

        private final CircuitBreaker circuitBreaker;
        private final Predicate<Response> responseSuccess;

        /**
         * Instantiates a new Circuit breaking call.
         *
         * @param call            the call
         * @param circuitBreaker  the circuit breaker
         * @param responseSuccess the response success
         */
        public CircuitBreakingCall(
                Call<T> call, CircuitBreaker circuitBreaker, Predicate<Response> responseSuccess) {
            super(call);
            this.circuitBreaker = circuitBreaker;
            this.responseSuccess = responseSuccess;
        }

        @Override
        public void enqueue(final Callback<T> callback) {
            try {
                circuitBreaker.acquirePermission();
            } catch (CallNotPermittedException cb) {
                callback.onFailure(delegate, cb);
                return;
            }

            final long start = System.nanoTime();
            delegate.enqueue(
                    new Callback<T>() {
                        @Override
                        public void onResponse(final Call<T> call, final Response<T> response) {
                            if (responseSuccess.test(response)) {
                                circuitBreaker.onResult(System.nanoTime() - start, TimeUnit.NANOSECONDS, response);
                            } else {
                                final Throwable throwable =
                                        new Throwable(
                                                "Response error: HTTP " + response.code() + " - " + response.message());
                                circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, throwable);
                            }
                            callback.onResponse(call, response);
                        }

                        @Override
                        public void onFailure(final Call<T> call, final Throwable t) {
                            if (call.isCanceled()) {
                                circuitBreaker.releasePermission();
                            } else {
                                circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, t);
                            }
                            callback.onFailure(call, t);
                        }
                    });
        }

        @Override
        public Response<T> execute() throws IOException {
            circuitBreaker.acquirePermission();
            final StopWatch stopWatch = StopWatch.start();
            try {
                final Response<T> response = delegate.execute();

                if (responseSuccess.test(response)) {
                    circuitBreaker.onResult(stopWatch.stop().toNanos(), TimeUnit.NANOSECONDS, response);
                } else {
                    final Throwable throwable =
                            new Throwable("Response error: HTTP " + response.code() + " - " + response.message());
                    circuitBreaker.onError(stopWatch.stop().toNanos(), TimeUnit.NANOSECONDS, throwable);
                }

                return response;
            } catch (Exception exception) {
                if (delegate.isCanceled()) {
                    circuitBreaker.releasePermission();
                } else {
                    circuitBreaker.onError(stopWatch.stop().toNanos(), TimeUnit.NANOSECONDS, exception);
                }
                throw exception;
            }
        }

        @Override
        @SuppressWarnings({
            "PMD.ProperCloneImplementation",
            "PMD.CloneMethodReturnTypeMustMatchClassName"
        })
        @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
        public Call<T> clone() {
            return new CircuitBreakingCall<>(delegate.clone(), circuitBreaker, responseSuccess);
        }
    }
}
