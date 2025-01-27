plugins {
    java
    id("eu.cloudnetservice.juppiter") version "0.2.0"
}

group = "dev.ein.cloudnet.module.backup"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io") }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        sourceCompatibility = JavaVersion.VERSION_23.toString()
        targetCompatibility = JavaVersion.VERSION_23.toString()
    }
}

var cliFile = "cloudnet-client.jar"

dependencies {
    moduleLibrary(libs.bundles.cloudnet) {
        exclude(group = "dev.derklaro.gulf", module = "gulf")
    }
    annotationProcessor(libs.bundles.cloudnetAnnotationProcessor)

    moduleLibrary(libs.lombok)
    annotationProcessor(libs.lombok)
    moduleLibrary(libs.zip4j)
    moduleLibrary(libs.apacheCompress)
    moduleLibrary(libs.bundles.cloud) {
        exclude(group = "org.incendo", module = "cloud-core")
    }


    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly(libs.bundles.tests)
}

moduleJson {
    main = "dev.ein.cloudnet.module.backup.CloudNetBackupModule"
    group = project.group.toString()
    runtimeModule = true
    storesSensitiveData = true
    name = "CloudNet-Backup"
    author = "MarkusTieger (Rewrite by EinDev)"
    description = "CloudNet-Backup is a module for the CloudNet network that allows you to automatically create backups of your servers."
    version = project.version.toString()
    website = "https://tigersystems.cf"
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    maxHeapSize = "1G"

    testLogging {
        events("passed")
    }
}