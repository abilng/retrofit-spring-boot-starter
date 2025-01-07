package in.abilng.springboot.retrofit.core;

import in.abilng.springboot.retrofit.annotation.Retrofit;
import in.abilng.springboot.retrofit.config.RetroFitProperties;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The Retrofit clients registrar.
 */
public class RetrofitClientsRegistrar
        implements ImportBeanDefinitionRegistrar,
                BeanFactoryAware,
                ResourceLoaderAware,
                EnvironmentAware {

    private ResourceLoader resourceLoader;

    private BeanFactory beanFactory;
    private RetroFitProperties properties;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        final BindResult<RetroFitProperties> propertiesBindResult =
                Binder.get(environment).bind(RetroFitProperties.PROPERTY_PREFIX, RetroFitProperties.class);
        this.properties =
                Optional.ofNullable(propertiesBindResult)
                        .filter(BindResult::isBound)
                        .map(BindResult::get)
                        .orElse(new RetroFitProperties());
    }

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerRetrofitClients(registry);
    }

    /**
     * Gets base packages.
     *
     * @return the base packages
     */
    protected Set<String> getBasePackages() {
        return Set.copyOf(AutoConfigurationPackages.get(this.beanFactory));
    }

    /**
     * Register retrofit clients.
     *
     * @param registry the registry
     */
    public void registerRetrofitClients(BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);

        Set<String> basePackages = getBasePackages();

        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(Retrofit.class);
        scanner.addIncludeFilter(annotationTypeFilter);

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
                    // verify annotated class is an interface
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(
                            annotationMetadata.isInterface(), "@Retrofit can only be specified on an interface");

                    Map<String, Object> attributes =
                            annotationMetadata.getAnnotationAttributes(Retrofit.class.getCanonicalName());

                    registerRetrofitClient(registry, annotationMetadata, attributes);
                }
            }
        }
    }

    private void registerRetrofitClient(
            BeanDefinitionRegistry registry,
            AnnotationMetadata annotationMetadata,
            Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        BeanDefinitionBuilder definition =
                BeanDefinitionBuilder.genericBeanDefinition(RetrofitClientFactoryBean.class);
        String name = getName(attributes);
        definition.addPropertyValue("name", name);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("properties", properties.getServices().get(name));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.setLazyInit(true);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        String alias = name.replaceAll("[^a-zA-Z0-9]", "") + "RetrofitClient";
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setPrimary(true);
        String qualifier = getQualifier(attributes);
        if (StringUtils.hasText(qualifier)) {
            alias = qualifier;
        }

        BeanDefinitionHolder holder =
                new BeanDefinitionHolder(beanDefinition, className, new String[] {alias});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    /**
     * Gets scanner.
     *
     * @return the scanner
     */
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {

            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }

    private String getQualifier(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String qualifier = (String) client.get("qualifier");
        if (StringUtils.hasText(qualifier)) {
            return qualifier;
        }
        return null;
    }

    private String getName(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String value = (String) client.get("value");
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("name");
        }
        if (StringUtils.hasText(value)) {
            return value;
        }

        throw new IllegalStateException(
                "Either 'name' or 'value' must be provided in @" + Retrofit.class.getSimpleName());
    }
}
