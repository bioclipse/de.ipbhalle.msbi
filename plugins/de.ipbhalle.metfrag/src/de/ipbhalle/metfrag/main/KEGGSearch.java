package de.ipbhalle.metfrag.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import org.openscience.cdk.Molecule;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;

import de.ipbhalle.metfrag.fragmenter.Fragmenter;
import de.ipbhalle.metfrag.fragmenter.NeutralLoss;
import de.ipbhalle.metfrag.keggWebservice.KeggWebservice;
import de.ipbhalle.metfrag.massbankParser.Peak;
import de.ipbhalle.metfrag.molDatabase.KEGGLocal;
import de.ipbhalle.metfrag.read.Molfile;
import de.ipbhalle.metfrag.scoring.Scoring;
import de.ipbhalle.metfrag.similarity.Similarity;
import de.ipbhalle.metfrag.similarity.SimilarityGroup;
import de.ipbhalle.metfrag.tools.DisplayStructure;
import de.ipbhalle.metfrag.tools.GetKEGGIdentifier;
import de.ipbhalle.metfrag.tools.PPMTool;
import de.ipbhalle.metfrag.tools.Render;
import de.ipbhalle.metfrag.tools.WritePDFTable;



public class KEGGSearch {
	
	
	private Vector<String> blackList;
    private String completeLog = "";
    private int foundPeaks = 0;
    private int allPeaks = 0;
    private boolean showDiagrams = false;
    private Vector<String> doneMols = new Vector<String>();
    private HashMap<Integer, ArrayList<String>> scoreMap = new HashMap<Integer, ArrayList<String>>();
    private String jdbc = "";
    private String username = "";
    private String password = "";
    private String histogram = "";
    private String histogramCompare = "";
    private String histogramReal = "";
    private String histogramPeaks = "";
    private String histogramPeaksAll = "";
    private String histogramPeaksReal = "";
    //list of peaks which are contained in the corresponding molecule
	private Vector<Peak> listOfPeaksCorresponding = new Vector<Peak>();
	//list of peaks which are not contained in the real molecule
	private Vector<Peak> listOfPeaks = new Vector<Peak>();
	private boolean hydrogenTest = false;
	private String keggPath = "";
    private long sumTime = 0;
	
    
	
	/**
	 * Instantiates a new KEGG search.
	 * 
	 * @param folder the folder
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param pdf the pdf
	 * @param showDiagrams the show diagrams
	 * @param recreateFrags the recreate frags
	 * @param breakAromaticRings the break aromatic rings
	 * @param sumFormulaRedundancyCheck the experimental redundancy check
	 * @param spectrum the spectrum
	 * @param username the username
	 * @param password the password
	 * @param jdbc the jdbc
	 * @param treeDepth the tree depth
	 * @param keggPath the kegg path
	 */
	public KEGGSearch(String folder, WrapperSpectrum spectrum, double mzabs, double mzppm, boolean pdf, boolean showDiagrams, boolean recreateFrags, boolean breakAromaticRings, boolean sumFormulaRedundancyCheck, String username, String password, String jdbc, int treeDepth, boolean hydrogenTest, String keggPath, boolean neutralLossCheck, boolean bondEnergyScoring, boolean isOnlyBreakSelectedBonds)
	{
		this.username = username;
		this.password = password;
		this.jdbc = jdbc;
		BlackList bl = new BlackList(sumFormulaRedundancyCheck);
		this.blackList = bl.getBlackList();
		this.hydrogenTest = hydrogenTest;
		this.keggPath = keggPath;
		this.showDiagrams = showDiagrams;
		KEGG(folder, spectrum, mzabs, mzppm, recreateFrags, breakAromaticRings, sumFormulaRedundancyCheck, pdf, treeDepth, neutralLossCheck, bondEnergyScoring, isOnlyBreakSelectedBonds);
	}
	
	
	/**
	 * Gets the complete log.
	 * 
	 * @return the complete log
	 */
	public String getCompleteLog()
	{
		return completeLog;
	}
	
	/**
	 * Gets the found peaks.
	 * 
	 * @return the found peaks
	 */
	public int getFoundPeaks()
	{
		return foundPeaks;
	}
	
	/**
	 * Gets the vector of peaks.
	 * 
	 * @return the vector of peaks
	 */
	public Vector<Peak> getVectorOfPeaks()
	{
		return this.listOfPeaks;
	}
	
	/**
	 * Gets the vector of correct peaks.
	 * 
	 * @return the vector of correct peaks
	 */
	public Vector<Peak> getVectorOfCorrectPeaks()
	{
		return this.listOfPeaksCorresponding;
	}
    
	/**
	 * Gets the all peaks.
	 * 
	 * @return the all peaks
	 */
	public int getAllPeaks()
	{
		return allPeaks;
	}
	
	/**
	 * Gets the histogram.
	 * 
	 * @return the histogram
	 */
	public String getHistogram()
	{
		return histogram;
	}
	
    /**
     * Histogram compare.
     * 
     * @return the string
     */
    public String getHistogramCompare()
    {
    	return histogramCompare;
    }
    
    /**
     * Gets the histogram real.
     * 
     * @return the histogram real
     */
    public String getHistogramReal()
    {
    	return histogramReal;
    }
    
    /**
     * Gets the histogram peaks.
     * 
     * @return the histogram peaks
     */
    public String getHistogramPeaks()
    {
    	return histogramPeaks;
    }
    
    /**
     * Gets the histogram peaks real.
     * 
     * @return the histogram peaks real
     */
    public String getHistogramPeaksReal()
    {
    	return histogramPeaksReal;
    }
    
    public String getHistogramPeaksAll()
    {
    	return this.histogramPeaksAll;
    }
    
	
	/**
	 * Add mol name without extension to the already done molecules
	 * 
	 * @param mol the mol
	 */
	private void addMol(String mol)
	{
		this.doneMols.add(mol);
	}
	
	/**
	 * Already done mol files.
	 * 
	 * @param candidate the candidate molecule
	 * 
	 * @return true, if successful
	 */
	private boolean alreadyDone(String candidate)
	{
		boolean test = false;
		
		if(doneMols.contains(candidate))
			test = true;
		
		return test;
	}
		
	/**
	 * Test one spectrum in KEGG. Download all the hits by mass and fragment them.
	 * 
	 * @param folder the folder
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param mergedSpectrum the merged spectrum
	 * @param recreateFrags the recreate frags
	 * @param breakAromaticRings the break aromatic rings
	 * @param experimentalRedundancyCheck the experimental redundancy check
	 * @param pdf the pdf
	 * @param treeDepth the tree depth
	 */
	private void KEGG(String folder, WrapperSpectrum mergedSpectrum, Double mzabs, Double mzppm, boolean recreateFrags, boolean breakAromaticRings, boolean experimentalRedundancyCheck, boolean pdf, int treeDepth, boolean neutralLossCheck, boolean bondEnergyScoring, boolean isOnlyBreakSelectedBonds)
	{
		String file = mergedSpectrum.getFilename();
		
		if(blackList.contains(file))
		{
			completeLog += "Blacklisted Molecule: " + file; 
			histogramReal += "\n" + file + "\tBLACKLIST\t";
			histogram += "\n" + file + "\tBLACKLIST\t";
			histogramCompare += "\n" + file + "\tBLACKLIST\t";
			return;
		}
		//timing
		long timeStart = System.currentTimeMillis();  
		
		//Test Data
		WrapperSpectrum spectrum = mergedSpectrum;
		Vector<Peak> peakList = spectrum.getPeakList();
		Map<Double, Vector<String>> realScoreMap = new HashMap<Double, Vector<String>>();
		
		double exactMass = spectrum.getExactMass();
		
		
		int mode = spectrum.getMode();
		
		//instatiate and read in CID-KEGG.txt
		String keggIdentifier = spectrum.getKEGG();
		
		if(keggIdentifier.equals("none"))
		{
			try
			{
				GetKEGGIdentifier keggID = new GetKEGGIdentifier(folder + "CID-KEGG/CID-KEGG.txt");
				//now find the corresponding KEGG entry
				if(keggID.existInKEGG(spectrum.getCID()))
					keggIdentifier = keggID.getKEGGID(spectrum.getCID());
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage());
				completeLog += "Error! Message: " + e.getMessage();
			}
		}
		
		completeLog += "\n\n============================================================================";
		completeLog += "\nFile: " + file + " (KEGG Entry: " + keggIdentifier + ")";
		
		//get candidates from kegg webservice...with with a given mzppm and mzabs
//		Vector<String> candidates = KeggWebservice.KEGGbyMass(exactMass, (mzabs+PPMTool.getPPM(exactMass, mzppm)));
		KEGGLocal kegg = new KEGGLocal(jdbc, username, password);
		double lowerBound = exactMass -(mzabs + PPMTool.getPPM(exactMass, mzppm)); 
		double upperBound = exactMass +(mzabs + PPMTool.getPPM(exactMass, mzppm)); 
		List<String> candidates = null;
		try {
			candidates = kegg.getHits(100, lowerBound, upperBound);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//comparison histogram
		if(keggIdentifier.equals("none"))
			histogramCompare += "\n" + file + "\t" + keggIdentifier + "\t\t" + exactMass;
		else
			histogramCompare += "\n" + file + "\t" + keggIdentifier + "\t" + candidates.size() + "\t" + exactMass;
		
		
		Map<String, Double> mapCandidateToEnergy = new HashMap<String, Double>();
		Map<String, Double> mapCandidateToHydrogenPenalty = new HashMap<String, Double>();
		//List storing all candidate structures
		Map<String, IAtomContainer> candidateToStructure = new HashMap<String, IAtomContainer>();
		
		//loop over all hits
		for (int c = 0; c < candidates.size(); c++) {
			
			//skip already done files kegg entries
			if(alreadyDone(candidates.get(c)))
				continue;
			
			//add mol to finished
			addMol(candidates.get(c));
			
			//get mol file from kegg....remove "cpd:"
			String candidate = KeggWebservice.KEGGgetMol(candidates.get(c), keggPath);
			
			
			try
			{	            
				//now fragment the retrieved molecule
		        IAtomContainer molecule = Molfile.ReadFromString(candidate);
		        
		        
		        List<IAtomContainer> l = null;
		        
		        //if the folder was created...if the folder exists --> skip....there are already fragments
		        if (recreateFrags)
		        {
			        System.out.println("Folder created: " + folder + file);
			        
			        try
			        {
				        //add hydrogens
				        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
				        CDKHydrogenAdder hAdder = CDKHydrogenAdder.getInstance(molecule.getBuilder());
				        hAdder.addImplicitHydrogens(molecule);
				        AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
			        }
			        //there is a bug in cdk?? error happens when there is a S or Ti in the molecule
			        catch(IllegalArgumentException e)
		            {
		            	completeLog += "Error: " + candidates.get(c) + " Message: " + e.getMessage();
		            	//skip it
		            	continue;
		            }
			        
			        Fragmenter fragmenter = new Fragmenter((Vector<Peak>)peakList.clone(), mzabs, mzppm, mode, breakAromaticRings, experimentalRedundancyCheck, neutralLossCheck, isOnlyBreakSelectedBonds);
			        long start = System.currentTimeMillis();
			        List<File> generatedFrags = null;
			        try
			        {
			        	generatedFrags = fragmenter.generateFragmentsEfficient(molecule, true, treeDepth, candidates.get(c));
				        l = Molfile.ReadfolderTemp(generatedFrags);
			        	
//			        	l = fragmenter.generateFragments(molecule, true, treeDepth);
			        	System.out.println("Candidate: " + candidates.get(c) + " Fragments: " + l.size());
			        }
			        catch(OutOfMemoryError e)
			        {
			        	System.out.println("OUT OF MEMORY ERROR! " + candidates.get(c));
			        	completeLog += "Error: " + candidates.get(c) + " Message: " + e.getMessage();
			        	continue;
			        }
			        long time = System.currentTimeMillis() - start;
			        

			        
			        System.out.println("Benötigte Zeit: " + time);
			        System.out.println("Got " + l.size() + " fragments");
			        System.out.println("Needed " + fragmenter.getNround() + " calls to generateFragments()");
			        
			        
		        
			        //Draw molecule and its fragments
			        if (showDiagrams)
			        	Render.Draw(molecule,l, "Original Molecule"); 
			        
			        if(pdf)
			        {
				        //Create PDF Output
				        l.add(molecule);
			        	DisplayStructure ds1 = null;
			        	//create pdf subfolder
			        	new File(folder + file + "/" + candidates.get(c) + "pdf/").mkdir();
			        	ds1 = new WritePDFTable(true, 300, 300, 0.9, 2, false, false, folder + file + "/" + candidates.get(c) + "pdf/");
			        	for (int i = 0; i < l.size(); i++) {
			                //ds = new displayStructure(false, 300, 300, 0.9, false, "PDF", "/home/basti/WorkspaceJava/TandemMSLookup/fragmenter/Test");
			                assert ds1 != null;
			                ds1.drawStructure(l.get(i), i);
			    		}
				        
				        if (ds1 != null) ds1.close();
			        }
		        }
		        else
		        	System.out.println("Could not create folder...frags already computed!!!!");
		        
				//now read the saved mol files
				List<IAtomContainer> fragments = l;
				
				//get the original peak list again
				peakList = spectrum.getPeakList();
				//clean up peak list
				CleanUpPeakList cList = new CleanUpPeakList(peakList);
				Vector<Peak> cleanedPeakList = cList.getCleanedPeakList(spectrum.getExactMass());
				
				
				//now find corresponding fragments to the mass
				AssignFragmentPeak afp = new AssignFragmentPeak();
				afp.setHydrogenTest(hydrogenTest);
				afp.AssignFragmentPeak(fragments, cleanedPeakList, mzabs, mzppm, spectrum.getMode(), false);
				Vector<PeakMolPair> hits = afp.getHits();
				
				Scoring score = new Scoring(spectrum.getPeakList());
				
				double currentScore = 0.0;
				if(bondEnergyScoring)
					currentScore = score.computeScoringWithBondEnergies(hits);
				else
					currentScore = score.computeScoringPeakMolPair(hits);
				
				double currentBondEnergy = score.getFragmentBondEnergy();
	
				if(currentBondEnergy > 0)
					currentBondEnergy = currentBondEnergy / afp.getHits().size();
				//set the added up energy of every fragment
				mapCandidateToEnergy.put(candidates.get(c), currentBondEnergy);
				mapCandidateToHydrogenPenalty.put(candidates.get(c), score.getPenalty());

				
				
				//save score in hashmap...if there are several hits with the same score --> vector of strings
				if(realScoreMap.containsKey(currentScore))
		        {
		        	Vector<String> tempList = realScoreMap.get(currentScore);
		        	tempList.add(candidates.get(c));
		        	realScoreMap.put(currentScore, tempList);
		        }
		        else
		        {
		        	Vector<String> temp = new Vector<String>();
		        	temp.add(candidates.get(c));
		        	realScoreMap.put(currentScore, temp);
		        }
				
				
				//save score in hashmap...if there are several hits with the same
				//amount of identified peaks --> ArrayList
				if(scoreMap.containsKey(hits.size()))
		        {
		        	ArrayList<String> tempList = scoreMap.get(hits.size());
		        	tempList.add(candidates.get(c));
		        	scoreMap.put(hits.size(), tempList);
		        }
		        else
		        {
		        	ArrayList<String> temp = new ArrayList<String>();
		        	temp.add(candidates.get(c));
		        	scoreMap.put(hits.size(), temp);
		        }
				
			
				//get all the identified peaks
				String peaks = "";
				for (int i = 0; i < hits.size(); i++) {
					String nlString = "";
					if(hits.get(i).getFragment().getProperty("NlElementalComposition") != null)
						nlString = " -" + (String)hits.get(i).getFragment().getProperty("NlElementalComposition");
					
					peaks += hits.get(i).getPeak().getMass() + nlString +  "[" + hits.get(i).getFragment().getProperty("BondEnergy") + "]" +  " ";
//					peaks += hits.get(i).getPeak().getMass() + " ";
					listOfPeaks.add(hits.get(i).getPeak());
					if(keggIdentifier.equals(candidates.get(c)))
						listOfPeaksCorresponding.add(hits.get(i).getPeak());
				}
				//write things to log file
				foundPeaks += hits.size();
				allPeaks += spectrum.getPeakList().size();
				completeLog += "\nFile: " + candidates.get(c) + "\t #Peaks: " + spectrum.getPeakList().size() + "\t #Found: " + hits.size();
				completeLog += "\tPeaks: " + peaks;
				
				List<IAtomContainer> hitsListTest = new ArrayList<IAtomContainer>();
				for (int i = 0; i < hits.size(); i++) {
					List<IAtomContainer> hitsList = new ArrayList<IAtomContainer>();
					hitsList.add(AtomContainerManipulator.removeHydrogens(hits.get(i).getFragment()));
					hitsListTest.add(hits.get(i).getFragment());
					//Render.Highlight(AtomContainerManipulator.removeHydrogens(molecule), hitsList , Double.toString(hits.get(i).getPeak()));
				}
				if (showDiagrams)
					Render.Draw(molecule, hitsListTest , "Fragmente von: " + candidates.get(c));
				
				
				//map storing all candidate structures...analyze further on the similarity of the hits
				candidateToStructure.put(candidate, molecule);
				
			}
			catch(CDKException e)
			{
				System.out.println("CDK error!" + e.getMessage());
				completeLog += "CDK Error! " + e.getMessage() + "File: " + candidates.get(c);
			}
			catch(FileNotFoundException e)
			{
				System.out.println("File not found" + e.getMessage());
				completeLog += "File not found error! "+ e.getMessage() + "File: " + candidates.get(c);
			}
			catch(IOException e)
			{
				System.out.println("IO error: " + e.getMessage());
				completeLog += "IO Error! "+ e.getMessage() + "File: " + candidates.get(c);
			}
			catch(Exception e)
			{
				System.out.println("Error " + e.getMessage() + "\n");
				e.printStackTrace();
				completeLog += "Error! "+ e.getMessage() + "File: " + candidates.get(c);
			}
			catch(OutOfMemoryError e)
			{
				System.out.println("Out of memory: " + e.getMessage() + "\n" + e.getStackTrace());
				System.gc();
				completeLog += "Out of memory! "+ e.getMessage() + "File: " + candidates.get(c);
			}
		}
		
		//only combine scores when there are more than two layers generated
		if(bondEnergyScoring)
			realScoreMap = Scoring.getCombinedScore(realScoreMap, mapCandidateToEnergy, mapCandidateToHydrogenPenalty);
		
		//easy scoring
		Integer[] keylist = new Integer[scoreMap.keySet().size()];
		Object[] keys = scoreMap.keySet().toArray();
		
		for (int i = 0; i < keys.length; i++) {
			keylist[i] = Integer.parseInt(keys[i].toString());
		}
		
		Arrays.sort(keylist);
		String scoreList = "";
		int place = 0;
		for (int i = keylist.length-1; i >= 0; i--) {
			boolean check = false;
			for (int j = 0; j < scoreMap.get(keylist[i]).size(); j++) {
				scoreList += "\n" + keylist[i] + " - " + scoreMap.get(keylist[i]).get(j);
				if(keggIdentifier.equals(scoreMap.get(keylist[i]).get(j)))
				{
					check = true;
				}
				//worst case: count all which are better or have a equal position
				place++;
			}
			if(check)
			{
				histogram += "\n" + file + "\t" + keggIdentifier + "\t" + place + "\t" + exactMass;
			}
		}
		
		if(keggIdentifier.equals("none"))
		{
			histogram += "\n" + file + "\t" + keggIdentifier + "\t\t" + exactMass;
		}
		
		completeLog += "\n\n*****************Scoring*****************************";
		completeLog += "Supposed to be: " + keggIdentifier;
		completeLog += scoreList;
		completeLog += "\n*****************************************************\n\n";
		//easy scoring end
		
		
		
		//real scoring
		Double[] keysScore = new Double[realScoreMap.keySet().size()];
		keysScore = realScoreMap.keySet().toArray(keysScore);
		
		Arrays.sort(keysScore);
		String scoreListReal = "";
		//now create the tanimoto distance matrix
		//to be able to group results with the same score
		//search molecules with the same connectivity
		String similarity = "";
		int rankTanimotoGroup = 0;
		int rankIsomorphism = 0;
		int rankWorstCase = 0;
		boolean stop = false;
		try {
			Similarity sim = new Similarity(candidateToStructure, (float)0.95);
			for (int i = keysScore.length-1; i >= 0; i--) {
				similarity += "\nScore: " + keysScore[i] + "\n";
				List<String> candidateGroup = new ArrayList<String>();
				for (int j = 0; j < realScoreMap.get(keysScore[i]).size(); j++) {
					candidateGroup.add(realScoreMap.get(keysScore[i]).get(j));
				}

				List<SimilarityGroup> groupedCandidates = sim.getTanimotoDistanceList(candidateGroup);
				for (SimilarityGroup similarityGroup : groupedCandidates) {				
					List<String> tempSimilar = similarityGroup.getSimilarCompounds();
					List<Float> tempSimilarTanimoto = similarityGroup.getSimilarCompoundsTanimoto();
					similarity += similarityGroup.getCandidateTocompare() + ": ";
					
					if(keggIdentifier.equals(similarityGroup.getCandidateTocompare()))
						stop = true;					
					
					for (int k = 0; k < tempSimilar.size(); k++) {

						if(keggIdentifier.equals(tempSimilar.get(k)))
							stop = true;
						
						similarity += tempSimilar.get(k) + "(" +  tempSimilarTanimoto.get(k);
					
						boolean isIsomorph = sim.isIsomorph(tempSimilar.get(k), similarityGroup.getCandidateTocompare());
						if(!isIsomorph)
							rankIsomorphism++;
						
						similarity += " -" + isIsomorph + ") ";
					}
					similarity += "\n";						
					rankTanimotoGroup++;
					rankIsomorphism++;
				}
				if(stop)
					break;
			}
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for (int i = keysScore.length-1; i >= 0; i--) {
			boolean check = false;
			int temp = 0;
			for (int j = 0; j < realScoreMap.get(keysScore[i]).size(); j++) {
				scoreListReal += "\n" + keysScore[i] + " - " + realScoreMap.get(keysScore[i]).get(j);
				if(keggIdentifier.compareTo(realScoreMap.get(keysScore[i]).get(j)) == 0)
				{
					check = true;
				}
				//worst case: count all which are better or have a equal position
				rankWorstCase++;
				temp++;
			}
			//add it to rank best case
			histogramReal = "\n" + file + "\t" + keggIdentifier + "\t" + rankWorstCase + "\t" + rankTanimotoGroup + "\t" + rankIsomorphism + "\t" + exactMass;

		}
		
		//timing
		long timeEnd = System.currentTimeMillis() - timeStart;
        sumTime += timeEnd;
		
		completeLog += "\n\n*****************Scoring(Real)*****************************";
		completeLog += "Supposed to be: " + keggIdentifier;
		completeLog += "\nTime: " + timeEnd;
		completeLog += scoreListReal;
		completeLog += "\n*****************************************************\n\n";	
		

		//write the data for peak histogram to log file
		for (int i = 0; i < listOfPeaks.size(); i++) {
			histogramPeaksAll += listOfPeaks.get(i) + "\n";
		}	
		
		//filter the peaks which are contained in the all peaks list. (exclusive)
		for (int i = 0; i < listOfPeaksCorresponding.size(); i++) {
			for (int j = 0; j < listOfPeaks.size(); j++) {
				Double valueA = listOfPeaks.get(j).getMass();
				Double valueB = listOfPeaksCorresponding.get(i).getMass();
				if(valueA.compareTo(valueB) == 0)
				{
					listOfPeaks.remove(j);
					break;
				}
			}
		}
		
		for (int i = 0; i < listOfPeaks.size(); i++) {
			histogramPeaks += listOfPeaks.get(i) + "\n";
		}
		
		for (int i = 0; i < listOfPeaksCorresponding.size(); i++) {
			histogramPeaksReal += listOfPeaksCorresponding.get(i) + "\n";
		}
		
		
	}

}
