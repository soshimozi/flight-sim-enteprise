package net.fseconomy.data;

import net.fseconomy.beans.*;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.DepartFlight;
import net.fseconomy.dto.DistanceBearing;
import net.fseconomy.util.Constants;
import net.fseconomy.util.GlobalLogger;

import java.sql.*;
import java.util.*;

import static net.fseconomy.data.Airports.getDistance;
import static net.fseconomy.data.Airports.getDistanceBearing;

public class Flights
{
    public static DepartFlight departAircraft(AircraftBean bean, int user, String location) throws DataError
    {
        ArrayList<AssignmentBean> result = new ArrayList<>();
        DepartFlight depart = new DepartFlight();

        boolean rentedDry = false;
        boolean allInFlight = false;

        try
        {
            ModelBean model = Models.getModelById(bean.getModelId());

            int seats = bean.getAvailableSeats();
            int crewWeight = bean.getCrewWeight();

            // initialize our current payload weight
            int totalWeight = crewWeight;
            double weightLeft = bean.maxPayloadWeightWithFuel() - crewWeight;

            //set rental timer start
            Timestamp now = new Timestamp(GregorianCalendar.getInstance().getTime().getTime());
            bean.setLockedSince(now);

            //Check that we have an aircraft at the departing airport
            String qry = "SELECT * FROM aircraft WHERE id = ? AND location = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, bean.getId(), location);
            if (!rs.next())
                throw new DataError("No active aircraft found");

            if(Assignments.hasAllInJobInQueue(user))
                allInFlight = true;

            qry = "SELECT (initialFuel is not null) AS rentedDry FROM aircraft WHERE id = ?";
            rentedDry = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), bean.getId());

            if (bean.getCanFlyAssignments(model))
            {
                int onBoard = 0;

                qry = "SELECT * FROM assignments WHERE (active = 1 OR (location = ? AND active <> 2)) AND userlock = ? ORDER BY active DESC";
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, location, user);
                while (rs.next())
                {
                    int passengers;
                    int weight;

                    if (rs.getString("units").equals("passengers"))
                    {
                        passengers = rs.getInt("amount");
                        weight = passengers * Constants.PASSENGER_WT_KG;
                    }
                    else
                    {
                        passengers = 0;
                        weight = rs.getInt("amount");
                    }

                    if (passengers <= seats && weight <= weightLeft)
                    {
                        seats -= passengers;

                        weightLeft -= weight;
                        totalWeight += weight;

                        AssignmentBean abean = new AssignmentBean(rs);
                        abean.setActive(Assignments.ASSIGNMENT_ENROUTE);
                        result.add(abean);

                        qry = "UPDATE assignments SET active = ? where id = ?";
                        DALHelper.getInstance().ExecuteUpdate(qry, Assignments.ASSIGNMENT_ENROUTE, rs.getInt("id"));
                        onBoard++;
                    }
                }

                if(allInFlight && onBoard != 1)
                    throw new DataError("All-In assignment not loaded. Cannot start the flight.");
            }

            // Update aircraft status to departed
            qry = "UPDATE aircraft SET departedFrom = ?, lockedSince = ?, location = null where id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, location, now, bean.getId());

            //update our departing data
            depart.payloadWeight = Math.round(totalWeight);
            depart.totalWeight = Math.round(totalWeight + (float)bean.getFuelWeight() + bean.getEmptyWeight());
            depart.assignments = result;
            depart.rentedDry = rentedDry;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return depart;
    }

    /**
     * This method processes an End flight conditon
     * @param user - logged in user
     * @param location - aircraft lat/lon
     * @param engineTime - seconds
     * @param engineTicks - tach ticks
     * @param fuel - aircraft fuel by tank (percentage)
     * @param night - is sim time currently night tiem
     * @param envFactor - sim weather condition
     * @param damage - aircraft damage array
     * @param simType - is the sim FSX
     * @return int - number of active assignments
     */
    public static synchronized int processFlight(UserBean user, CloseAirport location, int engineTime, int engineTicks, float[] fuel, int night, float envFactor, int[][] damage, SimClientRequests.SimType simType) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try //This try block is for SQL Exceptions
        {
            //Defined outside our inner try so that the aircraft lock can be evaluated in the finally block
            AircraftBean aircraft;
            int freeAircraft = 0;

            //Setup our connection needs
            conn = DALHelper.getInstance().getConnection();

            try //This try block is to free the aircraft processing lock whether processing completed or failed
            {
                /////////////////////////////////////////////////////////////////
                //Validity checks
                /////////////////////////////////////////////////////////////////

                // Get the currently locked aircraft for user
                aircraft = Aircraft.getAircraftForUser(user.getId());

                //Is valid aircraft
                if (aircraft == null)
                {
                    GlobalLogger.logFlightLog(new Timestamp(System.currentTimeMillis()) + " processFlight: No Aircraft lock.  SimType = " + simType.name() + ", User = " + user.getId(), Flights.class);
                    throw new DataError("VALIDATIONERROR: No aircraft in use, flight aborted.");
                }

                //Check flight in progress
                if (aircraft.getDepartedFrom() == null)
                {
                    GlobalLogger.logFlightLog(new Timestamp(System.currentTimeMillis()) + " processFlight: No flight in progress.  SimType = " + simType.name() + ", User = " + user.getId(), Flights.class);
                    throw new DataError("VALIDATIONERROR: It appears that a duplicate flight was submitted and canceled. Please check your My Flight page for current flight status.");
                }

                // Load up our model parameters for later checks
                // Need to do this after getAircraftForUser is validated
                ModelBean m = Models.getModelById(aircraft.getModelId());

                // Speed checks
                // Average Speed of flight should be less than 2.5 the models cruise speed, this is very generous
                int flightDistance = (int)Math.round(getDistance(location.icao, aircraft.getDepartedFrom()));
                Float flightMPH = (float) (flightDistance/(engineTime/3600.0));
                if (flightMPH > (m.getCruise()*1.5))
                {
                    cancelFlight(user);
                    //Added more debugging variables to the system message, this happens rarely but we have no idea why

                    GlobalLogger.logFlightLog(new Timestamp(System.currentTimeMillis()) + " Excess Speed Calculated, rejecting flight. SimType = " + simType.name() + ", Reg = " + aircraft.getRegistration() + " User = " + user.getId() + " DepartICAO = " + aircraft.getDepartedFrom() + " ArriveICAO = " + location.icao + " Distance = " + flightDistance + " Airspeed = " + flightMPH + " EngineTime = " + engineTime, Flights.class);
                    throw new DataError("VALIDATIONERROR: Invalid speed calculated. (" + flightMPH + "-MPH) between DepartICAO = " + aircraft.getDepartedFrom() + " to ArriveICAO = " + location.icao + " in " + (int)(engineTime/60.0 + .5) + " Minutes, flight aborted");
                }

                ///////////////////////////////////////////////////////
                // Checks done, start processing flight data
                ///////////////////////////////////////////////////////

                //modified to always zero out unused fuel tanks based on model data
                for (int i = 0; i < fuel.length; i++)
                {
                    int capacity = m.getCap(i);
                    if (capacity == 0) 	// Model has 0 fuel for this tank
                    {
                        fuel[i] = 0;	//Added just to make sure no ghost tanks with fuel - Airboss 3/5/11
                        continue; 		// so we need to continue or we get a divide by zero
                    }

                    //If FSX then we need to load up the percentage of fuel left in tank
                    if (simType == SimClientRequests.SimType.FSX || simType == SimClientRequests.SimType.XP)
                        fuel[i] /= capacity;
                }

                //Set the current aircraft fuel levels
                aircraft.setFuel(fuel);

                // Fuel checks, only sets flag, does not toss data error
                double initialFuel = aircraft.getInitialFuel();

                // More fuel then when started, if only that could be true!
                boolean invalidFuel = (!aircraft.wasWetRent()) && (initialFuel < aircraft.getTotalFuel());

                // Before we start we need to process Pax assignment to assign the
                // tax rate to each for computing costs so...

                // Get the total pax assignments on board
                int ptAssignmentCount = Assignments.getPTAssignmentCount(user.getId());

                // Compute Pax Taxes
                // If the pax count is greater then 5, compute the tax rate and assign to
                // all onboard pax assignments
                updateMultiPTtax(user.getId(), ptAssignmentCount);

                /////////////////////////////////////////////////
                //Start Computing Flight Costs
                /////////////////////////////////////////////////

                float income = 0, fuelCost = 0, rentalCost = 0, bonus = 0, rentalTime;
                float price = 0, crewCost = 0, fboAssignmentFee = 0, mpttax = 0;
                int totalPilotFee = 0;

                //Distance from home ICAO bonus charges
                DistanceBearing distanceFromHome = new DistanceBearing(0.0,0.0);

                float toPayOwner = 0;

                //Flight cost starts here at 0.00
                float flightCost = 0.0f;

                //All-In change to ensure no extra charges are added to the bill
                boolean allIn;
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

                //Is the pilot using an AllIn aircraft?
                String qry = "SELECT (count(aircraft.id) > 0) AS found FROM aircraft, assignments WHERE aircraft.id = assignments.aircraftid AND aircraft.userlock = ?";
                allIn = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user.getId());

                //AllIn flight with no assignment check - Airboss 11-9-12
                String allInAssignmentToIcao = "";
                if(allIn)
                {
                    //Get AllIn assignments that are enroute using the reported aircraft
                    qry = "SELECT (count(id) > 0) AS found FROM assignments WHERE active = 1 AND aircraftid = ?";
                    boolean allInAssignment = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getId());
                    if( !allInAssignment )
                    {
                        GlobalLogger.logFlightLog("  Flight kicked: AllIn flight with no assignment at current aircraft location", Flights.class);
                        String errmsg = "VALIDATIONERROR: AllIn flights must have the assignment co-located with aircraft. It appears the assignment was not transported -- flight aborted!";

                        cancelFlight(user);
                        throw new DataError(errmsg);
                    }
                    qry = "SELECT toicao FROM assignments WHERE active = 1 AND aircraftid = ?";
                    allInAssignmentToIcao = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), aircraft.getId());
                }

                //All-In change - don't pay rental, fuel, or distance fees
                if (!allIn)
                {
                    // Get Rental cost (Dry/Wet)
                    if (aircraft.wasWetRent())
                        price = aircraft.getRentalPriceWet(); // Wet rental
                    else
                        price = aircraft.getRentalPriceDry();  //Dry rental

                    // Get rental time in hours
                    rentalTime = (float)(engineTime/3600.0);

                    // Set our aircraft Rental Cost
                    rentalCost = price * rentalTime;

                    // Check to see if we need to add in fuel costs for Dry rental
                    // Currently if no fuel was used no fuel cost, Scot-Free???
                    if (!aircraft.wasWetRent() && !invalidFuel)
                    {
                        double cost = (initialFuel - aircraft.getTotalFuel()) * Goods.getFuelPrice(location.icao);
                        if (aircraft.getFuelType() == AircraftBean.FUELTYPE_JETA)
                            cost *= Goods.getJetaMultiplier();

                        fuelCost = (float)cost;
                    }

                    // if bonus setting is 0 or we are back to where we started, leave pilot bonus at 0
                    if(aircraft.getBonus() != 0 && !location.icao.contentEquals(aircraft.getDepartedFrom()))
                    {
                        // If we are not at home, we need to calculate distance/bearing otherwise leave at 0, 0
                        if(!location.icao.equals(aircraft.getHome()))
                            distanceFromHome = getDistanceBearing(location.icao, aircraft.getHome());

                        double dist = 0.0;

                        // If departure airport was home icao, no need to calculate the distance
                        if(!aircraft.getHome().contentEquals(aircraft.getDepartedFrom()))
                            dist = getDistance(aircraft.getHome(), aircraft.getDepartedFrom());

                        double b = aircraft.getBonus();

                        // If both distance and distanceFromHome are 0, bonus is 0
                        if( dist != 0 || distanceFromHome.distance != 0.0 )
                            bonus =(float)((b * (dist - distanceFromHome.distance)/100.0));
                    }

                    //Bonus can be positive or negative, if the plane moves toward home,
                    //then pilot is payed, if away from home then owner is paid
                    flightCost = rentalCost + fuelCost - bonus;

                    toPayOwner = flightCost;  // Pay this to the owner of the aircraft

                    if(flightCost == 0 && price != 0)
                        GlobalLogger.logFlightLog(
                                new StringBuilder()
                                        .append("****> Rental costs for Reg: ").append(aircraft.getRegistration())
                                        .append(", Date: ").append(new Timestamp(System.currentTimeMillis()))
                                        .append(", Owner: ").append(Accounts.getAccountNameById(aircraft.getOwner()))
                                        .append(", By: ").append(user.getName())
                                        .append(", From: ").append(aircraft.getDepartedFrom())
                                        .append(", To: ").append(location.icao)
                                        .append(", EngineTimeType: ").append(aircraft.getAccounting() == AircraftBean.ACC_HOUR ? "Hourly" : "Tach")
                                        .append(", EngineTime: ").append(engineTime).append(", RentalTime: ")
                                        .append(rentalTime).append(", RentalType: ").append(aircraft.wasWetRent() ? "Wet" : "Dry")
                                        .append(", InvalidFuel: ").append(invalidFuel)
                                        .append(", pricePerHour: ").append(price)
                                        .append(", Rental Cost: ").append(rentalCost)
                                        .append(", FuelCost: ").append(fuelCost)
                                        .append(", Bonus: ").append(bonus)
                                        .append(", TotalToOwner: ").append(flightCost).toString(),
                                Flights.class);

                    // Crew Cost added  - deducts $100 per hour per additional crew member from payout
                    crewCost = m.getCrew() * 100 * (float)(engineTime/3600.0);
                }	//All-In clause

                // Determine if Group flight and if so payments
                ////////////////////////////////////////////////////

                boolean groupFlight = false;
                int groupId = -1;
                UserBean groupToPay = null;

                // Determine if this is a group flight
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                rs = stmt.executeQuery("SELECT groupId FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND groupId is not null");
                if (rs.next())
                {
                    groupFlight = true;
                    groupId = rs.getInt(1);
                }
                rs.close();
                stmt.close();

                // If a group flight recalc pilot pay before payouts.
                // This adds in the Pilot Fee for those jobs they might have grabbed
                // while flying currently flying for a group that were not from the group assignment page,
                // and thus have no group ID and pilot fee assigned
                if (groupFlight)
                {
                    UserBean group = Accounts.getGroupById(groupId);
                    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                    stmt.executeUpdate("UPDATE assignments SET pilotFee=pay*amount*distance*0.01*" + group.getDefaultPilotFee()/100.0 + ", groupId = " + groupId + " WHERE active=1 and userlock = " + user.getId() + " and groupId is null");
                    stmt.close();

                    //gurka - added to keep track of the group to pay for closing exploit - to be used later on just before the committ to check balances of group bank account
                    groupToPay = group;
                }

                //////////////////////////////////////////////
                // Start Payouts
                //////////////////////////////////////////////

                // Determine who gets paid
                int payAssignmentToAccount = groupFlight ? groupId : user.getId();

                //-- Start exploit code
                //Determine what the current flight income is going to be
                //so that we can disallow negative personal balances for Players and Groups
                // Get all the current active assignments
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                rs = stmt.executeQuery("SELECT * FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");
                while (rs.next())
                {
                    AssignmentBean assignment = new AssignmentBean(rs);
                    int pilotFee = assignment.getPilotFee();

                    //Get assignment PAY here
                    float value = (float)assignment.calcPay();

                    int mptTaxRate = assignment.getmptTax();

                    // Charge mptTax - convert tax rate to a percent
                    if (mptTaxRate > 0)
                        mpttax += (value * (mptTaxRate * .01));

                    // Used for tracking log
                    totalPilotFee += pilotFee;
                    income += value;

                    fboAssignmentFee += Fbos.payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft.getId(), true);
                    fboAssignmentFee += Fbos.payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft.getId(), true);
                }
                rs.close();
                stmt.close();

                //added validation to make sure exploit not possible for group flights and rentals
                //validate the rental cost is not more then the bank account in the group account
                double netIncome = income - (flightCost + crewCost + totalPilotFee + fboAssignmentFee + mpttax);

                double balance;

                if( groupToPay != null )
                    balance = groupToPay.getMoney() + groupToPay.getBank();
                else
                    balance = user.getMoney() + user.getBank();

                boolean balanceKick = false;

                //group/pilot balance fail check
                if( groupToPay != null && netIncome < 0 && (balance + netIncome) < 0 )
                    balanceKick = true;
                else if(groupToPay == null && netIncome < 0 && (balance + netIncome) < -40000)
                    balanceKick = true;

                if( balanceKick )
                {
                    String errmsg;
                    if( groupToPay != null)
                        errmsg = "VALIDATIONERROR: Insufficient funds in the group \' " + groupToPay.getName() + "\' cash account to pay for the flight costs -- flight aborted!";
                    else
                        errmsg = "VALIDATIONERROR: Insufficient funds in your cash account to pay for the flight costs -- flight aborted!";

                    cancelFlight(user);
                    throw new DataError(errmsg);
                }
                //-- end exploit code

                //rezero out the the variables for actual payment loop
                income = 0;
                fboAssignmentFee = 0;
                mpttax = 0;
                totalPilotFee = 0;

                //--------------------------------------------
                //Fixed payouts based on aircraft rental
                //--------------------------------------------

                // Pay rental + fuel + bonus to owner of aircraft
                if (toPayOwner != 0)
                    Banking.doPayment(payAssignmentToAccount, aircraft.getOwner(), toPayOwner, PaymentBean.RENTAL, 0, -1, location.icao, aircraft.getId(), "", false);

                // Pay crew cost
                if (crewCost > 0)
                    Banking.doPayment(payAssignmentToAccount, 0, crewCost, PaymentBean.CREW_FEE, 0, -1, location.icao, aircraft.getId(), "", false);

                //---------------------------------------------
                // Variable payouts based on each assignment
                //---------------------------------------------

                // Get all the current active assignments
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                rs = stmt.executeQuery("SELECT * FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");
                while (rs.next())
                {
                    AssignmentBean assignment = new AssignmentBean(rs);
                    int pilotFee = assignment.getPilotFee();

                    //Get assignment PAY here
                    float value = (float)assignment.calcPay();
                    int owner = assignment.getOwner();
                    int mptTaxRate = assignment.getmptTax();

                    //log template jobs
                    if(assignment.getFromTemplate() > 0)
                        Logging.logTemplateAssignment(assignment, payAssignmentToAccount);

                    // Pay assignment to operator of flight
                    Banking.doPayment(owner, payAssignmentToAccount, value, PaymentBean.ASSIGNMENT, 0, -1, location.icao, aircraft.getId(), "", false);

                    // If group flight, pay the pilot fee
                    if (groupFlight && pilotFee > 0)
                        Banking.doPayment(payAssignmentToAccount, user.getId(), pilotFee, PaymentBean.PILOT_FEE, 0, -1, location.icao, aircraft.getId(), "", false);

                    // Charge mptTax - convert tax rate to a percent
                    if (mptTaxRate > 0)
                    {
                        Banking.doPayment(payAssignmentToAccount, 0, (value * (mptTaxRate * .01)), PaymentBean.MULTIPLE_PT_TAX, 0, -1, location.icao, aircraft.getId(), "", false);

                        // Used for tracking log
                        mpttax += (value * (mptTaxRate * .01));
                    }

                    int commodityId = assignment.getCommodityId();
                    if (commodityId > 0)
                    {
                        // added in check for aircraft shipping commodity id
                        if( commodityId < 99)
                        {
                            Goods.changeGoodsRecord(location.icao, commodityId, owner, assignment.getAmount(), false);
                        }
                        else
                        {
                            //Commodity field holds registration number in []
                            String sa = assignment.getCommodity();

                            //setup our indexes for pulling registration number out
                            int start = 9;
                            int end = sa.indexOf(" Shipment Crate");

                            //Get Registration and trim off any excess space at start and end
                            String reg = sa.substring(start,end).trim();
                            int aircraftId = Aircraft.getAircraftIdByRegistration(reg);

                            //Finalize our shipment
                            Aircraft.finalizeAircraftShipment(aircraftId, false, false);
                        }
                    }

                    // Used for tracking log
                    totalPilotFee += pilotFee;
                    income += value;

                    fboAssignmentFee += Fbos.payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft.getId(), false);
                    fboAssignmentFee += Fbos.payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft.getId(), false);
                }
                rs.close();
                stmt.close();

                //For completed Assignments, remove them
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                stmt.executeUpdate("DELETE from assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");
                stmt.close();

                //For assignments that have not reached their destination, reset location ICAO to current ICAO
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                stmt.executeUpdate("UPDATE assignments SET location = '" + location.icao + "', active=0 WHERE active=1 AND userlock=" + user.getId());
                stmt.close();

                // Determine if the user has any assignments are still active, or at the current location
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                rs = stmt.executeQuery("SELECT count(*) FROM assignments WHERE userlock=" + user.getId() + " AND (active = 1 OR location='" + location.icao + "')");
                rs.next();

                // if < 1 then no more assignments
                freeAircraft = rs.getInt(1);

                rs.close();
                stmt.close();

                // update the aircraft parameters
                /////////////////////////////////////////////////////
                int totalEngineTime;
                int totalAirframeTime;

                // Get an updatable recordset for the current aircraft
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                rs = stmt.executeQuery("SELECT * FROM aircraft WHERE userlock=" + user.getId());
                rs.next();

                totalEngineTime = rs.getInt("engine1") + engineTime;
                totalAirframeTime = rs.getInt("airframe") + engineTime;

                rs.updateInt("engine1", totalEngineTime);
                rs.updateInt("airframe", totalAirframeTime);
                rs.updateNull("departedFrom");
                rs.updateString("location", location.icao);
                rs.updateInt("distanceFromHome", (int)Math.round(distanceFromHome.distance));

                if (distanceFromHome.distance == 0)
                    rs.updateNull("bearingToHome");
                else
                    rs.updateInt("bearingToHome", (int)Math.round(distanceFromHome.bearing));

                if (!invalidFuel)
                    aircraft.writeFuel(rs);

                // if no awaiting assignments and no aircraft hold, or allin at it destination, then release the aircraft lock/rental
                if ((freeAircraft < 1 && !aircraft.getHoldRental())
                        || (allIn && location.icao.equals(allInAssignmentToIcao)) )
                {
                    rs.updateNull("userlock");
                    rs.updateNull("lockedSince");
                    rs.updateNull("initialFuel");
                }
                else // otherwise keep the aircraft locked / rented, and update initial fuel
                {
                    if (!aircraft.wasWetRent() && !invalidFuel)
                        rs.updateFloat("initialFuel", (float)aircraft.getTotalFuel());
                }

                // Update the aircraft record
                rs.updateRow();

                rs.close();
                stmt.close();

                // Update any damage conditions
                //////////////////////////////////////////////////////
                // Currently only Heat, and Mixture > 95% above 1000ft is checked in client
                for (int[] aDamage : damage)
                    Aircraft.addAircraftDamage(aircraft.getId(), aDamage[0], aDamage[1], aDamage[2]);

                for (int c=1; c<= aircraft.getEngines(); c++)
                    Aircraft.addAircraftDamage(aircraft.getId(), c, AircraftMaintenanceBean.DAMAGE_RUNTIME, engineTime);

                // Add log entry
                ///////////////////////////////////////////////////

                // Get a blank record, and move to insert
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                rs = stmt.executeQuery("SELECT * from log where 1=2");
                rs.moveToInsertRow();

                rs.updateTimestamp("time",new Timestamp(System.currentTimeMillis()));
                rs.updateInt("userid", user.getId());
                rs.updateInt("aircraftid", aircraft.getId());
                rs.updateString("from", aircraft.getDepartedFrom());
                rs.updateString("to", location.icao);
                rs.updateString("type", "flight");
                rs.updateInt("totalEngineTime", totalEngineTime);
                rs.updateFloat("fuelCost", fuelCost);
                rs.updateFloat("rentalCost", rentalCost);
                rs.updateFloat("income", income);
                rs.updateFloat("landingCost", 0); // not used
                rs.updateFloat("crewCost", crewCost);
                rs.updateFloat("rentalPrice", price);
                rs.updateString("rentalType", aircraft.wasWetRent() ? "wet" : "dry");
                rs.updateInt("flightEngineTime", engineTime);
                rs.updateInt("flightEngineTicks", engineTicks);
                rs.updateInt("accounting", aircraft.getAccounting());
                rs.updateInt("distance", flightDistance);
                rs.updateInt("night", night);
                rs.updateInt("pilotFee", totalPilotFee);
                rs.updateFloat("envFactor", envFactor);
                rs.updateFloat("fboAssignmentFee", fboAssignmentFee);
                rs.updateFloat("mpttax", mpttax);

                if (!Double.isNaN(bonus))
                    rs.updateFloat("bonus", bonus);

                if (groupFlight)
                    rs.updateInt("groupId", groupId);

                // insert the new log entry
                rs.insertRow();

                DALHelper.getInstance().tryClose(rs);
                DALHelper.getInstance().tryClose(stmt);

                // increment flight ops entry
                ///////////////////////////////////////////////////

                // Get a blank record, and move to insert
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1; //0 based instead of 1
                String fromICAO = aircraft.getDepartedFrom();
                String toICAO = location.icao;

                rs = stmt.executeQuery("SELECT * from flightops where opyear=" + year + " AND opmonth=" + month + " AND icao='" + fromICAO + "'");
                if(!rs.next())
                {
                    rs.moveToInsertRow();
                    rs.updateShort("opyear", (short)year);
                    rs.updateShort("opmonth", (short)month);
                    rs.updateString("icao", fromICAO);
                    rs.updateShort("ops", (short)1);

                    // update entry
                    rs.insertRow();
                }
                else
                {
                    int currops = rs.getShort("ops");
                    currops++;
                    rs.updateShort("ops", (short)currops);

                    // update entry
                    rs.updateRow();
                }

                //Close for next icao
                DALHelper.getInstance().tryClose(rs);

                rs = stmt.executeQuery("SELECT * from flightops where opyear=" + year + " AND opmonth=" + month + " AND icao='" + toICAO + "'");
                if(!rs.next())
                {
                    rs.moveToInsertRow();
                    rs.updateShort("opyear", (short)year);
                    rs.updateShort("opmonth", (short)month);
                    rs.updateString("icao", toICAO);
                    rs.updateShort("ops", (short)1);

                    // update entry
                    rs.insertRow();
                }
                else
                {
                    int currops = rs.getShort("ops") + 1;
                    rs.updateShort("ops", (short)currops);

                    // update entry
                    rs.updateRow();
                }
            }
            finally
            {
                DALHelper.getInstance().tryClose(rs);
                DALHelper.getInstance().tryClose(stmt);
                DALHelper.getInstance().tryClose(conn);
            }
            return freeAircraft;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }

        return -1;
    }

    public static void cancelFlight(UserBean user)
    {
        try
        {
            String qry = "UPDATE assignments SET active = 0 WHERE active = 1 AND userlock = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user.getId());

            qry = "SELECT * from aircraft WHERE userlock = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user.getId());
            if (rs.next())
            {
                String location;
                if(rs.getString("location") != null)
                {
                    location = rs.getString("location");
                }
                else if(rs.getString("departedFrom") == null)
                {
                    GlobalLogger.logFlightLog("Data error for aircraft " + rs.getString("registration") + ": location = null and departedFrom = null, reverting to home", Flights.class);
                    location = rs.getString("home");
                }
                else
                {
                    location = rs.getString("departedFrom");
                }

                qry = "UPDATE aircraft SET location = ?, departedFrom = NULL WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, location, rs.getInt("id"));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Update assignment.mpttax field for user enroute jobs
     */
    public static void updateMultiPTtax(int user, int ptAssignmentCount)
    {
        int mptTaxRate; // * 1; change if rate changes per assignment

        if( ptAssignmentCount > 5) //We only apply the tax on more then 5 assignments
        {
            //Tax is limited to 60, which just happens to be the limit of jobs as well
            if( ptAssignmentCount > 60 )
                mptTaxRate = 60;
            else
                mptTaxRate = ptAssignmentCount;
        }
        else //Otherwise there should be zero tax
        {
            mptTaxRate = 0;
        }

        try
        {
            String qry = "UPDATE assignments SET mpttax = ? WHERE userlock = ? AND active = 1 AND fromTemplate is null AND fromFboTemplate is not null AND mpttax < ?";
            DALHelper.getInstance().ExecuteUpdate(qry, mptTaxRate, user, mptTaxRate);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static Map<String, Integer> getMyFlightInfo(AircraftBean bean, int user) throws DataError
    {
        Map<String, Integer> result = new HashMap<>();
        String location = bean.getLocation();
        try
        {
            int groupId = -1;
            ModelBean model = Models.getModelById(bean.getModelId());

            int passengerCount = 0;

            int seats = bean.getAvailableSeats();
            int crewWeight = bean.getCrewWeight();

            // initialize our current payload weight
            int totalWeight = crewWeight;
            double weightLeft = bean.maxPayloadWeightWithFuel() - crewWeight;

            if (bean.getCanFlyAssignments(model))
            {
                String qry = "SELECT * FROM assignments WHERE userlock = ? AND (active =1 OR location=?) ORDER BY active DESC";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user, location);
                while (rs.next())
                {
                    int passengers;
                    int weight;
                    int active;

                    active = rs.getInt("active");

                    if (rs.getString("units").equals("passengers") && active < 2)
                    { // system or pt passenger assignment
                        passengers = rs.getInt("amount");
                        weight = passengers * Constants.PASSENGER_WT_KG;
                    }
                    else
                    { // cargo assignment
                        passengers = 0;
                        weight = rs.getInt("amount");
                    }

                    if (passengers <= seats && weight <= weightLeft && active < 2)
                    {
                        seats -= passengers;
                        passengerCount += passengers;

                        weightLeft -= weight;
                        totalWeight += weight;

                        result.put((new Integer(rs.getInt("id"))).toString(), null);
                        result.put("hasAssignment", null);

                        if (rs.getString("groupId") != null && groupId == -1)
                            groupId = rs.getInt("groupId");
                    }
                }
            }

            result.put("weight", totalWeight);
            result.put("passengers", passengerCount);

            if (groupId != -1)
                result.put("group", groupId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
