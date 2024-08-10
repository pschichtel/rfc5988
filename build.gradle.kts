import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    signing
    java
    `maven-publish`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin")
    id("io.gitlab.arturbosch.detekt")
}

group = "tel.schich"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("tel.schich:parser-kombinator:0.3.0")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    val traceProperty = "tel.schich.parser-kombinator.trace"
    systemProperties[traceProperty] = System.getProperty(traceProperty)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

fun isSnapshot() = version.toString().endsWith("-SNAPSHOT")

publishing {
    repositories {
        maven {
            name = "cubyte"
            url = uri("https://maven.cubyte.org/repository/${if (isSnapshot()) "snapshots" else "releases"}/")
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("rfc5988")
                description.set("A simple RFC 5988 parser.")
                url.set("https://github.com/pschichtel/rfc5988")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("pschichtel")
                        name.set("Phillip Schichtel")
                        email.set("phillip@schich.tel")
                    }
                }
                scm {
                    url.set("https://github.com/pschichtel/rfc5988")
                    connection.set("scm:git:https://github.com/pschichtel/rfc5988")
                    developerConnection.set("scm:git:git@github.com:pschichtel/rfc5988")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

nexusPublishing {
    repositories {
        sonatype()
    }
}
