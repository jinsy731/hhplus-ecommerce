plugins {
	kotlin("jvm") version "1.9.0"
	kotlin("kapt") version "1.9.0"
	kotlin("plugin.spring") version "1.9.0"
	kotlin("plugin.jpa") version "1.9.0"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}


fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
		jvmToolchain(17)
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

noArg {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
	invokeInitializers = true
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

dependencies {
	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.retry:spring-retry")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.redisson:redisson-spring-boot-starter:3.23.2")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.kafka:spring-kafka")
    // DB
	runtimeOnly("com.mysql:mysql-connector-j")

	// Doc
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	// Test
	// Mockito Core
	testImplementation("org.mockito:mockito-core:5.11.0")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testImplementation("io.kotest:kotest-assertions-core:5.8.0")
	testImplementation("io.mockk:mockk:1.13.17")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.testcontainers:kafka")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
	systemProperty("user.timezone", "UTC")
}
