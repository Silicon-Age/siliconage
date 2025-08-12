/*
 * Created on Apr 12, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.opal;

/**
 * @author topquark
 */
public abstract class JoinQueryFactory {
	protected JoinQueryFactory() {
		super();
	}
	
	public abstract Query createSourceToTargetQuery(Object argSource);
	
	public abstract Query createTargetToSourceQuery(Object argTarget);
}
