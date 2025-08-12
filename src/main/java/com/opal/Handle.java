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
public abstract class Handle<U extends IdentityUserFacing> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Handle() {
		super();
	}
	
	public abstract U pull();
	
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
		U lclThis = pull();
		
		if (lclThis == null) {
			return "Handle<null>";
		} else {
			return "Handle<" + lclThis.getClass() + ": " + lclThis.toString() + ">";
		}
	}
	
	public static <U extends IdentityUserFacing> Handle<U> on(U argU) {
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
	
	public static <U extends IdentityUserFacing> Set<Handle<U>> onAll(Set<U> argRaw) {
		Validate.notNull(argRaw);
		
		return argRaw.stream().map(Handle::on).collect(Collectors.toSet());
	}
	
	public static <U extends IdentityUserFacing> List<Handle<U>> onAll(Collection<U> argRaw) {
		Validate.notNull(argRaw);
		
		return argRaw.stream().map(Handle::on).collect(Collectors.toList());
	}
	
	public static <U extends IdentityUserFacing> Set<U> pullAll(Set<Handle<U>> argHandles) {
		Validate.notNull(argHandles);
		
		return argHandles.stream().map(Handle::pull).collect(Collectors.toSet());
	}
	
	public static <U extends IdentityUserFacing> List<U> pullAll(Collection<Handle<U>> argHandles) {
		Validate.notNull(argHandles);
		
		return argHandles.stream().map(Handle::pull).collect(Collectors.toList());
	}
	
	private static class NullHandle<U extends IdentityUserFacing> extends Handle<U> {
		private static final long serialVersionUID = 1L;
		
		private static final NullHandle<?> ourInstance = new NullHandle<>();
		
		@SuppressWarnings("unchecked")
		public static <U extends IdentityUserFacing> NullHandle<U> getInstance() {
			return (NullHandle<U>) ourInstance;
		}
		
		private NullHandle() {
			super();
		}
		
		@Override
		public U pull() {
			return null;
		}
	}
	
	private static class WrapperHandle<U extends IdentityUserFacing> extends Handle<U> {
		private static final long serialVersionUID = 1L;
		
		private final U myU;
		
		/* package */ WrapperHandle(U argU) {
			super();
			
			Validate.notNull(argU);
			Validate.isTrue(argU instanceof Serializable, "Must be Serializable!");
			myU = argU;
		}
		
		@Override
		public U pull() {
			return myU;
		}
	}
	
	private static class FactoryBasedHandle<U extends IdentityUserFacing> extends Handle<U> {
		private static final long serialVersionUID = 1L;
		
		private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(FactoryBasedHandle.class.getName());
		
		private final String myUniqueString;
		private final String myFactoryName;
		
		/* package */ FactoryBasedHandle(U argU) {
			super();
			
			Validate.notNull(argU);
			Validate.isTrue(argU instanceof OpalBacked);
			Validate.isTrue(!argU.isNew(), "Handles cannot be created on new objects");
			
			myUniqueString = argU.getUniqueString();
			
			@SuppressWarnings("unchecked")
			OpalBacked<U, Opal<? extends U>> lclOB = (OpalBacked<U, Opal<? extends U>>) argU;
			
			IdentityOpal<? extends U> lclOpal = (IdentityOpal<? extends U>) lclOB.getBottomOpal();
			
			/* This OpalFactory's definition is more wildcard-y than it probably has to be, but I can't figure out how
			 * to get the type parameters exactly right.
			 */
			OpalFactory<? extends U, ?> lclFactory = lclOpal.getOpalFactory();
			myFactoryName = lclFactory.getClass().getName();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public U pull() {
			try {
				Class<AbstractIdentityOpalFactory<U, IdentityOpal<U>>> lclFactoryClass = (Class<AbstractIdentityOpalFactory<U, IdentityOpal<U>>>) Class.forName(myFactoryName);
				
				AbstractIdentityOpalFactory<U, IdentityOpal<U>> lclFactoryInstance = (AbstractIdentityOpalFactory<U, IdentityOpal<U>>) MethodUtils.invokeStaticMethod(lclFactoryClass, "getInstance");
				
				IdentityOpal<U> lclOpal = lclFactoryInstance.forUniqueString(myUniqueString);
				
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
