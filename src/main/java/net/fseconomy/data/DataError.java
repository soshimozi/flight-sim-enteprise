package net.fseconomy.data;

public class DataError extends Throwable
{
	private static final long serialVersionUID = 1L;

	public DataError()
	{
		super();
	}

	public DataError(String message)
	{
		super(message);
	}
}