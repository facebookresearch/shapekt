/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */


plugins {
  // When updating the Kotlin version, remember to update the
  // kotlin version in the playground project as well.
  kotlin("jvm") version "1.7.0-dev-444"
  `maven-publish`
  `signing`
}

allprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "kotlin")
  apply(plugin = "signing")

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  repositories {
    mavenCentral()
    maven {
      name = "KotlinBootstrap"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
  }

  //sources
  val sourcesJar by tasks.register<Jar>("sourcesJar") {
    group = "build"
    description = "Assembles a jar archive containing the main sources."
    archiveClassifier.set("sources")

    from(sourceSets["main"].allSource)
    from("LICENSE")
  }

  //documentation
  val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from("$buildDir/javadoc")
  }

  val repositoryUsername: String? by project
  val repositoryPassword: String? by project
  val signingKey: String? by project
  val signingPassword: String? by project


  fun isRelease() = true

  publishing {
    repositories {
      maven {
        name = "diffkt"
        url = uri("https://maven.pkg.github.com/facebookresearch/diffkt")
        val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        name = "deploy"
        url = if (isRelease()) releasesRepoUrl else snapshotsRepoUrl
        credentials {
          username = System.getenv("repositoryUsername") ?: repositoryUsername
          password = System.getenv("repositoryPassword") ?: repositoryPassword
        }
      }
    }

    publications {
      create<MavenPublication>("Shapekt") {
        //from(components["javaPlatform"])
        from(components["java"])

        artifact(sourcesJar)
        artifact(javadocJar)

        pom {
          name.set("ShapeKt")
          description.set("Automatic differentiation in Kotlin")
          url.set("https://github.com/facebookresearch/shapekt")

          groupId = "com.facebook"
          artifactId = "shapekt"
          version = "0.1.0-SNAPSHOT"

          scm {
            connection.set("scm:git:https://github.com/facebookresearch/shapekt")
            developerConnection.set("scm:git:https://github.com/thomasnield/")
            url.set("https://github.com/facebookresearch/shapekt")
          }

          licenses {
            license {
              name.set("MIT-1.0")
              url.set("https://opensource.org/licenses/MIT")
            }
          }

          developers {
            developer {
              id.set("thomasnield")
              name.set("Thomas Nield")
              email.set("thomasnield@live.com")
            }
          }
        }
      }
    }
  }
  signing {
    val publications: PublicationContainer = (extensions.getByName("publishing") as PublishingExtension).publications

    if (isRelease()) {
      sign(publications)
    }
  }
  group = project.property("group") as String

  version = project.property("version") as String


  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+ProperCheckAnnotationsTargetInTypeUsePositions"
  }
}
