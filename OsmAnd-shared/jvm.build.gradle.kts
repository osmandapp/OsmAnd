import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("org.jetbrains.kotlin.multiplatform") version "2.0.0"
	id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

kotlin {
	jvm {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
			freeCompilerArgs.add("-Xjvm-default=all")
		}
	}

	val sqliteVersion = "2.3.1"
	val serializationVersion = "1.6.3"
	val coroutinesCoreVersion = "1.8.1"
	val datetimeVersion = "0.6.0"
	val okioVersion = "3.9.0"
	val kxml2Version = "2.3.0"
	val sqliterVersion = "1.3.1"
	val sqliteJDBCVersion = "3.34.0"
	val commonLoggingVersion = "1.2"

	sourceSets {
		commonMain.dependencies {
			implementation("org.jetbrains.kotlin:kotlin-stdlib")
			implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
			implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
			implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion")
			implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
			implementation("com.squareup.okio:okio:$okioVersion")
		}
		jvmMain.dependencies {
			implementation(kotlin("stdlib-jdk8"))
			implementation("net.sf.kxml:kxml2:$kxml2Version")
			implementation("org.xerial:sqlite-jdbc:$sqliteJDBCVersion")
			implementation("commons-logging:commons-logging:$commonLoggingVersion")
		}
	}
}