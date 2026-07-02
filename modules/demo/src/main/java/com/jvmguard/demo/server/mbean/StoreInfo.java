package com.jvmguard.demo.server.mbean;

import java.beans.ConstructorProperties;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class StoreInfo {

    private String name = "Cymbal Boutique";
    private int skuCount = 42;
    private Map<String, Integer> stockByRegion = new LinkedHashMap<>();
    private Map<String, BigInteger> revenueByRegion = new LinkedHashMap<>();
    private RegionInfo primaryRegion = new RegionInfo();

    public StoreInfo() {
        stockByRegion.put("US", 1200);
        stockByRegion.put("EU", 800);
        stockByRegion.put("APAC", 540);
        revenueByRegion.put("US", new BigInteger("184000"));
        revenueByRegion.put("EU", new BigInteger("121500"));
        revenueByRegion.put("APAC", new BigInteger("98250"));
    }

    @ConstructorProperties({"name", "skuCount", "stockByRegion", "revenueByRegion", "primaryRegion"})
    public StoreInfo(String name, int skuCount, Map<String, Integer> stockByRegion,
                     Map<String, BigInteger> revenueByRegion, RegionInfo primaryRegion) {
        this.name = name;
        this.skuCount = skuCount;
        this.stockByRegion = stockByRegion;
        this.revenueByRegion = revenueByRegion;
        this.primaryRegion = primaryRegion;
    }

    public String getName() {
        return name;
    }

    public int getSkuCount() {
        return skuCount;
    }

    public Map<String, Integer> getStockByRegion() {
        return stockByRegion;
    }

    public Map<String, BigInteger> getRevenueByRegion() {
        return revenueByRegion;
    }

    public RegionInfo getPrimaryRegion() {
        return primaryRegion;
    }
}
