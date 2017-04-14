import java.util.ArrayList;
/**
 * Created in IntelliJ IDEA.
 * Consumer.java
 * Copyright Jul 6, 2008 Mark James Thompson
 */
public class Consumer {
    //There is only 1 consumer per country, but could conceivably  extended to include poor and rich consumers with
    //different ownership
    String consumerCountry;
    String consumerType; //Extension hook for future welfare dynamics of kapital vs. labor class
    public static double SIGMA = 2; // 0<rho<1<sigma;
    public static double RHO = .5; // sigma = 1/(1-rho) > 1
    public static double WAGE = 1;  //Normalized to 1 Melitz 1699
    public double[] income = new double[MelitzModel.SIM_TIME];  //Initializes dynamic income vector
    public double[] utility = new double[MelitzModel.SIM_TIME]; //Initializes dynamic utility vector
    public static double SHARE_C_M = .5; //Share of expenditure manufactures
    //Itializes location indepdent ownership as in Baldwin and Okubo, pg. 328
    Consumer(ArrayList<Firm> companiesOwned, String countryName, String consumerType) {
        this.consumerCountry = countryName;
        this.consumerType = consumerType;
        this.income[0] = companiesOwned.size();
        Consumer.RHO = 1 - 1 / Consumer.SIGMA;
        System.out.println("Companies owned by " + this.consumerCountry + "= " + companiesOwned.size());
        for (int i = 0; i < companiesOwned.size(); i++) {
            if (countryName != "World") {
                companiesOwned.get(i).ownerCountry = countryName;
            }
            companiesOwned.get(i).ownerType = consumerType;
        }
    }
    //Collecting industrial output as income
    public static void collectIncome(ArrayList<Economy> economies, int time) {
        //World profit levels are altered, but this
        if (MelitzModel.BALDWINIAN_RELOCATION) {
            for (int i = 0; i < economies.get(0).manufacturers.size(); i++) {
                if (economies.get(0).manufacturers.get(i).ownerCountry == "North") {
                    economies.get(1).consumers.get(0).income[time] += Firm.profit(economies.get(0).manufacturers.get(i), economies, economies.get(0).manufacturers.get(i).currentLocation, time);
                } else if (economies.get(0).manufacturers.get(i).ownerCountry == "South") {
                    economies.get(2).consumers.get(0).income[time] += Firm.profit(economies.get(0).manufacturers.get(i), economies, economies.get(0).manufacturers.get(i).currentLocation, time);
                }
            }
            //Global Income calcuation, there is a "world consumer"
            economies.get(0).consumers.get(0).income[time] = (economies.get(1).consumers.get(0).income[time] + economies.get(2).consumers.get(0).income[time]);
            System.out.println("NorthIncome= " + economies.get(1).consumers.get(0).income[time] + " ; Southern Income" + economies.get(2).consumers.get(0).income[time]);
        } else {

            //Calculating income based on raw productive capacity, i.e. productivity, allowing for depreciation, and overhead expenses
            //trick would be to endogenize this
            double globalIncome = 0;

            //Capital stock reduction based on gov. effectiveness, minus governmental imposed overhead
            //Starting at 1 (North), because 0 (World)
            for (int i = 1; i < economies.size(); i++) {
                globalIncome += economies.get(i).manufacturers.size() * ((1 / economies.get(i).government.depreciation) * Firm.findAverageProductivity(economies, i) - economies.get(i).government.overhead);
            }

            //Another cute extensibility feature to N economies, but ...
            for (int i = 0; i < economies.size(); i++) {
                //Due to free capital movement, returns equalize, global income is allocation proportional to capital endowment to country i to the representatitive consumer
                economies.get(i).consumers.get(0).income[time] = globalIncome * (economies.get(i).Kapital / economies.get(0).Kapital);
            }
            System.out.println("Northern Income= " + economies.get(1).consumers.get(0).income[time] + "; Southern Income " + economies.get(2).consumers.get(0).income[time]);
        }
    }
// Baldwin and Okubo's denominator, pg 330

    // [Trying to deprecate this] by switching over to Melitz Style calculation.
    public static double denominator(ArrayList<Economy> economyList, String country) {
        double denominator = 0;
        double share_k_north = economyList.get(1).Kapital / economyList.get(0).Kapital;
        double share_k_south = economyList.get(2).Kapital / economyList.get(0).Kapital;
        double freeness = Math.pow(1 / Economy.tradeCosts[0][0], (1 - Consumer.SIGMA));
        if (country == "North") {
            denominator = share_k_north * Economy.getAggregateProductivity(economyList.get(0)) + freeness * (share_k_south * Economy.getAggregateProductivity(economyList.get(0)));
        }
        if (country == "South") {
            denominator = freeness * (share_k_north * Economy.getAggregateProductivity(economyList.get(0))) + share_k_south * Economy.getAggregateProductivity(economyList.get(0));
        }
        System.out.println("Demand= " + denominator + " freeness= " + freeness + " share_k_North= " + share_k_north + " share_k_South= " + share_k_south);
        return denominator;
    }
    //Baldwin and Okubo pg. 329
    public static double utility(ArrayList<Economy> economies) {
        double C_M = 0;
        double C_A = 1;
        for (int i = 0; i < economies.get(0).manufacturers.size(); i++) {
            C_M += Math.pow(economies.get(0).manufacturers.get(i).productivity, (1 - (1 / Consumer.SIGMA)));
        }
        C_M = Math.pow(C_M, (1 - (1 / Consumer.SIGMA)));
        return Consumer.SHARE_C_M * Math.log(C_M) + (1 - Consumer.SHARE_C_M) * (C_A);
    }
}
