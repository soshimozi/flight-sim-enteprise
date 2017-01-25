package net.fseconomy.dto;

//TODO This should be removed and just use ModelBean as there is little to no real difference
//Modelbean needs to be modified to use fcap instead of array.
public class AircraftConfig
{
    public String makemodel;
    public int seats,crew,fueltype,cruisespeed,fcapExt1,fcapLeftTip,fcapLeftAux,fcapLeftMain,fcapCenter,fcapCenter2,fcapCenter3,fcapRightMain,fcapRightAux,fcapRightTip,fcapExt2,gph,maxWeight,emptyWeight,price,engines,enginePrice,fcaptotal,modelId;
    public boolean canShip;

    public AircraftConfig(String makemodel, int crew, int fueltype, int seats, int cruisespeed, int fcapExt1, int fcapLeftTip, int fcapLeftAux, int fcapLeftMain, int fcapCenter, int fcapCenter2, int fcapCenter3, int fcapRightMain, int fcapRightAux, int fcapRightTip, int fcapExt2, int gph, int maxWeight, int emptyWeight, int price, int engines, int enginePrice, boolean canShip, int fcaptotal, int modelId)
    {
        this.makemodel = makemodel;
        this.seats = seats;
        this.crew = crew;
        this.fueltype = fueltype;
        this.cruisespeed = cruisespeed;
        this.fcapExt1 = fcapExt1;
        this.fcapLeftTip = fcapLeftTip;
        this.fcapLeftAux = fcapLeftAux;
        this.fcapLeftMain = fcapLeftMain;
        this.fcapCenter = fcapCenter;
        this.fcapCenter2 = fcapCenter2;
        this.fcapCenter3 = fcapCenter3;
        this.fcapRightMain = fcapRightMain;
        this.fcapRightAux = fcapRightAux;
        this.fcapRightTip = fcapRightTip;
        this.fcapExt2 = fcapExt2;
        this.gph = gph;
        this.maxWeight = maxWeight;
        this.emptyWeight = emptyWeight;
        this.price = price;
        this.engines = engines;
        this.enginePrice = enginePrice;
        this.canShip = canShip;
        this.fcaptotal = fcaptotal;
        this.modelId = modelId;
    }
}

