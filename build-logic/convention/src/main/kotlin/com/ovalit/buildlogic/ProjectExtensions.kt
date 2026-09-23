package com.ovalit.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.int(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

private fun VersionCatalog.string(alias: String): String =
    findVersion(alias).get().requiredVersion

/** 카탈로그의 `jvmTarget` 하나만 보게 한다. 코틀린과 자바가 따로 놀면 찾기 어렵다. */
internal val VersionCatalog.jvmTarget: JvmTarget
    get() = JvmTarget.fromTarget(string("jvmTarget"))

internal val VersionCatalog.javaVersion: JavaVersion
    get() = JavaVersion.toVersion(string("jvmTarget"))

/** `:shared:core:designsystem` → `com.ovalit.core.designsystem` */
internal val Project.ovalitNamespace: String
    get() = "com.ovalit." + path.removePrefix(":shared:").removePrefix(":").replace(':', '.')
