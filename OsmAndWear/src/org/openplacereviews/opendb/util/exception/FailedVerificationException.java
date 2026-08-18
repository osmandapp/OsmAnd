//Revision d1a1f6e81d0716a47cbddf5754ee77fa5fc6d1d8
package org.openplacereviews.opendb.util.exception;

public class FailedVerificationException extends Exception {

	private static final long serialVersionUID = -4936205097177668159L;


	public FailedVerificationException(Exception e) {
		super(e);
	}


	public FailedVerificationException(String msg) {
		super(msg);
	}
}