package com.opal.cma;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.siliconage.web.form.HiddenField;

/* package */ class OpalFormSecurityUtil {
	private OpalFormSecurityUtil() {
		throw new UnsupportedOperationException();
	}
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalFormSecurityUtil.class.getName());
	
	private static final Charset CHARSET = Charset.forName("UTF-8");

	private static final int DYNAMIC_SALT_LENGTH = 64;
	/* package */ static final String DYNAMIC_SALT_FIELD_NAME = "com.opal.cma.OpalFormSecurityUtil.dynamic_salt";
	/* package */ static final String DIGEST_FIELD_NAME = "com.opal.cma.OpalFormSecurityUtil.digest";
	
	private static String STATIC_SALT;
	static {
		try {
			InitialContext lclIC = new InitialContext();
			String lclSecuritySalt = (String) lclIC.lookup("java:comp/env/opal/forms/salt");
			if (StringUtils.isBlank(lclSecuritySalt)) {
				ourLogger.warn("No OpalForms security salt was provided in the InitialContext; OpalForms security will not be very effective");
				// STATIC_SALT will be null, which means it's not used, which means the digest is made exclusively from content that the user has access to
			} else {
				STATIC_SALT = lclSecuritySalt;
			}
		} catch (NamingException lclE) {
			ourLogger.warn("Couldn't look up OpalForms security salt; OpalForms security will not be very effective", lclE);
		}
	}
	
	/* package */ static String generateDigest(Collection<String> argFieldNames, String argDynamicSalt) {
		MessageDigest lclDigest = DigestUtils.getSha256Digest();
		
		List<String> lclHashInputs = argFieldNames.stream()
			.distinct()
			.sorted()
			.toList();
		for (String lclInput : lclHashInputs) {
			lclDigest.update(lclInput.getBytes(CHARSET));
		}
		if (ourLogger.isInfoEnabled()) {
			ourLogger.info("Generating digest: " + lclHashInputs);
		}
		
		if (STATIC_SALT != null) {
			lclDigest.update(STATIC_SALT.getBytes(CHARSET));
		}
		
		lclDigest.update(argDynamicSalt.getBytes(CHARSET));
		
		return Hex.encodeHexString(lclDigest.digest());
	}
	
	/* package */ static String generateSecurityFields(Collection<String> argFieldNames) {
		String lclDynamicSalt = RandomStringUtils.randomAlphanumeric(DYNAMIC_SALT_LENGTH);
		
		String lclDigestString = generateDigest(argFieldNames, lclDynamicSalt);
		
		return new HiddenField<>(DYNAMIC_SALT_FIELD_NAME, lclDynamicSalt).id(DYNAMIC_SALT_FIELD_NAME).toString() +
			new HiddenField<>(DIGEST_FIELD_NAME, lclDigestString).id(DIGEST_FIELD_NAME).toString();
	}
}
