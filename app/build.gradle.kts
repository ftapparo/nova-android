import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Lê app/google-services.json (projeto Firebase do push; o arquivo não contém segredo).
    alias(libs.plugins.google.services)
}

// Chave de upload da Play: fica fora do repositório (NOVA_KEYSTORE_PROPERTIES ou ../NovaResidence-chaves).
// Sem o arquivo, o release é gerado sem assinatura, para o build continuar funcionando em outra máquina.
val keystoreProperties = Properties().apply {
    val path = System.getenv("NOVA_KEYSTORE_PROPERTIES") ?: rootProject.file("../NovaResidence-chaves/keystore.properties").path
    val file = file(path)
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "br.com.novaresidence"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.novaresidence"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "V3_BASE_URL", "\"https://api.novaresidence.com.br\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"628782431663-jcdpiqiph63a50kk21iin4b0dfk32gga.apps.googleusercontent.com\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // R8: encolhe e otimiza o release; regras próprias em proguard-rules.pro.
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }    // O idioma é escolhido dentro do app (tela 11): todos os idiomas precisam vir instalados, não só o do aparelho.
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.location)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
