import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChildren
import java.net.URL

val isCiBuild = providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false).get()
val isSnapshot = providers.gradleProperty("isSnapshot").map { it.toBoolean() }.orElse(false).get()

plugins {
    java

    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
    alias(libs.plugins.download)

    `maven-publish`
    signing
}

allprojects {
    if (!isCiBuild) {
        version = "${version}.local"
    }
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
}

dependencies {
    implementation(libs.dsljson)
    implementation(libs.commons.collections)
    implementation(libs.flatlaf)

    testImplementation(libs.junit)
}

base {
    archivesName = project.name
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
//    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<Sign>().configureEach {
    enabled = isCiBuild && !isSnapshot
}

tasks {
    jar {
        enabled = false

        manifest {
            attributes("Enable-Native-Access" to "ALL-UNNAMED")
        }
    }

    shadowJar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to "LeafInstaller",
                    "Implementation-Version" to project.version,
                    "Main-Class" to "${project.group}.${project.name}.Main"
                )
            )
        }

        minimize {
            exclude(dependency("${libs.flatlaf.get()}"))
        }

        archiveClassifier.set("")
        exclude("icon.ico")
    }

    publish {
        mustRunAfter(checkVersion)
    }

    spotless {
        java {
            licenseHeaderFile(rootProject.file("HEADER"))
        }
    }
}

val checkVersion by tasks.registering {
    description = "Ensures that the version being released has not already been released"
    doFirst {
        val xml = URL(
            "https://repo.maven.apache.org/maven2/${
                rootProject.group.toString().replace(".", "/")
            }/${rootProject.name}/maven-metadata.xml"
        ).readText()
        val metadata = XmlSlurper().parseText(xml)

        val versioning = metadata.getProperty("versioning") as GPathResult
        val versions = versioning.getProperty("versions") as GPathResult
        val versionText = (versions.getProperty("version") as NodeChildren).map { it.toString() }
        if (versionText.contains(version)) {
            throw RuntimeException ("$version has already been released!")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            artifact(tasks.named("shadowJar")) {
                classifier = null
            }
            artifact(tasks.named("sourcesJar"))
//        artifact(tasks.named("javadocJar"))

            pom {
                name = rootProject.name
                group = rootProject.group
                description = rootProject.description
                url = property("url").toString()
                inceptionYear = "2025"
                developers {
                    developer {
                        id = "aoqia"
                        name = "aoqia"
                    }
                }
                issueManagement {
                    system = "GitHub"
                    url = "${property("url").toString()}/issues"
                }
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://spdx.org/licenses/Apache-2.0.html"
                    }
                }
                scm {
                    connection = "scm:git:${property("url").toString()}.git"
                    developerConnection =
                        "scm:git:${property("url").toString().replace("https", "ssh")}.git"
                    url = property("url").toString()
                }
            }
        }
    }

    repositories {
        maven {
            name = "leaf"
            url = uri("https://maven.aoqia.dev/${if (isSnapshot) "snapshots" else "releases"}")

            credentials {
                username = providers.gradleProperty("mavenUsername").orNull
                password = providers.gradleProperty("mavenPassword").orNull
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

signing {
    isRequired = isCiBuild and !isSnapshot

    val signingKey = providers.gradleProperty("signingKey")
    val signingPassword = providers.gradleProperty("signingPassword")
    if (signingKey.isPresent && signingPassword.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
    }

    sign(publishing.publications)
}
