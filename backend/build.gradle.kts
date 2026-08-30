plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    application
}

kotlin {
    jvmToolchain(17)
    explicitApiWarning()
}

application {
    mainClass.set("cz.b2brental.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
    implementation("io.ktor:ktor-server-auth-jvm:3.0.3")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.0.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.0.3")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.0.3")

    // Ktor client
    implementation("io.ktor:ktor-client-core-jvm:3.0.3")
    implementation("io.ktor:ktor-client-cio-jvm:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-client-json-jvm:3.0.3")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.4")

    // HikariCP
    implementation("com.zaxxer:HikariCP:5.1.0")

    // bcrypt
    implementation("org.mindrot:jbcrypt:0.4")

    // kotlinx-serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Logback
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // OpenPDF (pro generování PDF)
    implementation("com.github.librepdf:openpdf:1.3.43")

    // Money (JSR-354)
    implementation("org.javamoney:moneta:1.4.5")

    // Test dependencies
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.3")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        total {
            verify {
                // Brána pokrytí se do úlohy `check` zapne ve Vlně E (krok E.11);
                // dokud neexistují testy, porušovala by každé sestavení (0 % < 70 %).
                onCheck = false
                rule {
                    minBound(70)
                }
            }
        }
    }
}

ktlint {
    version.set("1.3.1")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}
