package in.abilng.springboot.retrofit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

/**
 * Annotates an interface as Retrofit service.
 * <br/>
 * Use this annotation to qualify a Retrofit annotated interface for auto-detection and automatic
 * instantiation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Retrofit {

    /**
     * The name of the service with optional protocol prefix. Synonym for {@link #name()
     * name}*. A name must be specified for all clients, whether an url is provided.
     * Can be specified as property key, eg: ${propertyKey}.
     *
     * @return the string
     */
    @AliasFor("name")
    String value() default "";

    /**
     * The service id with optional protocol prefix. Synonym for {@link #value() value}.
     *
     * @return the string
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Sets the <code>@Qualifier</code> value for the client.
     *
     * @return the string
     */
    String qualifier() default "";
}
