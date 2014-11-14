package com.panacea.sdk.exception;

/**
 * The exception that is thrown when the user tries to register with an invalid
 * phone number.
 * 
 * @author Cobi Interactive
 */
@SuppressWarnings("serial")
public class PMInvalidPhoneNumberException extends PMException
{
	public PMInvalidPhoneNumberException(String msg)
	{
		super(msg);
	}
}
