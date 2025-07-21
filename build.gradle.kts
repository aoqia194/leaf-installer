import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChildren
import org.gradle.internal.declarativedsl.schemaBuilder.schemaFromTypes
import org.jreleaser.model.Active
import org.jreleaser.model.Http
import java.net.URL

val env = System.getenv()!!
val isCiEnv = env["CI"].toBoolean()
val gpgKeyPassphrase = env["GPG_PASSPHRASE_KEY"]
val gpgKeyPublic = env["GPG_PUBLIC_KEY"]
val gpgKeyPrivate = env["GPG_PRIVATE_KEY"]
val mavenUsername = env["MAVEN_USERNAME"]
val mavenPassword = env["MAVEN_PASSWORD"]

plugins {
    java

    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
    alias(libs.plugins.download)
    // Publishing to Maven Central
    `maven-publish`
    alias(libs.plugins.jreleaser)
}

allprojects {
    if (!isCiEnv) {
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
    implementation(libs.jackson.databind)
    implementation(libs.commons.collections)
    testImplementation(libs.junit)
}

base {
    archivesName = project.name
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
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
                    "Main-Class" to "${project.group}.installer.Main"
                )
            )
        }

        minimize()
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

// A task to ensure that the version being released has not already been released.
val checkVersion by tasks.registering {
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
            throw RuntimeException ("${version} has already been released!")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

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

            artifact(tasks.named("shadowJar")) {
                classifier = null
            }
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        name = rootProject.name
        version = rootProject.version.toString()
        versionPattern = "SEMVER"
        authors = listOf("aoqia194", "FabricMC")
        maintainers = listOf("aoqia194")
        license = "MIT"
        inceptionYear = "2025"

        links {
            homepage = property("url").toString()
            license = "https://spdx.org/licenses/MIT.html"
        }
    }

    files {
        active = Active.ALWAYS

        artifact {
            path = tasks.shadowJar.get().archiveFile.get()
        }
    }

    signing {
        active = Active.ALWAYS
        armored = true
        passphrase = gpgKeyPassphrase
        publicKey = gpgKeyPublic
        secretKey = gpgKeyPrivate
    }

    deploy {
        maven {
            pomchecker {
                version = "1.14.0"
                failOnWarning = false // annoying
                failOnError = true
                strict = true
            }

            mavenCentral {
                create("sonatype") {
                    applyMavenCentralRules = true
                    active = Active.ALWAYS
                    snapshotSupported = true
                    authorization = Http.Authorization.BEARER
                    username = mavenUsername
                    password = mavenPassword
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                    verifyUrl = "https://repo1.maven.org/maven2/{{path}}/{{filename}}"
                    namespace = rootProject.group.toString()
                    retryDelay = 60
                    maxRetries = 30
                }
            }
        }
    }

    release {
        github {
            enabled = true
            repoOwner = "aoqia194"
            name = "leaf-${rootProject.name}"
            host = "github.com"
            releaseName = "{{tagName}}"
            sign = true
            overwrite = true

            uploadAssets = Active.ALWAYS
            artifacts = true
            checksums = true
            signatures = true

            changelog {
                formatted = Active.ALWAYS
                preset = "conventional-commits"
                extraProperties.put("categorizeScopes", "true")
            }
        }
    }
}
