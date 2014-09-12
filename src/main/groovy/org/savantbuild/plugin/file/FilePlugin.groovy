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

import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.BuildFailureException
import org.savantbuild.runtime.RuntimeConfiguration
import org.savantbuild.util.tar.TarTools
import org.savantbuild.util.zip.ZipTools

import java.nio.file.Files
import java.nio.file.Path

/**
 * File plugin.
 *
 * @author Brian Pontarelli
 */
class FilePlugin extends BaseGroovyPlugin {

  FilePlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
  }

  /**
   * Copies files around. This uses the {@link CopyDelegate} class to handle Closure methods.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.copy(to: "build/distributions/bin") {
   *     fileSet(dir: "src/main/scripts")
   *   }
   * </pre>
   *
   * @param attributes The named attributes (to is required).
   * @param closure The closure that is invoked.
   * @return The number of files copied.
   */
  int copy(Map<String, Object> attributes, Closure closure) {
    def delegate = new CopyDelegate(attributes, project)
    closure.delegate = delegate
    try {
      closure()

      int count = delegate.copier.copy()
      output.info("Copied [%d] files to [%s]", count, delegate.copier.to)
      return count
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
      return 0
    }
  }

  /**
   * Creates a JAR file from various files. This uses the {@link JarDelegate} class to handle Closure methods.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.jar(file: "build/jars/foo.jar") {
   *     fileSet(dir: "build/classes/main")
   *   }
   * </pre>
   *
   * @param attributes The named attributes (file is required).
   * @param closure The closure that is invoked.
   * @return The number of files added to the Jar.
   */
  int jar(Map<String, Object> attributes, Closure closure) {
    def delegate = new JarDelegate(attributes, project)
    closure.delegate = delegate
    try {
      closure()

      delegate.builder.ensureManifest("${project.group}.${project.name}".toString(), project.version.toString())

      int count = delegate.builder.build()
      output.info("Added [%d] files to JAR [%s]", count, delegate.builder.file)
      return count
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
      return 0
    }
  }

  /**
   * Creates a directory. Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.mkdir(dir: "foo")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void mkdir(Map<String, Object> attributes) {
    Files.createDirectories(FileTools.toPath(attributes["dir"]))
  }

  /**
   * Prunes the given directory. This does not traverse symlinks. It unlinks symlinks.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.prune(dir: "build/classes/main")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void prune(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir"], ["dir"], [:])) {
      throw new BuildFailureException("The file plugin prune method must be called like this:\n\n" +
          "  file.prune(dir: \"some dir\")")
    }

    Path directory = FileTools.toPath(attributes["dir"])
    try {
      FileTools.prune(project.directory.resolve(directory));
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
    }
  }

  /**
   * Creates a symbol link at the [link] attribute that points to the [target] attribute.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.symlink(target: "build/classes/main", link: "/tmp/foo/bar")
   * </pre>
   *
   * @param attributes The named attributes (target and link are required).
   */
  void symlink(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["target", "link"], ["target", "link"], [:])) {
      fail("The file plugin symlink method must be called like this:\n\n" +
          "  file.symlink(target: \"some dir\", link: \"some other dir\")")
    }

    Path target = project.directory.resolve(FileTools.toPath(attributes["target"])).toAbsolutePath()
    if (Files.notExists(target)) {
      fail("Invalid target directory [${target}]")
    }

    Path link = project.directory.resolve(FileTools.toPath(attributes["link"]))
    if (Files.exists(link) && Files.isSymbolicLink(link)) {
      Files.delete(link)
    } else if (Files.exists(link)) {
      fail("The link [${link}] exists and is a regular file or directory")
    } else if (Files.notExists(link.getParent())) {
      Files.createDirectories(link.getParent())
    }

    Files.createSymbolicLink(link, target)
  }

  /**
   * Creates a Tarball from various files. This uses the {@link TarDelegate} class to handle Closure methods.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.tar(file: "build/tars/foobar.tar.gz") {
   *     fileSet(dir: "src/main/java")
   *   }
   * </pre>
   *
   * @param attributes The named attributes (file is required).
   * @param closure The closure that is invoked.
   * @return The number of files added to the Tarball.
   */
  int tar(Map<String, Object> attributes, Closure closure) {
    def delegate = new TarDelegate(attributes, project)
    closure.delegate = delegate
    try {
      closure()

      int count = delegate.builder.build()
      output.info("Added [%d] files to TAR [%s]", count, delegate.builder.file)
      return count
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
      return 0
    }
  }

  /**
   * Untars a TAR file to a directory. This requires the [file] and [to] attributes.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.untar(file: "build/tars/foobar.tar.gz", to: "build/output")
   * </pre>
   *
   * @param attributes The named attributes (file is required).
   * @param closure The closure that is invoked.
   */
  void untar(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["file", "to", "useGroup", "useOwner"], ["file", "to"], [:])) {
      fail("You must supply a [file] and [to] attribute like this:\n" +
          "  file.unzip(file: \"foo.zip\", to: \"some-dir\")")
    }

    Path file = project.directory.resolve(FileTools.toPath(attributes["file"]))
    Path to = project.directory.resolve(FileTools.toPath(attributes["to"]))
    boolean useGroup = attributes["useGroup"] ? attributes["useGroup"] : false
    boolean useOwner = attributes["useOwner"] ? attributes["useOwner"] : false

    if (!Files.isRegularFile(file)) {
      fail("TAR file [${file}] does not exist")
    }

    try {
      output.info("Untarring [${file}] to [${to}]")
      TarTools.untar(file, to, useGroup, useOwner)
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
    }
  }

  /**
   * Unzips a ZIP file to a directory. This requires the [file] and [to] attributes.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.unzip(file: "build/zips/foobar.zip", to: "build/output")
   * </pre>
   *
   * @param attributes The named attributes (file is required).
   * @param closure The closure that is invoked.
   */
  void unzip(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["file", "to"], ["file", "to"], [:])) {
      fail("You must supply a [file] and [to] attribute like this:\n" +
          "  file.unzip(file: \"foo.zip\", to: \"some-dir\")")
    }

    Path file = project.directory.resolve(FileTools.toPath(attributes["file"]))
    Path to = project.directory.resolve(FileTools.toPath(attributes["to"]))

    if (!Files.isRegularFile(file)) {
      fail("Zip file [${file}] does not exist")
    }

    try {
      output.info("Unzipping [${file}] to [${to}]")
      ZipTools.unzip(file, to)
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
    }
  }

  /**
   * Creates a ZIP file from various files. This uses the {@link ZipDelegate} class to handle Closure methods.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   file.zip(file: "build/jars/foo.zip") {
   *     fileSet(dir: "build/classes/main")
   *   }
   * </pre>
   *
   * @param attributes The named attributes (file is required).
   * @param closure The closure that is invoked.
   * @return The number of files added to the ZIP.
   */
  int zip(Map<String, Object> attributes, Closure closure) {
    def delegate = new ZipDelegate(attributes, project)
    closure.delegate = delegate
    try {
      closure()

      int count = delegate.builder.build()
      output.info("Added [%d] files to ZIP [%s]", count, delegate.builder.file)
      return count
    } catch (IOException e) {
      output.debug(e)
      fail(e.getMessage())
      return 0
    }
  }
}
