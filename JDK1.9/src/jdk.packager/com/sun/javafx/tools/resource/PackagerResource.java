/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.sun.javafx.tools.resource;

import java.io.File;

public class PackagerResource {
    private static final ResourceFilter ACCEPT_ALL_FILTER =
            new ResourceFilter() {
                @Override
                public boolean descent(final File file,
                                       final String relativePath) {
                    return true;
                }

                @Override
                public boolean accept(final File file,
                                      final String relativePath) {
                    return true;
                }
            };

    private final File baseDir;
    private final File file;
    private final String relativePath;

    public PackagerResource(final File baseDir, final String path) {
        this(baseDir, createFile(baseDir, path));
    }

    public PackagerResource(final File baseDir, final File file) {
        final File nrmFile = normalizeFile(file);
        if (nrmFile == null) {
            throw new IllegalArgumentException("Invalid file specified");
        }

        if (baseDir != null) {
            final File nrmBaseDir = normalizeFile(baseDir);
            if (nrmBaseDir == null) {
                throw new IllegalArgumentException("Invalid basedir specified");
            }

            if (nrmFile.equals(nrmBaseDir)) {
                this.file = nrmFile;
                this.baseDir = nrmFile;
                this.relativePath = "";
                return;
            }

            final StringBuilder relativePathBuilder =
                    new StringBuilder(nrmFile.getName());

            File tempFile = nrmFile.getParentFile();
            while (tempFile != null) {
                if (tempFile.equals(nrmBaseDir)) {
                    this.file = nrmFile;
                    this.baseDir = nrmBaseDir;
                    this.relativePath = relativePathBuilder.toString();
                    return;
                }

                relativePathBuilder.insert(0, '/');
                relativePathBuilder.insert(0, tempFile.getName());
                tempFile = tempFile.getParentFile();
            }
        }

        final File nrmParentFile = nrmFile.getParentFile();

        this.file = nrmFile;
        this.baseDir = (nrmParentFile != null) ? nrmParentFile : nrmFile;
        this.relativePath = nrmFile.getName();
    }

    public final File getBaseDir() {
        return baseDir;
    }

    public final File getFile() {
        return file;
    }

    public final String getRelativePath() {
        return relativePath;
    }

    public final boolean traverse(final ResourceTraversal resourceTraversal) {
        return traverse(resourceTraversal, null);
    }

    public final boolean traverse(final ResourceTraversal resourceTraversal,
                                  final ResourceFilter resourceFilter) {
        return new TraversalOperation((resourceFilter != null)
                                              ? resourceFilter
                                              : ACCEPT_ALL_FILTER,
                                      resourceTraversal, this).execute();
    }

    private static File normalizeFile(final File inputFile) {
        return normalizeFileImpl(inputFile.getAbsoluteFile());
    }

    private static File normalizeFileImpl(final File inputFile) {
        if (inputFile.getParentFile() == null) {
            return inputFile;
        }

        final File partiallyNormalizedFile =
                normalizeFileImpl(inputFile.getParentFile());

        if (partiallyNormalizedFile == null) {
            // error
            return null;
        }

        final String fileName = inputFile.getName();

        if (fileName.equals(".")) {
            // ignore this path element
            return partiallyNormalizedFile;
        }

        if (fileName.equals("..")) {
            // remove the last path element
            return partiallyNormalizedFile.getParentFile();
        }

        return new File(partiallyNormalizedFile, fileName);
    }

    private static File createFile(final File baseDir, final String path) {
        final File testFile = new File(path);
        return testFile.isAbsolute()
                   ? testFile
                   : new File(baseDir == null
                                  ? null
                                  : baseDir.getAbsolutePath(),
                              path);
    }

    private static final class TraversalOperation {
        private final ResourceFilter resourceFilter;
        private final ResourceTraversal resourceTraversal;
        private final PackagerResource rootResource;
        private final StringBuilder relativePathBuilder;

        public TraversalOperation(final ResourceFilter resourceFilter,
                                  final ResourceTraversal resourceTraversal,
                                  final PackagerResource rootResource) {
            this.resourceFilter = resourceFilter;
            this.resourceTraversal = resourceTraversal;
            this.rootResource = rootResource;
            this.relativePathBuilder =
                    new StringBuilder(rootResource.relativePath);
        }

        public boolean execute() {
            return traverse(rootResource.file);
        }

        private boolean traverse(final File file) {
            final String relativePath = relativePathBuilder.toString();
            if (resourceFilter.accept(file, relativePath)
                    && !resourceTraversal.traverse(rootResource, file,
                                                   relativePath)) {
                return false;
            }

            if (!file.isDirectory()
                    || !resourceFilter.descent(file, relativePath)) {
                return true;
            }

            final int resetLength = relativePathBuilder.length();
            File[] children = file.listFiles();
            if (children != null) {
                for (final File nextFile : children) {
                    if (resetLength > 0) {
                        relativePathBuilder.append('/');
                    }
                    relativePathBuilder.append(nextFile.getName());
                    if (!traverse(nextFile)) {
                        return false;
                    }
                    relativePathBuilder.setLength(resetLength);
                }
            }

            return true;
        }
    }
}
