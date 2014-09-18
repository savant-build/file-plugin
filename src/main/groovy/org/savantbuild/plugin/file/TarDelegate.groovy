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
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException
import org.savantbuild.util.tar.TarBuilder

/**
 * Delegate for the tar method's closure. This does all the work of building Tarfiles.
 *
 * @author Brian Pontarelli
 */
class TarDelegate extends BaseFileDelegate {
  public static final String ERROR_MESSAGE = "The file plugin tar method must be called like this:\n\n" +
      "  file.tar(file: \"file.tar.gz\", compress: true, storeGroup: true, storeOwner: true) {\n" +
      "    fileSet(dir: \"some other dir\")\n" +
      "    tarFileSet(dir: \"some other dir\", prefix: \"some-prefix\")\n" +
      "  }"

  public final TarBuilder builder

  TarDelegate(Map<String, Object> attributes, Project project) {
    super(project)

    if (!GroovyTools.attributesValid(attributes, ["file", "compress"], ["file"], ["compress": Boolean.class])) {
      throw new BuildFailureException(ERROR_MESSAGE);
    }

    this.builder = new TarBuilder(project.directory.resolve(FileTools.toPath(attributes["file"])))
    if (attributes["compress"]) {
      this.builder.compress = attributes["compress"]
    }
    if (attributes["storeGroup"]) {
      this.builder.storeGroup = attributes["storeGroup"]
    }
    if (attributes["storeOwner"]) {
      this.builder.storeOwner = attributes["storeOwner"]
    }
  }

  /**
   * Adds a fileSet:
   *
   * <pre>
   *   fileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   * @return The TarBuilder.
   */
  TarBuilder fileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "includePatterns", "excludePatterns"], ["dir"], ["includePatterns": List.class, "excludePatterns": List.class])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    builder.fileSet(toFileSet(attributes))
    return builder
  }

  /**
   * Adds an optionalFileSet:
   *
   * <pre>
   *   optionalFileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   * @return The TarBuilder.
   */
  TarBuilder optionalFileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "includePatterns", "excludePatterns"], ["dir"], ["includePatterns": List.class, "excludePatterns": List.class])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    builder.optionalFileSet(toFileSet(attributes))
    return builder
  }

  /**
   * Adds a tarFileSet:
   *
   * <pre>
   *   tarFileSet(dir: "someDir", prefix: "some-prefix")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   * @return The TarBuilder.
   */
  TarBuilder tarFileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "prefix", "mode", "includePatterns", "excludePatterns"], ["dir", "prefix"], ["prefix": String.class, "mode": Integer.class, "includePatterns": List.class, "excludePatterns": List.class])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    builder.fileSet(toArchiveFileSet(attributes))
    return builder
  }
}
