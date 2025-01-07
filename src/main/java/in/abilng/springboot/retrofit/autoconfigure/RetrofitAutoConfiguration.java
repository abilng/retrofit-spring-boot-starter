package in.abilng.springboot.retrofit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abilng.springboot.retrofit.config.KeyCloakProperties;
import in.abilng.springboot.retrofit.config.RetroFitProperties;
import in.abilng.springboot.retrofit.convertor.QualifiedTypeConverterFactory;
import in.abilng.springboot.retrofit.core.RetrofitClientsRegistrar;
import in.abilng.springboot.retrofit.interceptor.ServiceAccountInterceptor;
import in.abilng.springboot.retrofit.utils.ObservationUtils;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb3.JaxbConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * The Retrofit autoconfiguration.
 */
@AutoConfiguration
@AutoConfigureAfter(JacksonAutoConfiguration.class)
@Import(RetrofitClientsRegistrar.class)
@EnableConfigurationProperties({KeyCloakProperties.class, RetrofitAutoConfiguration.class})
public class RetrofitAutoConfiguration {

    /**
     * Bean Names.
     */
    public static class BeanNames {

        /**
         * The constant JSON_CONVERTER_FACTORY.
         */
        public static final String JSON_CONVERTER_FACTORY = "jsonConverterFactory";

        /**
         * The constant XML_CONVERTER_FACTORY.
         */
        public static final String XML_CONVERTER_FACTORY = "xmlConverterFactory";

        /**
         * The constant SCALAR_CONVERTER_FACTORY.
         */
        public static final String SCALAR_CONVERTER_FACTORY = "scalarConverterFactory";
    }

    /**
     * Retrofit builder.
     *
     * @param jsonConverterFactory   the json converter factory
     * @param xmlConverterFactory    the xml converter factory
     * @param allConvertersFactories the all converters factories
     * @return the retrofit . builder
     */
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public Retrofit.Builder retrofitBuilder(
            @Qualifier(BeanNames.JSON_CONVERTER_FACTORY) Optional<Converter.Factory> jsonConverterFactory,
            @Qualifier(BeanNames.XML_CONVERTER_FACTORY) Optional<Converter.Factory> xmlConverterFactory,
            List<Converter.Factory> allConvertersFactories) {

        List<Converter.Factory> converterFactories = new ArrayList<>(allConvertersFactories);

        if (jsonConverterFactory.isPresent() && xmlConverterFactory.isPresent()) {
            // Both json & xml Factory are present then add QualifiedTypeConverterFactory
            converterFactories.add(
                    new QualifiedTypeConverterFactory(jsonConverterFactory.get(), xmlConverterFactory.get()));
            // then remove direct one
            converterFactories.remove(jsonConverterFactory.get());
            converterFactories.remove(xmlConverterFactory.get());
        }
        Retrofit.Builder builder = new Retrofit.Builder().validateEagerly(true);
        converterFactories.forEach(builder::addConverterFactory);
        return builder;
    }

    /**
     * Okhttp builder.
     *
     * @param interceptors the interceptors
     * @return the ok http client . builder
     */
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public OkHttpClient.Builder okhttpBuilder(List<Interceptor> interceptors) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        interceptors.forEach(builder::addInterceptor);
        return builder;
    }

    /**
     * The okhttp3 Interceptor configurations.
     */
    @Configuration
    @ConditionalOnClass(name = "okhttp3.Interceptor")
    public static class InterceptorConfiguration {

        /**
         * Logging interceptor.
         *
         * @param environment the environment
         * @return the interceptor
         */
        @Bean
        @ConditionalOnClass(name = "okhttp3.logging.HttpLoggingInterceptor")
        @ConditionalOnProperty(
                prefix = RetroFitProperties.PROPERTY_PREFIX,
                name = "log.enabled",
                havingValue = "true")
        public Interceptor loggingInterceptor(Environment environment) {
            HttpLoggingInterceptor.Level level;

            try {
                String levelProp = environment.getProperty("retrofit.log.level", "");
                level = HttpLoggingInterceptor.Level.valueOf(levelProp.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                level = HttpLoggingInterceptor.Level.NONE;
            }
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(level);
            return interceptor;
        }

        /**
         * Observation interceptor.
         *
         * @param observationRegistry the observation registry
         * @return the interceptor
         */
        @Bean
        @ConditionalOnClass(
                name = {
                    "io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor",
                    "io.micrometer.observation.ObservationRegistry"
                })
        public Interceptor observationInterceptor(
                ObjectProvider<ObservationRegistry> observationRegistry) {
            return OkHttpObservationInterceptor.builder(
                            observationRegistry.getIfAvailable(), "http.client.request")
                    .uriMapper(ObservationUtils::getUri)
                    .build();
        }

        /**
         * Service account interceptor.
         *
         * @param properties the KeyCloakProperties properties
         * @return the interceptor
         */
        @Bean
        @ConditionalOnProperty(
                prefix = KeyCloakProperties.PROPERTY_PREFIX,
                name = "enabled",
                havingValue = "true")
        public Interceptor serviceAccountInterceptor(KeyCloakProperties properties) {
            return new ServiceAccountInterceptor(properties);
        }
    }

    /**
     * The Jackson converter factory configuration.
     */
    @Configuration
    @ConditionalOnClass(name = "retrofit2.converter.jackson.JacksonConverterFactory")
    public static class JacksonConverterFactoryConfiguration {

        /**
         * Jackson Json converter factory.
         *
         * @param jsonObjectMapper the json object mapper
         * @return the converter . factory
         */
        @Bean(name = BeanNames.JSON_CONVERTER_FACTORY)
        @ConditionalOnMissingBean(name = BeanNames.JSON_CONVERTER_FACTORY)
        public Converter.Factory jsonConverterFactory(ObjectMapper jsonObjectMapper) {
            return JacksonConverterFactory.create(jsonObjectMapper);
        }

        /**
         * Jackson XML converter factory.
         *
         * @param xmlObjectMapper the xml object mapper
         * @return the converter . factory
         */
        @Bean(name = BeanNames.XML_CONVERTER_FACTORY)
        @ConditionalOnMissingBean(name = BeanNames.XML_CONVERTER_FACTORY)
        @ConditionalOnMissingClass("retrofit2.converter.jaxb3.JaxbConverterFactory")
        public Converter.Factory xmlConverterFactory(ObjectMapper xmlObjectMapper) {
            return JacksonConverterFactory.create(xmlObjectMapper);
        }
    }

    /**
     * The Jaxb converter factory configuration.
     */
    @Configuration
    @ConditionalOnClass(name = "retrofit2.converter.jaxb3.JaxbConverterFactory")
    public static class JaxbConverterFactoryConfiguration {
        /**
         * Jaxb Xml converter factory.
         *
         * @return the converter factory
         */
        @Bean(name = BeanNames.XML_CONVERTER_FACTORY)
        @ConditionalOnMissingBean(name = BeanNames.XML_CONVERTER_FACTORY)
        public Converter.Factory xmlConverterFactory() {
            return JaxbConverterFactory.create();
        }
    }

    /**
     * The Gson converter factory configuration.
     */
    @Configuration
    @ConditionalOnClass(name = "retrofit2.converter.gson.GsonConverterFactory")
    @ConditionalOnMissingClass("retrofit2.converter.jackson.JacksonConverterFactory")
    public static class GsonConverterFactoryConfiguration {
        /**
         * Gson Json converter factory.
         *
         * @return the converter factory
         */
        @Bean(name = BeanNames.JSON_CONVERTER_FACTORY)
        @ConditionalOnMissingBean(name = BeanNames.JSON_CONVERTER_FACTORY)
        public Converter.Factory jsonConverterFactory() {
            return GsonConverterFactory.create();
        }
    }

    /**
     * The Scalar factory configuration.
     */
    @Configuration
    @ConditionalOnClass(name = "retrofit2.converter.scalars.ScalarsConverterFactory")
    public static class ScalarFactoryConfiguration {
        /**
         * Scalar converter factory.
         *
         * @return the converter factory
         */
        @Bean(name = BeanNames.SCALAR_CONVERTER_FACTORY)
        @ConditionalOnMissingBean(name = BeanNames.SCALAR_CONVERTER_FACTORY)
        public Converter.Factory scalarConverterFactory() {
            return ScalarsConverterFactory.create();
        }
    }
}
