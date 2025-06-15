// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Do NOT add: dependencies { classpath("com.android.tools.build:gradle:...") }
// when you are using the plugins {} + version catalog aliases approach.
