package de.ipbhalle.metfrag.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class GenerateBeilsteinShQsub {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String writePath = "";
		
		if(args[0] != null)
			writePath = args[0];
		else
		{
			System.err.println("Error no path given!");
			System.exit(1);
		}
		
		String path = "/vol/mirrors/INP/data/";
		//loop over all files in folder
		File f = new File(path);
		File files[] = f.listFiles();
		Arrays.sort(files);
		

		int count = 0;
		int globalCount = 0;
		String fileNames = "";
		for (int i = 0; i < files.length; i++) {
			
			String fileName = files[i].getName();
			int dotPos = fileName.indexOf(".");
			String extension = "";
			if(dotPos >= 0)
				extension = fileName.substring(dotPos);

			if(files[i].isFile() && extension != "" && extension.equals(".txt"))
			{
				File f2 = new File(writePath + "Beilstein_" + globalCount + ".sh"); 
				
				BufferedWriter out = new BufferedWriter(new FileWriter(f2));
				out.write("#!/bin/bash");
				out.newLine();
		  		out.write("java -jar /home/swolf/sgeQsubScripts/BeilsteinToMassBankDB.jar \"" + files[i].getName() + "\"");
			  	out.close();
			  	
			  	count = 0;
			  	fileNames = "";
			  	globalCount++;
			}
		}

	}

}
