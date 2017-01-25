package net.fseconomy.servlets;

import net.fseconomy.util.CacheBuilder;
import net.fseconomy.util.GlobalLogger;
import org.infinispan.Cache;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;

/**
 * @author noconnor@redhat.com
 *
 */
public class CacheServletListener implements ServletContextListener
{
    private static CacheBuilder currCacheBuilder;
    private ServletContext context = null;

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0)
    {
        GlobalLogger.logApplicationLog("CacheServletListener contextDestroyed() called", CacheServletListener.class);

        currCacheBuilder.getCacheManager().stop();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0)
    {
        GlobalLogger.logApplicationLog("CacheServletListener contextInitialized() called", CacheServletListener.class);

        try
        {
            this.context = arg0.getServletContext();
            String x = ""; //arg0.getServletContext().getInitParameter("InfinispanConfigFile");

            currCacheBuilder =  new CacheBuilder(x);
            currCacheBuilder.getCacheManager().start();

            context.setAttribute("cacheManager", currCacheBuilder.getCacheManager());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println("in CacheServletListener...exit");
    }

    public static Cache getCache(String name)
    {
        return currCacheBuilder.getCacheManager().getCache(name);
    }
}

