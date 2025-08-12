package com.opal.creator;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.opal.creator.database.Index;

public class CompoundClassMember extends ArrayList<ClassMember> {
//	private ArrayList<ClassMember> myClassMembers = new ArrayList<>();
	
	private static final long serialVersionUID = 1L;
	
	public CompoundClassMember() {
		super();
	}
	
	public String generateCollectionFactoryFunctionName() {
		return generateFactoryFunctionName() + "Collection";
	}
	
	public String generateUniqueFactoryFunctionName() {
		return generateFactoryFunctionName();
	}
	
	public String generateFactoryFunctionName() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append("for");
		Iterator<ClassMember> lclCMI = iterator();
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			lclSB.append(lclCM.getBaseMemberName());
		}
		return lclSB.toString();
	}
	
//	public String generateFactoryFunctionCall(boolean argUnique) {
			
	public String generateOpalFactoryFunctionDefinition() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(generateCollectionFactoryFunctionName());
//		lclSB.append("for");
//		Iterator<ClassMember> lclI = createClassMembersIterator();
//		while (lclI.hasNext()) {
//			ClassMember lclCM = lclI.next();
//			lclSB.append(lclCM.getBaseMemberName());
//		}
		lclSB.append('(');
		Iterator<ClassMember> lclCMI = iterator();
		boolean lclFirst = true;
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getMemberType().getName());
			lclSB.append(' ');
			lclSB.append(lclCM.getPrimitiveMutatorArgumentName());
		}
		lclSB.append(')');
		
		return lclSB.toString();
	}
	
//	protected ArrayList<ClassMember> getClassMembers() {
//		return myClassMembers;
//	}
	
//	protected void setClassMembers(ArrayList<ClassMember> argClassMembers) {
//		myClassMembers = argClassMembers;
//	}
	
	public boolean hasIdenticalClassMembers(CompoundClassMember argCCM) {
		if (argCCM == null) {
			return false;
		}
//		List<ClassMember> lclCM1 = this.getClassMembers();
//		assert lclCM1 != null;
//		
//		List<ClassMember> lclCM2 = argCCM.getClassMembers();
//		assert lclCM2 != null;
				
		if (this.size() != argCCM.size()) {
			return false;
		}
		
		for (int lclI = 0; lclI < this.size(); ++lclI) {
			assert this.get(lclI) != null;
			assert argCCM.get(lclI) != null;
			
			if (this.get(lclI).equals(argCCM.get(lclI)) == false) {
				return false;
			} 
		}
		
		return true;
	}
	
	public boolean hasIdenticalClassMembers(Index argIndex) {
		if (argIndex == null) {
			return false;
		}
//		List<ClassMember> lclCM1 = this.getClassMembers();
//		assert lclCM1 != null;
		
		List<String> lclCM2 = argIndex.getColumnNames();
		assert lclCM2 != null;
				
		if (this.size() != lclCM2.size()) {
			return false;
		}
		
		for (int lclI = 0; lclI < this.size(); ++lclI) {
			assert this.get(lclI) != null;
			assert lclCM2.get(lclI) != null;
			
			if (this.get(lclI).getDatabaseColumn().getName().equals(lclCM2.get(lclI)) == false) {
				return false;
			} 
		}
		
		return true;
	}
	
	public boolean hasIdenticalClassMembers(MappedUniqueKey argMUK) {
		if (argMUK == null) {
			return false;
		}
		
		return hasIdenticalClassMembers(argMUK.getIndex());
	}
	
	@Override
	public boolean equals(Object argO) {
		if (argO == null) {
			return false;
		} else if (argO instanceof CompoundClassMember) {
			return hasIdenticalClassMembers((CompoundClassMember) argO);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int lclCode = 0;
		for (ClassMember lclCM : this) {
			assert lclCM != null;
			lclCode ^= lclCM.getDatabaseColumn().getName().hashCode();
		}
		
		return lclCode;
	}
	
	public boolean hasStoreGeneratedClassMember() {
		for (ClassMember lclCM : this) {
			if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean areAllNullable() {
		Iterator<ClassMember> lclCMI = iterator();
		while (lclCMI.hasNext()) {
			if (lclCMI.next().isNullAllowed() == false) {
				return false;
			}
		}
		return true;
	}
}
