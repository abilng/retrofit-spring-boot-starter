package in.abilng.springboot.retrofit.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The Key cloak properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = KeyCloakProperties.PROPERTY_PREFIX)
public class KeyCloakProperties {

    /**
     * To enable Key Cloak Service.
     */
    private Boolean enabled = false;

    /**
     * PROPERTY_PREFIX.
     */
    public static final String PROPERTY_PREFIX = "retrofit.keycloak";

    /**
     * The base url key-clock.
     */
    private String baseUrl;

    /**
     * The realm.
     */
    private String realm;

    /**
     * Service account credentials properties.
     */
    private Map<String, ServiceAccountCredentials> credentials = new HashMap<>();

    /**
     * The type Service account credentials.
     */
    @Data
    public static class ServiceAccountCredentials {
        /**
         * The Client id.
         */
        private String clientId;

        /**
         * The Client secret.
         */
        private String clientSecret;
    }
}
