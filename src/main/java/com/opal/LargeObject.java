package com.opal;

//import java.lang.ref.SoftReference;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author topquark
 */
public class LargeObject extends AbstractTransactionAware {
//	private static final SoftReference<byte[]> IS_NULL = new SoftReference<>(null);
	
//	private SoftReference<byte[]> myOld = null;
	private byte[] myNew = null;
	
	public LargeObject() {
		super();
	}
	
//	public Opal<?> getOpal() {
//		return myOpal;
//	}
	
//	public synchronized <U extends UserFacing, O extends Opal<U>> byte[] get(O argOpal, int argLargeFieldIndex) {
//		/* Does this LargeObject belong to the active TransactionContext? */
//		if (tryAccess()== false) {
//			/* No.  We reveal the old value. */
//			return getOld(argOpal, argLargeFieldIndex);
//		} else {
//			/* Yes.  We reveal the new value. */
//			return getNew();
//		}
//	}
	
//	public synchronized <O extends Opal<? extends UserFacing>> void set(O argOpal, int argLargeFieldIndex, byte[] argBytes) {
//		tryMutate();
//		myNew = argBytes;
//	}
	
//	protected <U extends UserFacing, O extends Opal<U>> byte[] loadValue(O argOpal, int argLargeFieldIndex) {
//		byte[] lclOld = ((AbstractDatabaseOpalFactory<U, O>) argOpal.getOpalFactory()).loadLargeObjectContent(
//				argOpal,
//				argLargeFieldIndex
//				);
//		if (lclOld == null) {
//			myOld = IS_NULL;
//			return null;
//		} else {
//			myOld = new SoftReference<>(lclOld);
//			return lclOld;
//		}
//	}
//	
//	public <U extends UserFacing, O extends Opal<U>> byte[] getOld(O argOpal, int argLargeFieldIndex) {
//		if (myOld == null) {
//			return loadValue(argOpal, argLargeFieldIndex);
//		} else if (myOld == IS_NULL) {
//			return null;
//		} else {
//			byte[] lclOld = myOld.get();
//			if (lclOld == null) {
//				return loadValue(argOpal, argLargeFieldIndex);
//			} else {
//				return lclOld;
//			}
//		}
//	}
//	
	public byte[] getNew() {
		return myNew;
	}
	
	@Override
	protected void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected void commitPhaseTwoInternal(TransactionParameter argTP) throws PersistenceException {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void joinTransactionContextInternal() {
		/* Do nothing */
	}
	
	@Override
	protected void rollbackInternal() {
//		myNew = null;
	}
}
