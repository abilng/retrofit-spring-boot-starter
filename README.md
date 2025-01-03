## Retrofit Spring Boot Auto Configuration

Retrofit 2 is just a simple abstraction for http request. The backend of its to perform http
request is mainly depends on `okhttp3`. We also support async http request out of the box.

### Dependency

```xml
<dependency>
    <groupId>in.abilng.spring</groupId>
    <artifactId>retrofit-spring-boot-starter</artifactId>
    <version>${latest_version}</version>
</dependency>
```

or Gradle:

```kotlin
implementation("in.abilng.spring:retrofit-spring-boot-starter:${latest_version}")
```

**Note**: To enable circuit breaker & retry metrics, you need add following along with `retrofit-spring-boot-starter`

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-micrometer</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId> <!-- or any other implementations  of micrometer -->
        </dependency>
    </dependencies>

```
or Gradle:
```kotlin
implmenation("org.springframework.boot:spring-boot-starter-actuator")
implmenation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implmenation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
implmenation("io.micrometer:micrometer-registry-prometheus") // or other implementations of micrometer
```


### Detailed Usage in Springboot Application

All the customize retrofit interface should be annotated with `@Retrofit` annotation,
that would make these interface could be registed our own bean creation process. e.g.:

```java
@Retrofit("open-library")
public interface IOpenLibClient {
    
    @GET("/search.json")
    Call<SearchResult> search(@Query("title") String title,
                              @Query("limit") Integer limit);
    //...

}
```

`@Retrofit` have three properties, the `name` or `value` stand for the identity of the retrofit configuration
that your would like to use (It would be clarified in configuration file part). `qualifier` attribute means the spring's
bean name.
If no attributes provided, we set `${name}RetrofitClient` as the bean name.

In you `application.{propeties|yaml}` set the configuration needed to send http request. 

For Example, following properties can be used to configure `open-library`
```properties

retrofit.services.open-library.base-url=https://openlibrary.org/ #The system prefix url
retrofit.services.open-library.connection.read-timeout=10000
retrofit.services.open-library.connection.write-timeout=10000
retrofit.services.open-library.connection.connect-timeout=10000

```

You can now `@Autowired` the retrofit interface to send real http request. e.g.:

```java
@Autowired
private final IOpenLibClient endpoint;

```

### Features

- [X] Retry
- [X] Circuit Breaker
- [X] Micrometer Metrics
- [X] Auth Header Propagation (when `propagate-auth-header=true`)

