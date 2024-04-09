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
		if (!((cgen.getClassName()).equals("comp0012.target.SimpleFolding")))
	    {
			this.optimized = gen.getJavaClass();
			return;
		}
		ConstantPoolGen cpgen = cgen.getConstantPool();

		Method[] methods = cgen.getMethods();

		for(Method method : methods)
		{
			MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
			InstructionList iList = methodGen.getInstructionList();
			for (InstructionHandle handle : iList.getInstructionHandles())
			{
				Instruction instruction = handle.getInstruction();
				if(instruction instanceof ArithmeticInstruction)
				{
					if(instruction instanceof IADD) //extend later to other classes
					{	
		 			// System.out.println("INSTRUCTION = " + instruction);
						
						Instruction ld1 = handle.getPrev().getInstruction();
						Instruction ld2 = handle.getPrev().getPrev().getInstruction();

						Integer val1, val2, result;

						if (ld1 instanceof LDC && ld2 instanceof LDC)
						{
							val1 = (Integer)((LDC)ld1).getValue(cpgen);
							val2 = (Integer)((LDC)ld2).getValue(cpgen);
							result = val1 + val2;

							// System.out.println("Result = " + result);

							Integer index = cpgen.addInteger(result);
							iList.insert(handle, new LDC(index)); 
							try 
							{
								iList.delete(ld2);
								iList.delete(ld1);		
								iList.delete(instruction);
									
							} catch (TargetLostException e) {
								System.err.println("Could not delete instruction");
							}	
						}
					}		
				}
			};
			// iList.setPositions(true);
			methodGen.setInstructionList(iList);
			methodGen.setMaxStack();
			methodGen.setMaxLocals();

			Method newMethod = methodGen.getMethod();
			gen.replaceMethod(method, newMethod);
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