package net.fseconomy.beans;

import java.io.Serializable;
import java.sql.*;
import java.util.Date;

import net.fseconomy.data.Data;
import net.fseconomy.util.Constants;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;

public class AircraftBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int ACC_TACHO=1;
	public static final int ACC_HOUR=2;
	
	public static final int ADV_FERRY = 1;
	
	public static final int ALLOW_REPAIR = 1;
	
	public static final int TBO_RECIP = 5400000;  //1500 hrs * 3600 seconds
	public static final int TBO_JET = 7200000; //2000 hrs * 3600 seconds
	
	public static final int REPAIR_RANGE_LOW = 50000;
	public static final int REPAIR_RANGE_HIGH = 50100;
	static final int DIVISOR = 150;
		
	public static final int FUELTYPE_100LL = 0;
	public static final int FUELTYPE_JETA = 1;

    int id;
	String make;
	String model;
	String registration;
	String location;
	String home;
	String departedFrom;
	int rentalPriceDry, rentalPriceWet;
	int userLock;
	int modelId;
	int maxRentTime;
	int bonus;
	int seats;
	Timestamp lockedSince;
	int accounting;
	int owner;
	int lessor; //added Airboss 5/6/11
	float fuel[];
	int capacity[];
	double initialFuel;
	int sellPrice;
	int equipment;
	int modelPrice;
	int bearing, distance;
	int maxWeight;
	int emptyWeight;
	int lastCheck;
	int totalEngineTime;
	int engines;
	int enginePrice;
	int gph;
	int cruise;
	int crew;
	boolean advertiseFerry;
	int airframe;
	int condition;
	public int[] addedPriceArray; 
	boolean allowRepair;
	int lastFix;
	int fueltype;
	
	//Added for aircraft shipping
	boolean canShip;
	int shippingState;
	Timestamp shippingStateNext;
	int shippedBy;
	int shippingAssignment;
	String shippingTo;

	//These aircraft shipping variable are only filled in when needed.
	int shippingStateDelay; //seconds
	double shippingCostPerKg;
	int shippingCostPerCrate;
	int shippingCostDisposal;
	
	//onboard fuel calculation helper values
	float fueltotal;	
	
	boolean holdRental;
	
	public AircraftBean(ResultSet rs) throws SQLException
	{
        setId(rs.getInt("id"));
		setRegistration(rs.getString("registration"));
		setHome(rs.getString("home"));
		setLocation(rs.getString("location"));
		setMake(rs.getString("models.make"));
		setModel(rs.getString("models.model"));
		setUserLock(rs.getInt("userlock"));
		setModelId(rs.getInt("models.id"));
		setSeats(rs.getInt("models.seats"));
		setLockedSince(rs.getTimestamp("lockedSince"));
		setRentalPriceDry(rs.getInt("aircraft.rentalDry"));
		setRentalPriceWet(rs.getInt("aircraft.rentalWet"));
		setInitialFuel(rs.getFloat("aircraft.initialFuel"));
		setSellPrice(rs.getInt("sellPrice"));		
		setMaxRentTime(rs.getString("aircraft.maxRentTime") == null ? rs.getInt("models.maxRentTime") : rs.getInt("aircraft.maxRentTime"));
		setAccounting();
		setDepartedFrom(rs.getString("departedFrom"));
		setBonus(rs.getString("aircraft.bonus") == null ? rs.getInt("models.bonus") : rs.getInt("aircraft.bonus"));
		setOwner(rs.getInt("owner"));
		setLessor(rs.getInt("lessor")); //added Airboss 5/6/11
		setEquipment(rs.getInt("equipment"));
		setModelPrice(rs.getInt("models.price"));
		setMaxWeight(rs.getInt("models.maxWeight"));
		setEmptyWeight(rs.getInt("models.emptyWeight"));
		setLastCheck(rs.getInt("aircraft.lastCheck"));
		setTotalEngineTime(rs.getInt("aircraft.engine1"));
		setEngines(rs.getInt("models.engines"));
		setEnginePrice(rs.getInt("models.enginePrice"));
		setGph(rs.getInt("models.gph"));
		setCruise(rs.getInt("models.cruisespeed"));
		setAdvertise(rs.getInt("aircraft.advertise"));
		setCrew(rs.getInt("models.crew")); 
		setAirframe(rs.getInt("aircraft.airframe"));
		setCondition(rs.getInt("condition"));
		setAllowFix(rs.getInt("aircraft.allowFix"));
		setLastFix(rs.getInt("lastFix"));
		setFuelType(rs.getInt("fueltype"));
		
		//Added for aircraft shipping - Airboss 12/21/10
		setCanShip(rs.getBoolean("canShip")); 		//value from models
		setShippingState(rs.getInt("shippingState"));
		setShippingStateNext(rs.getTimestamp("shippingStateNext"));
		setShippedBy(rs.getInt("shippedBy"));
		setShippingTo(rs.getString("shippingTo"));				
		
		if (rs.getString("aircraft.bearingToHome") == null)
			setBearing(-1);
		else
			setBearing(rs.getInt("aircraft.bearingToHome"));
		
		setFuel(new float[] {rs.getFloat("fuelCenter"), rs.getFloat("fuelLeftMain"), rs.getFloat("fuelLeftAux"), rs.getFloat("fuelLeftTip"),
			rs.getFloat("fuelRightMain"), rs.getFloat("fuelRightAux"), rs.getFloat("fuelRightTip"), rs.getFloat("fuelCenter2"), rs.getFloat("fuelCenter3"),
			rs.getFloat("fuelExt1"), rs.getFloat("fuelExt2")});
		capacity = new int[] {rs.getInt("fcapCenter"), rs.getInt("fcapLeftMain"), rs.getInt("fcapLeftAux"), rs.getInt("fcapLeftTip"),
			rs.getInt("fcapRightMain"), rs.getInt("fcapRightAux"), rs.getInt("fcapRightTip"), rs.getInt("fcapCenter2"), rs.getInt("fcapCenter3"),
			rs.getInt("fcapExt1"), rs.getInt("fcapExt2")};
		
		setHoldRental(rs.getBoolean("holdRental"));
	}

	/**
	 * Constructor for AircraftBean.
	 */
	public AircraftBean()
	{
		super();
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateInt("lessor", getLessor()); //added Airboss 5/6/11
		rs.updateString("home", getHome());
		rs.updateInt("bonus", getBonus());
		rs.updateString("accounting", getSAccounting());
		rs.updateInt("rentalDry", getRentalPriceDry());
		rs.updateInt("rentalWet", getRentalPriceWet());
		rs.updateInt("maxRentTime", getMaxRentTime());
		rs.updateInt("equipment", getEquipment());
		rs.updateInt("advertise", getAdvertise());
		rs.updateInt("allowFix", getAllowFix());
		if (home.equals(location))
		{
			rs.updateNull("bearingToHome");
			rs.updateInt("distanceFromHome", 0);
		} else
		{
			rs.updateInt("bearingToHome", getBearing());
			rs.updateInt("distanceFromHome", getDistance());
		}
		if (getSellPrice() != 0)
			rs.updateInt("sellPrice", getSellPrice());
		else
			rs.updateNull("sellPrice");
		
		rs.updateBoolean("holdRental", holdRental);
	}
	
	private String TruncateRegistration(String reg)
	{
		if(reg == null)
			return null;
		
		int MAXREGISTRATIONSIZE = 20;	
		int maxLength = (reg.length() < MAXREGISTRATIONSIZE) ? reg.length() : MAXREGISTRATIONSIZE;

		return reg.substring(0, maxLength);
	}
	
	private String TruncateComment(String comment)
	{
		if(comment == null)
			return null;
		
		int MAXCOMMENTSIZE = 255;	
		int maxLength = (comment.length() < MAXCOMMENTSIZE) ? comment.length() : MAXCOMMENTSIZE;

		return comment.substring(0, maxLength);
	}
	
	//updated by gurka for All-In fuel changes
	public void writeFuel(ResultSet rs) throws SQLException
	{
		double totalFuel = 0;
		int tankswithfuel = 0;
		
		if (capacity[0] > 0) 
		{
			rs.updateDouble("fuelCenter", fuel[0]);
			totalFuel += fuel[0];
			tankswithfuel++;
		}
		if (capacity[1] > 0) 
		{
			rs.updateDouble("fuelLeftMain", fuel[1]);
			totalFuel += fuel[1];
			tankswithfuel++;
		}
		if (capacity[2] > 0) 
		{
			rs.updateDouble("fuelLeftAux", fuel[2]);
			totalFuel += fuel[2];
			tankswithfuel++;
		}
		if (capacity[3] > 0) 
		{
			rs.updateDouble("fuelLeftTip", fuel[3]);
			totalFuel += fuel[3];
			tankswithfuel++;
		}
		if (capacity[4] > 0) 
		{
			rs.updateDouble("fuelRightMain", fuel[4]);
			totalFuel += fuel[4];
			tankswithfuel++;
		}
		if (capacity[5] > 0) 
		{
			rs.updateDouble("fuelRightAux", fuel[5]);
			totalFuel += fuel[5];
			tankswithfuel++;
		}
		if (capacity[6] > 0) 
		{
			rs.updateDouble("fuelRightTip", fuel[6]);
			totalFuel += fuel[6];
			tankswithfuel++;
		}
		if (capacity[7] > 0) 
		{
			rs.updateDouble("fuelCenter2", fuel[7]);
			totalFuel += fuel[7];
			tankswithfuel++;
		}
		if (capacity[8] > 0) 
		{
			rs.updateDouble("fuelCenter3", fuel[8]);
			totalFuel += fuel[8];
			tankswithfuel++;
		}
		if (capacity[9] > 0) 
		{
			rs.updateDouble("fuelExt1", fuel[9]);
			totalFuel += fuel[9];
			tankswithfuel++;
		}
		if (capacity[10] > 0) 
		{
			rs.updateDouble("fuelExt2", fuel[10]);
			totalFuel += fuel[10];
			tankswithfuel++;
		}
		
		if (rs.getString("initialFuel") != null)
			rs.updateFloat("initialFuel", (float)getTotalFuel());
		
		//All-In
		rs.updateDouble("fueltotal", totalFuel/tankswithfuel);				
	}
	
	double equalizeTanks(int tank1, int tank2, double maxFuel)
	{
		if (capacity[tank1] == 0)
			return 0;
		if (capacity[tank2] == 0)
			return 0;
		if (fuel[tank1] == fuel[tank2])
			return 0;
		int toFill = fuel[tank1] < fuel[tank2] ? tank1 : tank2;
		if (Math.abs(fuel[tank1] * capacity[tank1] - fuel[tank2] * capacity[tank2]) > maxFuel)
		{
			fuel[toFill] += maxFuel/capacity[toFill];
			return maxFuel;
		} else
		{
			double diff = Math.abs(fuel[tank1] - fuel[tank2]);
			fuel[toFill] += diff;
			return diff * capacity[toFill];
		}
	}
	double addTanks(int tank1, int tank2, double maxFuel)
	{
		if (capacity[tank1] == 0)
			return 0;
		if (capacity[tank2] == 0)
			return 0;
		if (fuel[tank1] == 1)
			return 0;
		double space = ((1 - fuel[tank1]) * capacity[tank1] + (1 - fuel[tank2]) * capacity[tank2]);
		if (maxFuel > space)
		{
			fuel[tank1] = fuel[tank2] = (float) 1.0;
			return space;
		} else
		{
			fuel[tank1] += maxFuel/(2 * capacity[tank1]);
			fuel[tank2] += maxFuel/(2 * capacity[tank2]);
			return maxFuel;
		}
	}
	double addTank(int tank, double maxFuel)
	{
		if (capacity[tank] == 0)
			return 0;
		if (fuel[tank] == 1)
			return 0;
		double space = (1 - fuel[tank]) * capacity[tank];
		if (maxFuel > space)
		{
			fuel[tank] = (float) 1.0;
			return space;
		} else
		{
			fuel[tank] += maxFuel/capacity[tank];
			return maxFuel;
		}
	}
	
	// emptyAllFuel used for defueling. emptyAllFuel() then addFuel()
	public void emptyAllFuel()
	{
		for (int count = 0; count< fuel.length; count++)
			fuel[count] = 0;
	}
	
	public void addFuel(int amount)
	{
		int max = getTotalCapacity();
		if (amount > max)
			amount = max;
		double togo = amount - getTotalFuel();
		if (togo <= 0)
			return;

		togo -= equalizeTanks(1, 4, togo);
		togo -= equalizeTanks(2, 5, togo);
		togo -= equalizeTanks(3, 6, togo);
		togo -= equalizeTanks(9, 10, togo);          // pa30 twin comanche

		if (capacity[4] == 0)                        // A26 has no right main tank
			togo -= addTank(1, togo);
		else
			togo -= addTanks(1, 4, togo);

		togo -= addTanks(2, 5, togo);
		togo -= addTanks(3, 6, togo);
		togo -= addTanks(9, 10, togo);                // pa30 twin comanche
		togo -= addTank(0, togo);
		togo -= addTank(7, togo);
		togo -= addTank(8, togo);
		togo -= addTank(9, togo);
		addTank(10, togo);
	}
	
	public int getMaintenancePrice(int type, FboBean fbo)
	{
		return (int)Math.round(getRawMaintenancePrice(type) * (1 + fbo.getRepairShopMargin()/100.0));
	}
	
	public int getRawMaintenancePrice(int type)
	{
		switch (type)
		{
			case AircraftMaintenanceBean.MAINT_100HOUR:
				return (int)(0.05/1.25 * engines * enginePrice);
			case AircraftMaintenanceBean.MAINT_REPLACEENGINE:
				return engines * enginePrice;		// was 0.8/1.25
			case AircraftMaintenanceBean.MAINT_FIXAIRCRAFT:
				return 0;
		}
		return 0;
	}
	
	/**
	 * Description: Additional maintenance cost due to aircraft age.  (Digital parsing code modified from Neal Ziring)
	 * @param aircraft AircraftBean
	 * @return int[]
	 */
	public int[] getConditionPrice(AircraftBean aircraft, int type)
	{
        //TODO huh?? Why are we passing in aircraft and not doing anything?
		int acCondition = this.getCondition();
		int[] acConditionArray;	
		acConditionArray = new int[5]; //set to 5 right now - increase here and in getCondition as needed		
		addedPriceArray = new int[4];	

       int num, digit, digits, divisor, i;

       // convert the input argument to an integer
       num = acCondition;
       i = 0;

       // compute number of digits-1
       digits = (int)Math.floor(Math.log10(num));
       
       // compute the highest power of 10 we need to
       // print all the digits
       divisor = (int)Math.pow(10.0, digits);

       // get each digit and print it, by dividing by
       // successively smaller powers of ten.
	   while(divisor > 0)
       {
           digit = (num / divisor) % 10;
           divisor = divisor / 10;
           acConditionArray[i]=digit;
            i++;
       }

		switch (type)
		{
			case AircraftMaintenanceBean.MAINT_100HOUR:
				int engine = this.getTotalEngineTime();
				int airframe = this.getAirframe();
				int basePrice = (int) Math.round(0.05/1.25 * engines * enginePrice);
				for (i = 0; i < acConditionArray.length-1; i++)			
				{
                    //read array. Use engine hours in first and airframe hours in the rest.
					if(i==0)
						addedPriceArray[i] = (int) Math.round((engine/3600 * .01 * basePrice * engines * (((double)acConditionArray[0]+ ((double)(acConditionArray[1])/10)) /DIVISOR)));
					else
						addedPriceArray[i] = (int) Math.round((airframe/3600 * .01 * basePrice * (((double)acConditionArray[0]+ ((double)(acConditionArray[i+1])/10)) /DIVISOR)));
				}
				return addedPriceArray;

			case AircraftMaintenanceBean.MAINT_REPLACEENGINE:
				for (i = 0; i < addedPriceArray.length; i++)	
				{
					addedPriceArray[i]=0;
				}
				return addedPriceArray;
				
			case AircraftMaintenanceBean.MAINT_FIXAIRCRAFT:
				for (i = 0; i < addedPriceArray.length; i++)	
				{
					addedPriceArray[i]=0;
				}
				return addedPriceArray;
		}

		return addedPriceArray;	
	}
	
	public static int getRawEquipmentPrice(int mask)
	{
		int returnValue = 0;
		if ((mask & ModelBean.EQUIPMENT_IFR_MASK) > 0)
			returnValue += ModelBean.IFR_COST;
		if ((mask & ModelBean.EQUIPMENT_GPS_MASK) > 0)
			returnValue += ModelBean.GPS_COST;
		if ((mask & ModelBean.EQUIPMENT_AP_MASK) > 0)
			returnValue += ModelBean.AP_COST;
		return returnValue;
	}
	
	public int getEquipmentPrice(int mask)
	{
		return AircraftBean.getRawEquipmentPrice(mask);
	}
	
	public static int getEquipmentCost(int mask)
	{
		float DefaultMargin = (1 + (float)FboBean.FBO_DEFAULT_EQUIPMENTMARGIN / 100);
		return Math.round(AircraftBean.getRawEquipmentPrice(mask) / DefaultMargin);
	}
	
	public int getEquipmentPriceFBO(int mask, FboBean fbo)
	{
		int fboEquipmentCost = AircraftBean.getEquipmentCost(mask);
		return Math.round((fboEquipmentCost * (1 + (float)fbo.getEquipmentInstallMargin() / 100)));
	}
	
	public int performMaintenance(int type, int logId, FboBean fbo, ResultSet aircraft, ResultSet damage, ResultSet maintenance) throws SQLException
	{
		// Third call for price (while performing maintenance)
		int cost = getMaintenancePrice(type, fbo);
		switch (type)
		{
			case AircraftMaintenanceBean.MAINT_100HOUR:
				aircraft.updateInt("lastCheck", aircraft.getInt("engine1"));
				AircraftMaintenanceBean mb = new AircraftMaintenanceBean(this, type, fbo);
				cost += mb.doMaintenance(damage, maintenance, logId);				
				break;
			case AircraftMaintenanceBean.MAINT_REPLACEENGINE:				
				aircraft.updateInt("lastCheck", 0);
				aircraft.updateInt("engine1",0);
				AircraftMaintenanceBean mb2 = new AircraftMaintenanceBean(this, type, fbo);
				cost += mb2.doMaintenance(damage, maintenance, logId);
				break;
			case AircraftMaintenanceBean.MAINT_FIXAIRCRAFT:				
				AircraftMaintenanceBean mb3 = new AircraftMaintenanceBean(this, type, fbo);
				cost += mb3.doRepair(maintenance, logId);
				break;
		}
		return cost;
	}
	
	public boolean changeAllowed(UserBean who)
	{
        return who != null && (who.getId() == owner || who.groupMemberLevel(owner) >= UserBean.GROUP_STAFF);
    }
	
	public boolean canAlwaysRent(UserBean who)
	{
        return who != null && (who.getId() == owner || who.groupMemberLevel(owner) >= UserBean.GROUP_MEMBER);
    }
	
	public int maxPayloadWeight()
	{
		return (int) Math.round(maxWeight - emptyWeight - (getTotalFuel() * Constants.GALLONS_TO_KG) - 77);
	}
	
	public boolean fitsAboard(AssignmentBean assignment)
	{
		int weight;
		int crewSeats = 1;
		if (crew > 0)  // Additional Crew
			crewSeats = 2;
		
		if (assignment.getUnits() == AssignmentBean.UNIT_PASSENGERS)
		{
			if (assignment.getAmount() > seats-crewSeats)  // Pilot plus additional crew
				return false;
			
			weight = 77 * assignment.getAmount();
		} 
		else 
			weight = assignment.getAmount();
		
		return maxPayloadWeight() >= weight;
	}
	
	public double getHoursSinceLastCheck()
	{
		int total = (totalEngineTime - lastCheck) / 60;
		int minutes = total % 60;
		int hours = total / 60;
		return hours + ((double)minutes / 60);
	}
	
	/**
	 * return an string 00:00 formatted HoursSinceLastCheck value
	 * @return String
	 */
	public String getHoursSinceLastCheckString()
	{
		int minutes = (int)(getHoursSinceLastCheck() * 60.0);

        return Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
	}
	
	public double getEngineHours()
	{
		int total = (totalEngineTime) / 60;
		int minutes = total % 60;
		int hours = total / 60;
		return hours + ((double)minutes / 60);
	}
	
	public double getAirframeHours()
	{
		int total = (airframe) / 60;
		int minutes = total % 60;
		int hours = total / 60;
		return hours + ((double)minutes / 60);
	}
	
	/**
	 * return an string 00:00 formatted EngineHour value
	 * @return String
	 */
	public String getEngineHoursString()
	{
		int minutes = (int)(getEngineHours() * 60.0);

        return Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
	}

    // TBO and Condition added PRD
    public boolean getCanFlyAssignments(ModelBean model)
    {
        int tbo = model.fueltype == 0 ? TBO_RECIP : TBO_JET;

        return (owner == 0) || ((getHoursSinceLastCheck() < 100) && (totalEngineTime < tbo) && !isBroken());
    }

    // TBO and Condition added PRD
    public boolean getCanFlyAssignments(int fueltype)
    {
        int tbo = fueltype == 0 ? TBO_RECIP : TBO_JET;

        return (owner == 0) || ((getHoursSinceLastCheck() < 100) && (totalEngineTime < tbo) && !isBroken());
    }

	/**
	 * Returns the brand.
	 * @return String
	 */
	public String getMake()
	{
		return make;
	}

	/**
	 * Returns the home.
	 * @return String
	 */
	public String getHome()
	{
		return home;
	}

	/**
	 * Returns the location.
	 * @return String
	 */
	public String getLocation()
	{
		return location;
	}

	/**
	 * Returns the model.
	 * @return String
	 */
	public String getModel()
	{
		return model;
	}

	/**
	 * Returns the registration.
	 * @return String
	 */
	public String getRegistration()
	{
		return registration;
	}


	/**
	 * Sets the brand.
	 * @param make The brand to set
	 */
	public void setMake(String make)
	{
		this.make = make;
	}

	/**
	 * Sets the home.
	 * @param home The home to set
	 */
	public void setHome(String home)
	{
		this.home = home.toUpperCase();
	}

	/**
	 * Sets the location.
	 * @param location The location to set
	 */
	public void setLocation(String location)
	{
		this.location = location;
	}

	/**
	 * Sets the model.
	 * @param model The model to set
	 */
	public void setModel(String model)
	{
		this.model = model;
	}

	/**
	 * Sets the registration.
	 * @param registration The registration to set
	 */
	public void setRegistration(String registration)
	{
		this.registration = TruncateRegistration(Converters.clearHtml(registration));
	}

	/**
	 * Returns the userLock.
	 * @return int
	 */
	public int getUserLock()
	{
		return userLock;
	}

	/**
	 * Sets the userLock.
	 * @param userLock The userLock to set
	 */
	public void setUserLock(int userLock)
	{
		this.userLock = userLock;
	}

	/**
	 * Returns the modelId.
	 * @return int
	 */
	public int getModelId()
	{
		return modelId;
	}

	/**
	 * Sets the modelId.
	 * @param modelId The modelId to set
	 */
	public void setModelId(int modelId)
	{
		this.modelId = modelId;
	}

	/**
	 * Returns the maxRentTime.
	 * @return int
	 */
	public int getMaxRentTime()
	{
		return maxRentTime;
	}

	/**
	 * Sets the maxRentTime.
	 * @param maxRentTime The maxRentTime to set
	 */
	public void setMaxRentTime(int maxRentTime)
	{
		this.maxRentTime = maxRentTime;
	}

	/**
	 * Returns the lockedSince.
	 * @return Timestamp
	 */
	public Timestamp getLockedSince()
	{
		return lockedSince;
	}

	/**
	 * Sets the lockedSince.
	 * @param lockedSince The lockedSince to set
	 */
	public void setLockedSince(Timestamp lockedSince)
	{
		this.lockedSince = lockedSince;
	}

	/**
	 * Returns the accounting.
	 * @return int
	 */
	public int getAccounting()
	{
		return accounting;
	}
	
	public String getSAccounting()
	{
		return accounting == ACC_TACHO ? "tacho" : "hour";
	}

	/**
	 * Sets the accounting.
     */
	public void setAccounting()
	{
		this.accounting = ACC_HOUR;
	}
	public void setAccounting(int accounting)
	{
		this.accounting = accounting;
	}	

	/**
	 * Returns the departedFrom.
	 * @return String
	 */
	public String getDepartedFrom()
	{
		return departedFrom;
	}

	/**
	 * Sets the departedFrom.
	 * @param departedFrom The departedFrom to set
	 */
	public void setDepartedFrom(String departedFrom)
	{
		this.departedFrom = departedFrom;
	}

	/**
	 * Returns the bonus.
	 * @return int
	 */
	public int getBonus()
	{
		return bonus;
	}

	/**
	 * Sets the bonus.
	 * @param bonus The bonus to set
	 */
	public void setBonus(int bonus)
	{
		this.bonus = bonus;
	}

	/**
	 * Returns the owner.
	 * @return int
	 */
	public int getOwner()
	{
		return owner;
	}

	/**
	 * Sets the owner.
	 * @param owner The owner to set
	 */
	public void setOwner(int owner)
	{
		this.owner = owner;
	}

	/**
	 * Returns the lessor.
	 * @return int
	 */
	public int getLessor() //added Airboss 5/6/11
	{
		return lessor;
	}

	/**
	 * Sets the lessor.
	 * @param lessor The lessor to set
	 */
	public void setLessor(int lessor) //added Airboss 5/6/11
	{
		this.lessor = lessor;
	}

	/**
	 * Returns the fuel.
	 * @return float[]
	 */
	public float[] getFuel()
	{
		return fuel;
	}
	
	public int[] getCapacity()
	{
		return capacity;
	}
	
	public float[] getFuelInGallons()
	{
		float[] result = new float[fuel.length];
		for (int c=0; c < fuel.length; c++)
			result[c] = fuel[c] * capacity[c];
		return result;
	}
	
	/**
	 * returns fueltype 0=avgas 1=jeta
	 * @return int fueltype
	 */
	public int getFuelType()
	{
		return fueltype;
	}

	/**
	 * Returns the totalCapacity in gallons.
	 * @return int
	 */
	public int getTotalCapacity()
	{
		int totalCapacity = 0;
		for (int count = 0; count< fuel.length; count++)
			totalCapacity+=capacity[count];	

		return totalCapacity;
	}

	/**
	 * Returns the totalFuel in gallons.
	 * @return int
	 */
	public double getTotalFuel()
	{
		double totalFuel = 0;
		for (int count = 0; count< fuel.length; count++)
			totalFuel+=capacity[count]*fuel[count];
		return totalFuel;
	}

	/**
	 * Returns the seats.
	 * @return int
	 */
	public int getSeats()
	{
		return seats;
	}

	/**
	 * Sets the seats.
	 * @param seats The seats to set
	 */
	public void setSeats(int seats)
	{
		this.seats = seats;
	}

	public boolean canRent()
	{
        return rentalPriceDry > 0 || rentalPriceWet > 0;

    }
	/**
	 * Returns the rentalPriceDry.
	 * @return int
	 */
	public int getRentalPriceDry()
	{
		return rentalPriceDry;
	}

	/**
	 * Returns the rentalPriceWet.
	 * @return int
	 */
	public int getRentalPriceWet()
	{
		return rentalPriceWet;
	}

	/**
	 * Sets the rentalPriceDry.
	 * @param rentalPriceDry The rentalPriceDry to set
	 */
	public void setRentalPriceDry(int rentalPriceDry)
	{
		this.rentalPriceDry = rentalPriceDry;
	}

	/**
	 * Sets the rentalPriceWet.
	 * @param rentalPriceWet The rentalPriceWet to set
	 */
	public void setRentalPriceWet(int rentalPriceWet)
	{
		this.rentalPriceWet = rentalPriceWet;
	}


	/**
	 * Returns the initialFuel.
	 * @return int
	 */
	public double getInitialFuel()
	{
		return initialFuel;
	}

	/**
	 * Sets the initialFuel.
	 * @param initialFuel The initialFuel to set
	 */
	public void setInitialFuel(double initialFuel)
	{
		this.initialFuel = initialFuel;
	}
	
	/*
	 * Last flight was rented wet
	 */
	public boolean wasWetRent()
	{
		return initialFuel == 0;
	}

	/**
	 * Returns the sellPrice.
	 * @return int
	 */
	public int getSellPrice()
	{
		return sellPrice;
	}

	/**
	 * Sets the sellPrice.
	 * @param sellPrice The sellPrice to set
	 */
	public void setSellPrice(int sellPrice)
	{
		this.sellPrice = sellPrice;
	}
	
	public String getSLocation()
	{
		return location == null ? "Airborne" : location;
	}
	
	public boolean isDeparted()
	{
		return location == null;
	}
	
	public String getMakeModel()
	{
		return make + " " + model;
	}

	/**
	 * Sets the fuel.
	 * @param fuel The fuel to set
	 */
	public void setFuel(float[] fuel)
	{
		this.fuel = fuel;
	}
	
	/**
	 * sets fuel type 0 = avgas 1 = jeta
	 * @param FuelType int
	 */
	public void setFuelType(int FuelType)
	{
		fueltype = FuelType;
	}

	//
	//Added for aircraft shipping - Airboss 12/20/10
	//
	
	public void setShippingConfigAircraft(int delay, double perKg, int perCrate, int disposal)
	{
		setShippingStateDelay(delay); 
		setShippingCostPerKg(perKg);
		setShippingCostPerCrate(perCrate);
		setShippingCostDisposal(disposal);	
	}

	/**
	 * Sets the canShip.
	 * @param canship boolean indicating if the aircraft can be shipped
	 */
	public void setCanShip(boolean canship)
	{
		this.canShip = canship;
	}
	
	public boolean getCanShip()
	{
		return this.canShip;
	}
	
	/**
	 * Sets the aircraft shipping state change delay in seconds.
	 * @param statedelay int
	 */
	public void setShippingStateDelay(int statedelay)
	{
		this.shippingStateDelay = statedelay;
	}

	public int getShippingStateDelay()
	{
		return this.shippingStateDelay;
	}
	
	/**
	 * Sets the aircraft shipping cost per kg
	 * @param statecostperkg double
	 */
	public void setShippingCostPerKg(double statecostperkg)
	{
		this.shippingCostPerKg = statecostperkg;
	}

	public double getShippingCostPerKg()
	{
		return this.shippingCostPerKg;
	}
	
	/**
	 * Sets the aircraft shipping cost per crate
	 * @param statecostpercrate int
	 */
	public void setShippingCostPerCrate(int statecostpercrate)
	{
		this.shippingCostPerCrate = statecostpercrate;
	}

	public int getShippingCostPerCrate()
	{
		return this.shippingCostPerCrate;
	}
	
	/**
	 * Sets the aircraft shipping cost for disposal
	 * @param statecostdisposal int
	 */
	public void setShippingCostDisposal(int statecostdisposal)
	{
		this.shippingCostDisposal = statecostdisposal;
	}

	public int getShippingCostDisposal()
	{
		return this.shippingCostDisposal;
	}
	
	/**
	 * Sets the aircraft shipping state.
	 * @param shippingstate (0 - active, 1 - disassembly, 2 - Crated, 3 - reassembly)
	 */
	public void setShippingState(int shippingstate)
	{
		this.shippingState = shippingstate;
	}

	public int getShippingState()
	{
		return this.shippingState;
	}
	
	public String getShippingStateString()
	{
		Timestamp diffns;
		double hours;
		String state;
		switch(this.shippingState)
		{
		case 0: state = "Active";
			break;
		case 1: 
			diffns = diffTimestamps(shippingStateNext, new Timestamp( new Date().getTime()));
			hours = diffns.getTime() / 1000.0 / 3600.0;
			state = "Disassembly [" + Formatters.oneDecimal.format(hours) + "] Hour(s) Left";
			break;
		case 2: state = "Crated";
			break;
		case 3: 
			diffns = diffTimestamps(shippingStateNext, new Timestamp( new Date().getTime()));
			hours = diffns.getTime() / 1000.0 / 3600.0;
			state = "Resassembly [" + Formatters.oneDecimal.format(hours) + "] Hour(s) Left";
			break;
		default: state = "Error, unknown shipping code!";
			break;
		}
		return state;
	}
		
	/**
	 * Sets the aircraft shipping state next event time.
	 * @param date Timestamp
	 */
	public void setShippingStateNext(Timestamp date)
	{
		this.shippingStateNext = date;
	}

	public Timestamp getShippingStateNext()
	{
		return this.shippingStateNext;
	}
	
	/**
	 * Sets the aircraft shipped by id.
	 * @param id - user id
	 */
	public void setShippedBy(int id)
	{
		this.shippedBy = id;
	}
	
	public int getShippedBy()
	{
		return this.shippedBy;
	}
	
	/**
	 * Sets the aircraft shipping to location
	 * @param icao String
	 */
	public void setShippingTo(String icao)
	{
		this.shippingTo = icao;
	}
	
	public String getShippingTo()
	{
		return this.shippingTo;
	}
	
	/**
	 * Returns the disassembly and reassembly shipping costs.
	 * @return double[] - 0 = disassembly, 1 = reassembly
	 */
	public double[] getShippingCosts(int numcrates)
	{
		double[] costs = new double[2];
		costs[0] = (getShippingCostPerKg() * getEmptyWeight()) + (getShippingCostPerCrate()*numcrates);
		costs[1] = (getShippingCostPerKg() * getEmptyWeight()) + getShippingCostDisposal();
		
		return costs;
	}
	
	/**
	 * Returns the total disassembly and reassembly shipping costs.
	 * @return string
	 */
	public String getShippingCostTotalString(int numcrates)
	{
		double[] costs = getShippingCosts(numcrates);
        net.fseconomy.data.Money total = new net.fseconomy.data.Money(costs[0] + costs[1]);

		return total.getAsString();
	}
	
	/**
	 * Returns the disassembly and reassembly shipping delay time in hours.
	 * @return double
	 */
	public String getShippingStateHours()
	{
		return Formatters.oneDecimal.format((double)getShippingStateDelay()/3600.0);
	}
	
	//
	//
	//
	/**
	 * Returns the equipment.
	 * @return int
	 */
	public int getEquipment()
	{
		return equipment;
	}

	/**
	 * Sets the equipment.
	 * @param equipment The equipment to set
	 */
	public void setEquipment(int equipment)
	{
		this.equipment = equipment;
	}
	
	public String getSEquipment()
	{
		String returnValue;
		
		returnValue = ((equipment & ModelBean.EQUIPMENT_IFR_MASK) == 0)?"VFR":"IFR";
		if ((equipment & ModelBean.EQUIPMENT_AP_MASK) > 0)
			returnValue += "/AP";

		if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) > 0)
			returnValue += "/GPS";
		return returnValue;
	}

	/**
	 * Returns the modelPrice.
	 * @return int
	 */
	public int getModelPrice()
	{
		return modelPrice;
	}

	/**
	 * Sets the modelPrice.
	 * @param modelPrice The modelPrice to set
	 */
	public void setModelPrice(int modelPrice)
	{
		this.modelPrice = modelPrice;
	}
	
	static final double MAXDEPRECIATIONTIMEHRS = 10000.0;
	static final double MAXDEPRECIATIONPERCENT = .25;
	static final double BASESELLBACKPERCENT = .60;
	static final double SALVAGEVALUEPERCENT = .25;

	//Added so that system sold aircraft will have airframe price reduced as it gains flight hours
	public int getSystemSellPrice() 
	{
		return getMinimumPrice(1.0, true);
	}
	
	public int getMinimumPrice()
	{
		return getMinimumPrice(BASESELLBACKPERCENT, false);
	}
	
	public int getMinimumPrice(double basesellbackprecent, boolean retail)
	{
		//8-2-12 Airboss - reworked to now include airframe time in sellback price reduction.
		
		//Get Airframe time in hours
		double airframeTimeHrs = airframe / 3600.0;
		
		//Calculate the hours for airframe depreciation
		double depreciatedAirframeAmount = MAXDEPRECIATIONPERCENT * (airframeTimeHrs/MAXDEPRECIATIONTIMEHRS);
			
		//Limit the max airframe depreciation, other wise it would continue to increase with airframe hours
		depreciatedAirframeAmount = depreciatedAirframeAmount > MAXDEPRECIATIONPERCENT ? MAXDEPRECIATIONPERCENT : depreciatedAirframeAmount; 

		//Subtract airframe depreciation for the base sell back price
		double depreciatedAmount = basesellbackprecent - depreciatedAirframeAmount;
		
		//Calculate the new sell back price
		int returnValue = (int)(getModelPrice() * depreciatedAmount);
		
		//Add in installed equipment
		if(retail)
			returnValue += AircraftBean.getRawEquipmentPrice(equipment); // includes default FBO margin 
		else
			returnValue += AircraftBean.getEquipmentCost(equipment); // Wholesale cost - no FBO margin
		
		//reduce sell back price by the time on the engines, the closer to TBO the bigger the reduction
		if(fueltype==0) // 100LL
		{	
			returnValue -= (int)((double)totalEngineTime/TBO_RECIP*enginePrice*engines);
		}
		else // JetA
		{
			returnValue -= (int)((double)totalEngineTime/TBO_JET*enginePrice*engines);
		}
		
		//sell back price cannot fall below salvage value.
		int salvagevaluemin = (int)(getModelPrice() * SALVAGEVALUEPERCENT);
		returnValue = returnValue >= salvagevaluemin ? returnValue : salvagevaluemin;
		
		return returnValue;
	}
	
	public boolean isBroken()
	{
		boolean broken = false;
		if (condition > AircraftBean.REPAIR_RANGE_LOW-1 && condition < AircraftBean.REPAIR_RANGE_HIGH+1 && owner > 0 && airframe >= lastFix + 3600)
			broken = true;
		return broken;
	}

	public int getBearing() {
		return bearing;
	}

	public void setBearing(int i) {
		bearing = i;
	}

	public int getBearingImage()
	{
		if (bearing == -1)
			return -1;

		return (int)Math.round(getBearing()/45.0)%8;
	}

	public int getMaxWeight() {
		return maxWeight;
	}

	public void setMaxWeight(int i) {
		maxWeight = i;
	}

	public int getEmptyWeight() {
		return emptyWeight;
	}

	public void setEmptyWeight(int i) {
		emptyWeight = i;
	}

	public int getLastCheck()
	{
		return lastCheck;
	}

	public void setLastCheck(int i)
	{
		lastCheck = i;
	}

	public int getTotalEngineTime()
	{
		return totalEngineTime;
	}

	public void setTotalEngineTime(int i)
	{
		totalEngineTime = i;
	}

	public void setAirframe(int i)
	{
		airframe = i;
	}

	public int getAirframe()
	{
		return airframe;
	}
	
	/**
	 * return an string 00:00 formatted AirframeHours value
	 * @return String
	 */
	public String getAirframeHoursString()
	{
		int minutes = (int)(getAirframe() / 60.0);

        return Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
	}
	
	public void setCondition(int i) 
	{
		condition = i;
	}

	public int getCondition()
	{		
       return condition;		
	}
	
	public void setLastFix(int i) 
	{
		lastFix = i;
	}

	public int getLastFix()
	{		
       return lastFix;		
	}
	
	public int getEnginePrice()
	{
		return enginePrice;
	}

	public int getEngines()
	{
		return engines;
	}

	public void setEnginePrice(int i)
	{
		enginePrice = i;
	}

	public void setEngines(int i)
	{
		engines = i;
	}

	public int getGph()
	{
		return gph;
	}

	public void setGph(int i)
	{
		gph = i;
	}

	public int getCruise()
	{
		return cruise;
	}

	public void setCruise(int i)
	{
		cruise = i;
	}
	
	public int getCrew()
	{
		return crew;
	}
	
	public void setCrew(int i)
	{
		crew = i;
	}

	public boolean isAdvertiseFerry()
	{
		return advertiseFerry;
	}

	public void setAdvertiseFerry(boolean b)
	{
		advertiseFerry = b;
	}

	public int getAdvertise()
	{
        return advertiseFerry ? AircraftBean.ADV_FERRY : 0;
	}

	public void setAdvertise(int i)
	{
		if ((i & AircraftBean.ADV_FERRY) > 0)
			advertiseFerry = true;
	}

	public boolean isAllowRepair()
	{
		return allowRepair;
	}
	
	public void setAllowRepair(boolean b)
	{
		allowRepair = b;
	}	
	
	public int getAllowFix()
	{
		if (allowRepair)
			return AircraftBean.ALLOW_REPAIR;
		else
		    return 0;
	}

	public void setAllowFix(int i)
	{
		if ((i & AircraftBean.ALLOW_REPAIR) > 0)
			allowRepair = true;
	}
	
	public int getDistance()
	{
		return distance;
	}

	public void setDistance(int i)
	{
		distance = i;
	}
	
	/**
	 * return a +/- timestamp value showing difference between two dates
	 * @param t1 Date
	 * @param t2 Date
	 * @return String
	 */
	public static Timestamp diffTimestamps(Date t1, Date t2)
	{     
		Boolean signNeg = false;
		
		// Make sure the result is always > 0     
		if (t1.compareTo (t2) < 0)     
		{ 
			signNeg = true;
			Date tmp = t1;
			t1 = t2;         
			t2 = tmp;     
		}      
		// Timestamps mix milli and nanoseconds in the API, so we have to separate the two     
		long diffSeconds = (t1.getTime () / 1000) - (t2.getTime () / 1000);     
		
		// For normals dates, we have millisecond precision     
		int nano1 = ((int) t1.getTime () % 1000) * 1000000;     
		
		// If the parameter is a Timestamp, we have additional precision in nanoseconds     
		if (t1 instanceof Timestamp)         
			nano1 = ((Timestamp)t1).getNanos ();     
		
		int nano2 = ((int) t2.getTime () % 1000) * 1000000;     
		
		if (t2 instanceof Timestamp)         
			nano2 = ((Timestamp)t2).getNanos ();      
		
		int diffNanos = nano1 - nano2;     
		if (diffNanos < 0)     
		{         
			// Borrow one second         
			diffSeconds --;         
			diffNanos += 1000000000;     
		}      
		// mix nanos and millis again     
		Timestamp result;
		if(signNeg)
			result = new Timestamp (-((diffSeconds * 1000) + (diffNanos / 1000000)));
		else	
			result = new Timestamp ((diffSeconds * 1000) + (diffNanos / 1000000));     
		
		// setNanos() with a value of in the millisecond range doesn't affect the value of the time field     
		// while milliseconds in the time field will modify nanos! Damn, this API is a *mess*     
		result.setNanos (diffNanos);     
	
		return result; 
	} 
	
	public float getFueltotal()
	{
		return fueltotal;
	}
	
	public void setFuelTotal(float i)
	{
		fueltotal = i;
	}
	
	public boolean getHoldRental()
	{
		return holdRental;
	}
	
	public void setHoldRental(boolean b)
	{
		holdRental = b;
	}

    public boolean isForSale()
    {
        return sellPrice > 0;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }
}
