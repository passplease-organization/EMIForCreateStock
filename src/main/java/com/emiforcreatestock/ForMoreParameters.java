package com.emiforcreatestock;

public class ForMoreParameters {
    public static final long playerNeedCountDefault = -1;
    @ForMoreParameter(usingClass = StockRequestHandler.class,dataFrom = "EmiScreenManagerMixin")
    public static long playerNeedCount;
}
