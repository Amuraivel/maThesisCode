import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.analysis.*;
import uchicago.src.sim.util.Random;

import java.util.ArrayList;
/**
 * Created in IntelliJ IDEA.
 * MelitzModel.java
 * Copyright September 21, 2007 Mark James Thompson
 */
public class MelitzModel extends SimModelImpl {
    /*
     * MODEL VARIABLES
     */
    /*
	FIRM PARAMETERS
	 */
    //Institutional efficiency level
    private double OVERHEAD_DIFFERENTIAL = 1; //1.876x difference in WB Survey, WB Survey 2008,
    private double DEPRECIATION_DIFFERENTIAL = 1; //"Likelihood of depreciating shock on firms, fire, riots, obsolete tech.
    private double BARRIERS_ENTRY_DIFFERENTIAL = 1; //Differential between barriers to entry in domestic market
    //Trade parameters
    public static double TRADE_COSTS = 1.1; //1.075322; //Transport and tariff costs between N. and S. pg. 8, ?'1 is the iceberg trade cost; assumed symmetric {GTAP marginal costs for Colombia-USA}
    //its endowment of labour and capital are proportionally larger than the south's
    public double RELATIVE_K_SIZE_NORTH = 1.5; //x times bigger than South
    public double LABOR_SHARE = 1;  //x times bigger than South
    public double SOUTH_KAPITAL_ENDOWMENT = 200;
    public double SOUTH_LABOR_ENDOWMENT = 100;
    private double CORRUPTION_NORTH = .83; //Mean of top twenty North (8.3) {Transparency Int. 2007 Denmark = 9.4 / Somalia = 1.4}
    private double CORRUPTION_SOUTH = .275; // Mean bottom twenty South (2.75)
    //Unimplemented, government responsiveness//
    private double RESPONSIVENESS_NORTH = 1;
    private double RESPONSIVENESS_SOUTH = 1; //
    public static final double PRODUCER_DISTRIBUTION = 12; //pareto shape parameto, this dicates technological level to a certain extent.
    public static final double PRODUCER_SCALE = Consumer.SIGMA; //We know for sure that a/phi* > Consumer.SIGMA, this is to avoid evaluating all from [0,Consumer.SIGMA), which will for sure not be productive enough.
    private double OPENNESS_NORTH = .47682;  //1st Quintile 2005  {KoF Globalization}
    private double OPENNESS_SOUTH = .83694;  //4th Quintile 2005 {KoF Globalization}
    //constr. quintile %per capita GDP in 1999 of Djankov, La Porta, López-de-Silanes & Shleifer – Regulation of Entry
    private double ENTRY_BARRIERS_NORTH = 193.89;  //5th constructed quintile
    private double ENTRY_BARRIERS_SOUTH = 2814.69; //1st constructed quintile
    public Economy NorthEconomy, SouthEconomy, WorldEconomy;
    public ArrayList<Economy> countryEconomyList; //extension hook for multiple countries and for global aggregation.
    public boolean ENDOGENOUS_GROWTH = false; //Extremely slow if toggle on!
    public static boolean BALDWINIAN_RELOCATION = true;
    /*
     Model stuff
      */
    public static final int SIM_TIME = 1000;
    private OpenSequenceGraph productivity;
    private Histogram countryProductivityDistribution;
    private static final boolean GUI = true;
    private Schedule schedule;
    private DataRecorder recorder;
    public void setup() {
        System.out.println("Running setup");
        if (GUI) {
            //Display tear-downs
            if (productivity != null) {
                productivity.dispose();
            }
            productivity = null;
            if (countryProductivityDistribution != null) {
                countryProductivityDistribution.display();
            }
            countryProductivityDistribution = null;
            //Displays creation
            productivity = new OpenSequenceGraph("Productivity Share", this);
            countryProductivityDistribution = new Histogram("Firm Productivity Distribution", 10, Consumer.SIGMA, 3.5);
            //Display registration
            this.registerMediaProducer("Plot", productivity);
        }
    }
    public void begin() {
        //Creating distributions for the CERN random number engine
        Random.createUniform(0, 1);
        Random.createPareto(1, 3);
        Random.createNormal(0, 1);
        buildModel();
        buildSchedule();
        if (GUI) {
            buildDisplay();
        }
        buildRecorder();
    }
    public void buildModel() {
        System.out.println("Building model");
        schedule = new Schedule(1);
        //Trade Costs are symmetric
        //Containers for global economic variables

        //we assume two regions, pg. 6: More regions can be added here
        countryEconomyList = new ArrayList<Economy>();

        //North this the baseline
        NorthEconomy = new Economy(SIM_TIME, RELATIVE_K_SIZE_NORTH * SOUTH_KAPITAL_ENDOWMENT, LABOR_SHARE * SOUTH_LABOR_ENDOWMENT, "North", TRADE_COSTS, countryEconomyList, 1, 1, 1, 1);
        //South has values with respect to north
        SouthEconomy = new Economy(SIM_TIME, SOUTH_KAPITAL_ENDOWMENT, SOUTH_LABOR_ENDOWMENT, "South", TRADE_COSTS, countryEconomyList, OVERHEAD_DIFFERENTIAL, DEPRECIATION_DIFFERENTIAL, BARRIERS_ENTRY_DIFFERENTIAL, 2);

        //Order matters !! Don't shuffle country lists
        countryEconomyList.add(NorthEconomy);  // index =1 for easier String-free manipulations, and extensibility
        countryEconomyList.add(SouthEconomy);  // index =2

        //World Economy Initalization, world government gets a bunch of 0s for institutional parameters because after all there is no world government...this avoids prgramming errors by throwing bizarre
        //values if called
        WorldEconomy = new Economy(SIM_TIME, (NorthEconomy.manufacturers.size() + SouthEconomy.manufacturers.size()), (NorthEconomy.Labor + SouthEconomy.Labor), "World", TRADE_COSTS, countryEconomyList, 0, 0, 0, 0);
        countryEconomyList.add(0, WorldEconomy);  //0 is all the economies.
    }
    //Eye candy
    public void buildDisplay() {
        System.out.println("Running BuildDisplay");
        productivity.addSequence("Raw Productivity Share of South", new productivity());

        //countryProductivityDistribution.createHistogramItem("N. Firm Productivity Distribution",NorthEconomy.manufacturers, new countryProductivity());
        countryProductivityDistribution.createHistogramItem("S. Firm Productivity Distribution", SouthEconomy.manufacturers, new countryProductivity());
        countryProductivityDistribution.setMovieName("ProdcutivityLossSouth", "video.quicktime");
        productivity.display();
        countryProductivityDistribution.display();
        countryProductivityDistribution.setBars(.1, .1);
        countryProductivityDistribution.setXIncrement(.1);
        countryProductivityDistribution.setXRange(0, 10);
    }
    public void buildSchedule() {
        System.out.println("Building schedule");
        class TradeModelDailyStep extends BasicAction {
            public void execute() {

                //Flag for GUI to boost performance for batch calculations
                if (GUI) {
                    guiUpdate();
                }

                //System.out.println("Northern productivity " + Economy.getAverageProductivity(NorthEconomy));
                //System.out.println("Southern productivity " + Economy.getAverageProductivity(SouthEconomy));

                //Relocation algorithm
                if (BALDWINIAN_RELOCATION) {
                    System.out.println("I might be missing out on profit, let's see if I should relocate today...");
                    int bestProducer = Firm.findMostProductive(SouthEconomy.manufacturers);
                    System.out.println("My productivity is = " + SouthEconomy.manufacturers.get(bestProducer).productivity);
                    double currentLocationProfit = Firm.profit(SouthEconomy.manufacturers.get(bestProducer), countryEconomyList, 2, (int) schedule.getCurrentTime());
                    double expectedRelocatedProfit = Firm.profit(SouthEconomy.manufacturers.get(bestProducer), countryEconomyList, 1, (int) schedule.getCurrentTime());
                    System.out.println("If I stay, my profit is " + currentLocationProfit + ". If I go, my profit is " + expectedRelocatedProfit + ". The difference= " + (expectedRelocatedProfit - currentLocationProfit));
                    if (expectedRelocatedProfit > currentLocationProfit) {
                        System.out.println("OK, let's move then...");
                        Firm.relocate(countryEconomyList, 2, 1, bestProducer);  //From South (2) to North (1)
                    }
                }
                //Based on Hopenhayn 1992 1130: #A4): Depreciates a part of the capital stock, then adds new entrants who receive a based productivity draw & competition in local market
                if (ENDOGENOUS_GROWTH) {
                    double mean;
                    double variance;
                    double prodGrowth;
                    for (int i = 1; i < countryEconomyList.size(); i++) {
                        double ZeroProfitFirmProductivity = Firm.findLeastProductive(countryEconomyList.get(i).manufacturers);
                        mean = Firm.findAverageProductivity(countryEconomyList, i);
                        variance = Math.sqrt(mean);
                        prodGrowth = mean * (.001);
                        System.out.println("Country = " + i + "; mean productivity=" + mean + "; shock variance= " + variance + "; Daily productivity growth= " + prodGrowth);
                        for (int k = 0; k < countryEconomyList.get(k).manufacturers.size(); k++) {
                            //Apply Hopenhayn type shock
                            countryEconomyList.get(i).manufacturers.get(k).productivity = countryEconomyList.get(i).manufacturers.get(k).productivity + Random.normal.nextDouble((mean - countryEconomyList.get(i).government.depreciation), variance);
                            //In equilibrium firms exit the industry whenever their state falls below a reservation level x.
                            if (countryEconomyList.get(i).manufacturers.get(k).productivity < ZeroProfitFirmProductivity) {
                                countryEconomyList.get(i).manufacturers.remove(k);
                                //Location and shape parameters for the productivity draw based on country economy, plus competition adjustment
                                countryEconomyList.get(i).manufacturers.add(k, new Firm(i, (countryEconomyList.get(i).technologicalFrontier + prodGrowth), countryEconomyList.get(i).technologicalFrontier, countryEconomyList.get(i)));
                            }
                        }
                    }
                }
                System.out.println("Average World prod = " + Firm.findAverageProductivity(countryEconomyList, 0) + "; Northern prod = " + Firm.findAverageProductivity(countryEconomyList, 1) + "; Southern prod = " + Firm.findAverageProductivity(countryEconomyList, 2));
                //Collecting consumer income
                Consumer.collectIncome(countryEconomyList, (int) schedule.getCurrentTime());
                //Updating national price levels to reflect market competition after migration
                for (int i = 0; i < countryEconomyList.size(); i++) {
                    countryEconomyList.get(i).aggregatePrice = Economy.getAveragePrice(countryEconomyList.get(i).manufacturers);
                    System.out.println("Country=" + i + "; Price = " + countryEconomyList.get(i).aggregatePrice);
                }
            }
        }
        schedule.scheduleActionAtInterval(1, new TradeModelDailyStep());
        schedule.scheduleActionAtEnd(this, "outPut");
    }
    public Schedule getSchedule() {
        return schedule;
    }
    /*
       THE FOLLOWING are REPAST INFRASTRUCTURE methods for variables
       Theorists stop reading, programmers keep reading
    */
    public void outPut() {
        System.out.println("Recorder is being called, hang on!");
        System.out.println("Current directory is " + System.getProperty("user.dir"));
        recorder.write();     //write all recorded values
        System.out.println("Movie is being made, hang on!");
        countryProductivityDistribution.closeMovie();
    }
    public void guiUpdate() {
        productivity.step();
        productivity.updateGraph();
        countryProductivityDistribution.step();
        countryProductivityDistribution.addMovieFrame();
    }
    public void stopMe() {
        super.stop();
    }
    //Shows name for the model we are working on.
    public String getName() {
        return "Heterogeneous firms, agglomeration and economic geography: Spatial selection and sorting";
    }
    public String[] getInitParam() {
        String[] initParams;
        initParams = new String[]{"OVERHEAD_DIFFERENTIAL", "BALDWINIAN_RELOCATION", "DEPRECIATION_DIFFERENTIAL", "ENDOGENOUS_GROWTH", "CORRUPTION_NORTH", "CORRUPTION_SOUTH", "TRADE_COSTS", "OPENNESS_SOUTH", "OPENNESS_NORTH", "BARRIERS_ENTRY_DIFFERENTIAL", "DEMAND_ELASTICITY", "RELATIVE_K_SIZE_NORTH"};
        return initParams;
    }
    public void buildRecorder() {
        recorder = new DataRecorder("modeldata.csv", this);
        recorder.createNumericDataSource("aggregateProductivity", this, "getSouthFirmShare");
    }
    //The all important class hook
    public static void main(String[] args) {
        SimInit init = new SimInit();
        MelitzModel melitzModel = new MelitzModel();
        init.loadModel(melitzModel, null, false);
    }
    //Recorder class for keeping tabs in a statistical format
    class NumDataSource implements NumericDataSource {
        public double execute() {
            return 0;
        }
    }
    //Sub-classes for open sequence graphs
    class productivity implements DataSource, Sequence {
        public Object execute() {
            return getSValue();
        }
        public double getSValue() {
            return getSouthFirmShare();
        }
    }
    class countryProductivity implements BinDataSource {
        public double getBinValue(Object o) {
            Firm p = (Firm) o;
            return p.productivity;
        }
    }
    public double getSouthFirmShare() {
        return Economy.getAggregateProductivity(SouthEconomy) / Economy.getAggregateProductivity(WorldEconomy);
    }
    public boolean getBALDWINIAN_RELOCATION() {
        return MelitzModel.BALDWINIAN_RELOCATION;
    }
    public void setBALDWINIAN_RELOCATION(boolean baldwinianRelocation) {
        MelitzModel.BALDWINIAN_RELOCATION = baldwinianRelocation;
    }
    public double getOVERHEAD_DIFFERENTIAL() {
        return this.OVERHEAD_DIFFERENTIAL;
    }
    public void setOVERHEAD_DIFFERENTIAL(double overheadDifferential) {
        this.OVERHEAD_DIFFERENTIAL = overheadDifferential;
    }
    public double getDEPRECIATION_DIFFERENTIAL() {
        return this.DEPRECIATION_DIFFERENTIAL;
    }
    public void setDEPRECIATION_DIFFERENTIAL(double depreciationDifferential) {
        this.DEPRECIATION_DIFFERENTIAL = depreciationDifferential;
    }
    public double getCORRUPTION_NORTH() {
        return this.CORRUPTION_NORTH;
    }
    public void setCORRUPTION_NORTH(double corruptionNorth) {
        this.CORRUPTION_NORTH = corruptionNorth;
    }
    public double getCORRUPTION_SOUTH() {
        return this.CORRUPTION_SOUTH;
    }
    public void setCORRUPTION_SOUTH(double corruptionSouth) {
        this.CORRUPTION_SOUTH = corruptionSouth;
    }
    public double getOPENNESS_SOUTH() {
        return this.OPENNESS_SOUTH;
    }
    public void setOPENNESS_SOUTH(double opennessSouth) {
        this.OPENNESS_SOUTH = opennessSouth;
    }
    public double getOPENNESS_NORTH() {
        return this.OPENNESS_NORTH;
    }
    public void setOPENNESS_NORTH(double opennessNorth) {
        this.OPENNESS_NORTH = opennessNorth;
    }
    public double getTRADE_COSTS() {
        return MelitzModel.TRADE_COSTS;
    }
    public void setTRADE_COSTS(double tradeCosts) {
        MelitzModel.TRADE_COSTS = tradeCosts;
    }
    public double getDEMAND_ELASTICITY() {
        return Consumer.SIGMA;
    }
    public void setDEMAND_ELASTICITY(double demandElasticity) {
        Consumer.SIGMA = demandElasticity;
    }
    public double getBARRIERS_ENTRY_DIFFERENTIAL() {
        return this.BARRIERS_ENTRY_DIFFERENTIAL;
    }
    public void setBARRIERS_ENTRY_DIFFERENTIAL(double barriersEntryDifferential) {
        this.BARRIERS_ENTRY_DIFFERENTIAL = barriersEntryDifferential;
    }
    public boolean getENDOGENOUS_GROWTH() {
        return this.ENDOGENOUS_GROWTH;
    }
    public void setENDOGENOUS_GROWTH(boolean endogenousGrowth) {
        this.ENDOGENOUS_GROWTH = endogenousGrowth;
    }

     public double getRELATIVE_K_SIZE_NORTH() {
        return this.RELATIVE_K_SIZE_NORTH;
    }
    public void setRELATIVE_K_SIZE_NORTH(double RELATIVE_K_SIZE_NORTH) {
        this.RELATIVE_K_SIZE_NORTH = RELATIVE_K_SIZE_NORTH;
    }
}