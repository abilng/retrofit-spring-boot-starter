package in.abilng.springboot.retrofit.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The Retro fit properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = RetroFitProperties.PROPERTY_PREFIX)
public class RetroFitProperties {

    /**
     *  PROPERTY_PREFIX.
     */
    public static final String PROPERTY_PREFIX = "retrofit";

    /**
     * Log properties.
     */
    private Log log = new Log();

    /**
     * Service properties.
     */
    private Map<String, ServiceProperties> services = new HashMap<>();

    /**
     * The Service properties.
     */
    @Data
    public static class ServiceProperties {
        /**
         * The base url of Service.
         */
        private String baseUrl;

        /**
         * Whether to propagate OAuth Header in request.
         */
        private boolean propagateAuthHeader = false;

        /**
         * Connection Properties of this service.
         */
        private ServiceConnectionProperties connection = new ServiceConnectionProperties();

        /**
         * Retry Properties of this service.
         */
        private RetryProperties retry = new RetryProperties();

        /**
         * Retry Properties of this service.
         */
        private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
    }

    /**
     * The Circuit breaker properties.
     */
    @Data
    public static class CircuitBreakerProperties {

        /**
         * To enable CircuitBreaker.
         */
        private Boolean enabled = true;

        /**
         * Configures The of the sliding window which is used to record the outcome of calls
         * when the CircuitBreaker is closed.
         * Sliding window can either be count-based or time-based.
         * <br/>
         * If the sliding window is COUNT_BASED, the last slidingWindowSize calls are recorded and aggregated.
         * If the sliding window is TIME_BASED, the calls of the last slidingWindowSize seconds recorded and aggregated.
         */
        private CircuitBreakerConfig.SlidingWindowType slidingWindowType =
                CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_TYPE;

        /**
         * Configures the size of the sliding window which is used to
         * record the outcome of calls when the CircuitBreaker is closed.
         */
        private int slidingWindowSize = CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;

        /**
         * Configures the minimum number of calls which are required (per sliding window period)
         * before the CircuitBreaker can calculate the error rate or slow call rate.
         */
        private int minimumNumberOfCalls = CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;

        /**
         * Configures the minimum number of calls which are required (per sliding window period)
         * before the CircuitBreaker can calculate the error rate or slow call rate.
         */
        private Duration waitDurationInOpenState =
                Duration.ofSeconds(CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE);

        /**
         * Configures the duration threshold above which calls are considered as slow
         * and increase the rate of slow calls.
         */
        private Duration slowCallDurationThreshold =
                Duration.ofSeconds(CircuitBreakerConfig.DEFAULT_SLOW_CALL_DURATION_THRESHOLD);

        /**
         * Configures the failure rate threshold in percentage.
         * <br/>
         * When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open
         * and starts short-circuiting calls.
         */
        private float failureRateThreshold = CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;

        /**
         * Configures a threshold in percentage. The CircuitBreaker considers a call as slow when
         * the call duration is greater than slowCallDurationThreshold
         * <br/>
         * When the percentage of slow calls is equal or greater the threshold,
         * the CircuitBreaker transitions to open and starts short-circuiting calls.
         */
        private float slowCallRateThreshold = CircuitBreakerConfig.DEFAULT_SLOW_CALL_RATE_THRESHOLD;

        /**
         * Configures the number of permitted calls when the CircuitBreaker is half open.
         */
        private int permittedNumberOfCallsInHalfOpenState =
                CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;

        /**
         * A list of exceptions that are recorded as a failure and thus increase the failure rate.
         */
        private List<Class<? extends Throwable>> recordExceptions =
                Arrays.asList(IOException.class, TimeoutException.class);

        /**
         * A list of exceptions that are ignored and neither count as a failure nor success.
         */
        private List<Class<? extends Throwable>> ignoreExceptions = Collections.emptyList();
    }

    /**
     * The Service connection properties.
     */
    @Data
    public static class ServiceConnectionProperties {
        /**
         * Read Timeout. Default Value 10s (10000ms).
         */
        private Duration readTimeout = Duration.ofMillis(10000L);

        /**
         * Write Timeout. Default Value 10s (10000ms).
         */
        private Duration writeTimeout = Duration.ofMillis(10000L);

        /**
         * Connection Timeout. Default Value 10s (10000ms).
         */
        private Duration connectTimeout = Duration.ofMillis(10000L);
    }

    /**
     * The Log properties.
     */
    @Data
    public static class Log {
        /**
         * To enable http logger.
         * Note: Need HttpLoggingInterceptor.
         */
        private Boolean enabled = false;

        /**
         * Log Level {@link okhttp3.logging.HttpLoggingInterceptor.Level}.
         */
        private String level = "NONE";
    }

    /**
     * The Retry properties.
     */
    @Data
    public static class RetryProperties {
        /**
         * To enable Retry.
         */
        private Boolean enabled = true;

        /**
         * retryable Exceptions.
         */
        private List<Class<? extends Throwable>> retryExceptions =
                Arrays.asList(IOException.class, TimeoutException.class);

        /**
         * ignore Exceptions.
         */
        private List<Class<? extends Throwable>> ignoreExceptions = Collections.emptyList();

        /**
         * Max Retry Attempts.
         */
        private Integer maxAttempts = 3;

        /**
         * Retry on 5XX response code.
         */
        private Boolean retryOn5xx = true;

        /**
         * Wait Duration.
         */
        private Duration waitDuration = Duration.ofMillis(100);
    }
}
