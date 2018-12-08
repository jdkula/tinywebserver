import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    java
    kotlin("jvm") version "1.3.11"
    application
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

group = "pw.jonak"
version = "1.0-SNAPSHOT"

val ktor_version = "1.0.1"

application {
    mainClassName = "pw.jonak.thesmallestwebserver.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = URI("https://dl.bintray.com/jdkula/subprocess")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:0.9.24")
    implementation("pw.jonak:subprocess:1.5-FINAL")
    implementation("com.beust:klaxon:3.0.1")

    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}