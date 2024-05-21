import java.util.Date
import org.gradle.crypto.checksum.Checksum

plugins {
  id("edu.sc.seis.version-class") version "1.3.0"
  id("org.gradle.crypto.checksum") version "1.4.0"
  `java-library`
    `java-library-distribution`
  eclipse
  `project-report`
  `maven-publish`
  signing
  application
  id("com.github.ben-manes.versions") version "0.51.0"
}

application {
  mainClass.set("edu.sc.seis.TauP.ToolRun")
    applicationName = "taup"
    //applicationName = "taupdev"
}

group = "edu.sc.seis"
version = "3.0.0-SNAPSHOT5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(arrayOf("-Xlint:deprecation"))
    // for picocli
    options.compilerArgs.addAll(arrayOf("-Aproject=${project.group}/${project.name}"))
}


sourceSets {
    create("webserver") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

java {
    registerFeature("webserver") {
        usingSourceSet(sourceSets.main.get())
    }
}

val webserverImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
dependencies {

    implementation("org.json:json:20240303")
    //implementation("edu.sc.seis:seisFile:2.1.0-SNAPSHOT4") {
    // or
    implementation("edu.sc.seis:seisFile:2.1.0") {
      // we need seisFile for sac/mseed3 output, but not all the other functionality
      exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    runtimeOnly("org.slf4j:slf4j-reload4j:2.0.5")

    webserverImplementation("io.undertow:undertow-core:2.3.13.Final")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

}


repositories {
    mavenCentral()
        mavenLocal()
    maven {
        name = "oss.sonatype.org snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

tasks {
  jar {
      manifest {
        attributes(
            mapOf("Implementation-Title" to project.name,
                  "Implementation-Version" to archiveVersion,
                  "TauP-Compile-Date" to Date()))
      }
  }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


tasks.named("sourcesJar") {
    dependsOn("makeVersionClass")
}


val dirName = project.name+"-"+version
val binDirName = project.name+"_bin-"+version

tasks.register<Sync>("webserverSphinxDocs") {
  from("src/doc/sphinx/build/html")
  into("src/webserver/resources/edu/sc/seis/webtaup/html/doc")
}

tasks.register<Jar>("webserverJar") {
    dependsOn("webserverClasses" )
    from(sourceSets["main"].output)
    from(sourceSets["webserver"].output)
    from("src/doc/sphinx/build/html") {
      into("edu/sc/seis/webtaup/html/doc")
    }
    archiveBaseName.set("taup_webserver")
}


val binDistFiles: CopySpec = copySpec {
    from("build/scripts") {
        include("*")
        into("bin")
    }
    from(tasks.named("jar")) {
        into("lib")
    }
    from(tasks.named("webserverJar")) {
        into("lib")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
    }
    /*
    // don't think this is needed...
    from(configurations.runtimeClasspath.get().allArtifacts.files) {
        into("lib")
    }
     */
    from("build/picocli/bash_completion") {
        include("taup_completion")
    }
}

val distFiles: CopySpec = copySpec {
    with(binDistFiles)
    from(".") {
        include("build.gradle.kts")
        include("settings.gradle.kts")
    }
    from("build/docs") {
        include("javadoc/**")
        into("docs")
    }
    from("docs") {
      include("*")
      into("docs")
    }
    from(".") {
        include("VERSION")
        include("CITATION.cff")
        include("LICENSE")
        include("jacl/**")
        include("groovy/**")
        include("native/**")
        include("src/**")
        include("README.md")
        exclude("**/*.svn")
    }
    from(".") {
        include("gradle/**")
        include("gradlew")
        include("gradlew.bat")
    }
    from("src/main/resources/edu/sc/seis/TauP") {
        include("defaultProps")
        into("docs")
    }
    from("src/main/resources/edu/sc/seis/TauP") {
        include("StdModels/*.tvel")
        include("StdModels/*.nd")
    }
    from("build/generated-src/modVersion") {
        include("java/**")
        into("src/main")
    }
}

tasks.register<Sync>("explodeBin") {
  dependsOn("jar")
    dependsOn("webserverJar")
  dependsOn("startScripts")
  dependsOn("genModels")
    with( binDistFiles)
  into( layout.buildDirectory.dir("explode"))
}

tasks.register<Tar>("tarBin") {
  archiveAppendix.set("bin")
  dependsOn("explodeBin")
  compression = Compression.GZIP
  into(dirName) {
      with( binDistFiles)
  }
}
tasks.register<Zip>("zipBin") {
  archiveAppendix.set("bin")
  dependsOn("explodeBin")
    into(dirName) {
        with( binDistFiles)
    }
}

tasks.register<Sync>("explodeDist") {
  dependsOn("explodeBin")
  dependsOn("javadoc")
  dependsOn("wrapper")
    with(distFiles)
    into( layout.buildDirectory.dir("explode"))
}

tasks.register<Tar>("tarDist") {
  dependsOn("explodeDist")
    compression = Compression.GZIP
    into(dirName) {
        with( distFiles)
    }
}


tasks.register<Checksum>("checksumDist") {
  dependsOn("tarBin")
  dependsOn("tarDist")
  dependsOn("zipDist")
  dependsOn("distZip")
  inputs.files(tasks.getByName("tarBin").outputs.files)
    inputs.files(tasks.getByName("tarDist").outputs.files)
    inputs.files(tasks.getByName("zipDist").outputs.files)
    outputs.dir(layout.buildDirectory.dir("distributions"))
  algorithm = Checksum.Algorithm.SHA512
}

tasks.register<Zip>("zipDist") {
  dependsOn("explodeDist")
    into(dirName) {
        with( distFiles)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
              name.set("TauP")
              description.set("Flexible Seismic Travel-Time and Raypath Utilities.")
              url.set("https://www.seis.sc.edu/TauP/")

              scm {
                connection.set("scm:git:https://github.com/crotwell/TauP.git")
                developerConnection.set("scm:git:https://github.com/crotwell/TauP.git")
                url.set("https://github.com/crotwell/TauP")
              }

              licenses {
                license {
                  name.set("GNU Lesser General Public License, Version 3")
                  url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                }
              }

              developers {
                developer {
                  id.set("crotwell")
                  name.set("Philip Crotwell")
                  email.set("crotwell@seis.sc.edu")
                }
              }
            }
        }
    }
    repositories {
      maven {
        name = "TestDeploy"
        url = uri(layout.buildDirectory.dir("repos/test-deploy"))
      }
      maven {
          val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
          val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
          url = uri(if ( version.toString().lowercase().endsWith("snapshot")) snapshotRepo else releaseRepo)
          name = "ossrh"
          // credentials in gradle.properties as ossrhUsername and ossrhPassword
          credentials(PasswordCredentials::class)
      }
    }

}

signing {
    sign(publishing.publications["mavenJava"])
    sign(tasks.getByName("tarDist"))
    sign(tasks.getByName("zipDist"))
    sign(tasks.getByName("tarBin"))
}

tasks.register("createRunScripts"){}
tasks.named("startScripts") {
    dependsOn("createRunScripts")
}

tasks.register<CreateStartScripts>("taup_web") {
    dependsOn(tasks.named("webserverJar"))
    outputDir = file("build/scripts")
    mainClass.set("edu.sc.seis.webtaup.TauP_Web")
    applicationName = "taup_web"
    classpath = sourceSets["webserver"].runtimeClasspath +
            project.tasks[JavaPlugin.JAR_TASK_NAME].outputs.files +
            project.tasks["webserverJar"].outputs.files
}
tasks.named("createRunScripts") {
    dependsOn("taup_web")
}

tasks.register<JavaExec>("genModels") {
  description = "generate TauP default model files"
  classpath = sourceSets.getByName("main").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.StdModelGenerator")

  val generatedSrcDir = "generated-src/StdModels"
  val resourceDir =  generatedSrcDir+ "/resources"
    val outDirStr =  resourceDir+ "/edu/sc/seis/TauP/StdModels/"
  val outDir =  layout.buildDirectory.dir(resourceDir+ "/edu/sc/seis/TauP/StdModels/")
  file(outDir).mkdirs()
  val inDirStr = "src/main/resources/edu/sc/seis/TauP/StdModels/"
  val inDir = layout.projectDirectory.dir(inDirStr)

  args = listOf(file(inDir).getPath(), file(outDir).getPath())
  dependsOn += tasks.getByName("compileJava")
  inputs.files("src/main/resources/edu/sc/seis/TauP/defaultProps")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135.tvel")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/iasp91.tvel")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/prem.nd")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135favg.nd")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135fcont.nd")
  outputs.files(layout.buildDirectory.file(outDirStr+"/ak135.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/iasp91.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/prem.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/qdt.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/ak135favg.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/ak135fcont.taup"))
}

tasks.register<Sync>("copyReflTranCompareFiles") {
  from("src/test/resources/edu/sc/seis/TauP/cmdLineTest/refltranCompare")
  into("build/cmdLineTest/refltranCompare")
}
tasks.register<JavaExec>("genCmdLineTestFiles") {
    description = "generate TauP cmd line test output files"
    classpath = sourceSets.getByName("test").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.CmdLineOutputTest")
    dependsOn += tasks.getByName("testClasses")
    dependsOn += tasks.getByName("copyReflTranCompareFiles")
    outputs.files(fileTree("build/cmdLineTest"))
    doFirst {

    }
}
tasks.register<Sync>("copyCmdLineTestFiles") {
  from(tasks.getByName("genCmdLineTestFiles").outputs)
  into("src/test/resources/edu/sc/seis/TauP/cmdLineTest")
  dependsOn("genCmdLineTestFiles")
}

tasks.register<JavaExec>("genCmdLineHelpFiles") {
  description = "generate TauP cmd line help output files"
  classpath = sourceSets["main"].runtimeClasspath + project.tasks[JavaPlugin.JAR_TASK_NAME].outputs.files
  getMainClass().set("edu.sc.seis.TauP.ToolRun")
  args = listOf( "--getcmdlinehelpfiles")
  dependsOn += tasks.getByName("classes")
  outputs.files(fileTree("src/doc/sphinx/source/cmdLineHelp"))
}

tasks.get("assemble").dependsOn(tasks.get("dependencyUpdates"))

// note can pass password for signing in with -Psigning.password=secret
tasks.get("assemble").dependsOn(tasks.get("signTarBin"))
tasks.get("assemble").dependsOn(tasks.get("signTarDist"))
tasks.get("assemble").dependsOn(tasks.get("signZipDist"))
tasks.get("signTarBin").dependsOn(tasks.get("checksumDist"))
tasks.get("signTarDist").dependsOn(tasks.get("checksumDist"))
tasks.get("signZipDist").dependsOn(tasks.get("checksumDist"))

val generatedSrcDir = file(layout.buildDirectory.dir("generated-src/StdModels"))
val resourceDir =  File(generatedSrcDir, "/resources")
val outDir =  File(resourceDir, "edu/sc/seis/TauP/StdModels/")
sourceSets.create("stdmodels").resources {
    srcDir(resourceDir)
}

sourceSets {
    getByName("main") {
       resources.srcDirs += resourceDir
    }
}
tasks.get("processStdmodelsResources").dependsOn("genModels")
tasks.jar {
    dependsOn("processStdmodelsResources")
    from(sourceSets["stdmodels"].output)
}

tasks.register("versionToVersionFile") {
  inputs.files("build.gradle.kts")
  outputs.files("VERSION")
  File("VERSION").writeText(""+version)
}
tasks.get("assemble").dependsOn("versionToVersionFile")


tasks.register<JavaExec>("genAutocomplete") {
  description = "generate TauP bash Autocomplete file"
  classpath = sourceSets.getByName("main").runtimeClasspath
  getMainClass().set("picocli.AutoComplete")
  val outDir =  layout.buildDirectory.dir("picocli/bash_completion")
  file(outDir).mkdirs()
  val outFile = File(file(outDir), "taup_completion")
  args = listOf("edu.sc.seis.TauP.ToolRun", "-f", "-o", outFile.path)
  dependsOn += tasks.getByName("compileJava")
  outputs.files(outFile)
}
tasks.get("explodeBin").dependsOn(tasks.get("genAutocomplete"))

// this is really dumb, but gradle wants something....
gradle.taskGraph.whenReady {
    allTasks
        .filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
        .forEach {
            it.setProperty("duplicatesStrategy", "EXCLUDE")
        }
}
