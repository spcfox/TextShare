import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.5.10"
    kotlin("plugin.allopen") version "1.5.10"
}

group = "ru.spcfox.sharetext"
version = "0.1.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "org.springframework.boot", name = "spring-boot-starter")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-web")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
    implementation(group = "org.springdoc", name = "springdoc-openapi-ui", version = "1.6.+")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8")
    implementation(group = "org.liquibase", name = "liquibase-core")
    implementation(group = "io.jsonwebtoken", name = "jjwt-api", version = "0.11.+")
    implementation(group = "org.hashids", name = "hashids", version = "1.0.3")
    implementation(group = "org.postgresql", name = "postgresql", version = "42.3.+")

    developmentOnly(group = "org.springframework.boot", name = "spring-boot-devtools")

    runtimeOnly(group = "io.jsonwebtoken", name = "jjwt-impl", version = "0.11.+")
    runtimeOnly(group = "io.jsonwebtoken", name = "jjwt-jackson", version = "0.11.+")

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.8.2")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.8.2")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = "5.8.2")
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
    testImplementation(group = "com.ninja-squad", name = "springmockk", version = "3.1.1")
    testImplementation(group = "io.mockk", name = "mockk", version = "1.12.3")
    testImplementation(group = "org.testcontainers", name = "postgresql", version = "1.17.2")
    testImplementation(group = "org.testcontainers", name = "junit-jupiter", version = "1.17.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
