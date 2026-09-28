pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

rootProject.name = providers.gradleProperty("name").get()
