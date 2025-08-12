package com.opal.creator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.siliconage.xml.ParseContext;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;

public class OpalParseContext extends ParseContext {
	private String myProjectName;
	private String mySourceDirectory;
	private String myDefaultSchema;
	private String myDefaultPackage;
	private String myPreviousSubpackage;
	private String myCurrentSubpackage;
	private String myApplicationSubpackage = "application";
	private String myPersistenceSubpackage = "persistence";
	private String myCMASubpackage = "cma";
	private String myDatabaseSubpackage;
	private String myAuthor;
	private String myDataSourceJava;
	
	private final DBOpts myCLDBOpts;
//	private final String myDatabaseUsername; // may be null
//	private final String myDatabasePassword; // may be null
	
	private ArrayList<String> myComparatorColumnNameList = new ArrayList<>();
	private ArrayList<String> myFilterColumnNameList = new ArrayList<>();
	private ArrayList<String> myUnmappedColumnNameList = new ArrayList<>();
	
	private SortedMap<TableName, MappedClass> myMappedClasses = new TreeMap<>(Comparator.comparing(TableName::getFullyQualifiedTableName));
	
	private RelationalDatabaseAdapter myRelationalDatabaseAdapter;
	
	/* package */ static final String DEFAULT_POOL_NAME = "Default";
	private Map<String, String> myPoolMap = new HashMap<>();
	
	public OpalParseContext(DBOpts argCLDBOpts) {
		super();

		if (argCLDBOpts == null) {
			throw new IllegalStateException("argCLDBOpts is null");
		}
		myCLDBOpts = argCLDBOpts;
//		myDatabaseUsername = argDatabaseUsername; // may be null
//		myDatabasePassword = argDatabasePassword; // may be null
	}
	
//	public OpalParseContext() {
//		this(null, null);
//	}
	
	public String getDefaultPackage() {
		return myDefaultPackage;
	}

	public String getDefaultSchema() {
		return myDefaultSchema;
	}
	
	public SortedMap<TableName, MappedClass> getMappedClasses() {
		return myMappedClasses;
	}
	
	protected String getProjectName() {
		return myProjectName;
	}
	
	public RelationalDatabaseAdapter getRelationalDatabaseAdapter() {
		return myRelationalDatabaseAdapter;
	}
	
	public Map<String, String> getPoolMap() {
		return myPoolMap;
	}
	
	public java.lang.String getSourceDirectory() {
		return mySourceDirectory;
	}
	
	protected void setDefaultPackage(String argDefaultPackage) {
		myDefaultPackage = argDefaultPackage;
	}
	
	public String getCurrentSubpackage() {
		return myCurrentSubpackage;
	}
	
	protected void setCurrentSubpackage(String argSubpackage) {
		myCurrentSubpackage = argSubpackage;
	}
	
	public String getPreviousSubpackage() {
		return myPreviousSubpackage;
	}
	
	protected void setPreviousSubpackage(String argSubpackage) {
		myPreviousSubpackage = argSubpackage;
	}
	
	protected void setDefaultSchema(String argDefaultSchema) {
		myDefaultSchema = argDefaultSchema;
	}
	
	protected void setProjectName(String argProjectName) {
		myProjectName = argProjectName;
	}
	
	protected void setRelationalDatabaseAdapter(RelationalDatabaseAdapter newRelationalDatabaseAdapter) {
		myRelationalDatabaseAdapter = newRelationalDatabaseAdapter;
	}
	
	protected void setSourceDirectory(java.lang.String newSourceDirectory) {
		mySourceDirectory = newSourceDirectory;
	}
	
	public String getApplicationSubpackage() {
		return myApplicationSubpackage;
	}
	
	public String getDatabaseSubpackage() {
		return myDatabaseSubpackage;
	}
	
	public String getPersistenceSubpackage() {
		return myPersistenceSubpackage;
	}
	
	public String getCMASubpackage() {
		return myCMASubpackage;
	}
	
	public void setApplicationSubpackage(String argApplicationSubpackage) {
		myApplicationSubpackage = argApplicationSubpackage;
	}
	
	public void setDatabaseSubpackage(String argDatabaseSubpackage) {
		myDatabaseSubpackage = argDatabaseSubpackage;
	}
	
	public void setPersistenceSubpackage(String argPersistenceSubpackage) {
		myPersistenceSubpackage = argPersistenceSubpackage;
	}
	
	public void setCMASubpackage(String argCMASubpackage) {
		myCMASubpackage = argCMASubpackage;
	}
	
	public String getAuthor() {
		return myAuthor;
	}
	
	public void setAuthor(String argAuthor) {
		myAuthor = argAuthor;
	}
	
	public List<String> getComparatorColumnNameList() {
		return myComparatorColumnNameList;
	}
	
	public List<String> getUnmappedColumnNameList() {
		return myUnmappedColumnNameList;
	}
	
	public List<String> getFilterColumnNameList() {
		return myFilterColumnNameList;
	}
	
	public String getDataSourceJava() {
		return myDataSourceJava;
	}
	
	public void setDataSourceJava(String argDataSourceJava) {
		myDataSourceJava = argDataSourceJava;
	}
	
	public DBOpts getCommandLineDBOpts() {
		return myCLDBOpts;
	}
	
	@Deprecated
	public String getDatabaseUsername() {
		return getCommandLineDBOpts().username();
	}
	
	@Deprecated
	public String getDatabasePassword() {
		return getCommandLineDBOpts().password();
	}

}
