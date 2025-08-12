package com.opal;

//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.Map;

public abstract class AbstractOpalFactoryFactory {

//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(AbstractOpalFactoryFactory.class.getName());

//	private final Map<Class<? extends IdentityUserFacing>, IdentityFactory<?>> myFactories = new HashMap<>();
	
	protected AbstractOpalFactoryFactory() {
		super();
	}

//	protected void buildMap() {
//		System.out.println("Building map");
//		if (myFactories.size() != 0) {
//			throw new IllegalStateException("Invoked buildMap when it had already been initialized.");
//		}
//		try {
//			for (Method lclM : this.getClass().getMethods()) {
//				System.out.println("Checking out " + lclM.getName());
//				if (lclM.getName().startsWith("get") == false) {
//					continue;
//				}
//				if (lclM.getParameterCount() != 0) {
//					continue;
//				}
//				if (IdentityFactory.class.isAssignableFrom(lclM.getReturnType().getClass()) == false) {
//					continue;
//				}
//				IdentityFactory<?> lclFactory = (IdentityFactory<?>) lclM.invoke(this);
//				if (lclFactory == null) {
//					continue;
//				}
//				Class<? extends IdentityUserFacing> lclUserFacingInterface = lclFactory.getUserFacingInterface();
//				
//				System.out.println("Mapping " + lclUserFacingInterface + " to " + lclFactory);
//				myFactories.put(lclUserFacingInterface, lclFactory);
//			}
//			System.out.println("Done building map");
//			
//			return;
//		} catch (Exception lclE) {
//			ourLogger.error("Could not initialize AbstractOpalFactoryFactory map linking interfaces to the default Factory that produces them.", lclE);
//		}
//	}
//	
//	public synchronized <U extends IdentityUserFacing> IdentityFactory<U> getFactory(Class<U> argUserFacingInterface) {
//		if (argUserFacingInterface == null) {
//			throw new IllegalArgumentException("argUserFacingInterface is null");
//		}
//		if (myFactories.size() == 0) {
//			buildMap();
//		}
//		
//		System.out.println("Looking up " + argUserFacingInterface);
//		
//		@SuppressWarnings("unchecked")
//		IdentityFactory<U> lclIF = (IdentityFactory<U>) myFactories.get(argUserFacingInterface); // Might return null
//
//		return lclIF;
//	}
	
}
