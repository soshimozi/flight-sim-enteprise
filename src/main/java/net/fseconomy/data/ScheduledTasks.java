package net.fseconomy.data;

import net.fseconomy.util.Formatters;
import net.fseconomy.util.GlobalLogger;
import net.fseconomy.util.Helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.*;

public class ScheduledTasks
{
    private static ScheduledTasks instance = null;
    private static ScheduledFuture<?> futureMaintenanceCycle = null;
    private static ScheduledFuture<?> futureDbTasks = null;
    public static MaintenanceCycle maintenanceObject = null;
    private ScheduledExecutorService executorMaint;
    private ScheduledExecutorService executorDbTasks;

    public static ScheduledTasks getInstance()
    {
        if ( instance == null )
            instance = new ScheduledTasks();

        return instance;
    }

    private ScheduledTasks()
    {
        //setup instance of maintenance cycle to run
        maintenanceObject = new MaintenanceCycle();
    }

    public void startScheduledTasks()
    {
        //do this section last as this kicks off the timer
        executorMaint = Executors.newSingleThreadScheduledExecutor();
        executorDbTasks = Executors.newSingleThreadScheduledExecutor();

        startMaintenanceCycle();
        startDbTasks();
    }

    public void endScheduledTasks()
    {
        futureMaintenanceCycle.cancel(false);
        futureDbTasks.cancel(false);
        executorMaint.shutdown();
        executorDbTasks.shutdown();
    }

    private void runDbTasks()
    {
        try
        {
            net.fseconomy.servlets.FullFilter.setMaintenanceMode(true);
            runDbOptimize();
            runDbBackup();
        }
        finally
        {
            net.fseconomy.servlets.FullFilter.setMaintenanceMode(false);
        }
    }

    private void runDbOptimize() throws RuntimeException
    {
        try
        {
            GlobalLogger.logApplicationLog("Table Optimize Started", ScheduledTasks.class);

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

    private String getDbBackupScript()
    {
        try
        {
            return DALHelper.getInstance().ExecuteScalar("SELECT sValue from sysvariables where VariableName='DbBackupScript'", new DALHelper.StringResultTransformer());
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public void runDbBackup() throws RuntimeException
    {
        GlobalLogger.logApplicationLog("Database Backup Started", ScheduledTasks.class);

        //start time
        long starttime = System.currentTimeMillis();

        try
        {
            ProcessBuilder pb;

            String dbScript = getDbBackupScript();
            if(Helpers.isNullOrBlank(dbScript))
            {
                GlobalLogger.logApplicationLog("Database Backup Script Missing!", ScheduledTasks.class);
                return;
            }

            // Find OS running on VM
            String operatingSystem = System.getProperty("os.name");

            // In case of windows run command
            // In case of Linux/Ubuntu run command using /bin/bash
            if (operatingSystem.toLowerCase().contains("window"))
            {
                pb = new ProcessBuilder("cmd", "/c", dbScript);
            }
            else
            {
                pb = new ProcessBuilder("/bin/bash", "-c", dbScript);
                pb.redirectErrorStream(true);
            }

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println("backup-databases output: " + line);

            process.waitFor();

            //start time
            long elapsed = System.currentTimeMillis() - starttime;

            GlobalLogger.logApplicationLog("Database Backup Completed: elapsed time = " + elapsed + "ms", ScheduledTasks.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            GlobalLogger.logApplicationLog("Database Backup failed", ScheduledTasks.class);
            throw new RuntimeException("Database Backup failed");
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

    private void startDbTasks()
    {

        int delay = (int)minutesToNextOptimize();

        Calendar clock = Calendar.getInstance();
        clock.add(Calendar.MINUTE, delay);

        GlobalLogger.logApplicationLog("Scheduled DbTasks - next run at [" + Formatters.dateyyyymmddhhmmzzz.format(clock.getTimeInMillis()) + "] in [" + delay + "] minutes", ScheduledTasks.class);

        long stdInterval = 24*60;

        if(Boolean.getBoolean("Debug"))
        {
            stdInterval = 5;
            delay = 0;
        }

        futureDbTasks = executorDbTasks.scheduleAtFixedRate(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        runDbTasks();
                    }
                }, delay, stdInterval, TimeUnit.MINUTES);

        Runnable watchdog = new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        futureDbTasks.get();
                    }
                    catch (ExecutionException e)
                    {
                        //handle it
                        startDbTasks();
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
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 10);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long secs = (c.getTimeInMillis()-System.currentTimeMillis()) / 1000;

        return secs / 60;
    }

}
