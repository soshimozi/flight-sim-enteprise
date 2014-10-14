package net.fseconomy.data;
/**
 * @author Marty
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DataError extends Throwable
{
	private static final long serialVersionUID = 1L;
	/**
	 * Constructor for DataError.
	 */
	public DataError()
	{
		super();
	}
	/**
	 * Constructor for DataError.
	 * @param message
	 */
	public DataError(String message)
	{
		super(message);
	}
}
