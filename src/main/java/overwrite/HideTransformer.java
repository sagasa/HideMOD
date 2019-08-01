package overwrite;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class HideTransformer implements IClassTransformer {
	//IClassTransformerにより呼ばれる書き換え用のメソッド。
	@Override
	public byte[] transform(final String name, final String transformedName, byte[] bytes) {
		//対象クラス以外を除外する。対象は呼び出し元があるクラスである。
		if (!"net.minecraft.client.renderer.entity.RenderLivingBase".equals(transformedName))
			return bytes;

		System.out.println("OVERWRITE");
		ClassReader cr = new ClassReader(bytes);
		ClassWriter cw = new ClassWriter(1);
		ClassVisitor cv = new ClassVisitor(ASM4, cw) {

			//クラス内のメソッドを訪れる。
			@Override
			public MethodVisitor visitMethod(int access, String methodName, String desc, String signature,
					String[] exceptions) {
				System.out.println("visitMethod " + methodName);
				MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
				//呼び出し元のメソッドを参照していることを確認する。
				String s1 = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(name, methodName, desc);
				//C:\Users\<ユーザー名>\.gradle\caches\minecraft\net\minecraftforge\forge\1.7.10-10.13.4.1558-1.7.10\forge-1.7.10-10.13.4.1558-1.7.10-decomp.jar\より名称を検索、比較してメソッドの難読化名を探す。
				if (s1.equals("<init>") || methodName.equals("<init>")) {
					//もし対象だったらMethodVisitorを差し替える。
					mv = new MethodVisitor(ASM4, mv) {
						//呼び出す予定のメソッドを読み込む。

						@Override
						public void visitInsn(int opcode) {
							if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
								System.out.println("MethodEnd?");
								mv.visitVarInsn(ALOAD, 0);
								mv.visitMethodInsn(INVOKESTATIC, "overwrite/HideHook", "test", Type
										.getMethodDescriptor(Type.VOID_TYPE,
												Type.getObjectType(
														"net/minecraft/client/renderer/entity/RenderLivingBase")),
										false);
							}
							super.visitInsn(opcode);
						}

						@Override
						public void visitMethodInsn(int opcode, String owner, String methodName, String desc,
								boolean itf) {
							//書き換え対象のメソッドであることを確認する。
							String s2 = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(name, methodName, desc);
							//             FMLDeobfuscatingRemapper.INSTANCE.map

							System.out
									.println("visit " + " " + methodName + " " + desc + " " + s2 + "  " + opcode + " ");
							super.visitMethodInsn(opcode, owner, methodName, desc, itf);

							if (s2.equals("<init>") || s2.equals("func_145950_i") || methodName.equals("isBurning")
									|| methodName.equals("func_145950_i")) {
								//引数として次に渡す値にthisを指定する。

								//今回はフックを差し込むだけだが、ここで書き換えも出来る。
							}
							//今回は最後に元のクラスを読み込んでreturnする。

						}
					};
				}
				return mv;
			}
		};
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		return cw.toByteArray();
	}
}