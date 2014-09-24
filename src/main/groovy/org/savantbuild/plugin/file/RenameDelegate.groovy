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
import org.savantbuild.io.FileSet
import org.savantbuild.io.Filter
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Delegate for the rename method.
 *
 * @author Brian Pontarelli
 */
class RenameDelegate extends BaseFileDelegate {
  public static final String ERROR_MESSAGE = "The file plugin rename method must be called like this:\n\n" +
      "  file.rename {\n" +
      "    fileSet(dir: \"some other dir\")\n" +
      "    filter(token: \"%TOKEN%\", value: \"value\")\n" +
      "  }"

  public final List<FileSet> fileSets = new ArrayList<>()

  public final List<Filter> filters = new ArrayList<>()

  RenameDelegate(Project project) {
    super(project)
  }

  /**
   * Adds a fileSet:
   *
   * <pre>
   *   fileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void fileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "includePatterns", "excludePatterns"], ["dir"], ["includePatterns": List.class, "excludePatterns": List.class])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    FileSet fileSet = toFileSet(attributes)
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory");
    }

    if (!Files.isDirectory(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] does not exist");
    }

    fileSets.add(fileSet)
  }

  /**
   * Adds a filter:
   *
   * <pre>
   *   filter(token: "%TOKEN%", value: "value")
   * </pre>
   *
   * @param attributes The named attributes (token and value are required).
   */
  void filter(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["token", "value"], ["token", "value"], [:])) {
      println "Has token attribute ${GroovyTools.hasAttributes(attributes, ["token"])}"
      println "Has value attributes ${GroovyTools.hasAttributes(attributes, ["value"])}"
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    filters.add(new Filter(attributes["token"].toString(), attributes["value"].toString()))
  }

  /**
   * Adds an optional fileSet:
   *
   * <pre>
   *   optionalFileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void optionalFileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "includePatterns", "excludePatterns"], ["dir"], ["includePatterns": List.class, "excludePatterns": List.class])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    FileSet fileSet = toFileSet(attributes)
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory");
    }

    if (Files.isDirectory(fileSet.directory)) {
      fileSets.add(fileSet)
    }
  }

  /**
   * Performs the rename operation.
   *
   * @return The number of files renamed.
   */
  int rename() {
    if (filters.isEmpty()) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    int count = 0
    fileSets.each { set ->
      set.toFileInfos().each { info ->
        String originalPathString = info.origin.toString()
        String newPathString = originalPathString
        filters.each { filter ->
          newPathString = newPathString.replace(filter.token, filter.value)
        }

        if (!originalPathString.equals(newPathString)) {
          Path newPath = Paths.get(newPathString)
          Files.move(info.origin, newPath, StandardCopyOption.REPLACE_EXISTING)
          count++
        }
      }
    }

    return count
  }
}
