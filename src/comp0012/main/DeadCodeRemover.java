package comp0012.main;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class DeadCodeRemover
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public DeadCodeRemover(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		Method[] methods = cgen.getMethods();

		for (Method m : methods) {
			MethodGen mg = new MethodGen(m, gen.getClassName(), cpgen);
			InstructionList iList = mg.getInstructionList();
			if (iList != null) {
				InstructionHandle[] handles = iList.getInstructionHandles();
				boolean reachable = true;
				for (InstructionHandle ih : handles) {
					Instruction inst = ih.getInstruction();
					if (inst instanceof ReturnInstruction || inst instanceof GOTO) {
						reachable = false;
					} else if (!reachable) {
						try {
							iList.delete(ih);
						} catch (TargetLostException e) {
							System.err.println("Could not delete instruction");
						}
					}
					if (inst instanceof BranchInstruction) {
						reachable = true;
					}
				}
				mg.setInstructionList(iList);
				mg.setMaxStack();
				gen.replaceMethod(m, mg.getMethod());
			}
		}
		gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
	}

	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}