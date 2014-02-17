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

/**
 * File plugin.
 *
 * @author Brian Pontarelli
 */
class FilePlugin extends BaseGroovyPlugin {

  FilePlugin(Project project, Output output) {
    super(project, output)
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
    closure()

    int count = delegate.copier.copy()
    output.info("Copied [%d] files to [%s]", count, delegate.copier.to)
    return count
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
    closure()

    int count = delegate.builder.build()
    output.info("Added [%d] files to JAR [%s]", count, delegate.builder.file)
    return count
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
    if (!GroovyTools.attributesValid(attributes, ["dir"], [:])) {
      throw new BuildFailureException("The file plugin prune method must be called like this:\n\n" +
          "  file.prune(dir: \"some dir\")")
    }

    def directory = FileTools.toPath(attributes["dir"])
    FileTools.prune(project.directory.resolve(directory));
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
    closure()

    int count = delegate.tar()
    output.info("Added [%d] files to JAR [%s]", count, delegate.file)
    return count
  }
}
