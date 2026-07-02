package com.jvmguard.demo.server.mbean;

import java.beans.ConstructorProperties;

@SuppressWarnings("unused")
public class RegionInfo {

    private String[] countries = {"US", "DE", "JP"};
    private String[] currencies = {"USD", "EUR", "JPY"};

    public RegionInfo() {
    }

    @ConstructorProperties({"countries", "currencies"})
    public RegionInfo(String[] countries, String[] currencies) {
        this.countries = countries;
        this.currencies = currencies;
    }

    public String[] getCountries() {
        return countries;
    }

    public String[] getCurrencies() {
        return currencies;
    }
}
