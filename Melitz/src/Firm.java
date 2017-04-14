import uchicago.src.sim.util.Random;

import java.util.ArrayList;
/**
 * Created in IntelliJ IDEA.
 * Firm.java
 * Copyright Jun 25, 2008 Mark James Thompson
 */
public class Firm {
    public int currentLocation; //Tracks the index ID of current location of the firm
    public String ownerCountry;    //Tracks corporate ownership to a consumer
    public String ownerType; //Tracks type
    //gamma, pg. 8 B&O
    public double productivity;    // a_i ; productivity/heterogeneity assigned to each unit of capital
    public double profit;
    public double price;
    public Firm(int location, double cutOffProductivity, double technologicalFrontier, Economy economy) {
        int trialCount = 0;
        while (productivity < cutOffProductivity) {
            productivity = Random.pareto.nextDouble(Consumer.SIGMA, technologicalFrontier);
            economy.entrapreneurialAttempts++;
        }
        currentLocation = location;
        price = Consumer.WAGE / Consumer.RHO;
    }
    // Melitz (2003: 1699 #3)
    public static double price(Firm firm) {
        return Consumer.WAGE / (Consumer.RHO * firm.productivity);
    }
    // Melitz (2003: 1699 #4)
    public static double revenue(Economy economy, Firm firm) {
        return economy.aggregateRevenue * Math.pow(economy.aggregatePrice * Consumer.RHO * firm.productivity, (Consumer.SIGMA - 1));  //All labor in economy is divided equally
    }
    //THere are two ways to calculate firm profit, Baldwin and Melitz, the Switch toggles the difference
    public static double profit(Firm firm, ArrayList<Economy> economies, int localMarketIndex, int time) {
        double total_profit = 0;
        int foreignMarketIndex = 0;
        if (localMarketIndex == 1) {
            foreignMarketIndex = 2;
        }
        if (localMarketIndex == 2) {
            foreignMarketIndex = 1;
        }

        //Baldwin & Okubo 330
        if (MelitzModel.BALDWINIAN_RELOCATION) {
            double freeness = Math.pow(Economy.tradeCosts[localMarketIndex][0], (1 - Consumer.SIGMA));
            double share_k_local = economies.get(localMarketIndex).Kapital / economies.get(0).Kapital;
            double share_k_foreign = economies.get(foreignMarketIndex).Kapital / economies.get(0).Kapital;
            double local_denominator = share_k_local * Economy.getAggregateProductivity(economies.get(0)) + share_k_foreign * freeness * Economy.getAggregateProductivity(economies.get(0));
            double foreign_denominator = freeness * share_k_foreign * Economy.getAggregateProductivity(economies.get(0)) + share_k_foreign * Economy.getAggregateProductivity(economies.get(0));

            // System.out.println("Denominator= " + (local_denominator+foreign_denominator) + " freeness= " + freeness + " share_k_local= " + share_k_local + " share_k_foreign= " + share_k_foreign);
            double local_revenue = economies.get(localMarketIndex).consumers.get(0).income[time - 1] / local_denominator;
            double foreign_revenue = freeness * economies.get(foreignMarketIndex).consumers.get(0).income[time - 1] / foreign_denominator;
            total_profit = Math.pow(firm.productivity, (1 - Consumer.SIGMA)) *
                    (local_revenue + foreign_revenue) *
                    ((economies.get(0).consumers.get(0).income[time - 1]) / (economies.get(0).manufacturers.size() * Consumer.SIGMA)) - economies.get(localMarketIndex).government.overhead;

            // Melitz (2003: 1699 #5)
        } else {
            firm.productivity = ((economies.get(1).consumers.get(0).income[time - 1] + economies.get(2).consumers.get(0).income[time - 1]) / Consumer.SIGMA) - economies.get(firm.currentLocation).government.overhead;
        }
        //Corruption effect
        total_profit = (economies.get(localMarketIndex).government.marginalCorruption) * total_profit - (1 - economies.get(localMarketIndex).government.overhead);

        //System.out.println("Firm profit= " + total_profit);
        return total_profit;
    }
    //Find the most productive producer in a given industry list
    public static int findMostProductive(ArrayList<Firm> producerList) {      //, int location
        int index = 0;
        double comparison = 0;
        for (int i = 0; i < producerList.size(); i++) {
            if ((producerList.get(i).productivity > comparison)) {           //&&(producerList.get(i).currentLocation == location
                comparison = producerList.get(i).productivity;
                index = i;
            }
        }
        return index;
    }
    //Find the least productive producer in a given industry list
    public static int findLeastProductive(ArrayList<Firm> producerList) {     //, int location
        int index = 0;
        double comparison = 1000000000;
        for (int i = 0; i < producerList.size(); i++) {
            if ((producerList.get(i).productivity < comparison)) {           //&&(producerList.get(i).currentLocation == location
                comparison = producerList.get(i).productivity;
                index = i;
            }
        }
        return index;
    }
    //Find the average raw productivity level within a market
    public static double findAverageProductivity(ArrayList<Economy> countryList, int economyIndex) {
        double avgProductivity = 0;
        for (int i = 0; i < countryList.get(economyIndex).manufacturers.size(); i++) {
            avgProductivity += countryList.get(economyIndex).manufacturers.get(i).productivity;
        }

        //Get average
        avgProductivity = avgProductivity / countryList.get(economyIndex).manufacturers.size();
        return avgProductivity;
    }

    //(1/countryList.get(economyIndex).government.depreciation)*(countryList.get(economyIndex).manufacturers.get(i).productivity - countryList.get(economyIndex).government.overhead);

    //Physical relocation of the firms into new environment
    public static void relocate(ArrayList<Economy> economies, int fromCountry, int toCountry, int relocatingFirmIndex) {
        economies.get(0).manufacturers.get(relocatingFirmIndex).currentLocation = toCountry;   //Change location world economy list
        economies.get(fromCountry).manufacturers.get(relocatingFirmIndex).currentLocation = toCountry;  //Tell firm where it's going to be
        economies.get(toCountry).manufacturers.add(economies.get(fromCountry).manufacturers.get(relocatingFirmIndex));    //Tell host country it has a new member
        economies.get(fromCountry).manufacturers.remove(relocatingFirmIndex); //Tell former host country adieu!
    }
}
