import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("io.ktor.plugin") version "2.3.2"
    id("org.openapi.generator") version "7.2.0"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.example.AppKt") 
}

// Configure Kotlin source sets to include generated code
sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated/src/main/kotlin")
        }
    }
}

dependencies {
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core-jvm:2.3.2")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.2")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.3.2")
    implementation("io.ktor:ktor-server-auth-jvm:2.3.2")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.2")
    implementation("io.ktor:ktor-server-resources:2.3.2")
    implementation("io.ktor:ktor-server-status-pages:2.3.2")
    implementation("io.ktor:ktor-server-default-headers:2.3.2")
    implementation("io.ktor:ktor-server-cors:2.3.2")
    implementation("io.ktor:ktor-server-compression:2.3.2")
    implementation("io.ktor:ktor-server-host-common:2.3.2")
    implementation("io.ktor:ktor-server-status-pages:2.3.2")

    // JWT
    implementation("com.auth0:java-jwt:4.2.1")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // OpenAPI/Swagger
    implementation("io.ktor:ktor-server-swagger:2.3.2")
    implementation("io.ktor:ktor-server-openapi:2.3.2")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    
    // PostgreSQL and database dependencies
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.22.3")
    
    // For testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.0")
    testImplementation("org.testcontainers:postgresql:1.19.1")
}

// Configure Kotlin JVM target
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

// Optional: Ktor plugin config (if you'd like to run using `gradle run`)
ktor {
    fatJar {
        archiveFileName.set("carcassonne-backend.jar")
    }
}

// Task to clean the generated directory
tasks.register<Delete>("cleanGenerated") {
    delete("$buildDir/generated")
}

// OpenAPI Generator configuration
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/resources/openapi.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("org.example.api.generated")
    modelPackage.set("org.example.model.generated")
    packageName.set("org.example")
    configOptions.set(mapOf(
        "serializationLibrary" to "jackson",
        "enumPropertyNaming" to "UPPERCASE",
        "sourceFolder" to "src/main/kotlin",
        "modelMutable" to "true"
    ))
    globalProperties.set(mapOf(
        "apis" to "none",  // Don't generate API interfaces
        "models" to "",    // Generate models
        "modelDocs" to "false",
        "modelTests" to "false",
        "apiTests" to "false",
        "modelPackage" to "org.example.model.generated" // Force the model package
    ))
    typeMappings.set(mapOf(
        "DateTime" to "java.time.OffsetDateTime"
    ))
}

// Make openApiGenerate depend on cleanGenerated
tasks.named("openApiGenerate") {
    dependsOn("cleanGenerated")
}

// Make compileKotlin depend on openApiGenerate
tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}
