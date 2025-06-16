import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "com.ernestoyaquello.dragdropswipelazycolumn"
version = "0.10.0"

android {
    namespace = "com.ernestoyaquello.dragdropswipelazycolumn"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "drag-drop-swipe-lazycolumn",
        version = version.toString(),
    )

    pom {
        name.set("DragDropSwipeLazyColumn")
        description.set(
            "Kotlin Android library for Jetpack Compose that implements a lazy column" +
                    "with drag-and-drop reordering and swipe-to-dismiss functionality.",
        )
        inceptionYear.set("2025")
        url.set("https://github.com/ernestoyaquello/DragDropSwipeLazyColumn")
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