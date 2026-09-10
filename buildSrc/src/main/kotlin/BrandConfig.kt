import org.gradle.api.Project
import java.io.File
import java.util.Properties

object BrandConfig {
    fun loadMergedProperties(vararg files: File): Properties = Properties().apply {
        files.filter { it.exists() }.forEach { file ->
            file.inputStream().use(::load)
        }
    }

    fun load(project: Project, name: String): Properties {
        val dir = project.rootProject.file("config")
        return loadMergedProperties(
            File(dir, "$name.properties"),
            File(dir, "$name.local.properties"),
        )
    }

    fun Properties.required(key: String): String =
        getProperty(key)?.takeIf { it.isNotBlank() }
            ?: error("Missing required config key: $key")

    fun Properties.optional(key: String, default: String = ""): String =
        getProperty(key)?.takeIf { it.isNotBlank() } ?: default

    fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
