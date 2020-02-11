import java.util.Date;

plugins {
  id("edu.sc.seis.version-class") version "1.1.1"
  "java"
  "maven"
  eclipse
  "project-report"
  "signing"
  application
}

application {
  mainClassName = "edu.sc.seis.TauP.TauP"
  applicationName = "taup"
}

group = "edu.sc.seis"
version = "2.4.6-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}


dependencies {
    implementation("edu.sc.seis:seisFile:1.8.0") {
      // we need seisFile for sac output, but not all the other functionality
      exclude("com.martiansoftware:jsap")
      exclude("org.rxtx:rxtx")
      exclude("org.codehaus.woodstox:woodstox-core-lgpl")
      exclude("net.java.dev.msv:msv-core")
      exclude("org.apache.httpcomponents:httpclient")
      exclude("mysql:mysql-connector-java")
    }
    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}


repositories {
    mavenCentral()
}

tasks {
  jar {
      manifest {
        attributes(
            mapOf("Implementation-Title" to project.name,
                  "Implementation-Version" to version,
                  "TauP-Compile-Date" to Date()))
      }
  }
}
val dirName = project.name+"-"+version

val binDistFiles: CopySpec = copySpec {
    from("build/scripts") {
        fileMode = 755
        include("*")
        into("bin")
    }
    from(configurations.default) {
        into("lib")
    }
    from(configurations.default.allArtifacts.files) {
        into("lib")
    }
}

val distFiles: CopySpec = copySpec {
    with(binDistFiles)
    from(".") {
        include("build.gradle")
        include("settings.gradle")
    }
    from("build/docs") {
        include("javadoc/**")
        into("doc")
    }
    from(".") {
        include("gpl-3.0.txt")
        include("doc/**")
        include("jacl/**")
        include("native/**")
        include("src/**")
        include("README")
        exclude("**/*.svn")
    }
    from("srl") {
        include("taup_srl_with_figs.pdf")
        into("doc")
    }
    from(".") {
        include("gradle/**")
        include("gradlew")
        include("gradlew.bat")
    }
    from(".") {
        fileMode=755
        include("gradlew")
        into("gradle")
    }
    from("src/main/resources/edu/sc/seis/TauP") {
        include("defaultProps")
        into("doc")
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
  dependsOn("createRunScripts")
  dependsOn("genModels")
  with( binDistFiles)
  into( file("$buildDir/explode"))
}

tasks.register<Tar>("tarBin") {
  dependsOn("explodeBin")
  compression = Compression.GZIP
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

/*
signing {
    sign configurations.archives
}

if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword") ) {
  uploadArchives {
    repositories {
      mavenDeployer {
        beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }

        repository(url: "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
          authentication(userName: ossrhUsername, password: ossrhPassword)
        }

        snapshotRepository(url: "https://oss.sonatype.org/content/repositories/snapshots/") {
          authentication(userName: ossrhUsername, password: ossrhPassword)
        }

        pom.project {
          name "TauP"
          packaging "jar"
          // optionally artifactId can be defined here
          description "A seismic travel time calculator."
          url "http://www.seis.sc.edu/TauP"

          scm {
            connection "scm:git:https://github.com/crotwell/TauP.git"
            developerConnection "scm:git:https://github.com/crotwell/TauP.git"
            url "https://github.com/crotwell/TauP"
          }

          licenses {
            license {
              name "The GNU General Public License, Version 3"
              url "http://www.gnu.org/licenses/gpl-3.0.html"
            }
          }

          developers {
            developer {
              id "crotwell"
              name "Philip Crotwell"
              email "crotwell@seis.sc.edu"
            }
          }
        }
      }
    }
  }
}
*/

tasks.register("createRunScripts"){}
tasks.named("startScripts") {
    dependsOn("createRunScripts")
}

val scriptNames = mapOf(
    "taup_time" to "edu.sc.seis.TauP.TauP_Time",
    "taup_pierce" to "edu.sc.seis.TauP.TauP_Pierce",
    "taup_path" to "edu.sc.seis.TauP.TauP_Path",
    "taup_create" to "edu.sc.seis.TauP.TauP_Create",
    "taup_curve" to "edu.sc.seis.TauP.TauP_Curve",
    "taup_setsac" to "edu.sc.seis.TauP.TauP_SetSac",
    "taup_wavefront" to "edu.sc.seis.TauP.TauP_Wavefront",
    "taup_table" to "edu.sc.seis.TauP.TauP_Table"
    //"taup" to  "edu.sc.seis.TauP.TauP"
)
for (key in scriptNames.keys) {
  tasks.register<CreateStartScripts>(key) {
    outputDir = file("build/scripts")
    mainClassName = scriptNames[key]
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
  main = "edu.sc.seis.TauP.StdModelGenerator"

  val generatedSrcDir = File(project.buildDir, "generated-src/stdmodels")
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
/*
tasks.register<JavaExec>("genModels") {
  dependsOn("compileJava")
  doLast {
    println("Generate models")
    outDir.mkdirs()
    val inDir = File(getProjectDir(), "src/main/resources/edu/sc/seis/TauP/StdModels/")
    String[] tvelModelNames = ["iasp91", "ak135"]
    String[] ndModelNames = ["prem"]
    val classLoader = new GroovyClassLoader(Project.class.classLoader)
    classLoader.addURL( File(getBuildDir(), "/classes/java/main").toURL())
    configurations.default.each { File file -> classLoader.addURL(file.toURL())}
    val taupCreate = classLoader.loadClass("edu.sc.seis.TauP.TauP_Create").newInstance()
    taupCreate.setDirectory(inDir.getPath())
    taupCreate.setVelFileType("tvel")
    val vMod
    val tMod
    tvelModelNames.each { String model ->
        taupCreate.setModelFilename(model)
        vMod = taupCreate.loadVMod()
        tMod = taupCreate.createTauModel(vMod)
        tMod.writeModel(new File(outDir, model+".taup").path)
    }
    taupCreate.setVelFileType("nd")
    ndModelNames.each { String model ->
        taupCreate.setModelFilename(model)
        vMod = taupCreate.loadVMod()
        tMod = taupCreate.createTauModel(vMod)
        tMod.writeModel(new File(outDir, model+".taup").path)
    }
    // qdt with bigger tol.
    taupCreate.setVelFileType("tvel")
    taupCreate.setMinDeltaP(0.5)
    taupCreate.setMaxDeltaP(50.0)
    taupCreate.setMaxDepthInterval(915.0)
    taupCreate.setMaxRangeInterval(10.0)
    taupCreate.setMaxInterpError(1.0)
    taupCreate.setAllowInnerCoreS(false)
    taupCreate.setModelFilename("iasp91")
    vMod = taupCreate.loadVMod()
    vMod.setModelName("qdt")
    tMod = taupCreate.createTauModel(vMod)
    tMod.writeModel(new File(outDir, "qdt.taup").path)
  }
}

genModels.inputs.files "src/main/resources/edu/sc/seis/TauP/defaultProps"
genModels.inputs.files "src/main/resources/edu/sc/seis/TauP/StdModels/ak135.tvel"
genModels.inputs.files "src/main/resources/edu/sc/seis/TauP/StdModels/iasp91.tvel"
genModels.inputs.files "src/main/resources/edu/sc/seis/TauP/StdModels/prem.nd"
genModels.outputs.files new File(outDir, "ak135.taup")
genModels.outputs.files new File(outDir, "iasp91.taup")
genModels.outputs.files new File(outDir, "prem.taup")
genModels.outputs.files new File(outDir, "qdt.taup")
*/

tasks.get("assemble").dependsOn(tasks.get("tarDist"))

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
