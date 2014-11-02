package net.fseconomy.data;
import net.fseconomy.util.Formatters;

public class Money
{
	private static short scale = 100;
	private long value = 0;
	
	public Money()
	{
		super();
	}
	public Money(Money money)
	{
		value = money.value;
	}
	public Money(double v)
	{
		super();
		setValue(v);
	}
	public Money(long v)
	{
		super();
		setValue(v);
	}
	
	public void setValue(double v)
	{
		value = Math.round(v * scale);
	}
	public void setValue(long v)
	{
		value = v * scale;
	}
	
	public String getAsString()
	{
		return Formatters.currency.format(getAsDouble());
	}
	
	public float getAsFloat()
	{
		return (float)value / scale;
	}
	public double getAsDouble()
	{
		return (double)value / scale;
	}
	public int getAsInt()
	{
		return (int)Math.round((double)value / scale);
	}
	public long getAsLong()
	{
		return Math.round((double)value / scale);
	}
	
	// Updates the value of this object with the result
	public void add(Money money)
	{
		value += money.value;
	}
	public void add(double v)
	{
		value += Math.round(v * scale);
	}
	public void add(long v)
	{
		value += v * scale;
	}
	
	public void subtract(Money money)
	{
		value -= money.value;
	}
	public void subtract(double v)
	{
		value -= Math.round(v * scale);
	}
	public void subtract(long v)
	{
		value -= v * scale;
	}
	
	public void multiply(double v)
	{
		value = Math.round(value * v);
	}
	public void multiply(long v)
	{
		value *= v;
	}
	
	// Truncate, don't round. Dividing $0.05 by 2 should not give 2 $0.03 parts.
	// parts = money / X.
	// parts * X is always <= money (or >= money when negative). 
	public void divide(long v)
	{
		value = value / v;
	}
	
	
	// Returns a new Money object with the result. Does not change value of this object.
	public Money plus(Money money)
	{
		Money result = new Money(this);
		result.add(money);
		return result;
	}
	public Money plus(double v)
	{
		return plus(new Money(v));
	}
	public Money plus(long v)
	{
		return plus(new Money(v));
	}
	
	public Money minus(Money money)
	{
		Money result = new Money(this);
		result.subtract(money);
		return result;
	}
	public Money minus(double v)
	{
		return minus(new Money(v));
	}
	public Money minus(long v)
	{
		return minus(new Money(v));
	}
	
	public Money times(double v)
	{
		Money result = new Money(this);
		result.multiply(v);
		return result;
	}
	public Money times(long v)
	{
		Money result = new Money(this);
		result.multiply(v);
		return result;
	}
	
	public Money dividedBy(long v)
	{
		Money result = new Money(this);
		result.divide(v);
		return result;
	}
	
}