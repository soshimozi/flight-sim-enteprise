package net.fseconomy.beans;

import net.fseconomy.dto.LatLon;
import net.fseconomy.dto.LatLonRadians;
import net.fseconomy.util.Helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

//for cached airport use, replacement of AirportInfo DTO
public class CachedAirportBean
{
    public static final int TYPE_CIVIL = 1;
    public static final int TYPE_MILITARY = 2;
    public static final int TYPE_WATER = 3;

    public static final int SERVICES_100LL = 1;
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

    int bucket;
    String icao;
    LatLon latlon;
    LatLonRadians latLonRadians;
    String name, title; //name, city, state, country;
    int elev, type, size, longestRwy, surfaceType;
    double price100ll, priceJetA;
    boolean fuel100ll, fuelJetA;
    String typeDescription;

    public CachedAirportBean(ResultSet rs) throws SQLException
    {
        fill(rs);
    }

    public void fill(ResultSet rs) throws SQLException
    {
        setBucket(rs.getInt("bucket"));
        setIcao(rs.getString("icao"));
        setTitle(rs.getString("name"), rs.getString("city"), rs.getString("state"), rs.getString("country"));
        setLatLon(rs.getDouble("lat"), rs.getDouble("lon"));
        setLatLonRadians(rs.getDouble("lat"), rs.getDouble("lon"));
        setElev(rs.getInt("elev"));
        setType(rs.getString("type"));
        setSize(rs.getInt("size"));
        setLongestRunway(rs.getInt("longestrwy"));
        setSurfaceType(rs.getInt("surfacetype"));
        setFuelAvail(rs.getInt("services"));
        typeDescription = getAirportTypeDescription(type, size);
    }

    public void setBucket(int i)
    {
        bucket = i;
    }

    public void setIcao(String icao)
    {
        this.icao = icao;
    }

    public void setTitle(String pname, String city, String state, String country)
    {
        if(!Helpers.isNullOrBlank(state))
            state = ", " + state;
        else
            state = "";

        name = pname;
        title = pname + ", " + city + state + ", " + country;
    }

    public void setLatLon(double lat, double lon)
    {
        latlon = new LatLon(lat, lon);
    }

    public void setLatLonRadians(double lat, double lon)
    {
        latLonRadians = new LatLonRadians(lat, lon);
    }

    public void setElev(int elev)
    {
        this.elev = elev;
    }

    public void setType(String apType)
    {
        type = getTypeFromString(apType);
    }

    public void setSize(int pSize)
    {
        size = pSize;
    }

    public void setLongestRunway(int pLongestRwy)
    {
        longestRwy = pLongestRwy;
    }

    public void setSurfaceType(int pSurfType)
    {
        surfaceType = pSurfType;
    }

    public void setFuelAvail(int s)
    {
        fuel100ll = (s & SERVICES_100LL) > 0;
        fuelJetA = (s & SERVICES_JETA) > 0;
    }
    public void setFuelPrice(double pPrice100ll, double pPriceJetA)
    {
        price100ll = pPrice100ll;
        priceJetA = pPriceJetA;
    }

    public int getBucket()
    {
        return bucket;
    }

    public String getIcao()
    {
        return icao;
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }

    public LatLonRadians getLatLonRadians()
    {
        return latLonRadians;
    }

    public LatLon getLatLon()
    {
        return latlon;
    }

    public int getElev()
    {
        return elev;
    }

    public int getType()
    {
        return type;
    }

    public int getSize()
    {
        return size;
    }

    public int getLongestRunway()
    {
        return longestRwy;
    }

    public int getSurfaceType()
    {
        return surfaceType;
    }

    public boolean has100ll()
    {
        return fuel100ll;
    }

    public boolean hasJetA()
    {
        return fuelJetA;
    }

    public double getPrice100ll()
    {
        return price100ll;
    }

    public double getPriceJetA()
    {
        return priceJetA;
    }

    public String getTypeDescription()
    {
        return typeDescription;
    }

    int getTypeFromString(String sType)
    {
        if(sType == null)
            sType = "";

        switch (sType)
        {
            case "civil":
                return TYPE_CIVIL;
            case "military":
                return TYPE_MILITARY;
            case "water":
                return TYPE_WATER;
            default:
                return TYPE_CIVIL;
        }
    }

    static String getAirportTypeDescription(int type, int size)
    {
        if (type == CachedAirportBean.TYPE_WATER)
        {
            return "Seaplane base";
        }

        String prefix = "";
        String postfix;
        String military = type == TYPE_MILITARY ? "Military " : "";

        if(size >= MIN_SIZE_BIG)
        {
            prefix = "Large ";
            postfix = " airport (3-Lot)";
        }
        else if(size >= MIN_SIZE_MED)
        {
            prefix = "Small ";
            postfix = " airport (2-Lot)";
        }
        else
        {
            postfix = "Airstrip (1-Lot)";
        }

        return prefix + military + postfix;
    }

    public String getSurfaceTypeName()
    {
        switch (surfaceType)
        {
            case SURFACETYPE_ASPHALT: return "Asphalt";
            case SURFACETYPE_CONCRETE: return "Concrete";
            case SURFACETYPE_CORAL: return "Coral";
            case SURFACETYPE_DIRT: return "Dirt";
            case SURFACETYPE_GRASS: return "Grass";
            case SURFACETYPE_GRAVEL: return "Gravel";
            case SURFACETYPE_HELIPAD: return "Helipad";
            case SURFACETYPE_OILTREATED: return "Oil Treated";
            case SURFACETYPE_SNOW: return "Snow";
            case SURFACETYPE_STEELMATS: return "Steel Mats";
            case SURFACETYPE_WATER: return "Water";
            default: return "Unknown";
        }
    }
}
