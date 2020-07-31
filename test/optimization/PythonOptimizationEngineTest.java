package test.optimization;

import optimization.PythonOptimizationEngine;
import optimization.PythonOptimizationEngineOld;
import optimization.PythonRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import state.StateSpaceManager;
import test.state.ActionEnumerationTest;
import test.state.PythonActionTest;
import test.state.PythonStateTest;
import test.state.StateEnumerationTest;

import java.io.*;
import java.nio.file.Files;

import static org.junit.Assert.*;

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

    private void registerActionsAndStates()
    {
        manager.registerActionEnumeration(ActionEnumerationTest.Action1.ONE);
        manager.registerActionEnumeration(ActionEnumerationTest.Action2.TWO);
        manager.registerStateEnumeration(StateEnumerationTest.COMBAT_POWER.BETTER);
    }

    @Test
    public void pythonOptimizationEngine()
    {
        System.out.println("pythonOptimizationEngine");
        PythonOptimizationEngine instance1 = new PythonOptimizationEngine(folder, py, python);
        registerActionsAndStates();
    }

    /**
     * Ensure selectNextState is random.
     */
    @Test
    public void selectNextState() throws InterruptedException, IOException {
//        System.out.println("selectNextState");
//        PythonOptimizationEngine instance = new PythonOptimizationEngine(folder, py, python);
//        registerActionsAndStates();
//        Thread mainRunner = new Thread(()->{
//            instance.selectNextState();
//            System.out.println("after seelect next state");
//        });
//        mainRunner.setName("TAIL_MAIN_RUNNER");
//        mainRunner.start();
//
//        Thread.sleep(5000);
//        System.out.println(String.format("%b, %b", mainRunner.isDaemon(), mainRunner.isAlive()));
//        assertSame(Thread.State.RUNNABLE, mainRunner.getState());
////        mainRunner.d
//        mainRunner.interrupt();
//
////        Thread mainRunner = new Thread(()->{
////            PythonRunner p = new PythonRunner(python, new File("test/python/Test_Infinite.py"));
////            p.run();
////            System.out.println("after seelect next state");
////        });
////        mainRunner.start();
////
////        Thread.sleep(5000);

        ProcessBuilder builder = new ProcessBuilder();
        builder.inheritIO();
        builder.command(
                new File("IPC/runner.sh").getAbsolutePath(),
                new File("test/python/venv/bin/python3").getAbsolutePath(),
                new File("test/python/.__TAIL_runner__.py").getAbsolutePath()
        );
        Process p = builder.start();
        Thread.sleep(5000);

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