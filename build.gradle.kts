import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.10"
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
        setUrl("https://dl.bintray.com/jdkula/subprocess")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.ktor:ktor-client-apache:$ktor_version")
    compile("io.ktor:ktor-server-netty:$ktor_version")
    compile("ch.qos.logback:logback-classic:0.9.24")
    compile("pw.jonak:subprocess:1.0-FINAL")

    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}