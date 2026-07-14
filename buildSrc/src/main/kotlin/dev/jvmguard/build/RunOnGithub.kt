package dev.jvmguard.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@UntrackedTask(because = "dispatches a GitHub Actions workflow")
abstract class RunOnGithub : DefaultTask() {
    @get:Inject
    abstract val execOps: ExecOperations

    @Internal
    val tasks: Property<String> = project.objects.property(String::class.java).convention("")
    @Internal
    val displayName: Property<String> = project.objects.property(String::class.java).convention("")
    @Internal
    val workflowFile: Property<String> = project.objects.property(String::class.java).convention("build.yml")
    @Internal
    val additionalScript: Property<String> = project.objects.property(String::class.java).convention("")
    @Internal
    val excludedTaskNames: SetProperty<String> = project.objects.setProperty(String::class.java)
    @Internal
    val runner: Property<String> = project.objects.property(String::class.java)

    private val startParamExcludedTaskNames = project.gradle.startParameter.excludedTaskNames.toSet()
    private val startParamSystemPropertiesArgs =
        project.gradle.startParameter.systemPropertiesArgs
            .filter { !it.key.startsWith("idea.") && !it.key.startsWith("user.") && it.key != "file.encoding" }
            .toMap()
    private val startParamProjectProperties =
        project.gradle.startParameter.projectProperties.toMap()
    private val githubConfigurationCacheDisabled  =
        project.providers.gradleProperty("githubConfigurationCache").map { it == "false" }.orElse(false).get()
    private val githubAdditionalScript =
        project.providers.gradleProperty("githubAdditionalScript").orElse("").get()

    @TaskAction
    fun run() {
        checkCurrentCommit()

        val branch = retrieveBranch()
        val javaMajorVersion = System.getProperty("java.version").substringBefore(".")
        var tasks = tasks.get()
        if (tasks.isEmpty()) {
            if (!path.endsWith("Github")) {
                throw RuntimeException("task must either end with GitHub or tasks property must be configured")
            }
            tasks = path.substringBeforeLast("Github")
        }

        for (excludedTaskName in excludedTaskNames.get() + startParamExcludedTaskNames) {
            tasks += " -x $excludedTaskName"
        }
        for ((key, value) in startParamSystemPropertiesArgs) {
            if (key == "githubInfo") {
                if (value == "true") {
                    tasks += " -i"
                }
            } else {
                tasks += " -D$key=$value"
            }
        }
        for ((key, value) in startParamProjectProperties) {
            tasks += " -P$key=$value"
        }
        if (githubConfigurationCacheDisabled) {
            tasks += " --no-configuration-cache --no-parallel"
        }

        val startTime = Instant.now().atZone(ZoneId.of("UTC")).toInstant().minus(1, ChronoUnit.MINUTES)

        val displayName = displayName.get()

        @Suppress("SpellCheckingInspection")
        val inputs = JSONObject()
            .put("branch", branch)
            .put("java", javaMajorVersion)
            .put("tasks", tasks)
            .put("displayname", displayName)
        if (additionalScript.get().isNotEmpty()) {
            inputs.put("additional", additionalScript.get())
        } else if (githubAdditionalScript.isNotEmpty()) {
            inputs.put("additional", githubAdditionalScript)
        }
        if (runner.isPresent) {
            inputs.put("runner", runner.get())
        }
        val workflowId = resolveWorkflowId(workflowFile.get())
        triggerWorkflow(workflowId, inputs)
        val fullTitle = displayName.ifEmpty { tasks } + " [" + branch + "]"
        openHtmlUrl(workflowFile.get(), startTime, fullTitle)
    }

    private fun checkCurrentCommit() {
        runCommand(listOf("git", "fetch", "origin"))
        val localRevision = runCommand(listOf("git", "rev-parse", "@")).lines().first().trim()
        val upstreamRevision = runCommand(listOf("git", "rev-parse", "@{u}")).lines().first().trim()
        if (localRevision != upstreamRevision) {
            throw GradleException("Local and upstream revision do not match: $localRevision != $upstreamRevision")
        }
        execOps.exec {
            commandLine = listOf(
                "git",
                "-c",
                "color.ui=always",
                "log",
                "-1",
                "--pretty=format:%nBuild with %C(yellow)%h%C(reset) %C(bold green)%d%C(reset)%n%s %C(red)(%cr) %C(blue)by %an%C(reset)%n"
            )
        }
    }

    private fun retrieveBranch(): String =
        runCommand(listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).lines().first().trim()

    private fun runCommand(command: List<String>) = ByteArrayOutputStream().use { output ->
        execOps.exec {
            commandLine = command
            workingDir = File(System.getProperty("user.dir"))
            standardOutput = output
        }
        output.toString(StandardCharsets.UTF_8.name())
    }

    private fun openHtmlUrl(workflowFile: String, startTime: Instant, title: String) {
        while (true) {
            Thread.sleep(2000)
            println("checking for run id")
            val runs = JSONArray(runCommand(listOf(
                "gh", "run", "list",
                "--workflow=$workflowFile",
                "--event=workflow_dispatch",
                "--json", "databaseId,displayTitle,createdAt"
            )))
            val runId = runs.filterIsInstance<JSONObject>()
                .firstOrNull {
                    Instant.parse(it.getString("createdAt")).isAfter(startTime) &&
                        it.getString("displayTitle") == title
                }?.getLong("databaseId")
            if (runId != null) {
                runCommand(listOf("gh", "run", "view", runId.toString(), "--web"))
                return
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://api.github.com/repos/jvmguard/jvmguard/actions/"
        private val client = HttpClient.newHttpClient()

        private val githubToken: String by lazy { resolveGithubToken() }

        private fun resolveGithubToken(): String {
            System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }?.let { return it }
            val token = runCatching {
                val process = ProcessBuilder("gh", "auth", "token").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText().trim()
                if (process.waitFor() == 0) output else null
            }.getOrNull()
            if (!token.isNullOrEmpty()) {
                return token
            }
            throw IllegalStateException(
                "No GitHub token found. Set the GITHUB_TOKEN environment variable or run 'gh auth login'."
            )
        }

        private fun resolveWorkflowId(workflowFile: String): Int =
            JSONObject(doRequest("workflows/$workflowFile")).getInt("id")

        private fun resolveDefaultBranch(): String =
            JSONObject(doRequest(BASE_URL.removeSuffix("/actions/"))).getString("default_branch")

        fun triggerWorkflow(workflowId: Int, inputs: JSONObject = JSONObject()) {
            doRequest(
                "workflows/${workflowId}/dispatches",
                JSONObject().put("ref", resolveDefaultBranch()).put("inputs", inputs).toString()
            )
        }

        private fun doRequest(url: String, body: String? = null): String {
            @Suppress("UastIncorrectHttpHeaderInspection")
            val request = HttpRequest.newBuilder(URI.create((if (url.startsWith("https:")) "" else BASE_URL) + url))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $githubToken")
                .header("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) {
                request.POST(BodyPublishers.ofString(body))
            }
            val response = client.send(request.build(), HttpResponse.BodyHandlers.ofString())
            if ((response.statusCode() / 100) != 2) {
                throw RuntimeException(response.body())
            }
            return response.body()
        }
    }
}
