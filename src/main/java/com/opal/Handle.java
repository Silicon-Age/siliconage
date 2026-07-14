package com.opal;

import java.lang.reflect.InvocationTargetException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
  * This class is designed to hold an opal in a serializable fashion,
  * for instance so that it can be stored in an HTTP session.
  */
public abstract class Handle<I extends IdentityUserFacing/*<? super I>*/> implements Serializable { // OPALFIXME
	private static final long serialVersionUID = 1L;
	
	private Handle() {
		super();
	}
	
	public abstract I pull();
	
	@Override
	public int hashCode() {
		return pull().hashCode();
	}
	
	@Override
	public boolean equals(Object that) {
		if (that == null) {
			return false;
		} else if (this == that) {
			return true;
		} else if (that instanceof Handle) {
			Handle<?> lclThat = (Handle<?>) that;
			return Objects.equals(this.pull(), lclThat.pull());
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		I lclThis = pull();
		
		if (lclThis == null) {
			return "Handle<null>";
		} else {
			return "Handle<" + lclThis.getClass() + ": " + lclThis.toString() + ">";
		}
	}
	
//	public static <U extends IdentityUserFacing<U>> Handle<U> on(U argU) {
//		if (argU == null) {
//			return NullHandle.getInstance();
//		} else if (argU instanceof OpalBacked) {
//			return new FactoryBasedHandle<>(argU);
//		} else if (argU instanceof Serializable) {
//			return new WrapperHandle<>(argU);
//		} else {
//			throw new IllegalArgumentException("We don't know how to handle this kind of opal");
//		}
//	}
	
	public static <I extends IdentityUserFacing/*<? super I>*/> Handle<I> on(I argU) { // OPALFIXME
		if (argU == null) {
			return NullHandle.getInstance();
		} else if (argU instanceof OpalBacked) {
			return new FactoryBasedHandle<>(argU);
		} else if (argU instanceof Serializable) {
			return new WrapperHandle<>(argU);
		} else {
			throw new IllegalArgumentException("We don't know how to handle this kind of opal");
		}
	}
	
	public static <U extends IdentityUserFacing/*<U>*/> Set<Handle<U>> onAll(Set<U> argRaw) { // OPALFIXME
		Objects.requireNonNull(argRaw);
		
		return argRaw.stream().map(Handle::on).collect(Collectors.toSet());
	}
	
	public static <U extends IdentityUserFacing/*<U>*/> List<Handle<U>> onAll(Collection<U> argRaw) { // OPALFIXME
		Objects.requireNonNull(argRaw);
		
		return argRaw.stream().map(Handle::on).collect(Collectors.toList());
	}
	
	public static <U extends IdentityUserFacing/*<U>*/> Set<U> pullAll(Set<Handle<U>> argHandles) { // OPALFIXME
		Objects.requireNonNull(argHandles);
		
		return argHandles.stream().map(Handle::pull).collect(Collectors.toSet());
	}
	
	public static <U extends IdentityUserFacing/*<U>*/> List<U> pullAll(Collection<Handle<U>> argHandles) { // OPALFIXME
		Objects.requireNonNull(argHandles);
		
		return argHandles.stream().map(Handle::pull).collect(Collectors.toList());
	}
	
	private static class NullHandle<I extends IdentityUserFacing/*<? super I>*/> extends Handle<I> { // OPALFIXME
		private static final long serialVersionUID = 1L;
		
		private static final NullHandle<?> ourInstance = new NullHandle<>();
		
		@SuppressWarnings("unchecked")
		public static <I extends IdentityUserFacing/*<? super I>*/> NullHandle<I> getInstance() { // OPALFIXME
			return (NullHandle<I>) ourInstance;
		}
		
		private NullHandle() {
			super();
		}
		
		@Override
		public I pull() {
			return null;
		}
	}
	
	private static class WrapperHandle<I extends IdentityUserFacing/*<? super I>*/> extends Handle<I> { // OPALFIXME
		private static final long serialVersionUID = 1L;
		
		private final I myI;
		
		/* package */ WrapperHandle(I argI) {
			super();
			
			Objects.requireNonNull(argI);
			Validate.isTrue(argI instanceof Serializable, "Must be Serializable!");
			myI = argI;
		}
		
		@Override
		public I pull() {
			return myI;
		}
	}
	
	private static class FactoryBasedHandle<I extends IdentityUserFacing/*<? super I>*/> extends Handle<I> { // OPALFIXME
		private static final long serialVersionUID = 1L;
		
		private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(FactoryBasedHandle.class.getName());
		
		private final String myUniqueString;
		private final String myFactoryName;
		
		/* package */ FactoryBasedHandle(I argU) {
			super();
			
			Objects.requireNonNull(argU);
			Validate.isTrue(argU instanceof OpalBacked);
			Validate.isTrue(!argU.isNew(), "Handles cannot be created on new objects");
			
			myUniqueString = argU.getUniqueString();
			
			@SuppressWarnings("unchecked")
			OpalBacked<I, Opal<? extends I>> lclOB = (OpalBacked<I, Opal<? extends I>>) argU;
			
			IdentityOpal<? extends I> lclOpal = (IdentityOpal<? extends I>) lclOB.getBottomOpal();
			
			/* This OpalFactory's definition is more wildcard-y than it probably has to be, but I can't figure out how
			 * to get the type parameters exactly right.
			 */
			OpalFactory<? extends I, ?> lclFactory = lclOpal.getOpalFactory();
			myFactoryName = lclFactory.getClass().getName();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public I pull() {
			try {
//				Class<AbstractIdentityOpalFactory<I, IdentityOpal<I>>> lclFactoryClass = (Class<AbstractIdentityOpalFactory<? suoer I, IdentityOpal<? super I>>>) Class.forName(myFactoryName);
//				
//				AbstractIdentityOpalFactory<I, IdentityOpal<I>> lclFactoryInstance = (AbstractIdentityOpalFactory<I, IdentityOpal<I>>) MethodUtils.invokeStaticMethod(lclFactoryClass, "getInstance");
//				
//				IdentityOpal<I> lclOpal = lclFactoryInstance.forUniqueString(myUniqueString);
				
				var lclFactoryClass = (Class<AbstractIdentityOpalFactory<? extends I, ?>>) Class.forName(myFactoryName);
				
				var lclFactoryInstance = (AbstractIdentityOpalFactory<? extends I, ?>) MethodUtils.invokeStaticMethod(lclFactoryClass, "getInstance");
				
				var lclOpal = lclFactoryInstance.forUniqueString(myUniqueString);

				Validate.notNull(lclOpal, "Factory came back with a null Opal for the unique string " + myUniqueString);
				
				return lclOpal.getUserFacing();
			} catch (ClassNotFoundException lclCNFE) {
				ourLogger.error("Could not find factory class " + myFactoryName, lclCNFE);
				return null;
			} catch (NoSuchMethodException lclNSME) {
				ourLogger.error("No getInstance() method on " + myFactoryName, lclNSME);
				return null;
			} catch (IllegalAccessException lclIAE) {
				ourLogger.error("Cannot access getInstance() method on " + myFactoryName, lclIAE);
				return null;
			} catch (InvocationTargetException lclITE) {
				ourLogger.error("Cannot invoke getInstance() method on " + myFactoryName, lclITE);
				return null;
			}
		}
	}
}
