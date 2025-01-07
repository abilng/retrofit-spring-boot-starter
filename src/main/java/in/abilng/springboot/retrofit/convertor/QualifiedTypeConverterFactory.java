package in.abilng.springboot.retrofit.convertor;

import in.abilng.springboot.retrofit.annotation.Json;
import in.abilng.springboot.retrofit.annotation.Xml;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * The Qualified type converter factory to delegates to either the jsonFactory or xmlFactory based on annotation.
 * <br/>
 * Use {@link Json} and {@link Xml} annotation to declare which serialization format each endpoint should use
 */
public class QualifiedTypeConverterFactory extends Converter.Factory {
    private final Converter.Factory jsonFactory;
    private final Converter.Factory xmlFactory;

    /**
     * Instantiates a new Qualified type converter factory.
     *
     * @param jsonFactory the json factory
     * @param xmlFactory  the xml factory
     */
    public QualifiedTypeConverterFactory(
            Converter.Factory jsonFactory, Converter.Factory xmlFactory) {
        this.jsonFactory = jsonFactory;
        this.xmlFactory = xmlFactory;
    }

    @Override
    public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
            @NotNull Type type, @NotNull Annotation[] annotations, @NotNull Retrofit retrofit) {

        for (Annotation annotation : annotations) {
            if (annotation instanceof Json) {
                return jsonFactory.responseBodyConverter(type, annotations, retrofit);
            }
            if (annotation instanceof Xml) {
                return xmlFactory.responseBodyConverter(type, annotations, retrofit);
            }
        }
        // default is Json
        return jsonFactory.responseBodyConverter(type, annotations, retrofit);
    }

    @Override
    public @Nullable Converter<?, RequestBody> requestBodyConverter(
            @NotNull Type type,
            @NotNull Annotation[] parameterAnnotations,
            @NotNull Annotation[] methodAnnotations,
            @NotNull Retrofit retrofit) {

        for (Annotation annotation : parameterAnnotations) {
            if (annotation instanceof Json) {
                return jsonFactory.requestBodyConverter(
                        type, parameterAnnotations, methodAnnotations, retrofit);
            }
            if (annotation instanceof Xml) {
                return xmlFactory.requestBodyConverter(
                        type, parameterAnnotations, methodAnnotations, retrofit);
            }
        }
        // default is Json
        return jsonFactory.requestBodyConverter(
                type, parameterAnnotations, methodAnnotations, retrofit);
    }
}
