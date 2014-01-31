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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.savantbuild.domain.Project
import org.savantbuild.io.ArchiveFileSet
import org.savantbuild.io.FileInfo
import org.savantbuild.io.FileSet
import org.savantbuild.io.FileTools
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
/**
 * Delegate for the tar method's closure. This does all the work of building Tarfiles.
 *
 * @author Brian Pontarelli
 */
class TarDelegate extends BaseFileDelegate {
  public static final String ERROR_MESSAGE = "The file plugin tar method must be called like this:\n\n" +
      "  file.tar(file: \"file.jar\") {\n" +
      "    fileSet(dir: \"some other dir\")\n" +
      "    tarFileSet(dir: \"some other dir\", prefix: \"some-prefix\")\n" +
      "  }"

  public final Path file

  public final boolean compress

  public final List<FileSet> fileSets = new ArrayList<>()

  TarDelegate(Map<String, Object> attributes, Project project) {
    super(project)

    if (!GroovyTools.attributesValid(attributes, ["file"], ["compress": Boolean.class])) {
      throw new BuildFailureException(ERROR_MESSAGE);
    }

    this.compress = attributes["compress"]
    this.file = project.directory.resolve(FileTools.toPath(attributes["file"]))
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
    if (!GroovyTools.attributesValid(attributes, ["dir"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    def dir = FileTools.toPath(attributes["dir"])
    addFileSet(new FileSet(project.directory.resolve(dir)))
  }

  /**
   * Adds an optionalFileSet:
   *
   * <pre>
   *   optionalFileSet(dir: "someDir")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void optionalFileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    def dir = FileTools.toPath(attributes["dir"])
    addOptionalFileSet(new FileSet(project.directory.resolve(dir)))
  }

  /**
   * Adds a tarFileSet:
   *
   * <pre>
   *   tarFileSet(dir: "someDir", prefix: "some-prefix")
   * </pre>
   *
   * @param attributes The named attributes (dir is required).
   */
  void tarFileSet(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["dir", "prefix"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    def dir = FileTools.toPath(attributes["dir"])
    def prefix = GroovyTools.toString(attributes, "prefix")
    addFileSet(new ArchiveFileSet(project.directory.resolve(dir), prefix))
  }

  /**
   * Creates the tar file.
   */
  int tar() {
    if (Files.exists(file)) {
      Files.delete(file)
    }

    if (!Files.isDirectory(file.getParent())) {
      Files.createDirectories(file.getParent())
    }

    int count = 0
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    new TarArchiveOutputStream(baos).withStream { tos ->
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

      for (FileSet fileSet : fileSets) {
        for (FileInfo fileInfo : fileSet.toFileInfos()) {
          TarArchiveEntry entry = new TarArchiveEntry(fileInfo.relative.toString())
          entry.size = fileInfo.size
          entry.mode = fileInfo.toMode()
          tos.putArchiveEntry(entry)
          tos.write(Files.readAllBytes(fileInfo.origin))
          tos.closeArchiveEntry()
          count++
        }
      }
    }

    if (compress) {
      new GZIPOutputStream(Files.newOutputStream(file)).withStream { gos ->
        gos.write(baos.toByteArray())
      }
    } else {
      Files.write(file, baos.toByteArray())
    }

    return count
  }

  private void addFileSet(FileSet fileSet) {
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory")
    }

    if (!Files.isDirectory(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] does not exist")
    }

    fileSets.add(fileSet)
  }

  private void addOptionalFileSet(FileSet fileSet) throws IOException {
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory")
    }

    // Only add if it exists
    if (Files.isDirectory(fileSet.directory)) {
      fileSets.add(fileSet)
    }
  }
}
