import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import java.io.File

abstract class DownloadPythonTask : DefaultTask() {
    enum class Version(val str: String) {
        Python3_9("3.9"),
        Python3_10("3.10"),
        Python3_11("3.11"),
        Python3_12("3.12"),
    }

    enum class Platform {
        Windows,
        Linux,
        MacOS,
    }

    @get:Input
    abstract val version: Property<Version>

    @get:Input
    abstract val platform: Property<Platform>

    @get:OutputFile
    abstract val tarFile: Property<File>

    init {
        tarFile.convention(Providers.changing {
            project.layout.buildDirectory.file("python-${version.get().str}-${platform.get()}.tar.gz").get().asFile
        })
    }

    @TaskAction
    fun download(): Unit = runBlocking {
        val version = version.get()
        val platform = platform.get()
        val data = client.get("https://api.github.com/repos/indygreg/python-build-standalone/releases?per_page=1").body<JsonArray>().first().jsonObject

        val hostOs = when (platform) {
            Platform.Windows -> "pc-windows-msvc"
            Platform.Linux -> "unknown-linux-gnu"
            Platform.MacOS -> "apple-darwin"
            else -> throw IllegalArgumentException("Unsupported platform: $platform")
        }
        val tmpFile by tarFile
        tmpFile.parentFile.mkdirs()

        val url = data["assets"]!!.jsonArray.map { obj ->
            obj.jsonObject["browser_download_url"]!!.jsonPrimitive.content
        }.first { it.contains("/cpython-${version.str}") && it.endsWith("x86_64-$hostOs-install_only.tar.gz") }

        client.get(url).bodyAsChannel().also { channel ->
            tmpFile.writeChannel().use {
                channel.copyTo(this)
            }
        }
    }

    companion object {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
