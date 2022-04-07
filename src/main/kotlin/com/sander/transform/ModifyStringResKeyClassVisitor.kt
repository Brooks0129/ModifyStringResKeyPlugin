package com.sander.transform

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ModifyStringResKeyClassVisitor(
    classVisitor: ClassVisitor?,
    val transformMap: MutableMap<String, String>
) :
    ClassVisitor(Opcodes.ASM9, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        methodVisitor = ModifyStringResKeyMethodVisitor(methodVisitor, transformMap);
        return methodVisitor
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        return super.visitField(access, name, descriptor, signature, value)
    }
}

class ModifyStringResKeyMethodVisitor(methodVisitor: MethodVisitor, val transformMap: MutableMap<String, String>) :
    MethodVisitor(Opcodes.ASM9, methodVisitor) {
    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        if (owner!!.endsWith("R\$string") && transformMap.containsKey(name)) {
            super.visitFieldInsn(opcode, owner, transformMap[name], descriptor)
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

}
