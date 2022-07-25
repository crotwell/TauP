import java.util.Date
import org.gradle.crypto.checksum.Checksum

plugins {
  id("edu.sc.seis.version-class") version "1.2.2"
  id("org.gradle.crypto.checksum") version "1.4.0"
  `java-library`
    `java-library-distribution`
  eclipse
  `project-report`
  `maven-publish`
  signing
  application
  id("com.github.ben-manes.versions") version "0.42.0"
}

application {
  mainClass.set("edu.sc.seis.TauP.ToolRun")
  applicationName = "taup"
}

group = "edu.sc.seis"
version = "2.6.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}
tasks.withType<JavaCompile>().configureEach { options.compilerArgs.addAll(arrayOf("-Xlint:deprecation")) }

dependencies {
    implementation("edu.sc.seis:seisFile:2.0.4") {
      // we need seisFile for sac output, but not all the other functionality
      exclude(group = "info.picocli", module = "picocli")
      exclude(group = "com.fasterxml.woodstox", module = "woodstox-core")
      exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    runtimeOnly( "org.slf4j:slf4j-reload4j:1.7.35")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

}


repositories {
    mavenCentral()
        mavenLocal()
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


val dirName = project.name+"-"+version
val binDirName = project.name+"_bin-"+version

val binDistFiles: CopySpec = copySpec {
    from("build/scripts") {
        include("*")
        into("bin")
    }
    from(tasks.named("jar")) {
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
  dependsOn("startScripts")
  dependsOn("genModels")
    with( binDistFiles)
  into( file("$buildDir/explode"))
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
    into( file("$buildDir/explode"))
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
  files = tasks.getByName("tarBin").outputs.files + tasks.getByName("tarDist").outputs.files + tasks.getByName("zipDist").outputs.files
  outputDir=File(project.buildDir, "distributions")
  algorithm = Checksum.Algorithm.SHA256
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
        url = uri("$buildDir/repos/test-deploy")
      }
      maven {
          val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
          val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
          url = uri(if ( version.toString().toLowerCase().endsWith("snapshot")) snapshotRepo else releaseRepo)
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

val scriptNames = mapOf(
// taup is created via startScripts as default for assemble plugin
//    "taup" to "edu.sc.seis.TauP.ToolRun",
    "taup_time" to "edu.sc.seis.TauP.TauP_Time",
    "taup_pierce" to "edu.sc.seis.TauP.TauP_Pierce",
    "taup_path" to "edu.sc.seis.TauP.TauP_Path",
    "taup_create" to "edu.sc.seis.TauP.TauP_Create",
    "taup_curve" to "edu.sc.seis.TauP.TauP_Curve",
    "taup_setsac" to "edu.sc.seis.TauP.TauP_SetSac",
    "taup_wavefront" to "edu.sc.seis.TauP.TauP_Wavefront",
    "taup_table" to "edu.sc.seis.TauP.TauP_Table"
)
for (key in scriptNames.keys) {
  tasks.register<CreateStartScripts>(key) {
    outputDir = file("build/scripts")
    getMainClass().set(scriptNames[key])
    applicationName = key
    classpath = sourceSets["main"].runtimeClasspath + project.tasks[JavaPlugin.JAR_TASK_NAME].outputs.files
  }
  tasks.named("createRunScripts") {
      dependsOn(key)
  }
}


tasks.register<JavaExec>("genModels") {
  description = "generate TauP default model files"
  classpath = sourceSets.getByName("main").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.StdModelGenerator")

  val generatedSrcDir = File(project.buildDir, "generated-src/StdModels")
  val resourceDir =  File(generatedSrcDir, "/resources")
  val outDir =  File(resourceDir, "edu/sc/seis/TauP/StdModels/")
  outDir.mkdirs()
  val inDir = File(project.projectDir, "src/main/resources/edu/sc/seis/TauP/StdModels/")

  args = listOf(inDir.path, outDir.path)
  dependsOn += tasks.getByName("compileJava")
  inputs.files("src/main/resources/edu/sc/seis/TauP/defaultProps")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135.tvel")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/iasp91.tvel")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/prem.nd")
  outputs.files(File(outDir, "ak135.taup"))
  outputs.files(File(outDir, "iasp91.taup"))
  outputs.files(File(outDir, "prem.taup"))
  outputs.files(File(outDir, "qdt.taup"))
}

tasks.register<JavaExec>("genCmdLineTestFiles") {
    description = "generate TauP cmd line test output files"
    classpath = sourceSets.getByName("test").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.CmdLineOutputTest")
    dependsOn += tasks.getByName("testClasses")
    outputs.files(fileTree("cmdLineTest"))
}
tasks.register<Sync>("copyCmdLineTestFiles") {
  from(tasks.getByName("genCmdLineTestFiles").outputs)
  into("src/test/resources/edu/sc/seis/TauP/cmdLineTest")
  dependsOn("genCmdLineTestFiles")
}

tasks.get("assemble").dependsOn(tasks.get("dependencyUpdates"))

// note can pass password for signing in with -Psigning.password=secret
tasks.get("assemble").dependsOn(tasks.get("signTarBin"))
tasks.get("assemble").dependsOn(tasks.get("signTarDist"))
tasks.get("assemble").dependsOn(tasks.get("signZipDist"))
tasks.get("signTarBin").dependsOn(tasks.get("checksumDist"))
tasks.get("signTarDist").dependsOn(tasks.get("checksumDist"))
tasks.get("signZipDist").dependsOn(tasks.get("checksumDist"))

val generatedSrcDir = file("$buildDir/generated-src/StdModels")
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

// this is really dumb, but gradle wants something....
gradle.taskGraph.whenReady {
    allTasks
        .filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
        .forEach {
            it.setProperty("duplicatesStrategy", "EXCLUDE")
        }
}
