package in.abilng.springboot.retrofit.core;

import in.abilng.springboot.retrofit.config.RetroFitProperties;
import in.abilng.springboot.retrofit.interceptor.AuthorizationInterceptor;
import in.abilng.springboot.retrofit.resilience4j.CircuitBreakerCallAdapter;
import in.abilng.springboot.retrofit.resilience4j.RetryCallAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * The Retrofit client factory bean.
 */
@Setter
@Getter
@Slf4j
public class RetrofitClientFactoryBean implements FactoryBean<Object>, ApplicationContextAware {

    private Class<?> type;

    private String name;

    private RetroFitProperties.ServiceProperties properties;

    private ApplicationContext applicationContext;

    @Override
    public Object getObject() throws Exception {

        Assert.hasText(this.name, "Name must be set.");

        Assert.notNull(
                this.properties, "retrofit.services.%s.* properties are missing.".formatted(this.name));

        Retrofit.Builder builder = retrofitBuilder();
        OkHttpClient.Builder clientBuilder = clientBuilder();

        if (clientBuilder != null) {
            if (properties.isPropagateAuthHeader()) {
                clientBuilder.addInterceptor(AuthorizationInterceptor.getInstance());
            }
            builder.client(clientBuilder.build());
        }

        Retrofit retrofit = buildAndSave(builder);
        return retrofit.create(this.type);
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    /**
     * Retrofit builder.
     *
     * @return the retrofit builder
     */
    protected Retrofit.Builder retrofitBuilder() {
        final String baseUrl =
                Objects.requireNonNull(
                        properties.getBaseUrl(),
                        "retrofit.services.%s.base-url must be non null".formatted(this.name));

        Retrofit.Builder builder =
                getBean(Retrofit.Builder.class).baseUrl(baseUrl).validateEagerly(true);

        getRetry().ifPresent(retry -> builder.addCallAdapterFactory(RetryCallAdapter.of(retry)));

        getCircuitBreaker()
                .ifPresent(
                        circuitBreaker ->
                                builder.addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker)));

        return builder;
    }

    private Optional<CircuitBreaker> getCircuitBreaker() {
        final RetroFitProperties.CircuitBreakerProperties cbProperties =
                this.properties.getCircuitBreaker();

        if (cbProperties.getEnabled()) {
            final Class<? extends Throwable>[] recordExceptions =
                    toClassArray(cbProperties.getRecordExceptions());

            final Class<? extends Throwable>[] ignoreExceptions =
                    toClassArray(cbProperties.getIgnoreExceptions());

            final CircuitBreakerConfig circuitBreakerConfig =
                    CircuitBreakerConfig.custom()
                            .failureRateThreshold(cbProperties.getFailureRateThreshold())
                            .slowCallRateThreshold(cbProperties.getSlowCallRateThreshold())
                            .waitDurationInOpenState(cbProperties.getWaitDurationInOpenState())
                            .slowCallDurationThreshold(cbProperties.getSlowCallDurationThreshold())
                            .permittedNumberOfCallsInHalfOpenState(
                                    cbProperties.getPermittedNumberOfCallsInHalfOpenState())
                            .minimumNumberOfCalls(cbProperties.getMinimumNumberOfCalls())
                            .slidingWindowType(cbProperties.getSlidingWindowType())
                            .slidingWindowSize(cbProperties.getSlidingWindowSize())
                            .recordExceptions(recordExceptions)
                            .ignoreExceptions(ignoreExceptions)
                            .build();

            final CircuitBreakerRegistry registry =
                    getOptionalBean(CircuitBreakerRegistry.class)
                            .orElseGet(CircuitBreakerRegistry::ofDefaults);

            final CircuitBreaker circuitBreaker =
                    registry.circuitBreaker(this.name, circuitBreakerConfig);
            return Optional.of(circuitBreaker);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets resilience 4j retry.
     *
     * @return the retry
     */
    protected Optional<Retry> getRetry() {
        final RetroFitProperties.RetryProperties retryProperties = this.properties.getRetry();
        if (retryProperties.getEnabled()) {
            final Integer maxAttempts = retryProperties.getMaxAttempts();
            final Duration waitDuration = retryProperties.getWaitDuration();
            final Predicate<Response<?>> responsePredicate =
                    retryProperties.getRetryOn5xx() ? response -> response.code() >= 500 : response -> false;

            final Class<? extends Throwable>[] retryExceptions =
                    toClassArray(retryProperties.getRetryExceptions());

            final Class<? extends Throwable>[] ignoreExceptions =
                    toClassArray(retryProperties.getIgnoreExceptions());

            final RetryConfig retryConfig =
                    RetryConfig.<Response<?>>custom()
                            .maxAttempts(maxAttempts)
                            .waitDuration(waitDuration)
                            .retryOnResult(responsePredicate)
                            .retryExceptions(retryExceptions)
                            .ignoreExceptions(ignoreExceptions)
                            .failAfterMaxAttempts(true)
                            .build();
            final RetryRegistry registry =
                    getOptionalBean(RetryRegistry.class).orElseGet(RetryRegistry::ofDefaults);
            final Retry retry = registry.retry(this.name, retryConfig);
            return Optional.of(retry);
        } else {
            return Optional.empty();
        }
    }

    /**
     * ok http client builder.
     *
     * @return the ok http client builder
     */
    protected OkHttpClient.Builder clientBuilder() {
        return getOptionalBean(OkHttpClient.Builder.class)
                .orElseGet(OkHttpClient.Builder::new)
                .readTimeout(properties.getConnection().getReadTimeout())
                .writeTimeout(properties.getConnection().getWriteTimeout())
                .connectTimeout(properties.getConnection().getConnectTimeout())
                .retryOnConnectionFailure(true);
    }

    /**
     * Build and save retrofit.
     *
     * @param builder the builder
     * @return the retrofit
     */
    protected Retrofit buildAndSave(Retrofit.Builder builder) {
        Retrofit retrofit = builder.build();

        ConfigurableListableBeanFactory beanFactory =
                ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        // add retrofit to this.names context as a bean;
        beanFactory.registerSingleton(this.name, retrofit);
        return retrofit;
    }

    /**
     * Get bean of type T.
     *
     * @param <T>  The parameter
     * @param type class of T
     * @return the bean
     *
     * @throws IllegalStateException if bean not found
     */
    protected <T> T getBean(Class<T> type) {
        return getOptionalBean(type)
                .orElseThrow(() -> new IllegalStateException("No bean found of type " + type));
    }

    /**
     * Gets bean of type T if present, else return {@link Optional#empty}.
     *
     * @param <T>  type
     * @param type class of T
     * @return the Optional of bean.
     */
    protected <T> Optional<T> getOptionalBean(Class<T> type) {
        try {
            return Optional.of(applicationContext.getBean(this.name, type));
        } catch (NoSuchBeanDefinitionException e) {
            if (log.isDebugEnabled()) {
                log.debug("No bean found of type " + type + " for " + this.name + "fallback to default");
            }
        }
        try {
            return Optional.of(applicationContext.getBean(type));
        } catch (NoSuchBeanDefinitionException e) {
            if (log.isDebugEnabled()) {
                log.debug("No bean found of type " + type);
            }
            return Optional.empty();
        }
    }

    @NotNull
    private static Class<? extends Throwable>[] toClassArray(
            List<Class<? extends Throwable>> properties) {
        @SuppressWarnings("unchecked")
        final Class<? extends Throwable>[] array =
                properties.stream().filter(Objects::nonNull).toArray(Class[]::new);
        return array;
    }
}
