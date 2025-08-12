package com.opal.creator;

/* package */ record DBOpts(
		String connectString,
		String driverName,
		String JNDIName,
//		String defaultDatabase,
//		String defaultOwner,
		String username, // This might also appear in the connect string
		String password // This might also appear in the connect string
		) {
	
	/* Only default implementations. */
}
