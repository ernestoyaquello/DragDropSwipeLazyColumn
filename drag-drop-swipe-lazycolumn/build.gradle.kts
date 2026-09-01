import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "com.ernestoyaquello.dragdropswipelazycolumn"
version = "0.11.0"

configure<LibraryExtension> {
    namespace = "com.ernestoyaquello.dragdropswipelazycolumn"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2Api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    api(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    // This is a transitive dependency, so technically we don't need it here;
    // however, we have to set it to a higher version to avoid a crash on SDK 37.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "drag-drop-swipe-lazycolumn",
        version = version.toString(),
    )

    pom {
        name.set("DragDropSwipeLazyColumn")
        url.set("https://github.com/ernestoyaquello/DragDropSwipeLazyColumn")
        inceptionYear.set("2025")
        description.set(
            "Kotlin Android library for Jetpack Compose that implements a lazy column" +
                    "with drag-and-drop reordering and swipe-to-dismiss functionality.",
        )

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("ernestoyaquello")
                name.set("Julio Ernesto Rodríguez Cabañas")
                url.set("https://julioernesto.me/")
            }
        }

        scm {
            url.set("https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/")
            connection.set("scm:git:git://github.com/ernestoyaquello/DragDropSwipeLazyColumn.git")
            developerConnection.set("scm:git:ssh://git@github.com/ernestoyaquello/DragDropSwipeLazyColumn.git")
        }
    }
}
