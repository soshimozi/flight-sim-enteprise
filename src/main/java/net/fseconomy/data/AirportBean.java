package net.fseconomy.data;

import java.io.Serializable;
import java.sql.*;

public class AirportBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int TYPE_CIVIL = 1;
	public static final int TYPE_MILITARY = 2;
	public static final int TYPE_WATER = 3;
	
	public static final int SERVICES_AVGAS = 1;
	public static final int SERVICES_JETA = 2;
	
	public static final int MIN_SIZE_MED = 1000;
	public static final int MIN_SIZE_BIG = 3500;
	
	public static final int SURFACETYPE_ASPHALT = 1;
	public static final int SURFACETYPE_CONCRETE = 2;
	public static final int SURFACETYPE_CORAL = 3;
	public static final int SURFACETYPE_DIRT = 4;
	public static final int SURFACETYPE_GRASS = 5;
	public static final int SURFACETYPE_GRAVEL = 6;
	public static final int SURFACETYPE_HELIPAD = 7;
	public static final int SURFACETYPE_OILTREATED = 8;
	public static final int SURFACETYPE_SNOW = 9;
	public static final int SURFACETYPE_STEELMATS = 10;
	public static final int SURFACETYPE_WATER = 11;
	
	public boolean available;
	String icao, country, city, state, name;
	int elev, type, size;
	int longestRwy, surfaceType;
	double fuelPrice,landingFee,JetAPrice,JetAMult;
	double lat, lon;
	public Data.closeAirport[] closestAirports;
	int bucket;
	boolean avgas;
	boolean jeta;
	
	/**
	 * Constructor for Airport.
	 */
	public AirportBean()
	{
		super();
	}
	
	public AirportBean(ResultSet rs) throws SQLException
	{
		fill(rs);
	}
	
	public void fill(ResultSet rs) throws SQLException
	{
		setIcao(rs.getString("icao"));
		setName(rs.getString("name"));
		setCountry(rs.getString("country"));
		setCity(rs.getString("city"));
		setLat(rs.getDouble("lat"));
		setLon(rs.getDouble("lon"));
		setElev(rs.getInt("elev"));
		setState(rs.getString("state"));
		setAvailable(true);
		setType(rs.getString("type"));
		setSize(rs.getInt("size"));
		setLongestRunway(rs.getInt("longestrwy"));
		setSurfaceType(rs.getInt("surfacetype"));
		setBucket(rs.getInt("bucket"));
		setServices(rs.getInt("services"));
	}

	public boolean hasServices(Data data)
	{
        return size >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE
                || data.hasRepairShop(icao);
    }
	
	public boolean hasGoodsForSale(Data data)
	{
		if(size > MIN_SIZE_BIG)
			return true;
		
		for (int c = 0; c < data.commodities.length; c++)
        {
            if (data.commodities[c] != null && size > data.commodities[c].getMinAirportSize())
                return true;
        }

		return data.hasSuppliesForSale(icao);
	}
	
	public String getIcao()
	{
		return icao;
	}

	public void setIcao(String icao)
	{
		if (icao != null && icao.length() > 4)
			icao = null;

		this.icao = icao;	
	}
	
	public boolean isAvailable()
	{
		return available;
	}

	public String getCountry()
	{
		return country;
	}

	public String getCity()
	{
		if (state != null && !state.equals(""))
			return city + ", " + state;

		return city;
	}

	public String getName()
	{
		return name;
	}

	public double getFuelPrice()
	{
		return fuelPrice;
	}

	public void setFuelPrice(double FuelPrice)
	{
		this.fuelPrice = FuelPrice;
	}

	public void setJetaPrice(double mult)
	{
		this.JetAPrice = fuelPrice * mult;
		this.JetAMult = mult;
	}

	public double getJetAPrice()
	{
		return JetAPrice;
	}

	public double getJetAMult()
	{
		return JetAMult;
	}

	public void setCity(String city)
	{
		this.city = city;
	}

	public void setCountry(String country)
	{
		this.country = country;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setAvailable(boolean available)
	{
		this.available = available;
	}

	public double getLandingFee()
	{
		return landingFee;
	}

	public void setLandingFee(double landingFee)
	{
		this.landingFee = landingFee;
	}

	public double getLat()
	{
		return lat;
	}

	public double getLon()
	{
		return lon;
	}

	public int getElev()
	{
		return elev;
	}

	public void setLat(double lat)
	{
		this.lat = lat;
	}

	public void setLon(double lon)
	{
		this.lon = lon;
	}

	public void setElev(int elev)
	{
		this.elev = elev;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}
	
	public String getTitle()
	{
		return getName() + ", " + getCity() + ", " + getCountry();
	}


	public int getType()
	{
		return type;
	}
	
	public String getTypeDescription()
	{
		if (type == TYPE_WATER)
			return "Seaplane base";

		String add = type == TYPE_MILITARY ? "military " : "";
		
		if (size < MIN_SIZE_MED)
			return type == TYPE_MILITARY ? "Military airstrip" : "Airstrip";
		else if (size < MIN_SIZE_BIG)
			return "Small " + add + "airport";

        return "Large " + add + "airport";
	}
	
	public String getDescriptiveImage(FboBean[] fbos)
	{
		String base;
		String ext="";
		boolean hasFbo = fbos.length > 0;
		boolean hasFuel = isAvgas();

		switch (type)
		{
			case TYPE_WATER:
				base = "seaplane";
				break;
			case TYPE_MILITARY:
				base = "military";
				break;
			default:
				if (size < MIN_SIZE_MED)
					base = "airstrip";
				else if (size < MIN_SIZE_BIG)
					base = "small-airport";

				base = "large-airport";
		}

		if (hasFbo)
			ext = "-fbo";
		else if (hasFuel)
			ext = "-fuel";
		
		return "img/" + base + ext + ".gif";
	}

	public int getFboSlots()
	{
		if (size < MIN_SIZE_MED)
			return 1;
		else if (size < MIN_SIZE_BIG)
			return 2;

    	return 3;
	}

	public void setType(int i)
	{
		type = i;
	}

	public void setType(String apType)
	{
        if(apType == null)
            apType = "";

        switch (apType)
        {
            case "civil":
                type = TYPE_CIVIL;
                break;
            case "military":
                type = TYPE_MILITARY;
                break;
            default:
                type = TYPE_WATER;
                break;
        }
	}

	public int getSize()
	{
		return size;
	}

	public void setSize(int i)
	{
		size = i;
	}
	
	public int getLongestRunway()
	{
		return longestRwy;
	}

	public void setLongestRunway(int i)
	{
		longestRwy = i;
	}

	public int getSurfaceType()
	{
		return surfaceType;
	}

	public void setSurfaceType(int i)
	{
		surfaceType = i;
	}
	
	public static int bucket(double lat, double lon)
	{
		int bucketSize = 2;
		
		int x, y;
		if (lat >= 85 )
			y = (int) Math.ceil(180/bucketSize);
		else if (lat <= -85)
			y = 0;
		else
			y = (int)((90+lat)/bucketSize);
		
		x = (int) ((180+lon)/(bucketSize/Math.cos(Math.toRadians(lat))));
		
		return x * (int)Math.ceil(360/bucketSize) + y;
	}
	
	public static String bucketList(double lat, double lon)
	{
		StringBuilder returnValue = new StringBuilder("(");
		int bucket;
		
		bucket = bucket(lat - 2, lon);
		returnValue.append(bucket - 1);
		returnValue.append(",");
		returnValue.append(bucket);
		returnValue.append(",");
		returnValue.append(bucket + 1);
		
		bucket = bucket(lat, lon);
		returnValue.append(bucket - 1);
		returnValue.append(",");
		returnValue.append(bucket);
		returnValue.append(",");
		returnValue.append(bucket + 1);
		
		bucket = bucket(lat + 2, lon);
		returnValue.append(bucket - 1);
		returnValue.append(",");
		returnValue.append(bucket);
		returnValue.append(",");
		returnValue.append(bucket + 1);
		
		returnValue.append(")");

		return returnValue.toString();
	}

	public int getBucket()
	{
		return bucket;
	}

	public void setBucket(int i)
	{
		bucket = i;
	}
	
	public void setServices(int s)
	{
		avgas = (s & SERVICES_AVGAS) > 0;
		jeta = (s & SERVICES_AVGAS) > 0;
	}

	public boolean isAvgas()
	{
		return avgas;
	}

	public boolean isJetA()
	{
		return jeta;
	}
}
