import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.unsafe

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
    alias(libs.plugins.kobwebx.markdown)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "net.numa08.niconico_advertiser_list2"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            description.set("ニコニコ動画の広告履歴から広告主リストを取得し、整形して表示するWebアプリケーション。動画投稿者が広告をくださった方々への感謝を表明する際に便利です。")
            lang.set("ja")

            head.add {
                // OGP Meta Tags
                meta {
                    attributes["property"] = "og:type"
                    content = "website"
                }
                meta {
                    attributes["property"] = "og:url"
                    content = "https://niconico-advertisers.numa08.dev/"
                }
                meta {
                    attributes["property"] = "og:title"
                    content = "ニコニコ動画 広告主リスト取得"
                }
                meta {
                    attributes["property"] = "og:description"
                    content = "ニコニコ動画の広告履歴から広告主リストを取得し、整形して表示するWebアプリケーション。動画投稿者が広告をくださった方々への感謝を表明する際に便利です。"
                }
                meta {
                    attributes["property"] = "og:image"
                    content = "https://niconico-advertisers.numa08.dev/ogp.png"
                }
                meta {
                    attributes["property"] = "og:locale"
                    content = "ja_JP"
                }
                meta {
                    attributes["property"] = "og:site_name"
                    content = "ニコニコ動画 広告主リスト取得"
                }

                // Twitter Card
                meta {
                    attributes["property"] = "twitter:card"
                    content = "summary_large_image"
                }
                meta {
                    attributes["property"] = "twitter:url"
                    content = "https://niconico-advertisers.numa08.dev/"
                }
                meta {
                    attributes["property"] = "twitter:title"
                    content = "ニコニコ動画 広告主リスト取得"
                }
                meta {
                    attributes["property"] = "twitter:description"
                    content = "ニコニコ動画の広告履歴から広告主リストを取得し、整形して表示するWebアプリケーション。動画投稿者が広告をくださった方々への感謝を表明する際に便利です。"
                }
                meta {
                    attributes["property"] = "twitter:image"
                    content = "https://niconico-advertisers.numa08.dev/ogp.png"
                }
                meta {
                    attributes["property"] = "twitter:creator"
                    content = "@numa_radio"
                }

                // Google Analytics 4
                val ga4MeasurementId = System.getenv("GA4_MEASUREMENT_ID")
                if (!ga4MeasurementId.isNullOrEmpty()) {
                    script {
                        async = true
                        src = "https://www.googletagmanager.com/gtag/js?id=$ga4MeasurementId"
                    }
                    script {
                        unsafe {
                            +"""
                            window.dataLayer = window.dataLayer || [];
                            function gtag(){dataLayer.push(arguments);}
                            gtag('js', new Date());
                            gtag('config', '$ga4MeasurementId', {
                                'send_page_view': false
                            });
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}

kotlin {
    configAsKobwebApplication("niconico_advertiser_list2", includeServer = true)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kobwebx.serialization.kotlinx)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(libs.kobwebx.markdown)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            compileOnly(libs.kobweb.api)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ksoup)
            implementation(libs.caffeine)
        }
    }

    // JS browser test configuration
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
    // 生成されたコードのスタイル違反は無視
    ignoreFailures.set(true)
    filter {
        exclude("**/build/**")
    }
}

// detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}
