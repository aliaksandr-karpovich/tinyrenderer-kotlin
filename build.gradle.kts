import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "2.3.20"
val coroutinesVersion = "1.10.2"
val imagejVersion = "1.54i"
val tornadofxVersion = "1.7.20"

plugins {
    id("java")
    id("application")
    kotlin("jvm") version "2.3.20"
    kotlin("kapt") version "2.3.20"

    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
    mainClass.set("com.akar.tinyrenderer.gui.AppKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("net.imagej:ij:$imagejVersion")
    implementation("no.tornado:tornadofx:$tornadofxVersion") {
        exclude("org.jetbrains.kotlin")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.akar.tinyrenderer.gui.AppKt"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks {
    "build" {
        dependsOn
    }
}

