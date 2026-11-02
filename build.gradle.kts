import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    // Episode 5: one set of conventions, enforced. Google style, 2 spaces, 100 columns --
    // the code is quoted in the articles, so an unformatted excerpt is a published defect.
    id("com.diffplug.spotless") version "8.10.4"
    id("net.ltgt.errorprone") version "4.1.0"
    checkstyle
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
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    // Episode 5: the seams between generic search and this archive, asserted like anything else.
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Fail on any warning: a warning nobody acts on is noise that hides the next one.
    options.compilerArgs.addAll(
        listOf(
            "-Werror",
            "-Xlint:all",
            "-Xlint:-processing", // JUnit 5's auto-registration triggers this; noise, not signal.
        ),
    )
    options.errorprone {
        // Error Prone's defaults, plus the ones it ships switched off that match how this code is
        // read. Each was turned on, run against the whole tree, and kept only when what it found
        // was worth fixing. Two were tried and rejected, which is worth recording:
        //
        // `PreferredInterfaceType` wanted `IdentifierLookup.lookup` to return `List` rather than
        // `SequencedCollection`, and the narrower type is the point -- these addresses are ordered,
        // not indexed.
        //
        // `UnnecessarilyFullyQualified` found one accident in `CitationChecker` and five
        // deliberate uses in `GoldenRun.hash`, where a fully qualified name makes a method
        // self-contained for a reader who is seeing it quoted in an article without its imports.
        // A check that fights that is not worth the suppressions it would need.
        enable(
            "ImmutableSetForContains",
            "ConstantPatternCompile",
            "IdentifierName",
            "StatementSwitchToExpressionSwitch",
            "MissingDefault",
            "RedundantOverride",
            "UnnecessaryStaticImport",
        )
    }
}

checkstyle {
    toolVersion = "14.3.0"
    // Layout belongs to the formatter and Error Prone owns correctness; this counts the two things
    // neither can see: how long a method is and how many things it is handed. Both reached zero
    // before it was allowed to fail the build, so the limit is where the code is, not the reverse.
    maxWarnings = 0
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.36.1") // Google style: 2-space indent, 100 columns
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Let -Dspectastic.path=… and friends reach the test JVM, so a worktree can point at the
    // checkout it is measuring rather than assuming a sibling directory. Without this the ratchet
    // tests skip silently, which is a bad way to pass.
    for (name in listOf("spectastic.path", "vectors.cache", "ollama.url", "rerank.url")) {
        System.getProperty(name)?.let { systemProperty(name, it) }
    }
    // sqlite-jdbc loads its native library; say so, rather than be warned about it.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// ENG-04, SAI-15: `./gradlew test` is kind to a reader with no archive and skips the ratchet.
// This task is not kind: it demands the checkout, the pinned model and the reranker, and fails if
// any is missing. Episode 4 sells the ratchet as a regression test, and a regression test that
// turns green when its inputs are gone is a false guarantee -- anyone copying it into CI got no
// protection. Both audiences are served, by two tasks rather than one lenient one.
tasks.register<Test>("ratchetTest") {
    description = "Runs the ratchet and fails, rather than skips, when its inputs are missing."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    // Everything that needs the frozen checkout, the pinned models or the reranker, and so must
    // fail rather than skip when a measurement cannot be made. Named one by one rather than by a
    // wildcard, so adding a measurement is a decision and not an accident of spelling.
    filter {
        includeTestsMatching("*RatchetTest")
        includeTestsMatching("*SplitPartHitsTest")
        includeTestsMatching("*StepLatencyTest")
    }
    systemProperty("ratchet.required", "true")
    for (name in listOf("spectastic.path", "vectors.cache", "ollama.url", "rerank.url")) {
        System.getProperty(name)?.let { systemProperty(name, it) }
    }
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    outputs.upToDateWhen { false }
}

// Lets `--args='--archive.run=ask'` read follow-up questions from the terminal.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    standardInput = System.`in`
}
