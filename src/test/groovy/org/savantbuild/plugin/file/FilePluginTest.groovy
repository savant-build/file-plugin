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
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertTrue

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

    plugin = new FilePlugin(project, output)
  }

  @Test
  public void copyDirectoryToDirectoryWithPaths() throws Exception {
    FileTools.prune projectDir.resolve("build/test/copy")
    plugin.copy {
      to Paths.get("build/test/copy")
      fileSet Paths.get("src/main/groovy")
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/org/savantbuild/plugin/file/FilePlugin.groovy")))
  }

  @Test
  public void copyDirectoryToDirectoryWithStrings() throws Exception {
    FileTools.prune projectDir.resolve("build/test/copy")
    plugin.copy {
      to "build/test/copy"
      fileSet "src/main/groovy"
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/org/savantbuild/plugin/file/FilePlugin.groovy")))
  }

  @Test
  public void jarWithPaths() throws Exception {
    FileTools.prune projectDir.resolve("build/test/jar")
    plugin.jar(Paths.get("build/test/jar/test.jar")) {
      fileSet Paths.get("build/classes/main")
    }

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class")
  }

  @Test
  public void jarWithStrings() throws Exception {
    FileTools.prune projectDir.resolve("build/test/jar")
    plugin.jar("build/test/jar/test.jar") {
      fileSet "build/classes/main"
    }

    assertJarContains(projectDir.resolve("build/test/jar/test.jar"), "org/savantbuild/plugin/file/FilePlugin.class")
  }

  private static void assertJarContains(Path jarFile, String... entries) {
    JarFile jf = new JarFile(jarFile.toFile())
    entries.each({ entry -> assertNotNull(jf.getEntry(entry), "Jar [${jarFile}] is missing entry [${entry}]") })
    jf.close()
  }
}
