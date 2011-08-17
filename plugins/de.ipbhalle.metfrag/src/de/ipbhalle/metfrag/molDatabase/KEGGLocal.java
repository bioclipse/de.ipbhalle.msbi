package de.ipbhalle.metfrag.molDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

import de.ipbhalle.metfrag.tools.MolecularFormulaTools;



public class KEGGLocal {

	private String url = ""; 
    private String username = ""; 
    private String password = ""; 
	
	
	public KEGGLocal(String databseUrl, String username, String password)
	{
		this.url = databseUrl;
		this.username = username;
		this.password = password;
		
	}
	
	/**
	 * Gets the hits (only the pubchem id's) from a local database with a MassBank schema
	 * and with specified lower and upper bound.
	 * 
	 * @param limit the limit
	 * @param lowerBound the lower bound
	 * @param upperBound the higher bound
	 * 
	 * @return the hits
	 * 
	 * @throws SQLException the SQL exception
	 * @throws ClassNotFoundException the class not found exception
	 */
	public List<String> getHits(int limit, double lowerBound, double upperBound) throws SQLException, ClassNotFoundException
	{
		List<String> candidatesString = new ArrayList<String>();    
		System.out.println("Lower bound: " + lowerBound + " Upper bound: " + upperBound);
		//now retrieve the search results
        String driver = "com.mysql.jdbc.Driver"; 
        Connection con = null; 
		Class.forName(driver); 
		DriverManager.registerDriver (new com.mysql.jdbc.Driver()); 
        // JDBC-driver
        Class.forName(driver);
        con = DriverManager.getConnection(url, username, password);
	    
        Statement stmt = null;
	    stmt = con.createStatement();
	    //now select only the pubchem entries!
	    ResultSet rs = stmt.executeQuery("SELECT ID FROM RECORD WHERE CHONSP = '1' and SUBSTRING(ID,1,1) = 'C' and EXACT_MASS >= '" + lowerBound + "' and EXACT_MASS <= '" + upperBound + "' limit " + limit);

	    while(rs.next())
	    {
	    	candidatesString.add(rs.getString("id"));
	    }
        con.close();
        
        return candidatesString;
	}
	
	
	/**
	 * Gets the mol file from the specified database ID.
	 * 
	 * @param KEGGID the kEGGID
	 * 
	 * @return the mol
	 * 
	 * @throws SQLException the SQL exception
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InvalidSmilesException the invalid smiles exception
	 */
	public IAtomContainer getMol(String KEGGID) throws SQLException, ClassNotFoundException, InvalidSmilesException
	{
		//now retrieve the search results
        String driver = "com.mysql.jdbc.Driver"; 
        Connection con = null; 
		Class.forName(driver); 
		DriverManager.registerDriver (new com.mysql.jdbc.Driver()); 
        // JDBC-driver
        Class.forName(driver);
        con = DriverManager.getConnection(url, username, password);
        Statement stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT SMILES FROM RECORD WHERE ID = '" + KEGGID + "';");
	    SmilesParser sp1 = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	    IAtomContainer molecule = null;
	    while(rs.next())
	    {
	    	//Name
	    	System.out.print(KEGGID);
	    	String smiles = rs.getString("smiles");
	    	if(MolecularFormulaTools.isBiologicalCompound(smiles))
	    		molecule = sp1.parseSmiles(smiles);
	    }
	    con.close();
	    
	    return molecule;
	}

}
