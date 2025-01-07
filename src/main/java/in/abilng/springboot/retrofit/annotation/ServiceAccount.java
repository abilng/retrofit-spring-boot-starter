package in.abilng.springboot.retrofit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Indexed;

/**
 * The interface Service account.
 * Example Usage: <pre>
 * {@code
 *     @ServiceAccount("service1")
 *     @PUT("leads")
 *     Call<Void> updateLeads(@Body LeadWorkerRequest leadWorkerRequest );
 *
 *     @ServiceAccount("service2", scopes = {"service2:read", "service2:write"})
 *     @GET("NowTrack_API_UAT/getapplications/0{phoneNumber}")
 *     Call<Applications> getApplicationsByPhoneNumber(@Path("phoneNumber") String phoneNumber);
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface ServiceAccount {

    /**
     * accountName.
     *
     * @return the string
     */
    String value();

    /**
     * List of optional scopes to request.
     *
     * @return the string [ ]
     */
    String[] scopes() default {};
}
