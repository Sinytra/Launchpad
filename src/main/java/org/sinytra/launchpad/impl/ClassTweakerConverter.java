/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor.AccessType;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ClassTweakerConverter {

    public static String createAccessTransformer(BufferedReader reader, String fileName) throws IOException {
        Map<String, AccessType> classAccess = new HashMap<>();
        Map<String, Map<String, AccessType>> classFields = new HashMap<>();
        StringBuilder builder = new StringBuilder();

        builder.append("# Access Transformer file converted from %s by Launchpad\n".formatted(fileName));

        ClassTweakerReader.create(new ClassTweakerVisitor() {
            @Override
            public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
                String normalOwner = owner.replace('/', '.');
                
                return new AccessWidenerVisitor() {
                    @Override
                    public void visitClass(AccessType access, boolean transitive) {
                        // AcessWidener silently also access widens owners of methods that are being AW'd, but never calls visitClass for those entries
                        // Therefore, we have to replicate this behavior ourselves in visitMethod below. In addition, we first gather all class AWs in a map
                        // and only translate them once all AW entries have been processed.
                        // This prevents conflicts in case visitMethod generates an AW entry for a class that already has one, except with lower access.
                        classAccess.compute(
                            normalOwner,
                            (value, existing) -> existing == null || access.ordinal() > existing.ordinal() ? access : existing
                        );
                    }

                    @Override
                    public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
                        String modifier = switch (access) {
                            case ACCESSIBLE -> "public";
                            case EXTENDABLE -> "protected-f";
                            default -> throw new IllegalArgumentException("Invalid access type " + access + " for method");
                        };
                        builder.append(modifier).append(" ")
                            .append(normalOwner).append(" ")
                            .append(name)
                            .append(descriptor)
                            .append("\n");

                        // Make parent class accessible / extensible if necessary
                        visitClass(access, false);
                    }

                    @Override
                    public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
                        classFields.computeIfAbsent(normalOwner, n -> new HashMap<>())
                            .compute(name, (value, existing) -> existing == null || access.ordinal() > existing.ordinal() ? access : existing);

                        // Make parent class accessible / extensible if necessary
                        if (access != AccessType.MUTABLE) {
                            visitClass(access, false);
                        }
                    }
                };
            }
        }).read(reader, "official");

        // Translate class AWs
        classAccess.forEach((name, access) -> {
            String modifier = switch (access) {
                case ACCESSIBLE -> "public";
                case EXTENDABLE -> "public-f";
                default -> throw new IllegalArgumentException("Invalid access type " + access + " for class");
            };
            builder.append(modifier).append(" ").append(name).append("\n");
        });

        // Translate field AWs
        classFields.forEach((owner, fields) -> fields.forEach((name, access) -> {
            String modifier = switch (access) {
                case ACCESSIBLE -> "public";
                case MUTABLE -> "public-f";
                default -> throw new IllegalArgumentException("Invalid access type " + access + " for field");
            };
            builder.append(modifier).append(" ")
                .append(owner).append(" ")
                .append(name)
                .append("\n");
        }));

        return builder.toString();
    }

    private ClassTweakerConverter() {
    }
}
