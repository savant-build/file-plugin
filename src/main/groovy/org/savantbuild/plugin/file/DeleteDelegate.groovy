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
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import java.nio.file.Files
/**
 * Delegate for the delete method's closure. This uses FileSets to delete 0 or more files.
 *
 * @author Brian Pontarelli
 */
class DeleteDelegate extends BaseFileDelegate {
  public static final String ERROR_MESSAGE = "The file plugin [delete] method must be called like this:\n\n" +
      "  file.delete {\n" +
      "    fileSet(dir: \"some other dir\")\n" +
      "  }"

  public final List<FileSet> fileSets = new ArrayList<>()

  DeleteDelegate(Project project) {
    super(project)
  }

  /**
   * Deletes the files specified by the FileSets.
   *
   * @return The number of files deleted.
   */
  int delete() {
    int count = 0
    fileSets.each { fileSet ->
      fileSet.toFileInfos().each { info ->
        if (Files.deleteIfExists(info.origin)) {
          count++
        }
      }
    }

    return count
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
}
