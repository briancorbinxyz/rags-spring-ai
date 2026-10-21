plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "xyz.briancorbin"
version = "0.3.0-SNAPSHOT"
description = "RAGs to Riches with Spring AI"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "2.0.1"

dependencies {
    // Episode 3 leaves tic-tac-toe behind: no tictactoe-api.
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    // Extract: read HTML into Documents.
    implementation("org.springframework.ai:spring-ai-jsoup-document-reader")
    // Load and retrieve: SimpleVectorStore, and the advisor that searches it on every call.
    implementation("org.springframework.ai:spring-ai-vector-store")
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")
    // Episode 4: Spring AI's retriever and post-processor interfaces, and keyword search in SQLite.
    implementation("org.springframework.ai:spring-ai-rag")
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")
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
    // sqlite-jdbc loads its native library; say so, rather than be warned about it.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Lets `--args='--archive.run=ask'` read follow-up questions from the terminal.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    standardInput = System.`in`
}
