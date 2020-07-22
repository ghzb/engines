
package test.functional;

import manager.PolicyManager;
import optimization.Correlation;
import optimization.OptimizationManager;
import optimization.QLearningEngine;
import optimization.StateTransition;
import org.junit.*;
import state.State;
import state.StateEnumeration;
import state.StateSpaceManager;
import test.state.ActionEnumerationTest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 *
 * @author Zeb Burroughs
 */
public class TestFunctionalQLearning implements Serializable
{

    private static File folder;
    private static StateSpaceManager sManager;
    private static OptimizationManager oManager;
    private File policyFile;

    public TestFunctionalQLearning()
    {
    }

    @BeforeClass
    public static void setUpClass() 
    {
    }

    @AfterClass
    public static void tearDownClass() 
    {
    }

    @Before
    public void setUp() 
    {
        try
        {
            folder = Files.createTempDirectory("QLearning_").toFile();
            sManager = StateSpaceManager.getManager(folder);
            oManager = OptimizationManager.getManager(folder);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }  
    }

    @After
    public void tearDown() 
    {
//        for (File file : folder.listFiles())
//        {
//            file.delete();
//        }
//        folder.delete();
    }

    @Test
    public void mainTest()
    {
        configureStateSpaceManager();
        configureOptimizationManger(100, 1000);

        assertCanReadPolicyFile();
        assertCanWriteTransitionMatrix();
        assertPolicyCorrelatesWithValueIteration(false);
    }

    @Test
    public void additionalTest()
    {
        configureStateSpaceManager();
        configureOptimizationManger(100, 200);

        assertCanReadPolicyFile();
        assertCanWriteTransitionMatrix();
        assertPolicyCorrelatesWithValueIteration(true);
    }

    private void configureStateSpaceManager()
    {
        sManager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        sManager.registerActionEnumeration(ActionEnumerationTest.Action2.ONE);
        sManager.registerStateEnumeration(COMBAT_POWER_FEASIBLE.BETTER);
        sManager.registerStateEnumeration(TRANSPORTS_FEASIBLE.ELEVEN_PLUS);
    }

    private void configureOptimizationManger (int episodes, int steps) {
        HundredRepExcursionBuilder EBRR = new HundredRepExcursionBuilder(folder, sManager.getStateEnumOrder());
        oManager = new OManagerForTest(folder);
        oManager.setExcursionBuilder(EBRR);
        oManager.setResultReader(EBRR);
        oManager.setResultScorer(new TestResultScorer(folder));
        QLearningEngine engine = new QLearningEngine(folder, episodes, steps);
        engine.setLearningRate(0.7);
        engine.setExplorationRate(0.3);
        engine.setDiscountRate(0.9);
        oManager.setOptimizationEngine(new QLearningEngine(folder, episodes, steps));
        oManager.startOptimization(true);
//        oManager.getPolicy()
        oManager.save();
    }

    private void assertCanReadPolicyFile()
    {
        PolicyManager pm = PolicyManager.getManager(folder);
        policyFile = pm.refreshPolicyFile();
        assertTrue(policyFile.canRead());
    }

    private void assertCanWriteTransitionMatrix()
    {
        int numStates = sManager.getNumberOfStates();
        int numCombinations = sManager.getNumberOfStates() * sManager.getNumberOfActions();
        try
        {
            List<String> lines = Files.readAllLines(policyFile.toPath());
            //Also account for the header line
            assertEquals(numStates+1, lines.size());
        }
        catch (IOException e)
        {
            fail();
        }
//
    }

    private int[] indexOrdering(double [] arr)
    {
        Map<Integer, Double> map = new HashMap<>();
        int i = 0;
        for(double item: arr)
        {
            map.put(i, item);
            i++;
        }

        Map<Integer, Double> sorted = map.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        Set<Integer> keys = sorted.keySet();

        int[] res = new int[keys.size()];
        i = 0;
        for (Integer key: keys)
        {
            res[i] = key;
            i++;
        }
        return res;
    }

    private void assertPolicyOrderingMatches(double[] p1, double[] p2)
    {
        int[] ordering1 = indexOrdering(p1);
        int[] ordering2 = indexOrdering(p2);
        System.out.println(Arrays.toString(p1));
        System.out.println(Arrays.toString(p2));
        System.out.println(Arrays.toString(ordering1));
        System.out.println(Arrays.toString(ordering2));
        assertArrayEquals(ordering1, ordering2);
    }

    private void assertPolicyCorrelatesWithValueIteration (boolean testOrdering)
    {
        try
        {
            int i = 0;
            List<Double> qpolicylist = new ArrayList<>();
            double[] vpolicy = {2.635245, 4.69526, 6.680257, 1.934909, 4.075843, 6.052354, 0.691317, 2.581134, 4.610989};
            List<String> lines = Files.readAllLines(policyFile.toPath());
            for(String line: lines)
            {
                int j = 0;
                if (i > 0)
                {
                    for (String item: line.split(","))
                    {
                        if (j == 4)
                        {
                            qpolicylist.add(Double.parseDouble(item));
                        }
                        j++;
                    }
                }
                i++;
            }
            double[] qpolicy = qpolicylist.stream().mapToDouble(d -> d).toArray();
            if (testOrdering)
            {
                assertPolicyOrderingMatches(qpolicy, vpolicy);
            }
            double corr = Correlation.fromArray(qpolicy, vpolicy);
            System.out.println(corr);
            assertTrue(corr >= 0.8);
        }
        catch (IOException e)
        {
            fail();
        }
    }


     
    public class HundredRepExcursionBuilder extends TestExcursionBuilderResultReader
    {
       public HundredRepExcursionBuilder(File folder, List<StateEnumeration> stateOrder) 
       {
           super(folder, stateOrder);
       }

       @Override
       public int getNumRequiredRepetitions() 
       {
           return 100;
       }
       
       
       @Override
       public double getProbOfDefaultState()
       {
           return 0.2;
       }
       
        @Override
        public State readNewState(State oldState, File outputLocation)
        {
            State origState = super.readNewState(oldState, setOutputLocation(saveFolder));
            
            List<StateEnumeration> fixedVals = new ArrayList<StateEnumeration>();
            fixedVals.add(COMBAT_POWER_FEASIBLE.values()[((Enum)origState.getValue(0)).ordinal()]);
            fixedVals.add(TRANSPORTS_FEASIBLE.values()[((Enum)origState.getValue(1)).ordinal()]);
            
            return new State(fixedVals);
        }
    }
    
    public class OManagerForTest extends OptimizationManager
    {
        public OManagerForTest(File folder)
        {
            super(folder);
            managers.put(folder.getAbsolutePath(), this);
        }
        
        public Map<Integer, Map<Integer, StateTransition>> getExcursionResults()
        {
            return this.excursionResults;
        }
    }
    
    public enum COMBAT_POWER_FEASIBLE implements StateEnumeration<Double>
    {
        BETTER, TIED, WORSE; 
        
        @Override
        public COMBAT_POWER_FEASIBLE getEnumForValue(Double value)
        {
            if (value > 0.6)
            {
                return BETTER;
            }
            else if (value > 0.4)
            {
                return TIED;
            }
            else
            {
                return WORSE;
            }
        }
        
        @Override
        public Double getValueForEnum()
        {
            switch(this)
            {
                case BETTER:
                    return 0.75;
                case TIED:
                    return 0.5;
                case WORSE:
                    return 0.25;
                default:
                    return -1.0;
            }
        }
        
        @Override
        public boolean isFeasibleStartState()
        {
            return true;
        }
        
        @Override
        public COMBAT_POWER_FEASIBLE getDefaultValue()
        {
            return TIED;
        }
        
        @Override
        public boolean getsDefaulted() {
            return true;
        }
    }
    
    public enum TRANSPORTS_FEASIBLE implements StateEnumeration<Integer>
    {
        ONE_FIVE, SIX_TEN, ELEVEN_PLUS;
        
        @Override
        public TRANSPORTS_FEASIBLE getEnumForValue(Integer value)
        {
            if (value < 6)
            {
                return ONE_FIVE;
            }
            else if (value < 10)
            {
                return SIX_TEN;
            }
            else
            {
                return ELEVEN_PLUS;
            }
        }
        
        @Override
        public Integer getValueForEnum()
        {
            switch(this)
            {
                case ONE_FIVE:
                    return 3;
                case SIX_TEN:
                    return 7;
                case ELEVEN_PLUS:
                    return 12;
                default:
                    return -1;
            }
        }
        
        @Override
        public boolean isFeasibleStartState()
        {
            return true;
        }
        
        @Override
        public TRANSPORTS_FEASIBLE getDefaultValue()
        {
            return SIX_TEN;
        }
        
        @Override
        public boolean getsDefaulted() {
            return true;
        }
    }    
}