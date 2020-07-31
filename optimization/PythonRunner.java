package optimization;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PythonRunner {

    public final File pythonPath;
    public final File pythonFile;
    public final File runner;
    private Process pythonProcess;
    private AtomicBoolean threadShouldStop = new AtomicBoolean(false);

    public PythonRunner(File pythonPath, File pythonFile) {
        this.pythonFile = pythonFile;
        this.pythonPath = pythonPath;
        this.runner = new File("IPC/runner.sh");
        createPythonRunner();
    }

    /**
     * In order to send fatal python errors back to Java, a temporary python script is created. The target python code
     * is injected inside a try/except statement. The exception will generate a traceback and the $ISSUE channel will
     * be notified. However, the traceback line numbers will not be accurate, so the temporary file is not deleted for
     * debugging purposes.
     */
    private void createPythonRunner() {
        try {
            File runner = new File(pythonFile.getParentFile().getAbsolutePath(), ".__TAIL_runner__.py");
            runner.createNewFile();
            FileWriter writer = new FileWriter(runner.getAbsolutePath());
            BufferedReader reader = new BufferedReader(new FileReader("IPC/__runner__.py"));
            BufferedReader inject = new BufferedReader(new FileReader(pythonFile.getAbsolutePath()));
            String line;
            StringBuilder code = new StringBuilder();
            StringBuilder imports = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("#INJECT")) {
                    String injected_line;
                    while ((injected_line = inject.readLine()) != null) {
                        if (injected_line.startsWith("from") || injected_line.startsWith("import")) {
                            imports.append(injected_line).append(System.lineSeparator());
                        } else {
                            code.append("    ").append(injected_line).append(System.lineSeparator());
                        }
                    }
                } else {
                    if (!line.startsWith("#")) {
                        code.append(line).append(System.lineSeparator());
                    }
                }
            }
            writer.write(imports.toString());
            writer.write(code.toString());
            inject.close();
            reader.close();
            writer.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }


    /**
     * Passes the python environment and the python file as arguments to a bash file. This is necessary so 3rd party
     * Python packages are resolved. For example, gym, numpy, pandas.
     */
    public void run() {
        try {
            String pythonExe;
             if (pythonPath == null)
             {
                 pythonExe = "python";
             } else
             {
                 pythonExe = pythonPath.getAbsolutePath();
             }

             String py = pythonFile.getParentFile().getAbsolutePath() + "/.__TAIL_runner__.py";

//             ProcessBuilder builder = new ProcessBuilder(runner.getAbsolutePath(), pythonExe, py);
//             builder.redirectErrorStream(true);
//             pythonProcess = builder.start();

            File out = new File("stdout"); // File to write stdout to
            File err = new File("stderr"); // File to write stderr to
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File("test"));
            builder.command(runner.getAbsolutePath(), pythonExe, py);
            builder.redirectOutput(out); // Redirect stdout to file
            if(out == err) {
                builder.redirectErrorStream(true); // Combine stderr into stdout
            } else {
                builder.redirectError(err); // Redirect stderr to file
            }
            pythonProcess = builder.start();



//             pythonProcess = Runtime.getRuntime().exec(new String[] {runner.getAbsolutePath(), pythonExe, py});
//             Thread inThread = new Thread(()->{
//                 InputStream in = pythonProcess.getInputStream();
//                 BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
//                 String line = null;
//                 while (!threadShouldStop.get())
//                 {
//                     try
//                     {
//                         line = inReader.readLine();
//                         if (line != null) {
//                             System.out.println(line);
//                         }
//                     } catch (IOException e) {
//                         System.out.println("asdfasdf");
//                         e.printStackTrace();
//                         break;
//                     }
//                     System.out.flush();
//                 }
//             });
//             inThread.setName("TAIL_READER");
////             inThread.join();
//             inThread.start();
//
//            Thread closeChildThread = new Thread() {
//                public void run() {
//                    System.out.println("DONE PYTHONRUNNER");
//                    threadShouldStop.set(true);
//                    pythonProcess.destroyForcibly();
//                }
//            };
//
//            Runtime.getRuntime().addShutdownHook(closeChildThread);


        } catch (Exception err)
        {
            pythonProcess = null;
            err.printStackTrace();
        }
    }

    public void destroy() {
        if (pythonProcess != null) {
            threadShouldStop.set(true);
            pythonProcess.destroy();
        }
    }
}
