plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    // One set of conventions, enforced. Google style, 2 spaces, 100 columns: the code is quoted in
    // the articles, so an unformatted excerpt is a published defect.
    id("com.diffplug.spotless") version "8.8.0"
    id("net.ltgt.errorprone") version "4.1.0"
    checkstyle
}

group = "org.xxdc.oss.example"
version = "0.2.0-SNAPSHOT"
description = "Over-Engineering Tic-Tac-Toe w. Spring AI"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// Episode 1 ran on Spring Boot 3.5 and Spring AI 1.0. Spring AI 2.0 needs Boot 4.
extra["springAiVersion"] = "2.0.1"

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    implementation("org.xxdc.oss.example:tictactoe-api:3.0.0-jdk25")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    // Fail on any warning: a warning nobody acts on is noise that hides the next one.
    options.compilerArgs.addAll(
        listOf(
            "-Werror",
            "-Xlint:all",
            "-Xlint:-processing", // JUnit 5's auto-registration triggers this; noise, not signal.
            // Game comes from tictactoe-api and its close() declares InterruptedException. The
            // warning is about a dependency's signature, and there is nothing here to fix.
            "-Xlint:-try",
        ),
    )
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.35.0") // Google style: 2-space indent, 100 columns
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

checkstyle {
    toolVersion = "14.3.0"
    maxWarnings = 0
}
