package test.optimization;

import optimization.QLearningEngine;
import optimization.ValueIterationEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import state.ActionEnumeration;
import state.State;
import state.StateSpaceManager;
import test.state.ActionEnumerationTest;
import test.state.StateEnumerationTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 *
 * @author Zeb Burroughs
 */
public class QLearningEngineTest {

    private static File folder;
    private static StateSpaceManager manager;

    @Before
    public void setUp() {
        try
        {
            folder = Files.createTempDirectory("optimizationLibrary").toFile();
            manager = StateSpaceManager.getManager(folder);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(){
    }

    @Test
    public void qLearningEngine()
    {
        System.out.println("selectNextState");
        QLearningEngine instance1 = new QLearningEngine(folder);
        QLearningEngine instance2 = new QLearningEngine(folder, 10);
        QLearningEngine instance3 = new QLearningEngine(folder, 5, 10);
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
    }

    /**
     * Ensure selectNextState is random.
     */
    @Test
    public void selectNextState() {
        System.out.println("selectNextState");
        QLearningEngine instance = new QLearningEngine(folder, 100, 10);
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);

        Set<int[]> states = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            states.add(instance.selectNextState());
        }

        assertTrue(states.size() > 1);
    }

    @Test
    public void processResults() {
        System.out.println("processResults");
        QLearningEngine instance = new QLearningEngine(folder);
        double alpha = 0.2;
        double gamma = 0.5;
        double reward = 2;
        instance.setAlpha(alpha);
        instance.setGamma(gamma);
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);

        instance.processResults(manager.getStateForID(0), manager.getActionsForID(0),  manager.getStateForID(1), 0, reward);

        instance.finishOptimization();
        Map<Integer, Map<Integer, Double>> table = instance.getResults();

        double expectedValue = 0 + alpha * (reward + gamma * 0 - 0);
        double actualValue = table.get(0).get(0);
        assertEquals(expectedValue, actualValue, 0);
    }


    @Test
    public void continueIterating()
    {
        System.out.println("continueIterating");
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder, 10, 10);
        for (int i=0; i < 100; i++)
        {
            assertTrue(instance.continueIterating());
            instance.selectNextState();
        }

        assertFalse(instance.continueIterating());
    }

    @Test
    public void finishOptimization()
    {
        System.out.println("finishOptimization");
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder);
        for (int i=0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                instance.processResults(manager.getStateForID(i), manager.getActionsForID(j),  manager.getStateForID(i), 0, 0);
            }
        }
        instance.finishOptimization();
        assertFalse(instance.continueIterating());
        File csv = new File(folder, "qtable.csv");
        assertTrue(csv.exists());
    }

    @Test
    public void reset() {
        System.out.println("reset");

        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder, 10, 10);
        for (int i=0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                instance.processResults(manager.getStateForID(i), manager.getActionsForID(j),  manager.getStateForID(i), 0, 0);
            }
        }

        instance.finishOptimization();
        instance.reset();
        assertTrue(instance.continueIterating());
    }


    @Test
    public void requiresFixedExcursionOrdering() {
        System.out.println("requiresFixedExcursionOrdering");
        QLearningEngine instance = new QLearningEngine(folder);
        assertFalse(instance.requiresFixedExcursionOrdering());
    }

    @Test
    public void getResults() {
        System.out.println("getResults");
        QLearningEngine instance = new QLearningEngine(folder);
        try {
            instance.getResults();
        } catch (QLearningEngine.PrematureInvocationException err)
        {
            System.out.println("caught premature invocation");
        }
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);

        for (int i=0; i < manager.getNumberOfStates(); i++)
        {
            for (int j = 0; j < manager.getNumberOfActions(); j++)
            {
                instance.processResults(manager.getStateForID(i), manager.getActionsForID(j),  manager.getStateForID(i), 0, 0);
            }
        }
        instance.finishOptimization();

        Map<Integer, Map<Integer, Double>> table = instance.getResults();
        assertEquals(table.size(), manager.getNumberOfStates());
        for (int i=0; i < manager.getNumberOfStates(); i++)
        {
            assertEquals(table.get(i).size(), 1);
        }
    }

    @Test
    public void setSaveLocation() {
        System.out.println("setSaveLocation");
        QLearningEngine instance = new QLearningEngine(folder);
        File csv;

        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        try {
            File newFolder = Files.createTempDirectory("optimizationLibrary2").toFile();
            instance.setSaveLocation(newFolder);
            csv = new File(newFolder, "qtable.csv");
            try {
                instance.processResults(manager.getStateForID(0), manager.getActionsForID(0),  manager.getStateForID(0), 0, 0);
            }
            catch (IllegalArgumentException err)
            {
                System.out.println("caught manager mismatch");
            }
            instance.selectNextState();
            instance.finishOptimization();
            assertTrue(csv.exists());
        } catch (IOException err)
        {
            fail("Could not create temp directory");
        }
    }

    @Test
    public void TestGetSetAlpha() {
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder);

        double expectedInvalidRate = 1.1;
        double expectedValidRate = 0.321;

        instance.setAlpha(expectedInvalidRate);
        assertNotEquals(expectedInvalidRate, instance.getAlpha(), 0);

        instance.setAlpha(expectedValidRate);
        assertEquals(expectedValidRate, instance.getAlpha(), 0);
    }

    @Test
    public void TestGetSetEpsilon() {
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder);

        double expectedInvalidRate = 1.1;
        double expectedValidRate = 0.321;

        instance.setEpsilon(expectedInvalidRate);
        assertNotEquals(expectedInvalidRate, instance.getEpsilon(), 0);

        instance.setEpsilon(expectedValidRate);
        assertEquals(expectedValidRate, instance.getEpsilon(), 0);
    }

    @Test
    public void TestGetSetGamma() {
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder);

        double expectedInvalidRate = 1.1;
        double expectedValidRate = 0.321;

        instance.setGamma(expectedInvalidRate);
        assertNotEquals(expectedInvalidRate, instance.getGamma(), 0);

        instance.setGamma(expectedValidRate);
        assertEquals(expectedValidRate, instance.getGamma(), 0);
    }

    @Test
    public void TestSetNumberOfEpisodesAndSteps()
    {
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        QLearningEngine instance = new QLearningEngine(folder);

        int episodes = 10;
        int steps = 10;
        int iterations = 0;

        instance.setNumberOfEpisodes(episodes);
        instance.setMaxNumberOfSteps(steps);

        while (instance.continueIterating()) {
            instance.selectNextState();
            iterations++;
        }

        assertEquals(episodes * steps, iterations);
    }

    @Test
    public void IterationSpeed()
    {
        long start = System.nanoTime();
        for (int i=0; i<1000; i++)
        {
            //
        }
        long finish = System.nanoTime();
        System.out.println(String.format("Elapsed Time: %fs", (finish - start) / 1000000.0));
    }
}