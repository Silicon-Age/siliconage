package com.opal.creator;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Key;
import com.siliconage.xml.XMLElement;

public class Reference extends OpalXMLElement {
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Reference.class.getName());

	
	public Reference(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		String lclName = getAttributeValue("Name", "UNNAMED_REFERENCE");
		
		boolean lclMapped = Boolean.parseBoolean(getAttributeValue("Mapped", "True"));
		if (lclMapped == false) {
			ourLogger.warn("Doing an artificial foreign key for which Mapped is false!");
		}
		
		String lclSpecifiedBaseName = getAttributeValue("Field");
		String lclSpecifiedCollectionName = getAttributeValue("CollectionName");
		if (lclSpecifiedCollectionName != null) {
			if (ourLogger.isTraceEnabled()) {
				ourLogger.trace("Reference " + lclName + " has specified a custom collection name of \"" + lclSpecifiedCollectionName + "\".");
			}
		}
		String lclSpecifiedCollectionItemName = getAttributeValue("CollectionItemName");		
		if (lclSpecifiedCollectionItemName != null) {
			if (ourLogger.isTraceEnabled()) {
				ourLogger.trace("Reference " + lclName + " has specified a custom collection item name of \"" + lclSpecifiedCollectionItemName + "\".");
			}
		}
		
		Element lclSource = getSingleChildElement("Source");
		
		Validate.notNull(lclSource, "Reference " + lclName + " does not have a Source child.");
		
		String lclSourceTypeName = XMLElement.getAttributeValue(lclSource, "Type");
		Validate.notNull(lclSourceTypeName, "Source child of Reference " + lclName + " does not specify a type.");
		
		MappedClass lclSourceMappedClass = identifyType(lclSourceTypeName, argContext);
		if (lclSourceMappedClass == null) {
			ourLogger.error("Reference " + lclName + " has a Source Type of " + lclSourceTypeName + " which cannot be resolved.");
			return;
		}
		String lclSourceRolePrefix = XMLElement.getAttributeValue(lclSource, "RolePrefix");
		
		String lclSourceAccess = XMLElement.getAttributeValue(lclSource, "Access");
		
		Validate.notNull(lclSourceMappedClass, "Reference " + lclName + " has a Source Type of " + lclSourceTypeName + " which does not exist.");
		
		Element lclTarget = getSingleChildElement("Target");
		Validate.notNull(lclTarget, "Reference " + lclName + " does not have a Target child.");
		
		String lclTargetTypeName = XMLElement.getAttributeValue(lclTarget, "Type");
		Validate.notNull(lclTargetTypeName, "Target child of Reference " + lclName + " does not specify a type.");
		
		MappedClass lclTargetMappedClass = identifyType(lclTargetTypeName, argContext);
		if (lclTargetMappedClass == null) {
			ourLogger.error("Reference " + lclName + " has a Target Type of " + lclTargetTypeName + ", which cannot be resolved.");
			return;
		}
		
		Validate.notNull(lclTargetMappedClass, "Reference " + lclName + " has a Target Type of " + lclTargetTypeName + " which does not exist.");
		
		String lclTargetRolePrefix = XMLElement.getAttributeValue(lclTarget, "RolePrefix");
		
		String lclTargetAccess = XMLElement.getAttributeValue(lclTarget, "Access");
		
		Key lclSourceKey = new Key(lclSourceMappedClass.getTableName(), String.valueOf(lclName) + "_SOURCE", false /* not required */);
		{
			NodeList lclList = lclSource.getElementsByTagName("Column");
			int lclLength = lclList.getLength();
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				lclSourceKey.getColumnNames().add((lclList.item(lclI)).getTextContent());
			}
		}
		
		// TODO: null names
		Key lclTargetKey = new Key(lclTargetMappedClass.getTableName(), String.valueOf(lclName) + "_TARGET", false /* not required */);
		{
			NodeList lclList = lclTarget.getElementsByTagName("Column");
			int lclLength = lclList.getLength();
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				lclTargetKey.getColumnNames().add((lclList.item(lclI)).getTextContent());
			}
		}

		lclSourceMappedClass.complain(MessageLevel.Info, "SourceKey = " + lclSourceKey);
		lclSourceMappedClass.complain(MessageLevel.Info, "TargetKey = " + lclTargetKey);
		
		/* If no target columns were supplied, use that table's primary key */
		if (lclTargetKey.getColumnNames().size() == 0) {
			lclSourceMappedClass.complain(MessageLevel.Info, "Using the primary key for " + lclTargetKey.getTableName() + ", which is the target of a foreign key from " + lclSourceKey.getTableName() + ", since no columns were explicitly specified.");
			lclTargetKey.getColumnNames().addAll(argContext.getRelationalDatabaseAdapter().getPrimaryKey(lclTargetKey.getTableName()).getColumnNames());
		}
		
		/* Extract the name of the JoinQueryFactory */
		if (lclSourceKey.getColumnNames().size() > 0 && lclTargetKey.getColumnNames().size() > 0 && lclSourceKey.getColumnNames().size() != lclTargetKey.getColumnNames().size()) {
			throw new IllegalStateException("Different number of source and target key columns specified for the Reference from " + lclSourceMappedClass.getTableName().getTableName() + " to " + lclTargetMappedClass.getTableName().getTableName() + ".");
		}
		
		String lclJoinQueryFactoryName;
		Element lclJQFElement = getSingleChildElement("JoinQueryFactory");
		if (lclJQFElement != null) {
			lclJoinQueryFactoryName = lclJQFElement.getTextContent();
			lclSourceMappedClass.complain(MessageLevel.Info, "lclJQFName = " + lclJoinQueryFactoryName);
		} else {
			lclJoinQueryFactoryName = null;
		}
		
		/* Is this manually marked as a one-to-one relationship?  That is, should we construct the opals as if there were
		 * a unique-if-not-null constraint on the *source* key?
		 */
		
		String lclOneToOneString = XMLElement.getAttributeValue(lclTarget, "OneToOne");
		boolean lclOneToOne = Boolean.parseBoolean(lclOneToOneString);

		if (lclOneToOne) {
			lclSourceMappedClass.complain(MessageLevel.Info, "Foreign key is manually marked as representing a one-to-one relationship.");
		}
		
		/* Extract the Collection to be used */
		
		Class<?> lclCollectionClass;
		String lclCollectionClassName = XMLElement.getAttributeValue(lclTarget, "CollectionClass");
		if (lclCollectionClassName == null) {
			lclCollectionClass = ForeignKey.USE_DEFAULT_COLLECTION_CLASS;
		} else {
			if ("None".equalsIgnoreCase(lclCollectionClassName)) {
				lclCollectionClass = null;
			} else {
				lclCollectionClass = Class.forName(lclCollectionClassName);
			}
			lclTargetMappedClass.complain(MessageLevel.Info, "lclCollectionClass = " + (lclCollectionClass == null ? "None" : lclCollectionClass.getName()));
		}
		
		// TODO: null names
		ForeignKey lclFK = new ForeignKey(
			lclSourceKey,
			lclTargetKey,
			String.valueOf(lclName) + "_FK",
			ReferentialAction.NO_ACTION, // Delete (TODO: What would it mean to specify these for an FK that doesn't actually exist?)
			ReferentialAction.NO_ACTION, // Update (TODO: What would it mean to specify these for an FK that doesn't actually exist?)
			lclSourceRolePrefix,
			lclTargetRolePrefix,
			lclSourceAccess,
			lclTargetAccess,
			lclJoinQueryFactoryName,
			lclOneToOne,
			lclCollectionClass,
			lclSpecifiedBaseName,
			lclSpecifiedCollectionItemName,
			lclSpecifiedCollectionName,
			lclMapped
		);
		argContext.getRelationalDatabaseAdapter().addArtificialForeignKey(lclFK);
		
		return;
	}
	
	private static MappedClass identifyType(String argTypeName, OpalParseContext argContext) {
		Iterator<MappedClass> lclI = argContext.getMappedClasses().values().iterator();
		while (lclI.hasNext()) {
			MappedClass lclMC = lclI.next();
			if (lclMC.getTypeName().equals(argTypeName)) {
				return lclMC;
			}
		}
		return null;
	}
}
