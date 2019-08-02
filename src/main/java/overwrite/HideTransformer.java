package overwrite;

import static org.objectweb.asm.Opcodes.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class HideTransformer implements IClassTransformer {
	//IClassTransformerにより呼ばれる書き換え用のメソッド。

	private static final Logger log = LogManager.getLogger();

	@Override
	public byte[] transform(final String name, final String transformedName, byte[] bytes) {
		//対象クラス以外を除外する。対象は呼び出し元があるクラスである。
		if ("net.minecraft.client.renderer.entity.RenderLivingBase".equals(transformedName)) {
			ClassReader cr = new ClassReader(bytes);
			ClassWriter cw = new ClassWriter(1);
			ClassVisitor cv = new ClassVisitor(ASM4, cw) {
				//クラス内のメソッドを訪れる。
				@Override
				public MethodVisitor visitMethod(int access, String methodName, String desc, String signature,
						String[] exceptions) {
					MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
					//呼び出し元のメソッドを参照していることを確認する。
					//String s1 = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(name, methodName, desc);
					//コンストラクタのため省略
					//C:\Users\<ユーザー名>\.gradle\caches\minecraft\net\minecraftforge\forge\1.7.10-10.13.4.1558-1.7.10\forge-1.7.10-10.13.4.1558-1.7.10-decomp.jar\より名称を検索、比較してメソッドの難読化名を探す。
					if (methodName.equals("<init>")) {
						//もし対象だったらMethodVisitorを差し替える。
						mv = new MethodVisitor(ASM4, mv) {
							//メゾットの終了の直前に命令を追加
							@Override
							public void visitInsn(int opcode) {
								if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
									mv.visitVarInsn(ALOAD, 0);
									mv.visitMethodInsn(
											INVOKESTATIC, "overwrite/HideHook", "hookOnMakeLivingRender", Type
													.getMethodDescriptor(Type.VOID_TYPE,
															Type.getObjectType(
																	"net/minecraft/client/renderer/entity/RenderLivingBase")),
											false);
								}
								super.visitInsn(opcode);
							}
						};
					}
					return mv;
				}
			};
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			return cw.toByteArray();
		}
		if ("net.minecraft.client.model.ModelBiped".equals(transformedName)) {
			ClassReader cr = new ClassReader(bytes);
			ClassWriter cw = new ClassWriter(1);
			ClassVisitor cv = new ClassVisitor(ASM4, cw) {
				//クラス内のメソッドを訪れる。
				@Override
				public MethodVisitor visitMethod(int access, String methodName, String desc, String signature,
						String[] exceptions) {
					MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
					//呼び出し元のメソッドを参照していることを確認する。
					String s1 = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(name, methodName, desc);

					log.info("methodName " + s1 + " " + methodName);

					//C:\Users\<ユーザー名>\.gradle\caches\minecraft\net\minecraftforge\forge\1.7.10-10.13.4.1558-1.7.10\forge-1.7.10-10.13.4.1558-1.7.10-decomp.jar\より名称を検索、比較してメソッドの難読化名を探す。
					if (s1.equals("setRotationAngles") || s1.equals("func_78087_a")) {
						//もし対象だったらMethodVisitorを差し替える。
						mv = new MethodVisitor(ASM4, mv) {
							//メゾットの終了の直前に命令を追加
							@Override
							public void visitInsn(int opcode) {
								if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
									mv.visitVarInsn(ALOAD, 0);
									mv.visitVarInsn(ALOAD, 7);
									mv.visitMethodInsn(INVOKESTATIC, "overwrite/HideHook", "hookOnSetAngle", Type
											.getMethodDescriptor(Type.VOID_TYPE,
													Type.getObjectType(
															"net/minecraft/client/model/ModelBiped"),
													Type.getObjectType(
															"net/minecraft/entity/Entity")),
											false);
								}
								super.visitInsn(opcode);
							}
						};
					}
					return mv;
				}
			};
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
			return cw.toByteArray();
		}
		return bytes;

	}
}