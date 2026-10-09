/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.InjectedInterface;
import net.fabricmc.classtweaker.utils.EntryTriple;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.sinytra.launchpad.api.Constants;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassTweakerProcessor implements ClassProcessor {
    private final ClassTweaker classTweaker;
    private final Collection<Type> targets;

    public ClassTweakerProcessor(ClassTweaker classTweaker) {
        this.classTweaker = classTweaker;

        this.targets = classTweaker.getTargets().stream()
            .map(Type::getObjectType)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ProcessorName name() {
        return Constants.CT_PROCESSOR;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && this.targets.contains(context.type());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        ClassNode node = context.node();

        applyAW(node);
        applyInterfaceInjections(node);

        return ComputeFlags.SIMPLE_REWRITE;
    }

    private void applyAW(ClassNode node) {
        AccessWidener widener = this.classTweaker.getAccessWidener(node.name);
        int access = node.access;

        node.access = widener.getClassAccess().apply(access, node.name, access);

        if (widener.getClassAccess().isExtendable()) {
            node.permittedSubclasses = null;
        }

        for (InnerClassNode inner : node.innerClasses) {
            inner.access = this.classTweaker.getAccessWidener(inner.name).getClassAccess().apply(inner.access, inner.name, access);
        }

        for (FieldNode field : node.fields) {
            EntryTriple key = new EntryTriple(node.name, field.name, field.desc);
            field.access = widener.getFieldAccess(key).apply(field.access, field.name, access);
        }

        String canonicalDesc = getCanonicalDesc(node);
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>") && method.desc.equals(canonicalDesc)) {
                method.access = widener.getCanonicalConstructorAccess().apply(method.access, method.name, access);
            }

            EntryTriple triple = new EntryTriple(node.name, method.name, method.desc);
            method.access = widener.getMethodAccess(triple).apply(method.access, method.name, access);

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && isWidenedMethod(widener, node.name, call.owner, call.name, call.desc)
                ) {
                    call.setOpcode(Opcodes.INVOKEVIRTUAL);
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof Handle h && h.getTag() == Opcodes.H_INVOKESPECIAL
                            && isWidenedMethod(widener, node.name, h.getOwner(), h.getName(), h.getDesc())
                        ) {
                            indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKEVIRTUAL, h.getOwner(), h.getName(), h.getDesc(), h.isInterface());
                        }
                    }
                }
            }
        }
    }

    private void applyInterfaceInjections(ClassNode node) {
        List<InjectedInterface> injections = this.classTweaker.getInjectedInterfaces(node.name);
        if (injections.isEmpty()) {
            return;
        }

        StringBuilder signature = node.signature != null ? new StringBuilder(node.signature) : null;

        if (signature == null && injections.stream().anyMatch(InjectedInterface::hasGenerics)) {
            signature = new StringBuilder("L").append(node.superName).append(";");
            for (String itf : node.interfaces) {
                signature.append("L").append(itf).append(";");
            }
        }

        for (InjectedInterface injection : injections) {
            if (!node.interfaces.contains(injection.getInterfaceName())) {
                node.interfaces.add(injection.getInterfaceName());
                if (signature != null) {
                    signature.append(injection.getInterfaceSignature());
                }
            }
        }

        if (signature != null) {
            node.signature = signature.toString();
        }
    }

    @Nullable
    private static String getCanonicalDesc(ClassNode node) {
        if ((node.access & Opcodes.ACC_RECORD) == 0) {
            return null;
        }
        String inner = (node.recordComponents != null ? node.recordComponents : List.<RecordComponentNode>of()).stream()
            .map(c -> c.descriptor)
            .collect(Collectors.joining());
        return "(%s)V".formatted(inner);
    }

    private static boolean isWidenedMethod(AccessWidener aw, String className, String owner, String name, String desc) {
        return owner.equals(className) && !name.equals("<init>") && aw.getMethodAccess(new EntryTriple(owner, name, desc)).isChanged();
    }
}
