plugins {
  kotlin("jvm") version "2.2.20"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  application
}

application {
  mainClass.set("MainKt")
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
  implementation(kotlin("stdlib-jdk8"))
}
tasks.jar {
  manifest {
    attributes["Main-Class"] = "MainKt"
  }
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}

tasks.shadowJar {
  archiveBaseName.set("chess")
  archiveClassifier.set("") // Keeps the filename clean (e.g., chess-tcp-1.0-SNAPSHOT.jar)

  manifest {
    attributes["Main-Class"] = "MainKt"
  }


}