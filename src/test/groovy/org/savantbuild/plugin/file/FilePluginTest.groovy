/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.file

import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Version
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest

import static org.testng.Assert.*

/**
 * Tests the FilePlugin class.
 *
 * @author Brian Pontarelli
 */
class FilePluginTest {
  public static Path projectDir

  Output output

  Project project

  FilePlugin plugin

  @BeforeSuite
  public static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../file-plugin")
    }
  }

  @BeforeMethod
  public void beforeMethod() {
    output = new SystemOutOutput(true)
    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "file-plugin-test"
    project.version = new Version("1.0")
    project.license = License.Apachev2

    plugin = new FilePlugin(project, new RuntimeConfiguration(), output)
  }

  @Test
  public void copyDirectoryToDirectoryWithPaths() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/copy"))
    plugin.copy(to: Paths.get("build/test/copy")) {
      fileSet(dir: Paths.get("src/main/groovy"))
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/org/savantbuild/plugin/file/FilePlugin.groovy")))
  }

  @Test
  public void copyWithIncludePatterns() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/copy"))
    plugin.copy(to: Paths.get("build/test/copy")) {
      fileSet(dir: Paths.get("src/main/groovy"), includePatterns: [/.*\/file\/.*/])
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/org/savantbuild/plugin/file/FilePlugin.groovy")))
  }

  @Test
  public void copyDirectoryToDirectoryWithStrings() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/copy"))
    plugin.copy(to: "build/test/copy") {
      fileSet(dir: "src/main/groovy")
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/org/savantbuild/plugin/file/FilePlugin.groovy")))
  }

  @Test
  public void jarWithPaths() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/jar"))
    plugin.jar(file: Paths.get("build/test/jar/test.jar")) {
      fileSet(dir: Paths.get("build/classes/main"))
    }

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class")
    assertJarFileEquals(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", projectDir.resolve("build/classes/main/org/savantbuild/plugin/file/FilePlugin.class"))
  }

  @Test
  public void jarWithStrings() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/jar"))
    plugin.jar(file: "build/test/jar/test.jar") {
      fileSet(dir: "build/classes/main")
    }

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class")
    assertJarFileEquals(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", projectDir.resolve("build/classes/main/org/savantbuild/plugin/file/FilePlugin.class"))
  }

  @Test
  public void jarManifestFile() throws Exception {
    println "Start file"
    FileTools.prune(projectDir.resolve("build/test/jar"))
    plugin.jar(file: "build/test/jar/test.jar") {
      fileSet(dir: "build/classes/main")
      manifest(file: "src/test/resources/MANIFEST.MF")
    }

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", "META-INF/MANIFEST.MF")
    assertJarFileEquals(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", projectDir.resolve("build/classes/main/org/savantbuild/plugin/file/FilePlugin.class"))
    assertJarManifest(projectDir.resolve("build/test/jar/test.jar"), new Manifest(Files.newInputStream(projectDir.resolve("src/test/resources/MANIFEST.MF"))))
    println "End file"
  }

  @Test
  public void jarManifestMap() throws Exception {
    println "Start map"
    FileTools.prune(projectDir.resolve("build/test/jar"))
    println "Exists ${Files.isRegularFile(projectDir.resolve("build/test/jar/test.jar"))}"
    plugin.jar(file: "build/test/jar/test.jar") {
      fileSet(dir: "build/classes/main")
      manifest(map: [
          "Specification-Version": "1.0",
          "Specification-Vendor" : "FooBar",
      ])
    }
    println "Called"

    Manifest expected = new Manifest()
    expected.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
    expected.getMainAttributes().put(Attributes.Name.SPECIFICATION_VERSION, "1.0")
    expected.getMainAttributes().put(Attributes.Name.SPECIFICATION_VENDOR, "FooBar")

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", "META-INF/MANIFEST.MF")
    assertJarFileEquals(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class", projectDir.resolve("build/classes/main/org/savantbuild/plugin/file/FilePlugin.class"))
    assertJarManifest(projectDir.resolve("build/test/jar/test.jar"), expected)
    println "End map"
  }

  @Test
  public void mkdir() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/dir"))
    assertFalse(Files.isDirectory(projectDir.resolve("build/test/dir")))

    plugin.mkdir(dir: "build/test/dir")
    assertTrue(Files.isDirectory(projectDir.resolve("build/test/dir")))
  }

  @Test
  public void tarCompressed() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/tar"))
    plugin.tar(file: Paths.get("build/test/tar/test.tar.gz"), compress: true) {
      fileSet(dir: Paths.get("build/classes/main"))
      tarFileSet(prefix: "testing123", dir: Paths.get("build/classes/test"))
    }

    Process process = "gunzip test.tar.gz".execute([], projectDir.resolve("build/test/tar").toFile())
    process.consumeProcessOutput()
    process.waitFor()

    assertEquals(process.exitValue(), 0)

    process = "tar -xvf test.tar".execute([], projectDir.resolve("build/test/tar").toFile())
    process.consumeProcessOutput()
    process.waitFor()

    assertEquals(process.exitValue(), 0)
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/org/savantbuild/plugin/file/FilePlugin.class")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/testing123/org/savantbuild/plugin/file/FilePluginTest.class")))
  }

  @Test
  public void tarWithPaths() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/tar"))
    plugin.tar(file: Paths.get("build/test/tar/test.tar")) {
      fileSet(dir: Paths.get("build/classes/main"))
      tarFileSet(prefix: "testing123", dir: Paths.get("build/classes/test"))
    }

    Process process = "tar -xvf test.tar".execute([], projectDir.resolve("build/test/tar").toFile())
    process.consumeProcessOutput()
    process.waitFor()

    assertEquals(process.exitValue(), 0)
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/org/savantbuild/plugin/file/FilePlugin.class")))
    assertTrue(Files.isReadable(projectDir.resolve("build/test/tar/org/savantbuild/plugin/file/FilePlugin.class")))
    assertTrue(Files.isWritable(projectDir.resolve("build/test/tar/org/savantbuild/plugin/file/FilePlugin.class")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/testing123/org/savantbuild/plugin/file/FilePluginTest.class")))
    assertTrue(Files.isReadable(projectDir.resolve("build/test/tar/testing123/org/savantbuild/plugin/file/FilePluginTest.class")))
    assertTrue(Files.isWritable(projectDir.resolve("build/test/tar/testing123/org/savantbuild/plugin/file/FilePluginTest.class")))
  }

  @Test
  public void tarWithStrings() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/tar"))
    plugin.tar(file: "build/test/tar/test.tar") {
      fileSet(dir: "build/classes/main")
      tarFileSet(prefix: "testing123", dir: "build/classes/test")
    }

    Process process = "tar -xvf test.tar".execute([], projectDir.resolve("build/test/tar").toFile())
    process.consumeProcessOutput()
    process.waitFor()

    assertEquals(process.exitValue(), 0)
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/org/savantbuild/plugin/file/FilePlugin.class")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/tar/testing123/org/savantbuild/plugin/file/FilePluginTest.class")))
  }

  private static void assertJarContains(Path jarFile, String... entries) {
    JarFile jf = new JarFile(jarFile.toFile())
    entries.each({ entry -> assertNotNull(jf.getEntry(entry), "Jar [${jarFile}] is missing entry [${entry}]") })
    jf.close()
  }

  private static void assertJarFileEquals(Path jarFile, String entry, Path original) throws IOException {
    JarInputStream jis = new JarInputStream(Files.newInputStream(jarFile))
    JarEntry jarEntry = jis.getNextJarEntry()
    while (jarEntry != null && !jarEntry.getName().equals(entry)) {
      jarEntry = jis.getNextJarEntry()
    }

    if (jarEntry == null) {
      fail("Jar [" + jarFile + "] is missing entry [" + entry + "]")
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    byte[] buf = new byte[1024]
    int length
    while ((length = jis.read(buf)) != -1) {
      baos.write(buf, 0, length)
    }

    assertEquals(Files.readAllBytes(original), baos.toByteArray())
    assertEquals(jarEntry.getSize(), Files.size(original))
    assertEquals(jarEntry.getCreationTime(), Files.getAttribute(original, "creationTime"))
    jis.close()
  }

  private static void assertJarManifest(Path jarFile, Manifest expected) throws IOException {
    JarFile jf = new JarFile(jarFile.toFile())
    Manifest actual = jf.getManifest()
    println "jarFile ${jarFile} mf ${actual.getMainAttributes()}"
    expected.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0.0")
    expected.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "org.savantbuild.test.file-plugin-test")

    assertEquals(actual.getMainAttributes(), expected.getMainAttributes(), "Actual " + actual.getMainAttributes() + " expected " + expected.getMainAttributes());
    jf.close()
  }
}
