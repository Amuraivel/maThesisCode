import java.util.ArrayList;
/**
 * Created in IntelliJ IDEA.
 * Government.java
 * Copyright Jul 10, 2008 Mark James Thompson
 */
public class Government {
    //Key political variables
    double marginalCorruption = .5; // 0<mC<1
    double barriersToEntry;  // 1<bTEC
    //Show tradeoff between public goods and tax
    double taxRate; //unimplemented
    double revenue; //unimplemented
    double publicGoods = 2;  //
    double responsiveness; //static régime variable which reduces barriers + corruption
    double redTapeEntry; //entry cost for businesses Melitz (2003: 1699, #5, f)
    double depreciation;
    double overhead;
    //public double[][] tariffs = new double[1][1]; //Bilateral tariffs from Country A to Country B, NxN country extensibility hook

    //Governmental initialization with a few of our institutional variables
    Government(double overhead, double depreciation, double redTapeEntry) {
        this.overhead = overhead;
        this.depreciation = depreciation;
        this.redTapeEntry = redTapeEntry;
    }
    public static void collectTaxRevenue(ArrayList<Economy> countryEconomies, int countryIndex, int time) {
        Economy country = countryEconomies.get(countryIndex);
        for (int i = 0; i < countryEconomies.get(countryIndex).manufacturers.size(); i++) {
            double profit = Firm.profit(countryEconomies.get(countryIndex).manufacturers.get(i), countryEconomies, countryIndex, time);
            country.government.revenue += profit * country.government.taxRate;
        }
    }
}
