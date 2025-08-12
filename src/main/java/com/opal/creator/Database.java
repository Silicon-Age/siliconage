package com.opal.creator;

//import java.util.Iterator;

import javax.sql.DataSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.opal.creator.database.mysql.MySQLAdapter;
import com.opal.creator.database.oracle.OracleAdapter;
import com.opal.creator.database.postgres.PostgresAdapter;
import com.opal.creator.database.sqlserver.SQLServerAdapter;
import com.opal.creator.database.sybase.SybaseAdapter;

import com.siliconage.database.DirectConnectionPoolFactory;

public class Database extends OpalXMLElement {
	public Database(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	private String getValueOrCLDefault(boolean argRequired, String argXMLAttributeName, String argCLDefault) {
		if (argXMLAttributeName == null) {
			throw new IllegalArgumentException("argXMLAttributeName is null");
		}
		String lclS = getAttributeValue(argXMLAttributeName);
//		System.out.println(argXMLAttributeName + " -> " + lclS);
		String lclFS;
		if (lclS != null) {
			lclFS = lclS;
		} else if (argRequired && (argCLDefault == null)) {
			throw new IllegalStateException("Attribute \"" + argXMLAttributeName + "\" was not specified in the <Database> element and the corresponding command-line option waqs also null.");
		} else {
			lclFS = argCLDefault;
		}
		return lclFS;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {

//		System.out.println("<Database>");
//		var lclAttributes = getNode().getAttributes();
//		for (int lclI = 0; lclI < lclAttributes.getLength(); ++lclI) {
//			var lclItem = lclAttributes.item(lclI);
//			System.out.println(lclItem.getNodeName() + " : " + lclItem.getNodeType() + " : " + lclItem.getNodeValue());
//		}
//		System.out.println("</Database>");
		
		DBOpts lclDBOpts = argContext.getCommandLineDBOpts();
		
		String lclDriverClassName = getValueOrCLDefault(true, "Driver", lclDBOpts.driverName());
		try {
			Class.forName(lclDriverClassName);
		} catch (ClassNotFoundException lclE) {
			throw new RuntimeException("Could not find JDBC driver \"" + lclDriverClassName + "\"", lclE);
		}
		
		DataSource lclDataSource;
		String lclUrl = getValueOrCLDefault(true, "Url", lclDBOpts.connectString());
		
		String lclDatabaseUsername = getValueOrCLDefault(false, "User", lclDBOpts.username());
		
		String lclDatabasePassword = getValueOrCLDefault(false, "Password", lclDBOpts.password());
		
		// lclDatabaseUsername and lclDatabasePassword could still be null, in which case lclUrl better have that information.
		if (lclDatabaseUsername != null && lclDatabasePassword != null) {
			lclDataSource = DirectConnectionPoolFactory.getInstance().create(lclUrl, lclDatabaseUsername, lclDatabasePassword);
		} else {
			lclDataSource = DirectConnectionPoolFactory.getInstance().create(lclUrl);
		}
		
		if (lclUrl.startsWith("jdbc:oracle:")) {
			argContext.setRelationalDatabaseAdapter(new OracleAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:microsoft:")) {
			argContext.setRelationalDatabaseAdapter(new SQLServerAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:sqlserver:")) {
			argContext.setRelationalDatabaseAdapter(new SQLServerAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:jtds:")) {
			argContext.setRelationalDatabaseAdapter(new SQLServerAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:sybase:")) {
			argContext.setRelationalDatabaseAdapter(new SybaseAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:mysql:")) {
			argContext.setRelationalDatabaseAdapter(new MySQLAdapter(lclDataSource));
		} else if (lclUrl.startsWith("jdbc:postgresql:")) {
			argContext.setRelationalDatabaseAdapter(new PostgresAdapter(lclDataSource));
		} else {
			throw new IllegalStateException("Unable to determine correct RelationalDatabaseAdapter for the JDBC connect string \"" + lclUrl + "\".");
		}

		String lclJNDIName = getValueOrCLDefault(false, "JNDIName", lclDBOpts.JNDIName());
		if (lclJNDIName != null) {
			argContext.getPoolMap().put(OpalParseContext.DEFAULT_POOL_NAME, lclJNDIName);
		}
		// TODO: Document what happens if there is no JNDI name.
		
//		argContext.getRelationalDatabaseAdapter().setDataSourceName(getRequiredAttributeValue("JNDIName"));
		
		argContext.getRelationalDatabaseAdapter().initialize((Element) getNode());
	}
}
