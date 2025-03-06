plugins {
    idea
    java
    `java-library`
    `signing`
    `maven-publish`
    pmd
    jacoco
    checkstyle
    alias(libs.plugins.spotless)
    alias(libs.plugins.release)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.spring)
    alias(libs.plugins.springboot) apply false
}

group = "in.abilng"
description = "Spring Boot :: Starter :: Retrofit"

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
    // declare an "optional feature"
    registerFeature("optional") {
        usingSourceSet(sourceSets["main"])
    }
}

spotless {
    java {
        removeUnusedImports()
        googleJavaFormat("1.19.2").formatJavadoc(false)
        indentWithTabs(2)
        indentWithSpaces(4)
    }
}

pmd {
    toolVersion = "7.0.0"
    rulesMinimumPriority = 5
    ruleSetFiles = files("${project.rootDir}/config/pmd/ruleset.xml")
    reportsDir = rootProject.layout.buildDirectory.file("reports/pmd/${project.name}").get().asFile
}

checkstyle {
    toolVersion = "10.14.0"
    isIgnoreFailures = false
    maxWarnings = 0
    reportsDir = rootProject.layout.buildDirectory.file("reports/checkstyle/${project.name}").get().asFile
}

spotbugs {
    toolVersion = "4.8.3"
    excludeFilter = file("${rootProject.projectDir}/config/findbugs/excludeBugsFilter.xml")
    reportsDir = rootProject.layout.buildDirectory.file("reports/spotbugs/${project.name}").get().asFile
}

tasks {

    spotbugsMain {
        reports.create("html") {
            required = true
            setStylesheet("fancy-hist.xsl")
        }
    }

    pmdMain {
        reports {
            xml.required = true
            html.required = true
        }
    }

    jacocoTestReport {
        reports {
            xml.required = true
            xml.outputLocation = rootProject.layout.buildDirectory.file("reports/jacoco/${project.name}/report.xml").get().asFile
            html.required = true
            html.outputLocation = rootProject.layout.buildDirectory.dir("reports/jacoco/${project.name}/html").get().asFile
            csv.required = false
        }
    }

    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport) // report is always generated after tests run
        reports {
            html.outputLocation = rootProject.layout.buildDirectory.dir("reports/unit-test/${project.name}/html").get().asFile
            junitXml.outputLocation = rootProject.layout.buildDirectory.dir("reports/unit-test/${project.name}").get().asFile
        }
    }

    spotbugsTest {
        enabled = false
    }
    pmdTest {
        enabled = false
    }
    checkstyleTest {
        enabled = false
    }

    withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }

    withType<Javadoc>() {
        options {
            (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
        }
    }
}

fun DependencyHandler.optional(dependencyNotation: Any): Dependency? =
    add("optionalImplementation", dependencyNotation)

dependencies {
    api(libs.retrofit2.retrofit)
    api(libs.okhttp3.okhttp)
    api(libs.spring.boot.starter)
    api(libs.slf4j.api)
    api(libs.resilience4j.circuitbreaker)
    api(libs.resilience4j.retry)
    optional(libs.spring.boot.web)
    optional(libs.retrofit2.jackson)
    optional(libs.retrofit2.gson)
    optional(libs.retrofit2.scalars)
    optional(libs.retrofit2.xml)
    optional(libs.okhttp3.logging.interceptor)
    optional(libs.micrometer.core)
    optional(libs.micrometer.tracing)
    optional(libs.micrometer.otel)

    compileOnly(libs.findbugs)
    compileOnly(libs.spotbugs.annotations)
    compileOnly(libs.spring.boot.autoconfigure.processor)
    compileOnly(libs.spring.boot.configuration.processor)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.spring.boot.autoconfigure.processor)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.jaxb.runtime)
    testImplementation(libs.jaxb.bind)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks {
    create<Copy>("updateGitHooks") {
        destinationDir = File("$projectDir/.git/hooks")
        from("$projectDir/scripts/pre-commit") to "$projectDir/.git/hooks"
    }

    build {
        dependsOn(":updateGitHooks")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        suppressPomMetadataWarningsFor("optionalApiElements")
        suppressPomMetadataWarningsFor("optionalRuntimeElements")
    }

    publications {
        repositories {
            maven {
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                name = "OSSRH"
                url = if(version.toString().endsWith("-SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/abilng/retrofit-spring-boot-starter")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

val signingKey: String? by project
val signingPassword: String? by project

if (!version.toString().endsWith("-SNAPSHOT") && (signingPassword != null || signingKey != null)) {
    signing {
        if (signingKey != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        sign(publishing.publications["maven"])
    }
}
