package net.fseconomy.servlets;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A simple CDI service which is able to say hello to someone
 *
 * @author Pete Muir
 *
 */
public class HelloService
{
    @PostConstruct
    public void init(){
        System.out.println("NewSingletonBean INIT");
    }

    public String createHelloMessage(String name)
    {
        return "Hello " + name + "!";
    }
}
