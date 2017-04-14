import java.util.ArrayList;
/**
 * Created in IntelliJ IDEA.
 * Economy.java
 * Copyright Jul 6, 2008 Mark James Thompson
 * This class handles most of the economy parameter initalization like the sectors, consummers, and macro-calculations
 */
public class Economy {
    //Macro unit of analysis
    public String countryName;
    public int countryIndex;
    //One sector, Melitz leaves the other sectors out, and Baldwin assumes CRS in a numeraire to ignore it, so for
    //simplification there is only one sector, which avoids nested C.E.S. preferences between goods
    public ArrayList<Firm> manufacturers = new ArrayList<Firm>(); //Manufacturing sector
    public ArrayList<Consumer> consumers = new ArrayList<Consumer>();
    //two factors of production, pg. 6 Baldwin;
    public double Labor;
    public double Kapital;
    //Some useful macro-variables
    public double aggregateQuantity; //Total quantity Melitz 2003: 1699   exogenous/endogenous??
    public double aggregateRevenue; //aggrevenue = aggexpenditure
    public double aggregatePrice; //price of the firm's variety
    //Government object for policy variables
    public Government government;
    //Bilateral trade costs from country A to country B.
    //Extension hook for N*N matrix of bilateral tariffs
    public static double[][] tradeCosts = new double[3][3];  //initialized values 3x3 [World, North, South]
    //Basic productivity distribution from possible technologies, Melitz's g(phi)
    public double cutOffProductivity = 0;
    public double technologicalFrontier = MelitzModel.PRODUCER_DISTRIBUTION; //Pareto shape parameter
    public double entrapreneurialAttempts;
    public Economy(int SIM_TIME, double Kapital, double Labor, String countryName, double tradeCosts, ArrayList<Economy> countryEconomies, double overhead, double depreciation, double redTapeEntry, int countryIndex) {
        //Assigning country attributes
        this.countryName = countryName;
        this.Labor = Labor;
        this.Kapital = Kapital;
        //Establishing a government
        this.government = new Government(overhead, depreciation, redTapeEntry);
        //Modifying firms contained within the government's productivity draws based on institutional variables
        this.cutOffProductivity = (Consumer.SIGMA * this.government.overhead); //{Melitz 2003: 1703 #10}
        //this.technologicalFrontier =  this.technologicalFrontier -(Math.pow(this.government.overhead,2.5)) ; // can be used to shape distribution,
        this.cutOffProductivity = (this.cutOffProductivity / (this.government.depreciation * this.government.redTapeEntry));

        //Inserting trade costs into global bilateral matrix, from Country_N to Rest of World (GTAP RoW)
        Economy.tradeCosts[countryIndex][0] = MelitzModel.TRADE_COSTS;
        System.out.println("CountryTrade= " + Economy.tradeCosts[countryIndex][0]);
        //Generating a number of heterogenous firms
        if (countryName == "World") {
            for (int i = 0; i < countryEconomies.size(); i++) {
                //Aggregates all manufacturers into world economy
                this.manufacturers.addAll(countryEconomies.get(i).manufacturers);
            }
            //Giving ownership of firms to 1 representitive country consumer, according to Baldwin and Okubo
            this.consumers.add(new Consumer(manufacturers, countryName, "Representative"));
        } else {

            //Creating producers up to initial kapital endowment as Baldwin&Okubo
            for (int i = 0; i < Kapital; i++) {
                manufacturers.add(new Firm(countryIndex, this.cutOffProductivity, this.technologicalFrontier, this));
                //Inverting all productivity to match Baldwin and Okubo's "cost formulation", i.e. higher = more cost in labor units
                // if (MelitzModel.BALDWINIAN_RELOCATION){ manufacturers.get(i).productivity = 1/manufacturers.get(i).productivity; System.out.println("Country= " + countryIndex + "Producer= " + manufacturers.get(i).productivity);}

                //initializing revenue to avoid empty set at T_0
                aggregateRevenue += manufacturers.get(i).productivity;
            }
            //For illustrative purposes to show how barriers bog down entrepreneurship
            System.out.println("Entrepreneurial attempts in country " + this.countryName + "= " + this.entrapreneurialAttempts);
            //Giving ownership of firms to 1 representitive country consumer (utopia, most poor countries exhibit
            //unequal ownership, but following Baldwin and Okubo.
            this.consumers.add(new Consumer(manufacturers, countryName, "Representative"));
        }

        //Initializing price levels in the economy
        this.aggregatePrice = Economy.getAveragePrice(this.manufacturers);
    }
    //Melitz (2003: 1700 #7); Baldwin & Okubo (2006: 330 #4)
    public static double getAggregatePrice(Economy country) {
        double aggregateProductivity = 0;
        for (int i = 0; i < country.manufacturers.size(); i++) {
            aggregateProductivity += Math.pow(country.manufacturers.get(i).productivity, (1 - Consumer.SIGMA));
        }
        return Math.pow(aggregateProductivity, (1 - Consumer.SIGMA));
    }
    //Productive force of the entire economy for statistical polling
    public static double getAggregateProductivity(Economy country) {
        double aggregateProductivity = 0;
        for (int i = 0; i < country.manufacturers.size(); i++) {
            aggregateProductivity += country.manufacturers.get(i).productivity;
        }
        return aggregateProductivity;
    }
    //Melitz (2003: 1698 #1)
    public static double getAveragePrice(ArrayList<Firm> firmList) {
        double averagePrice = 0;
        for (int i = 0; i < firmList.size(); i++) {
            // Melitz(2003:1699 #3)
            firmList.get(i).price = Consumer.WAGE / (Consumer.RHO * firmList.get(i).productivity);
            averagePrice += Math.pow(firmList.get(i).price, (1 - Consumer.SIGMA)) * firmList.size();
        }
        return Math.pow(averagePrice, (1 / (1 - Consumer.SIGMA)));
    }
}