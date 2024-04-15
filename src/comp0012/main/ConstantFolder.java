package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;


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

	
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
	
		ConstantPoolGen cpgen = cgen.getConstantPool();

		Method[] methods = cgen.getMethods();

		for(Method method : methods) //simple folding
		{
			MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
			InstructionList iList = methodGen.getInstructionList();
			for (InstructionHandle handle : iList.getInstructionHandles())
			{
				Instruction i = handle.getInstruction();
				if(i instanceof ArithmeticInstruction)
				{
					if(i instanceof IADD || i instanceof ISUB || i instanceof IDIV || i instanceof IMUL) 
					{	
						foldInt(handle, i, iList, cpgen);
					}	
					else if(i instanceof FADD || i instanceof FSUB || i instanceof FDIV || i instanceof FMUL)
					{	
						foldFloat(handle, i, iList, cpgen);
					}		
					else if(i instanceof DADD || i instanceof DSUB || i instanceof DDIV || i instanceof DMUL)
					{	
						foldDouble(handle, i, iList, cpgen);
					}
					else if(i instanceof LADD || i instanceof LSUB || i instanceof LDIV || i instanceof LMUL)
					{	
						foldLong(handle, i, iList, cpgen);
					}
				}
			};
			if (iList != null) 	//dead code removal
			{ 
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
			}
			iList.setPositions(true);
			methodGen.setInstructionList(iList);
			methodGen.setMaxStack();
			methodGen.setMaxLocals();

			Method newMethod = methodGen.getMethod();
			gen.replaceMethod(method, newMethod);
		}
		gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
	}
	
	public void foldInt(InstructionHandle handle, Instruction i, InstructionList iList , ConstantPoolGen cpgen)
	{
		Instruction ld1 = handle.getPrev().getInstruction();
		Instruction ld2 = handle.getPrev().getPrev().getInstruction();

		Integer val1, val2;
		Integer result = 0;
	
		if (ld1 instanceof LDC && ld2 instanceof LDC)
		{
			val1 = (Integer)((LDC)ld1).getValue(cpgen);
			val2 = (Integer)((LDC)ld2).getValue(cpgen);
			
			if(i instanceof IADD)
			{result = val1 + val2; }
			else if(i instanceof ISUB)
			{result = val1 - val2; }
			else if(i instanceof IMUL)
			{result = val1 * val2 ;}
			else if(i instanceof IDIV)
			{result = val1 / val2; }

			Integer index = cpgen.addInteger(result);
			iList.insert(handle, new LDC(index)); 
			try 
			{
				iList.delete(ld2);
				iList.delete(ld1);		
				iList.delete(i);
					
			} catch (TargetLostException e) {
				System.err.println("Could not delete instruction");
			}	
		}
	}
	
	public void foldFloat(InstructionHandle handle, Instruction i, InstructionList iList , ConstantPoolGen cpgen)
	{
		Instruction ld1 = handle.getPrev().getInstruction();
		Instruction ld2 = handle.getPrev().getPrev().getInstruction();

		Float val1, val2;
		Float result = 0f;
	
		if (ld1 instanceof LDC && ld2 instanceof LDC)
		{
			val1 = (Float)((LDC)ld1).getValue(cpgen);
			val2 = (Float)((LDC)ld2).getValue(cpgen);
			
			if(i instanceof FADD)
			{result = val1 + val2; }
			else if(i instanceof FSUB)
			{result = val1 - val2; }
			else if(i instanceof FMUL)
			{result = val1 * val2 ;}
			else if(i instanceof FDIV)
			{result = val1 / val2 ;} 

			Integer index = cpgen.addFloat(result);
			iList.insert(handle, new LDC(index)); 
			try 
			{
				iList.delete(ld2);
				iList.delete(ld1);		
				iList.delete(i);
					
			} catch (TargetLostException e) {
				System.err.println("Could not delete instruction");
			}	
		}
	}

	public void foldDouble(InstructionHandle handle, Instruction i, InstructionList iList , ConstantPoolGen cpgen)
	{
		Instruction ld1 = handle.getPrev().getInstruction();
		Instruction ld2 = handle.getPrev().getPrev().getInstruction();

		Double val1, val2;
		Double result = 0.0;
	
		if (ld1 instanceof LDC2_W && ld2 instanceof LDC2_W)
		{
			val1 = (Double)((LDC2_W)ld1).getValue(cpgen);
			val2 = (Double)((LDC2_W)ld2).getValue(cpgen);
			
			if(i instanceof DADD)
			{result = val1 + val2; }
			else if(i instanceof DSUB)
			{result = val1 - val2; }
			else if(i instanceof DMUL)
			{result = val1 * val2 ;}
			else if(i instanceof DDIV)
			{result = val1 / val2 ;} 

			Integer index = cpgen.addDouble(result);
			iList.insert(handle, new LDC2_W(index)); 
			try 
			{
				iList.delete(ld2);
				iList.delete(ld1);		
				iList.delete(i);
					
			} catch (TargetLostException e) {
				System.err.println("Could not delete instruction");
			}	
		}
	}

	public void foldLong(InstructionHandle handle, Instruction i, InstructionList iList , ConstantPoolGen cpgen)
	{
		Instruction ld1 = handle.getPrev().getInstruction();
		Instruction ld2 = handle.getPrev().getPrev().getInstruction();

		Long val1, val2;
		Long result = 0l;
	
		if (ld1 instanceof LDC2_W && ld2 instanceof LDC2_W)
		{
			val1 = (Long)((LDC2_W)ld1).getValue(cpgen);
			val2 = (Long)((LDC2_W)ld2).getValue(cpgen);
			
			if(i instanceof LADD)
			{result = val1 + val2; }
			else if(i instanceof LSUB)
			{result = val1 - val2; }
			else if(i instanceof LMUL)
			{result = val1 * val2 ;}
			else if(i instanceof LDIV)
			{result = val1 / val2 ;} 

			Integer index = cpgen.addLong(result);
			iList.insert(handle, new LDC2_W(index)); 
			try 
			{
				iList.delete(ld2);
				iList.delete(ld1);		
				iList.delete(i);
					
			} catch (TargetLostException e) {
				System.err.println("Could not delete instruction");
			}	
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