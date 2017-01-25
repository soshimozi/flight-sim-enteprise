package net.fseconomy.services;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ResponseContainer
{
    ResponseContainer()
    {
        meta = new meta();
    }

    meta meta;
    Object data;

    public meta getMeta()
    {
        return meta;
    }

    public void setMeta(meta meta)
    {
        this.meta = meta;
    }

    public Object getData()
    {
        return data;
    }

    public void setData(Object data)
    {
        this.data = data;
    }

}

