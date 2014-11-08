package net.fseconomy.dto;

import net.fseconomy.beans.AssignmentBean;

import java.util.List;

public class DepartFlight
{
    public int payloadWeight;
    public int totalWeight;
    public List<AssignmentBean> assignments;
    public boolean rentedDry;
}
