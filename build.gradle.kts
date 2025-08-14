import java.util.Date
import org.gradle.crypto.checksum.Checksum
import org.jreleaser.model.Active
import org.jreleaser.model.Distribution

plugins {
  id("edu.sc.seis.version-class") version "1.4.1"
  id("org.gradle.crypto.checksum") version "1.4.0"
  `java-library`
  `java-library-distribution`
  `project-report`
  `maven-publish`
  signing
  application
  id("com.github.ben-manes.versions") version "0.52.0"
  id("org.jreleaser") version "1.19.0"
}

application {
  mainClass.set("edu.sc.seis.TauP.cmdline.ToolRun")
  applicationName = "taup"
  //applicationName = "taupdev"
  //
  // below to address undertow, jboss-threads warning:
  // WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
  // WARNING: sun.misc.Unsafe::objectFieldOffset has been called by org.jboss.threads.JBossExecutors
  // can be removed if/when undertow upgrades dependency
  // but this jvm arg not available on java11 or 17, so...
  // applicationDefaultJvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
}

group = "edu.sc.seis"
version = "3.1.0"
val zenodo_rel_id = "10794858"
val doifile = "src/doc/sphinx/source/zenodo_id_num.txt"

jreleaser {
  dryrun.set(true)
  project {
    description.set("The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities")
    authors.add("Philip Crotwell")
    license.set("LGPL-3.0")
    links {
        homepage.set("https://github.com/crotwell/TauP")
    }
    inceptionYear.set("1999")
  }

  release {
      github {
          repoOwner.set("crotwell")
          overwrite.set(true)
      }
  }
  distributions {
      create("taup") {
        distributionType.set(Distribution.DistributionType.JAVA_BINARY)
         artifact {
             path.set(file("build/distributions/{{distributionName}}-{{projectVersion}}.zip"))
         }
         artifact {
             path.set(file("build/distributions/{{distributionName}}-{{projectVersion}}.tar"))
         }
      }
  }
  packagers {
    brew {
      active.set(Active.ALWAYS)
    }
    docker {
          active.set(Active.ALWAYS)
          postCommands.add("EXPOSE 7409")
    }
    snap {
          active.set(Active.ALWAYS)
          grade.set("devel")
          remoteBuild.set(true)

    }
  }
  signing {
    setActive("ALWAYS")
    armored.set(true)
  }
  deploy {
    maven {
      mavenCentral {
        create("sonatype") {
          setActive("ALWAYS")
          url= "https://central.sonatype.com/api/v1/publisher"
          stagingRepository("build/staging-deploy")
        }
      }
    }
  }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.register("versionToVersionFile") {
  inputs.files("build.gradle.kts")
  outputs.files("VERSION")
  File("VERSION").writeText(""+version)
}

tasks.register("zenodoDoi") {
  // value read by sphinx from file in conf.py

  inputs.files("build.gradle.kts")
  outputs.files(doifile)
  File(doifile).writeText(""+zenodo_rel_id)
}

distributions {
  main {
    distributionBaseName = "TauP"
    contents {
      from(".") {
          include("CITATION.cff")
          include("LICENSE")
          include("README.md")
      }
      from(tasks.named("versionToVersionFile")) {
        into(".")
      }
      from("docs") {
        into("docs")
      }
      from(tasks.named("javadoc")) {
          into("docs/javadoc")
      }
      from(".") {
          include("build.gradle.kts")
          include("settings.gradle.kts")
      }
      from(".") {
          include("src/**")
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
  }
}

tasks.withType<Tar>() {
    compression=Compression.GZIP
    archiveExtension="tgz"
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(arrayOf("-Xlint:deprecation"))
    // for picocli
    options.compilerArgs.addAll(arrayOf("-Aproject=${project.group}/${project.name}"))
}


sourceSets {
    create("example") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

java {
    registerFeature("example") {
        usingSourceSet(sourceSets["example"])
    }
}

dependencies {
    implementation("org.json:json:20250517")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("edu.sc.seis:seisFile:2.3.1")
    //implementation("edu.sc.seis:seisFile:2.3.1-SNAPSHOT")
    implementation("edu.sc.seis:seedCodec:1.2.0")

    // temporary use modified picocli to allow sort of ArgGroup options
    // see src/main/java/picocli
    //implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    implementation("org.slf4j:slf4j-reload4j:2.0.6")


    implementation("io.undertow:undertow-core:2.3.18.Final")

        // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.1")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}


repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri(layout.buildDirectory.dir("staging-deploy"))
    }
}

tasks {
  jar {
      manifest {
        attributes(
            mapOf("Automatic-Module-Name" to "edu.sc.seis.TauP",
                  "Implementation-Title" to project.name,
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

tasks.named("makeVersionClass") {
  inputs.files("src/main/")
  inputs.files("build.gradle.kts")
  mustRunAfter("copyJavascriptResources")
}

tasks.register<Checksum>("checksumDist") {
  dependsOn("distZip")
  dependsOn("distTar")
  inputs.files(tasks.getByName("distTar").outputs.files)
  inputs.files(tasks.getByName("distZip").outputs.files)
  outputs.dir(layout.buildDirectory.dir("distributions"))
  checksumAlgorithm.set(Checksum.Algorithm.SHA512)
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
        url = uri(layout.buildDirectory.dir("staging-deploy"))
      }
    }

}

signing {
    sign(publishing.publications["mavenJava"])
    sign(tasks.getByName("distTar"))
    sign(tasks.getByName("distZip"))
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
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135fsyngine.nd")
  inputs.files("src/main/resources/edu/sc/seis/TauP/StdModels/ak135fcont.nd")
  outputs.files(layout.buildDirectory.file(outDirStr+"/ak135.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/iasp91.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/prem.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/ak135favg.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/ak135fsyngine.taup"))
    outputs.files(layout.buildDirectory.file(outDirStr+"/ak135fcont.taup"))
}


tasks.register<JavaExec>("genAk135Plots") {
    description = "generate TauP AK135 compare plots"
    classpath = sourceSets.getByName("test").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.AK135Test")
    dependsOn += tasks.getByName("classes")
    dependsOn += tasks.getByName("testClasses")
    outputs.files(fileTree("build/ak135Compare"))
}
tasks.register<Sync>("copyReflTranCompareFiles") {
  from("src/test/resources/edu/sc/seis/TauP/cmdLineTest/refltranCompare")
  into("build/cmdLineTest/refltranCompare")
}
tasks.register<JavaExec>("genCmdLineTestFiles") {
    description = "generate TauP cmd line test output files"
    classpath = sourceSets.getByName("test").runtimeClasspath
    getMainClass().set("edu.sc.seis.TauP.cmdline.CmdLineOutputTest")
    dependsOn += tasks.getByName("classes")
    dependsOn += tasks.getByName("testClasses")
    dependsOn += tasks.getByName("copyReflTranCompareFiles")
    dependsOn += tasks.getByName("genAk135Plots")
    outputs.files(fileTree("build/cmdLineTest"))
}
tasks.register<Sync>("copyCmdLineTestFiles") {
  from(tasks.getByName("genCmdLineTestFiles").outputs)
  into("src/test/resources/edu/sc/seis/TauP/cmdLineTest")
  dependsOn("genCmdLineTestFiles")
}
tasks.get("test").mustRunAfter("copyCmdLineTestFiles")
tasks.get("jar").mustRunAfter("copyCmdLineTestFiles")
tasks.get("distTar").mustRunAfter("copyCmdLineTestFiles")
tasks.get("distZip").mustRunAfter("copyCmdLineTestFiles")

tasks.register<JavaExec>("genCmdLineHelpFiles") {
  inputs.files("build.gradle.kts") // for version.json
  description = "generate TauP cmd line help output files"
  classpath = sourceSets.getByName("test").runtimeClasspath
  getMainClass().set("edu.sc.seis.TauP.cmdline.GenCmdLineUsage")
  dependsOn += tasks.getByName("testClasses")
  outputs.files(fileTree("build/cmdLineHelp"))
}
tasks.register<Sync>("copyCmdLineHelpFiles") {
  from(tasks.getByName("genCmdLineHelpFiles").outputs)
  into("src/doc/sphinx/source/cmdLineHelp")
  dependsOn("genCmdLineHelpFiles")
}

tasks.register<Sync>("copyProgramExampleFiles") {
  from("src/example/java/edu/sc/seis/example/TimeExample.java")
  from("src/example/python/grab_taup_times.py")
  from("src/example/python/grab_taup_times_http.py")
  into("src/doc/sphinx/source/programming")
}

tasks.register<Sync>("copyStdModelsToSphinx") {
  from("src/main/resources/edu/sc/seis/TauP/StdModels") {
      include("*.tvel")
      include("*.nd")
  }
  into("src/doc/sphinx/source/_static/StdModels")
}

tasks.register<JavaExec>("genAutocomplete") {
  description = "generate TauP cmd line help output files"
  dependsOn += tasks.getByName("classes")
  classpath = sourceSets.getByName("main").runtimeClasspath
  getMainClass().set("picocli.AutoComplete")
  args = listOf("edu.sc.seis.TauP.cmdline.ToolRun", "--force")
  dependsOn += tasks.getByName("compileJava")
  workingDir = File("build/autocomplete")
  outputs.files(fileTree("build/autocomplete"))
}
distributions {
  main {
    contents {
      from(tasks.named("genAutocomplete")) {
        into(".")
      }
    }
  }
}

tasks.register<Exec>("sphinxMakeHtml") {
  workingDir("src/doc/sphinx")
  commandLine("make", "html")
  inputs.files(tasks.named("zenodoDoi"))
  inputs.files(fileTree(project.file("src/doc/sphinx")))
  outputs.files(fileTree(layout.buildDirectory.dir("sphinx/html")))
  dependsOn("copyProgramExampleFiles")
  dependsOn("copyCmdLineHelpFiles")
  dependsOn("copyStdModelsToSphinx")
}
tasks.register<Sync>("copySphinxToDocs") {
  from(tasks.named("sphinxMakeHtml"))
  into("docs/manual")
  dependsOn("sphinxMakeHtml")
  exclude("_sources")
  exclude(".buildinfo")
}
tasks.register("sphinx") {
  dependsOn("sphinxMakeHtml")
  dependsOn("copySphinxToDocs")
}

tasks.get("assemble").dependsOn(tasks.get("dependencyUpdates"))

// note can pass password for signing in with -Psigning.password=secret
tasks.get("assemble").dependsOn(tasks.get("signDistZip"))
tasks.get("assemble").dependsOn(tasks.get("signDistTar"))
tasks.get("signDistTar").dependsOn(tasks.get("checksumDist"))
tasks.get("signDistZip").dependsOn(tasks.get("checksumDist"))

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
    from("docs/manual") {
      into("edu/sc/seis/TauP/html/doc")
    }
    from("src/doc/favicon") {
      into("edu/sc/seis/TauP/html")
    }
    mustRunAfter("sphinx")
}


tasks.withType<Javadoc>() {
  exclude("src/main/picocli/CommandLine.java")
  exclude("src/main/picocli/AutoComplete.java")
  //(options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.register<Exec>("createJavascriptResources") {
  workingDir("src/web")
  commandLine("npm", "run", "esstandalone")
  outputs.files(//fileTree("src/web/dist"),
    file("src/web/node_modules/sortable-tablesort/dist/sortable.js"),
    file("src/web/node_modules/sortable-tablesort/dist/sortable.css"))
  inputs.file("src/web/package.json")
}
tasks.register<Sync>("copyJavascriptResources") {
  from(tasks.named("createJavascriptResources"))
  into("src/main/resources/edu/sc/seis/TauP/html/js")
  exclude("_sources")
  exclude(".buildinfo")
}
tasks.get("processResources").mustRunAfter("copyJavascriptResources")


tasks.get("installDist").mustRunAfter("sphinx")
tasks.get("installDist").mustRunAfter("copyCmdLineHelpFiles")
tasks.get("installDist").mustRunAfter("copyStdModelsToSphinx")
tasks.get("installDist").mustRunAfter("copyProgramExampleFiles")
tasks.get("publish").dependsOn("assemble")
tasks.get("publish").dependsOn("installDist")


tasks.register<JavaExec>("genPythonBindings") {
  description = "generate TauP python binding files"
  dependsOn += tasks.getByName("classes")
  classpath = sourceSets.getByName("main").runtimeClasspath
  getMainClass().set("edu.sc.seis.TauP.cmdline.PythonBindings")
  args = listOf("edu.sc.seis.TauP.cmdline.Time")
  dependsOn += tasks.getByName("compileJava")
  workingDir = File("build/python")
  outputs.files(fileTree("build/python"))
}


// this is really dumb, but gradle wants something....
gradle.taskGraph.whenReady {
    allTasks
        .filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
        .forEach {
            it.setProperty("duplicatesStrategy", "EXCLUDE")
        }
}
