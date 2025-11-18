package com.opal.creator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.UnimplementedOperationException;

import com.opal.PersistenceException;
import com.opal.ReferenceType;
import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.ForeignKey;

public class MappedForeignKey implements Comparable<MappedForeignKey> {

	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(MappedForeignKey.class.getName());

	private final ForeignKey myForeignKey;
	private final MappedClass mySourceMappedClass;
	private final MappedClass myTargetMappedClass;
	
	private CompoundClassMember mySource;
	private CompoundClassMember myTarget;
	
	private String mySourceRolePrefix;
	private String myTargetRolePrefix;
	
	private String myDeterminedRoleCollectionItemName; // null until correct value is cached
	private String myDeterminedRoleCollectionName; // null until correct value is cached
	private String myDeterminedRoleOpalCollectionName; // null until correct value is cached
	
	private Class<?> myCollectionType; // For caching our Collection type once we figure it out.
	private ReferenceType myCollectionReferenceType = ReferenceType.SOFT;
	
	/* Whether the REFERENCING table has a unique constraint on the columns comprising the foreign key.  If it does, then this doesn't
	 * actually represent a "many-to-one" relationship but a "maybe-one-to-one" relationship.  This will almost always be false.  Note
	 * that it is very common (if not mandatory) to have a unique constraint on the REFERENCED table, but this relates to the
	 * existence of one on the REFERENCING table.  
	 */
	private boolean myUnique;
	
	private boolean myRepresentsOneToOneRelationship;
	private boolean myRepresentsPolymorphism;

	private ReferentialAction myDeleteAction;
	private ReferentialAction myUpdateAction;
	
	// Computed ClassMembers that depend on the value of this reference (foreign key)
	private final List<ClassMember> myDependents = new ArrayList<>();

	private static final String MEMBER_PREFIX = "my";
	private static final String STATIC_MEMBER_PREFIX = "our";
	
	private static final String OLD_PREFIX = "Old";	
	private static final String NEW_PREFIX = "New";
	
	private static final String REVERSE_TARGET_ROLE_PREFIX = "Reverse";
	
	private static final String ACCESSOR_PREFIX = "get";
	private static final String MUTATOR_PREFIX = "set";

	private static final String RETRIEVE_METHOD_PREFIX = "retrieve";

	private static final String LOADER_SUFFIX = "Loader";
	
	private static final String ACQUIRE_METHOD_PREFIX = "acquire";
	private static final String STREAM_METHOD_PREFIX = "stream";
	
	private static final String DEFAULT_COLLECTION_SUFFIX = "Set";
	private static final String ARRAY_METHOD_SUFFIX = "Array";
	private static final String OPAL_METHOD_SUFFIX = "Opal";
	private static final String DEFAULT_OPAL_COLLECTION_SUFFIX = OPAL_METHOD_SUFFIX + DEFAULT_COLLECTION_SUFFIX;
	private static final String INTERNAL_METHOD_SUFFIX = "Internal";
	
	public MappedForeignKey(ForeignKey argFK, MappedClass argSourceMC, MappedClass argTargetMC) {
		super();
		
		Validate.notNull(argFK);
		myForeignKey = argFK;
		
		Validate.notNull(argSourceMC);
		mySourceMappedClass = argSourceMC;
		
		Validate.notNull(argTargetMC);
		myTargetMappedClass = argTargetMC;
		
		determineSourceAndTarget();
	}
	
	protected void determineSourceAndTarget() {
		Iterator<String> lclI;
		
		mySource = new CompoundClassMember();
		
		lclI = getForeignKey().getSourceKey().getColumnNames().iterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = getSourceMappedClass().getClassMemberByColumnName(lclI.next());
			assert lclCM != null;
			mySource.add(lclCM);
		}
		
		myTarget = new CompoundClassMember();
		
		lclI = getForeignKey().getTargetKey().getColumnNames().iterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = getTargetMappedClass().getClassMemberByColumnName(lclI.next());
			assert lclCM != null;
			myTarget.add(lclCM);
		}
	}
	
	
	public ForeignKey getForeignKey() {
		return myForeignKey;
	}
	
	public boolean isRequired() {
		return getForeignKey().isRequired();
	}
	
	/* This is the name of the class member that appears in the source class.  For instance, if
	 * City has a foreign key to State; then this would manifest itself as the member
	 * 
	 * State City.myState;
	 * 
	 * to be accessed via calls like lclCity.getState().  In this case, getSourceMemberName is "State."
	 * 
	 * Of course, Opal doesn't directly store the State in a member called myState (the actual data
	 * is placed in behind-the-scenes Opals that mirror the database structure), but it gives the
	 * illusion that this is what is happening.
	 * 
	 * This member name will default to the name of the target UserFacing (which itself will default
	 * to something very close to the name of the database table).  The user can override this by
	 * specifying a Field attribute on a <Reference> element in the Opal configuration file.
	 */
	protected String getSourceFieldName() {
		String lclSpecifiedName = getForeignKey().getSpecifiedSourceBaseName();
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getForeignKey().getSpecifiedBaseName();
		} else {
			throw new IllegalStateException("### We had a specified target base name of " + lclSpecifiedName + " ###"); // FIXME: Should log instead
		}
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getDefaultSourceFieldName();
		}
		return lclSpecifiedName; 
	}
	
	/* This is the corresponding "base" member name that would appear in CityOpal.  It will actually
	 * be used to generate two fields, myOldState and myNewState that are used for transaction
	 * processing.
	 * 
	 * TODO: Allow the user to manually specify a different base name in the Opal configuration file.
	 */
	protected String getSourceOpalFieldName() {
		String lclSpecifiedName = getForeignKey().getSpecifiedSourceBaseName();
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getForeignKey().getSpecifiedBaseName();
		}
		if (lclSpecifiedName != null) {
			return lclSpecifiedName + "Opal"; 
		} else {
			return getDefaultSourceOpalFieldName();
		}
	}
	
	protected String getTargetFieldName() {
		String lclSpecifiedName = getForeignKey().getSpecifiedTargetBaseName();
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getForeignKey().getSpecifiedBaseName();
		} else {
			throw new IllegalStateException("### We had a specified target base name of " + lclSpecifiedName + " ###"); // FIXME: Should log instead
		}
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getDefaultTargetFieldName();
		}
		return lclSpecifiedName; 
	}
	
	/* This is the corresponding "base" member name that would appear in CityOpal.  It will actually be used to
	 * generate two fields, myOldState and myNewState that are used to ensure transaction semantics.
	 * 
	 * TODO: Allow the user to manually specify a different base name in the Opal configuration file.
	 */
	protected String getTargetOpalFieldName() {
		String lclSpecifiedName = getForeignKey().getSpecifiedTargetBaseName();
		if (lclSpecifiedName == null) {
			lclSpecifiedName = getForeignKey().getSpecifiedBaseName();
		}
		if (lclSpecifiedName != null) {
			return lclSpecifiedName + "Opal"; 
		} else {
			return getDefaultTargetOpalFieldName();
		}
	}
	
	/* When asked for the member name in the source UserFacing, we will default to using one built
	 * from the name of the target UserFacing (itself probably defaulting from the name of the
	 * database table).  This method returns that default.  This can be overridden by specifying a
	 * Field attribute on a <Reference> element in the Opal configuration file.
	 */
	protected String getDefaultSourceFieldName() {
		return getTargetMappedClass().getTypeName();
	}
	
	protected String getDefaultTargetFieldName() {
		return getSourceMappedClass().getTypeName();
	}
	
	/* This method returns a similar default for the names of the members in the Opal classes. */
	protected String getDefaultSourceOpalFieldName() {
		return getTargetMappedClass().getOpalClassName();
	}
	
	protected String getDefaultTargetOpalFieldName() {
		return getSourceMappedClass().getOpalClassName();
	}
	
	protected String prependSourceRole(String argName) {
		Validate.notNull(argName);
		/* Not sure that this will ever do anything. */
		
		String lclRolePrefix = getSourceRolePrefix();
		if (argName.startsWith(lclRolePrefix)) {
			return argName;
		} else {
			return lclRolePrefix + argName;
		}
	}
	
	protected String prependTargetRole(String argName) {
		/* Not sure that this will ever do anything. */
		
		String lclRolePrefix = getTargetRolePrefix();
		if (argName.startsWith(lclRolePrefix)) {
			return argName;
		} else {
			return lclRolePrefix + argName;
		}
	}
	
	public String getRoleSourceFieldName() {
		return prependSourceRole(getSourceFieldName());
	}
	
	public String getOldSourceFieldName() {
		return MEMBER_PREFIX + OLD_PREFIX + getSourceFieldName();
	}
	
	public String getNewSourceFieldName() {
		return MEMBER_PREFIX + NEW_PREFIX + getSourceFieldName();
	}
	
	public String getOldRoleSourceFieldName() {
		return MEMBER_PREFIX + OLD_PREFIX + getRoleSourceFieldName();
	}
	
	public String getNewRoleSourceFieldName() {
		return MEMBER_PREFIX + NEW_PREFIX + getRoleSourceFieldName();
	}
	
	public String getRoleSourceOpalFieldName() {
		return prependSourceRole(getSourceOpalFieldName());
	}
	
	public String getOldRoleSourceOpalFieldName() {
		return MEMBER_PREFIX + OLD_PREFIX + getRoleSourceOpalFieldName();
	}
	
	public String getNewRoleSourceOpalFieldName() {
		return MEMBER_PREFIX + NEW_PREFIX + getRoleSourceOpalFieldName();
	}
	
	public String getRoleTargetFieldName() {
		Validate.isTrue(representsOneToOneRelationship());
		return prependTargetRole(getTargetFieldName());
	}
	
	public String getRoleTargetOpalFieldName() {
		Validate.isTrue(representsOneToOneRelationship());
		return prependTargetRole(getTargetOpalFieldName());
	}
	
	public String getOldRoleTargetOpalFieldName() {
		Validate.isTrue(representsOneToOneRelationship());
		return MEMBER_PREFIX + OLD_PREFIX + getRoleTargetOpalFieldName();
	}
	
	public String getNewRoleTargetOpalFieldName() {
		Validate.isTrue(representsOneToOneRelationship());
		return MEMBER_PREFIX + NEW_PREFIX + getRoleTargetOpalFieldName();
	}
	
	public String getRoleCollectionLoaderName() {
		Validate.isTrue(representsManyToOneRelationship());
		Validate.notNull("Cannot call getRoleCollectionLoaderName() when there is no collection.");
		return STATIC_MEMBER_PREFIX + prependTargetRole(getSourceMappedClass().getOpalClassName() + LOADER_SUFFIX);
	}
	
	public String getRoleCollectionMemberName() {
		Validate.isTrue(representsManyToOneRelationship());
		Validate.notNull("Cannot call getRoleCollectionMemberName() when there is no collection.");
		return MEMBER_PREFIX + getRoleCollectionName();
	}
	
	public String getRoleOpalCollectionAccessorName() {
		return ACCESSOR_PREFIX + getRoleOpalCollectionName();
	}
	
	public String getRoleCollectionAccessorName() {
		return ACCESSOR_PREFIX + getRoleCollectionName();
	}
	
	public String getSourceRolePrefix() {
		if (mySourceRolePrefix == null) {
			mySourceRolePrefix = determineSourceRolePrefix();
		}
		Validate.notNull(mySourceRolePrefix);
		return mySourceRolePrefix;
	}
	
	protected String determineSourceRolePrefix() {
		if (getForeignKey().getSpecifiedSourceBaseName() != null) {
			return "";
		}
		
		if (getForeignKey().getSourceRolePrefix() != null) {
			return getForeignKey().getSourceRolePrefix();
		}
		
		String lclPrefix = "";
		
		Iterator<ClassMember> lclI = getSource().iterator();
		Iterator<ClassMember> lclJ = getTarget().iterator();
		while (lclI.hasNext() && lclJ.hasNext()) {
			ClassMember lclS = lclI.next();
			ClassMember lclT = lclJ.next();
			
			int lclPos = lclS.getBaseMemberName().indexOf(lclT.getBaseMemberName());
			if (lclPos > 0) {
				String lclRolePrefix = lclS.getBaseMemberName().substring(0, lclPos);
				
				/* This is a check so that a column "member_id" pointing at a column "id" in the table
				"Member" doesn't generate a role prefix of "Member". */
				if (!lclRolePrefix.equalsIgnoreCase(getTargetMappedClass().getTypeName())) {
					lclPrefix = lclRolePrefix;
					break;
				}
			}
		}
		
		if (lclPrefix.endsWith(getTargetMappedClass().getTypeName())) {
			lclPrefix = lclPrefix.substring(0, lclPrefix.length() - getTargetMappedClass().getTypeName().length());
		}
		
		if (StringUtils.isNotBlank(lclPrefix)) {
			getSourceMappedClass().complain(MessageLevel.Info, "Found SourceRolePrefix of \"" + lclPrefix + "\" for " + this);
		}
		
		return lclPrefix;
	}
	
	public String getTargetRolePrefix() {
		if (myTargetRolePrefix == null) {
			myTargetRolePrefix = determineTargetRolePrefix();
		}
		Validate.notNull(myTargetRolePrefix);
		return myTargetRolePrefix;
	}
	
	protected String determineTargetRolePrefix() {
		String lclPrefix;
		if (getForeignKey().getTargetRolePrefix() != null) {
			lclPrefix = getForeignKey().getTargetRolePrefix();
		} else if (getForeignKey().getSpecifiedTargetBaseName() != null) {
			lclPrefix = ""; // getForeignKey().getSpecifiedTargetBaseName();
		} else {
			lclPrefix = "";
			Iterator<ClassMember> lclI = getSource().iterator();
			Iterator<ClassMember> lclJ = getTarget().iterator();
			while (lclI.hasNext() && lclJ.hasNext()) {
				ClassMember lclS = lclI.next();
				ClassMember lclT = lclJ.next();
				
				int lclPos = lclS.getBaseMemberName().indexOf(lclT.getBaseMemberName());
				if (lclPos > 0) {
					String lclRolePrefix = lclS.getBaseMemberName().substring(0, lclPos);
					
					/* This is a check so that a column "member_id" pointing at a column "id" in the table
					"Member" doesn't generate a role prefix of "Member". */
					
					if (!lclRolePrefix.equalsIgnoreCase(getTargetMappedClass().getTypeName())) {
						lclPrefix = lclRolePrefix;
						break;
					}
				}
			}
		}
		
		String lclT = getTargetMappedClass().getTypeName();
		if (lclPrefix.endsWith(lclT)) {
			lclPrefix = lclPrefix.substring(0, lclPrefix.length() - lclT.length());
		}
		
		if (representsOneToOneRelationship()) {
			if (getSourceMappedClass() == getTargetMappedClass()) {
				if (getSourceRolePrefix().equals(lclPrefix)) {
					lclPrefix = REVERSE_TARGET_ROLE_PREFIX + lclPrefix;
				}
			}
		}
		
		if (StringUtils.isNotBlank(lclPrefix)) {
			getSourceMappedClass().complain(MessageLevel.Info, "Found TargetRolePrefix of \"" + lclPrefix + "\" for " + this);
		}
		
		return lclPrefix;
	}
	
	public String getDefaultCollectionContentsName() {
		return getSourceMappedClass().getOpalClassName();
	}
	
	public String getCustomCollectionName() {
		return getForeignKey().getSpecifiedCollectionName(); // Might be null
	}
	
	public String getCustomCollectionItemName() {
		return getForeignKey().getSpecifiedCollectionItemName(); // Might be null
	}
	
	public String getRoleCollectionItemName() {
		if (myDeterminedRoleCollectionItemName == null) {
			myDeterminedRoleCollectionItemName = determineRoleCollectionItemName();
			Validate.notNull(myDeterminedRoleCollectionItemName);
		}
		return myDeterminedRoleCollectionItemName;
	}
	
	private String determineRoleCollectionItemName() {
		String lclS = getCustomCollectionItemName();
		if (lclS != null) {
			ourLogger.warn("*** Non-null RoleCollectionItemName of \"" + lclS + "\" ***");
			return lclS;
		}
		return prependTargetRole(getSourceMappedClass().getTypeName());
	}
	
	public String getRoleCollectionName() {
		if (myDeterminedRoleCollectionName == null) {
			myDeterminedRoleCollectionName = determineRoleCollectionName();
			Validate.notNull(myDeterminedRoleCollectionName);
		}
		return myDeterminedRoleCollectionName;
	}
		
	private String determineRoleCollectionName() {
		String lclS = getCustomCollectionName();
		if (lclS != null) {
			return lclS;
		}
		return getRoleCollectionItemName() + DEFAULT_COLLECTION_SUFFIX;
	}
	
	public String getRoleOpalCollectionName() {
		if (myDeterminedRoleOpalCollectionName == null) {
			myDeterminedRoleOpalCollectionName = determineRoleOpalCollectionName();
			Validate.notNull(myDeterminedRoleOpalCollectionName);
		}
		return myDeterminedRoleOpalCollectionName;
	}
		
	private String determineRoleOpalCollectionName() {
		String lclS = getCustomCollectionName();
		if (lclS != null) {
			return lclS;
		}
		return getRoleCollectionItemName() + DEFAULT_OPAL_COLLECTION_SUFFIX;
	}
	
	public CompoundClassMember getSource() {
		return mySource;
	}
	
	public boolean hasBackCollection() {
		return getForeignKey().getCollectionClass() != null;
	}
	
	/* This returns the actual Collection class generally used to store the back collection.  It will probably be different
	 * from the declared type of the reference to that Collection in the Opal to allow for the substitution of canonical
	 * empty sets or other performance improvements. */
	
	public Class<?> getCollectionType() {
		if (myCollectionType == null) {
			myCollectionType = determineCollectionType();
			Validate.notNull(myCollectionType);
		}
		return myCollectionType;
	}
	
	/* This is the type (hard, weak, soft) of the Reference to the entire Collection that is held by the referenced Opal.
	 * In particular, we have something like XReference<Collection<TOpal>> rather than Collection<XReference<TOpal>>.
	 */
	public ReferenceType getCollectionReferenceType() {
		return myCollectionReferenceType;
	}
	
	private Class<?> determineCollectionType() {
		if (hasBackCollection() == false) {
			getTargetMappedClass().complain(MessageLevel.Error, "Tried to determineCollectionType() for ForeignKey " + getForeignKey() + " which should not have a back collection.");
			return null;
		}
		if (getForeignKey().isSetToUseDefaultCollectionClass()) {
			String lclCollectionsLibraryName = getTargetMappedClass().getCollections();
			if ("Trove".equalsIgnoreCase(lclCollectionsLibraryName)) {
				try {
					Class<?> lclC = Class.forName("gnu.trove.set.hash.THashSet");
					return lclC;
				} catch (ClassNotFoundException lclE) {
					getTargetMappedClass().complain(MessageLevel.Error, "Tried to use Trove Collections, but the relevant class names could not be resolved.");
					Class<?> lclC = HashSet.class;
					return lclC;
				}
			} else if ("Java".equalsIgnoreCase(lclCollectionsLibraryName)) {
				Class<?> lclC = HashSet.class;
				return lclC;
			} else {
				getTargetMappedClass().complain(MessageLevel.Error, "Unknown Collections library name \"" + lclCollectionsLibraryName + "\".");
				Class<?> lclC = HashSet.class;
				return lclC;
			}
		} else {
			Class<?> lclB = getForeignKey().getCollectionClass();
			if (lclB == null) {
				getTargetMappedClass().complain(MessageLevel.Warning, "Collection class for " + getForeignKey() + " is null.");
			}
			Validate.notNull(lclB);
			Class<?> lclC;
			if (Set.class.isAssignableFrom(lclB)) {				
				lclC = lclB;
			} else {
				getTargetMappedClass().complain(MessageLevel.Error, "Collection class " + lclB.getName() + " cannot be assigned to a reference of type " + Set.class.getName() + ".  Defaulting to HashSet.");
				lclC = HashSet.class;
			}
			return lclC; 
		}
	}

	public MappedClass getSourceMappedClass() {
		return mySourceMappedClass;
	}
	
	public CompoundClassMember getTarget() {
		return myTarget;
	}
	
	public MappedClass getTargetMappedClass() {
		return myTargetMappedClass;
	}
	
	/* This will return the actual name of the Opal class used to represent the target.  This is
	 * not necessarily linked to either the name that the foreign key is supposed to use to
	 * manifest the relationship or to the field name that the user will see when manipulating
	 * UserFacing objects.
	 */
	public String getTargetOpalClassName() {
		return getTargetMappedClass().getOpalClassName();
	}
	
	public String getSourceOpalClassName() {
		return getSourceMappedClass().getOpalClassName();
	}
	
	protected void setSource(CompoundClassMember newSource) {
		mySource = newSource;
	}
	
	protected void setTarget(CompoundClassMember newTarget) {
		myTarget = newTarget;
	}
	
	@Override
	public String toString() {
		return "[" + getSourceMappedClass().getOpalClassName() + "->" + getTargetMappedClass().getOpalClassName() + "]";
	}
	
	public String getSourceFieldAccess() {
		return getForeignKey().getSourceAccess();
	}
	
	public String getTargetFieldAccess() {
		Validate.isTrue(representsOneToOneRelationship());
		return getForeignKey().getTargetAccess();
	}
	
	public String getTargetCollectionAccess() {
		Validate.isTrue(representsManyToOneRelationship());
		return getForeignKey().getTargetAccess();
	}
	
	public boolean representsPolymorphism() {
		return myRepresentsPolymorphism;
	}
	
	public void setRepresentsPolymorphism(boolean argRepresentsPolymorphism) {
		myRepresentsPolymorphism = argRepresentsPolymorphism;
	}
	
	public boolean isUnique() {
		return myUnique;
	}
	
	public void setUnique(boolean argUnique) {
		myUnique = argUnique;
	}
	
	public boolean representsOneToOneRelationship() {
		return myRepresentsOneToOneRelationship;
	}
	
	public void setRepresentsOneToOneRelationship(boolean argRepresentsOneToOneRelationship) {
		myRepresentsOneToOneRelationship = argRepresentsOneToOneRelationship;
	}
	
	public boolean representsManyToOneRelationship() {
		return representsOneToOneRelationship() == false;
	}
	
	public String generateUniqueFactoryFunctionCall() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getTarget().generateUniqueFactoryFunctionName());
		/* Now add the parameters */
		lclSB.append('(');
		Iterator<ClassMember> lclCMI = getSource().iterator();
		boolean lclFirst = true;
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getObjectAccessorName());
			lclSB.append("()");
		}
		lclSB.append(')');
		return lclSB.toString();
	}
	
	public String generateSourceUniqueFactoryFunctionCall() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getSource().generateUniqueFactoryFunctionName());
		/* Now add the parameters */
		lclSB.append('(');
		Iterator<ClassMember> lclCMI = getTarget().iterator();
		boolean lclFirst = true;
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getObjectAccessorName());
			lclSB.append("()");
		}
		lclSB.append(')');
		return lclSB.toString();
	}
	
	public String generateSourceFactoryFunctionCall() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getTarget().generateCollectionFactoryFunctionName());
		/* Now add the parameters */
		lclSB.append('(');
		Iterator<ClassMember> lclCMI = getSource().iterator();
		boolean lclFirst = true;
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getObjectAccessorName());
			lclSB.append("()");
		}
		lclSB.append(')');
		return lclSB.toString();
	}
	
	public String generateTargetFactoryFunctionCall() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getSource().generateCollectionFactoryFunctionName());
		lclSB.append('(');
		Iterator<ClassMember> lclCMI = getTarget().iterator();
		boolean lclFirst = true;
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getObjectAccessorName());
			lclSB.append("()");
		}
		lclSB.append(')');
		return lclSB.toString();
	}
	
	public String getSafeOpalMutatorName() {
		return MUTATOR_PREFIX + getRoleSourceFieldName() + OPAL_METHOD_SUFFIX;
	}

	public String getUnsafeOpalMutatorName() {
		return MUTATOR_PREFIX + getRoleSourceFieldName() + OPAL_METHOD_SUFFIX + INTERNAL_METHOD_SUFFIX;
	}
	
	public String getOpalAccessorName() {
		return ACCESSOR_PREFIX + getRoleSourceFieldName() + OPAL_METHOD_SUFFIX;
	}

	public String getStreamMethodName() {
		Validate.isTrue(representsManyToOneRelationship());
		return STREAM_METHOD_PREFIX + getRoleCollectionName();
	}
	
	public String getStreamOpalMethodName() {
		Validate.isTrue(representsManyToOneRelationship());
		return STREAM_METHOD_PREFIX + getRoleCollectionName() + OPAL_METHOD_SUFFIX;
	}
	
	public String getAcquireMethodName() {
		Validate.isTrue(representsManyToOneRelationship());
		return ACQUIRE_METHOD_PREFIX + getRoleCollectionName();
	}
	
	public String getArrayMethodName() {
		Validate.isTrue(representsManyToOneRelationship());
		return "create" + getRoleCollectionItemName() + ARRAY_METHOD_SUFFIX;
	}
	
	public String getArrayOpalMethodName() {
		Validate.isTrue(representsManyToOneRelationship());
		return "create" + getRoleCollectionItemName() + OPAL_METHOD_SUFFIX + ARRAY_METHOD_SUFFIX;
	}
	
	public String sourceMethodDeprecation() {
		if (getTargetMappedClass().isDeprecated() && (getSourceMappedClass().isDeprecated() == false)) {
			return "\t@Deprecated" + System.lineSeparator();
		} else {
			return "";
		}
	}
	
	public String targetMethodDeprecation() {
		if (getSourceMappedClass().isDeprecated() && (getTargetMappedClass().isDeprecated() == false)) {
			return "\t@Deprecated" + System.lineSeparator();
		} else {
			return "";
		}
	}
	
	public boolean appearsInSourceUserFacing() {
		return getSource().stream().allMatch(x -> x.appearsInUserFacing(getSourceMappedClass()));
	}
	
	public boolean appearsInTargetUserFacing() {
		return getTarget().stream().allMatch(x -> x.appearsInUserFacing(getTargetMappedClass()));
	}
	
	public String getAccessorName() {
		return ACCESSOR_PREFIX + getRoleSourceFieldName();
	}
	
	public String getMutatorName() {
		return MUTATOR_PREFIX + getRoleSourceFieldName();
	}
	
	@Override
	public int compareTo(MappedForeignKey that) {
		Validate.notNull(that);
		
		return this.getForeignKey().getName().compareTo(that.getForeignKey().getName());
	}
	
	public String getRetrieveOpalMethodName() {
		return RETRIEVE_METHOD_PREFIX + getRoleSourceOpalFieldName();
	}
	
	public void generateOpalFactoryFieldLoader(PrintWriter argBW) {
		Validate.isTrue(hasBackCollection());
		argBW.println("\tpublic " + getCollectionType().getName() + "<" + getSourceMappedClass().getOpalClassName() + "> " + getSource().generateOpalFactoryFunctionDefinition() + " throws " + PersistenceException.class.getName() + ';');
		argBW.println();
	}
	
	public String getOpalFactoryOpalLoaderMethodName() {
		Validate.isTrue(hasBackCollection());
		return "for" + getRoleSourceOpalFieldName() + "Collection";
	}
	
	public String getOpalFactoryOpalCounterMethodName() {
		Validate.isTrue(hasBackCollection());
		return "for" + getRoleSourceOpalFieldName() + "Count";		
	}
	
	public void generateOpalFactoryOpalLoader(PrintWriter argBW) {
		Validate.isTrue(hasBackCollection());
		argBW.println("\tdefault public " + getCollectionType().getName() + "<" + getSourceMappedClass().getOpalClassName() + "> " + getOpalFactoryOpalLoaderMethodName() + "(" + getTargetMappedClass().getOpalClassName() + " argParent) throws " + PersistenceException.class.getName() + " {");
		argBW.println("\t\tif (argParent == null) {");
		argBW.println("\t\t\tthrow new IllegalStateException(\"argParent is null.\");");
		argBW.println("\t\t}");
		for (ClassMember lclCM : getTarget()) {
			String lclV = "lcl" + lclCM.getBaseMemberName();
			argBW.println("\t\t" + lclCM.getMemberType().getName() + " " + lclV + " = argParent." + lclCM.getObjectAccessorName() + "();");
			argBW.println("\t\tif (" + lclV + " == null) { throw new IllegalStateException(\"Key value is null.\"); }");
		}
		argBW.print("\t\treturn " + getSource().generateCollectionFactoryFunctionName() + "(");
		argBW.print(getTarget().stream().map(x -> "lcl" + x.getBaseMemberName()).collect(Collectors.joining(", "))); // Create the parameters being passed to the function
		argBW.println(");");
		argBW.println("\t}");
		argBW.println();
	}

	public void generateOpalFactoryOpalCounter(PrintWriter argBW) {
		argBW.println("\tdefault public int " + getOpalFactoryOpalCounterMethodName() + "(" + getTargetMappedClass().getOpalClassName() + " argParent) throws " + PersistenceException.class.getName() + " {");
		argBW.println("\t\tif (argParent == null) {");
		argBW.println("\t\t\tthrow new IllegalStateException(\"argParent is null.\");");
		argBW.println("\t\t}");
		argBW.println("\t\tthrow new " + UnimplementedOperationException.class.getName() + "();");
		argBW.println("\t}");
		argBW.println();
	}

	public boolean areAllSourceClassMembersNullable() {
		return getSource().areAllNullable();
	}

	public List<ClassMember> getDependents() {
		return myDependents;
	}
	
	public boolean hasDependents() {
		return getDependents().isEmpty() == false;
	}
	
	public boolean shouldSourceExtendSupplierInterface() { // FIXME: Make this configurable // FIXME: New name?
		return ("Protected".equalsIgnoreCase(getSourceFieldAccess()) == false)
				&& appearsInSourceUserFacing()
				&& "".equals(getSourceRolePrefix())
				;
	}

	public boolean createMatchesPredicate() { // FIXME: Make this configurable // FIXME: New name?
		return ("Protected".equalsIgnoreCase(getSourceFieldAccess()) == false)
				&& appearsInSourceUserFacing()
				&& "".equals(getSourceRolePrefix())
				;
	}

	public ReferentialAction getDeleteAction() {
		if (myDeleteAction != null) {
			return myDeleteAction;
		} else {
			return getForeignKey().getDeleteAction();
		}
	}
	
	public void setDeleteAction(ReferentialAction argDeleteAction) { // null means use value from FK
		myDeleteAction = argDeleteAction;
	}

	public ReferentialAction getUpdateAction() {
		if (myUpdateAction != null) {
			return myUpdateAction;
		} else {
			return getForeignKey().getUpdateAction();
		}
	}
	
	public void setUpdateAction(ReferentialAction argUpdateAction) { // null means use value from FK
		myUpdateAction = argUpdateAction;
	}
	
	private static ClassMember getMatchingMember(CompoundClassMember argFind, CompoundClassMember argReturn, ClassMember argCM) {
		if (argCM == null) {
			return null;
		}
		int lclIndex = argFind.indexOf(argCM);
		if (lclIndex == -1) {
			return null;			
		}
		return argReturn.get(lclIndex);
	}
	
	public ClassMember getSourceClassMemberMatching(ClassMember argTCM) {
		return getMatchingMember(getTarget(), getSource(), argTCM);
	}
	
	public ClassMember getTargetClassMemberMatching(ClassMember argSCM) {
		return getMatchingMember(getSource(), getTarget(), argSCM);
	}
	
}

