package net.fseconomy.data;

import net.fseconomy.util.GlobalLogger;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.*;

public class ScheduledTasks
{
    private static ScheduledTasks instance = null;
    private static ScheduledFuture<?> futureMaintenanceCycle = null;
    private static ScheduledFuture<?> futureDbOptimize = null;
    public static MaintenanceCycle maintenanceObject = null;
    private ScheduledExecutorService executorMaint;
    private ScheduledExecutorService executorOptimize;

    public static ScheduledTasks getInstance()
    {
        if ( instance == null )
            instance = new ScheduledTasks();

        return instance;
    }

    private ScheduledTasks()
    {
        //do this section last as this kicks off the timer
        maintenanceObject = new MaintenanceCycle();
    }

    public void startScheduledTasks()
    {
        //do this section last as this kicks off the timer
        executorMaint = Executors.newSingleThreadScheduledExecutor();
        executorOptimize = Executors.newSingleThreadScheduledExecutor();

        startMaintenanceCycle();
        startDbOptimize();
    }

    public void endScheduledTasks()
    {
        futureMaintenanceCycle.cancel(false);
        futureDbOptimize.cancel(false);
        executorMaint.shutdown();
        executorOptimize.shutdown();
    }

    private void startMaintenanceCycleOld()
    {
        if(Boolean.getBoolean("Debug"))
        {
            //5 minute cycles if Debug set on command line
            futureMaintenanceCycle = executorMaint.scheduleWithFixedDelay(maintenanceObject, 0, 5, TimeUnit.MINUTES);
        }
        else
        {
            long delay = minutesToNextHalfHour();

            GlobalLogger.logApplicationLog("Restart: Main cycle starts in (minutes): " + delay, ScheduledTasks.class);

            //if delay is 3 minutes or greater then run the cycle now to update stats
            if(delay >= 3)
            {
                //Do it now, then setup the schedule runs
                maintenanceObject.SetOneTimeStatsOnly(true);
                executorMaint.execute(maintenanceObject);
            }

            //Schedule it at the top and bottom of the hour
            futureMaintenanceCycle = executorMaint.scheduleAtFixedRate(maintenanceObject, delay, 30, TimeUnit.MINUTES);
        }
    }


    private void dbOptimize() throws RuntimeException
    {
        try
        {
            GlobalLogger.logApplicationLog("dbOptimize executed", ScheduledTasks.class);

            //start time
            long starttime = System.currentTimeMillis();

            DALHelper.getInstance().ExecuteNonQuery("{call optimizetables()}");

            //start time
            long elapsed = System.currentTimeMillis() - starttime;

            GlobalLogger.logApplicationLog("Table Optimize Completed: elapsed time = " + elapsed + "ms", ScheduledTasks.class);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            GlobalLogger.logApplicationLog("dbOptimize failed", ScheduledTasks.class);
            throw new RuntimeException("Optimize failed");
        }
    }

    private void startMaintenanceCycle()
    {
        int stdInterval = 30;

        long delay = minutesToNextHalfHour();
        GlobalLogger.logApplicationLog("Scheduled Maintenance cycle starts in [" + delay + "] minutes", ScheduledTasks.class);

        //if delay is 3 minutes or greater then run the cycle now to update stats
        if(delay >= 3)
        {
            //Do it now, then setup the schedule runs
            maintenanceObject.SetOneTimeStatsOnly(true);
            executorMaint.execute(maintenanceObject);
        }

        if(Boolean.getBoolean("Debug"))
        {
            stdInterval = 10;
            delay = 0;
        }

        futureMaintenanceCycle = executorMaint.scheduleAtFixedRate(maintenanceObject, delay, stdInterval, TimeUnit.MINUTES);

        Runnable watchdog = new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        futureMaintenanceCycle.get();
                    }
                    catch (ExecutionException e)
                    {
                        //handle it
                        startMaintenanceCycle();
                        return;
                    }
                    catch (InterruptedException e)
                    {
                        //handle it
                        return;
                    }
                }
            }
        };
        new Thread(watchdog).start();
    }

    private void startDbOptimize()
    {
        long delay = minutesToNextOptimize();
        GlobalLogger.logApplicationLog("Scheduled DbOptimize - next optimize in [" + delay + "] minutes", ScheduledTasks.class);

        futureDbOptimize = executorOptimize.scheduleAtFixedRate(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        dbOptimize();
                    }
                }, delay, 24*60, TimeUnit.MINUTES);

        Runnable watchdog = new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        futureDbOptimize.get();
                    }
                    catch (ExecutionException e)
                    {
                        //handle it
                        startDbOptimize();
                        return;
                    }
                    catch (InterruptedException e)
                    {
                        //handle it
                        return;
                    }
                }
            }
        };
        new Thread(watchdog).start();
    }

    private static long minutesToNextHalfHour()
    {
        Calendar calendar = Calendar.getInstance();

        int minutes = calendar.get(Calendar.MINUTE);
        //int seconds = calendar.get(Calendar.SECOND);
        //int millis = calendar.get(Calendar.MILLISECOND);
        int total = 60 - minutes;

        if(total > 30)
            total -= 30;

        return total;
    }

    private static long minutesToNextOptimize()
    {
        Calendar calendar = Calendar.getInstance();

        int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        //int seconds = calendar.get(Calendar.SECOND);
        //int millis = calendar.get(Calendar.MILLISECOND);
        int delayHours = 0;
        int delayMinutes = 0;
        if(hour24 > 6)
        {
            delayHours = 24 - hour24 + 6;
        }
        else
        {
            delayHours = 6 - hour24;
        }
        if(minutes > 15)
        {
            delayMinutes = (60 - minutes) + 15;
            delayHours--;
        }
        else
        {
            delayMinutes = 15 - minutes;
        }

        int total = delayHours*60 + minutes;

        return total;
    }

}
