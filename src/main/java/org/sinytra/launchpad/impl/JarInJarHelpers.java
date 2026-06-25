/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package org.sinytra.launchpad.impl;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.util.PathPrettyPrinting;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import net.neoforged.neoforgespi.locating.ModFileLoadingException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// The following functions have been borrowed and adapted from net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator
public class JarInJarHelpers {
    private static final Logger LOGGER = LogUtils.getLogger();

    // See also: JarInJarDependencyLocator
    public static IModFile loadModFileFrom(IModFile file, String relativePath, IDiscoveryPipeline pipeline) {
        // Copy it to disk as we go, while hashing it
        Path jijCacheDir = FMLPaths.JIJ_CACHEDIR.get();
        Path tempFile;
        try {
            tempFile = Files.createTempFile(jijCacheDir, "_jij", ".tmp");
        } catch (IOException e) {
            throw new ModFileLoadingException("Failed to create a temporary file for JIJ in " + jijCacheDir + ": " + e);
        }

        // Copy the file to the temp-file, while hashing it to produce its final filename
        Path finalPath;
        try {
            String checksum = extractEmbeddedJarFile(file, relativePath, tempFile);

            // We must maintain the original filename, as it could be used to determine the module name and version
            String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            finalPath = jijCacheDir.resolve(checksum + "/" + filename);
            // If the file already exists, reuse it, since it might already be opened.
            if (!Files.isRegularFile(finalPath)) {
                moveExtractedFileIntoPlace(tempFile, finalPath);
            }

            // Mark the extracted file with its source for the duration of this session
            PathPrettyPrinting.addSubstitution(
                finalPath,
                PathPrettyPrinting.prettyPrint(file.getFilePath()) + " > " + filename,
                ""
            );
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                LOGGER.error("Failed to remove temporary file {}: {}", tempFile, e);
            }
        }

        JarContents jar;
        try {
            jar = JarContents.ofPath(finalPath);
        } catch (IOException e) {
            LOGGER.error("Failed to read Jar-in-Jar file {} extracted from mod file {} to {}", relativePath, file, finalPath, e);
            throw new ModFileLoadingException("Failed to load mod file " + relativePath + " from " + file, e);
        }

        return pipeline.readModFile(jar, ModFileDiscoveryAttributes.DEFAULT.withParent(file));
    }

    // See also: JarInJarDependencyLocator
    private static String extractEmbeddedJarFile(IModFile file, String relativePath, Path destination) {
        try (InputStream inStream = file.getContents().openFile(relativePath);
             OutputStream outStream = Files.newOutputStream(destination)
        ) {
            if (inStream == null) {
                LOGGER.error("Mod file {} declares Jar-in-Jar {} but does not contain it.", file, relativePath);
                throw new ModFileLoadingException("Mod file " + file + " declares Jar-in-Jar " + relativePath + " but does not contain it.");
            }

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Missing default JCA algorithm SHA-256.", e);
            }

            var digestOut = new DigestOutputStream(outStream, digest);
            inStream.transferTo(digestOut);

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            LOGGER.error("Failed to copy Jar-in-Jar file {} from mod file {} to {}", relativePath, file, destination, e);
            throw new ModFileLoadingException("Failed to load mod file " + file.getFileName(), e);
        }
    }

    // See also: JarInJarDependencyLocator
    private static void moveExtractedFileIntoPlace(Path source, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create parent directory for extracted JiJ-file " + source + " at " + destination, e);
        }

        try {
            try {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to move temporary JiJ-file " + source + " to its final location " + destination, e);
        }
    }
}
