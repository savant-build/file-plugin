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
import org.savantbuild.io.Copier
import org.savantbuild.io.FileTools
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException
/**
 * Delegate for the copy method's closure. This passes through everything to the Copier.
 *
 * @author Brian Pontarelli
 */
class CopyDelegate extends BaseFileDelegate {
  public static final String ERROR_MESSAGE = "The file plugin copy method must be called like this:\n\n" +
      "  file.copy(to: \"some dir\") {\n" +
      "    fileSet(dir: \"some other dir\")" +
      "  }"

  public final Copier copier

  CopyDelegate(Map<String, Object> attributes, Project project) {
    super(project)

    if (!GroovyTools.attributesValid(attributes, ["to"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE);
    }

    def to = FileTools.toPath(attributes["to"])
    this.copier = new Copier(project.directory.resolve(to))
  }

  /**
   * Adds a fileSet:
   *
   * <pre>
   *   fileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   * @return The Copier.
   */
  Copier fileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    def dir = FileTools.toPath(attributes["dir"])
    copier.fileSet(project.directory.resolve(dir))
    return copier
  }
}
