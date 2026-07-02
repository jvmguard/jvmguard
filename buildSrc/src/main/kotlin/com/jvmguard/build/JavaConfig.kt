package com.jvmguard.build

import org.gradle.api.Project

const val JAVA_BASELINE_VERSION = 25

var Project.javaVersion: String? by NamedTaskExtraPropertyDelegate("classes")
val Project.usedJavaVersion: String get() = javaVersion ?: JAVA_BASELINE_VERSION.toString()
var Project.classFileVersion: String? by NamedTaskExtraPropertyDelegate("classes")
