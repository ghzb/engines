package test.optimization;

import optimization.PythonOptimizationEngineOld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import state.StateSpaceManager;
import test.state.PythonActionTest;
import test.state.PythonStateTest;

import java.io.File;
import java.nio.file.Files;

/**
 *
 * @author Zeb Burroughs
 */
public class PythonOptimizationEngineTest {

    private static File folder;
    private static StateSpaceManager manager;
    private static File python;
    private static File py;

    @Before
    public void setUp() {
        try
        {
            folder = Files.createTempDirectory("optimizationLibrary").toFile();
            manager = StateSpaceManager.getManager(folder);
            python = new File("test/python/venv/bin/python");
            py = new File("test/python/Test_QLearning.py");
            System.out.println(python.getAbsolutePath());
            System.out.println(py.getAbsolutePath());
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
    }

    /**
     * Ensure selectNextState is random.
     */
    @Test
    public void selectNextState() {
    }

    @Test
    public void processResults() {

    }


    @Test
    public void continueIterating()
    {
        System.out.println("continueIterating");
        PythonOptimizationEngineOld instance = new PythonOptimizationEngineOld(folder, python, py);
//        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
//        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
//        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
        manager.registerActionEnumeration(PythonActionTest.MOUNTAIN_CAR_ACTIONS.STAY);
        manager.registerStateEnumeration(PythonStateTest.MOUNTAIN_CAR_POSITION.ZERO);
        manager.registerStateEnumeration(PythonStateTest.MOUNTAIN_CAR_VELOCITY.ZERO);


//        System.out.println(PythonActionTest.MOUNTAIN_CAR_ACTIONS.STAY.);

//        System.out.println(StateEnumerationTest.MOUNTAIN_CAR_VELOCITY.ZERO.getEnumForValue(0.05));
        while (instance.continueIterating())
        {
            int[] nextState = instance.selectNextState();
            System.out.print("continueIterating: ");
            System.out.println(String.valueOf(nextState[0]) + ", " + String.valueOf(nextState[1]));
//            manager.st
            instance.processResults(manager.getStateForID(0), manager.getActionsForID(0), manager.getStateForID(1), 0, 1);
        }
        System.out.println("done");
    }

    @Test
    public void finishOptimization()
    {
    }

    @Test
    public void reset() {
    }


    @Test
    public void requiresFixedExcursionOrdering() {
    }

    @Test
    public void getResults() {
    }

    @Test
    public void setSaveLocation() {
    }

    @Test
    public void TestGetSetAlpha() {
    }

    @Test
    public void TestGetSetEpsilon() {
    }

    @Test
    public void TestGetSetGamma() {
    }

    @Test
    public void TestSetNumberOfEpisodesAndSteps()
    {
    }

    @Test
    public void IterationSpeed()
    {
    }
}