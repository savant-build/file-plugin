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
import org.savantbuild.io.ArchiveFileSet
import org.savantbuild.io.FileSet
import org.savantbuild.io.FileTools
import org.savantbuild.parser.groovy.GroovyTools

import java.nio.file.Path
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Base class for delegates that work on files.
 *
 * @author Brian Pontarelli
 */
abstract class BaseFileDelegate {
  protected final Project project

  BaseFileDelegate(Project project) {
    this.project = project
  }

  FileSet toFileSet(Map<String, Object> attributes) {
    Path dir = project.directory.resolve(FileTools.toPath(attributes["dir"]))
    List includePatterns = attributes["includePatterns"]
    if (includePatterns != null) {
      GroovyTools.convertListItems(includePatterns, Pattern.class, { value -> Pattern.compile(value.toString()) } as Function<Object, Pattern>)
    }

    List excludePatterns = attributes["excludePatterns"]
    if (excludePatterns != null) {
      GroovyTools.convertListItems(excludePatterns, Pattern.class, { value -> Pattern.compile(value.toString()) } as Function<Object, Pattern>)
    }

    return new FileSet(dir, includePatterns, excludePatterns)
  }

  FileSet toArchiveFileSet(Map<String, Object> attributes) {
    Path dir = project.directory.resolve(FileTools.toPath(attributes["dir"]))
    List includePatterns = attributes["includePatterns"]
    if (includePatterns != null) {
      GroovyTools.convertListItems(includePatterns, Pattern.class, { value -> Pattern.compile(value.toString()) } as Function<Object, Pattern>)
    }

    List excludePatterns = attributes["excludePatterns"]
    if (excludePatterns != null) {
      GroovyTools.convertListItems(excludePatterns, Pattern.class, { value -> Pattern.compile(value.toString()) } as Function<Object, Pattern>)
    }

    return new ArchiveFileSet(dir, attributes["prefix"], attributes["mode"], attributes["userName"], attributes["groupName"], includePatterns, excludePatterns)
  }
}
