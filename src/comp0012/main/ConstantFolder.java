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
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import java.util.ArrayList;



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
		if (!((cgen.getClassName()).equals("comp0012.target.SimpleFolding") || cgen.getClassName().equals("comp0012.target.ConstantVariableFolding") || cgen.getClassName().equals("comp0012.target.DynamicVariableFolding"))) {
			this.optimized = gen.getJavaClass();
			return;
		}
		ConstantPoolGen cpgen = cgen.getConstantPool();
		for (Method m : cgen.getMethods()) {
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
			InstructionList iList = mg.getInstructionList();
			Map<Integer, Number> constantVars = new HashMap<>(); // map for the constant values of local variables
			Map<Integer, List<Pair<InstructionHandle, InstructionHandle>>> variableIntervals = new HashMap<>(); // map for the intervals of constant values for each variable
			for (InstructionHandle ih = iList.getStart(); ih != null; ih = ih.getNext()) {
				Instruction ins = ih.getInstruction();
		
				if (ins instanceof StoreInstruction) { // If store instruction
					StoreInstruction storeInstruction = (StoreInstruction) ins;
					Instruction prevInstruction = ih.getPrev().getInstruction();
					if (prevInstruction instanceof ConstantPushInstruction) {
						ConstantPushInstruction pushInstruction = (ConstantPushInstruction) prevInstruction;
						constantVars.put(storeInstruction.getIndex(), pushInstruction.getValue());
						if (variableIntervals.containsKey(storeInstruction.getIndex())) {
							List<Pair<InstructionHandle, InstructionHandle>> intervals = variableIntervals.get(storeInstruction.getIndex());
							Pair<InstructionHandle, InstructionHandle> lastInterval = intervals.get(intervals.size() - 1);
							intervals.set(intervals.size() - 1, Pair.of(lastInterval.getLeft(), ih));
						}
						List<Pair<InstructionHandle, InstructionHandle>> newInterval = new ArrayList<>();
						newInterval.add(Pair.of(ih, null));
						variableIntervals.put(storeInstruction.getIndex(), newInterval);
					}
				}
	
				else if (ins instanceof LoadInstruction) {// If load instruction
					LoadInstruction loadInstruction = (LoadInstruction) ins;
	
					// If the local variable has a constant value, replace the load instruction with a push instruction
					if (constantVars.containsKey(loadInstruction.getIndex())) {
						InstructionHandle pushHandle = iList.append(ih, new PUSH(cpgen, constantVars.get(loadInstruction.getIndex())));
						try {
							iList.delete(ih);
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
	
			mg.setInstructionList(iList);
			mg.setMaxStack();
			mg.setMaxLocals();
	
			cgen.replaceMethod(m, mg.getMethod());
		}
	
		byte[] bcelBytes = cgen.getJavaClass().getBytes();// Convert the BCEL JavaClass object to a byte array
		ClassReader cr = new ClassReader(bcelBytes); // Use ASM to update the StackMapTable
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
		cr.accept(cw, 0);
		byte[] asmBytes = cw.toByteArray();
		try {
			ClassParser cp = new ClassParser(new ByteArrayInputStream(asmBytes), "optimized_class"); // Convert back to a BCEL JavaClass object
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