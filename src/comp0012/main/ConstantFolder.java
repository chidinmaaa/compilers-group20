package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import java.util.Map;
import java.util.HashMap;
//
import org.apache.bcel.classfile.JavaClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import java.io.ByteArrayInputStream;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	

	public void optimize() {
		ClassGen cgen = new ClassGen(original);
		if (!((cgen.getClassName()).equals("comp0012.target.SimpleFolding") || cgen.getClassName().equals("comp0012.target.ConstantVariableFolding"))) {
			this.optimized = gen.getJavaClass();
			return;
		}
		ConstantPoolGen cpgen = cgen.getConstantPool();
	
		// Iterate over all methods in the class
		for (Method m : cgen.getMethods()) {
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
	
			// Get the method's instructions
			InstructionList il = mg.getInstructionList();
	
			// Create a map to store the constant values of local variables
			Map<Integer, Number> constantVars = new HashMap<>();
	
			// Iterate over the instructions
			for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
				Instruction ins = ih.getInstruction();
	
				// If the instruction is a store instruction (i.e., it assigns a value to a local variable)
				if (ins instanceof StoreInstruction) {
					StoreInstruction storeInstruction = (StoreInstruction) ins;
	
					// If the previous instruction is a constant push instruction (i.e., it pushes a constant onto the stack)
					Instruction prevInstruction = ih.getPrev().getInstruction();
					if (prevInstruction instanceof ConstantPushInstruction) {
						ConstantPushInstruction pushInstruction = (ConstantPushInstruction) prevInstruction;
	
						// Add the local variable and its constant value to the map
						constantVars.put(storeInstruction.getIndex(), pushInstruction.getValue());
					}
				}
	
				// If the instruction is a load instruction (i.e., it loads a value from a local variable)
				else if (ins instanceof LoadInstruction) {
					LoadInstruction loadInstruction = (LoadInstruction) ins;
	
					// If the local variable has a constant value, replace the load instruction with a push instruction
					if (constantVars.containsKey(loadInstruction.getIndex())) {
						InstructionHandle pushHandle = il.append(ih, new PUSH(cpgen, constantVars.get(loadInstruction.getIndex())));
						try {
							il.delete(ih);
							ih = pushHandle;
						} catch (TargetLostException e) {
							for (InstructionHandle target : e.getTargets()) {
								for (InstructionTargeter targeter : target.getTargeters()) {
									targeter.updateTarget(target, pushHandle);
								}
							}
						}
					}
				}
			}
	
			// Update the method's instructions
			mg.setInstructionList(il);
			mg.setMaxStack();
			mg.setMaxLocals();
	
			// Replace the old method with the optimized method
			cgen.replaceMethod(m, mg.getMethod());
		}
	
		// Convert the BCEL JavaClass object to a byte array
		byte[] bcelBytes = cgen.getJavaClass().getBytes();
	
		// Use ASM to update the StackMapTable
		ClassReader cr = new ClassReader(bcelBytes);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
		cr.accept(cw, 0);
		byte[] asmBytes = cw.toByteArray();
	
		// Convert the byte array back to a BCEL JavaClass object
		try {
			ClassParser cp = new ClassParser(new ByteArrayInputStream(asmBytes), "optimized_class");
			this.optimized = cp.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
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