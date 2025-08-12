package com.opal.creator;

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.opal.creator.database.Index;
import com.opal.creator.database.PrimaryKey;

/**
 * @author topquark
 */
public class SingleLookUp extends OpalXMLElement {
	
	public SingleLookUp(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		List<Element> lclChildren = getChildElements("Column");
		boolean lclPK = "True".equalsIgnoreCase(getAttributeValue("PrimaryKey", "False"));
		boolean lclMapped = "True".equalsIgnoreCase(getAttributeValue("Mapped", "True"));
		boolean lclUniqueStringKey = "True".equalsIgnoreCase(getAttributeValue("UniqueString", "False"));
		
		MappedClass lclMC = ((Mapping) getParent()).getMappedClass();

		if (lclChildren.isEmpty()) {
			lclMC.complain(MessageLevel.Warning, "SingleLookUp has no children specifying component columns.  Skipping.");
		} else {
			
			Index lclIndex = new Index(
				lclMC.getTableName(),
				"UNNAMED_INDEX",
				true, /* This is a unique index. */
				null /* And it is not a filtered index.  FIXME: It should be possible to manually specify that it is. */
			);
			
			lclChildren.stream()
				.map(Element::getTextContent)
				.forEach(lclIndex.getColumnNames()::add);
			
			lclMC.complain(MessageLevel.Info, "Created SingleLookUp index " + lclIndex);
			
			Index lclExistingIndex = null;
			Collection<Index> lclIndexes = argContext.getRelationalDatabaseAdapter().getUniqueIndexes(lclMC.getTableName());
			Outer: for (Index lclJ : lclIndexes) {
				if (lclIndex.getColumnNames().size() != lclJ.getColumnNames().size()) {
					continue;
				}
				for (int lclL = 0; lclL < lclIndex.getColumnNames().size(); ++lclL) {
					if (lclIndex.getColumnNames().get(lclL).equals(lclJ.getColumnNames().get(lclL)) == false) {
						continue Outer;
					}
				}
				lclExistingIndex = lclJ;
				lclMC.complain(MessageLevel.Info, "Found existing index (" + lclExistingIndex.toString() + ") referenced by SingleLookUp element.");
				break;
			}
			
			if (lclExistingIndex == null) {
				if (lclMapped) {
					lclMC.addMappedUniqueKey(
						new MappedUniqueKey(
							lclMC,
							lclIndex,
							false /* Not yet marked as a primary key, even if it is to be one. */
						)
					);
					lclMC.complain(MessageLevel.Info, "Added MappedUniqueKey for " + lclMC + " with fake index " + lclIndex + " on " + lclIndex.getColumnNames());
				}
				lclExistingIndex = lclIndex;
			}
			
			lclExistingIndex.setMapped(lclMapped);
			
			if (lclPK) {
				PrimaryKey lclPrimaryKey = new PrimaryKey(lclMC.getTableName(), "UNNAMED_PRIMARY_KEY");
				lclPrimaryKey.getColumnNames().addAll(lclIndex.getColumnNames());
				argContext.getRelationalDatabaseAdapter().addArtificialPrimaryKey(lclPrimaryKey);
				lclMC.complain(MessageLevel.Info, "Added artificial primary key " + lclPrimaryKey);
			}
			
			if (lclUniqueStringKey) {
				lclExistingIndex.setUniqueStringKey(true);
			}
		}
		return;
	}
}
