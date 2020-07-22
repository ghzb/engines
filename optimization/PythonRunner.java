package optimization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class PythonRunner {

    private final File pythonPath;
    private final File pythonFile;
    private Process pythonProcess;

    public PythonRunner(File pythonPath, File pythonFile) {
        this.pythonFile = pythonFile;
        this.pythonPath = pythonPath;
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
             pythonProcess = Runtime.getRuntime().exec(new String[] {
                    new File("IPC/runner.sh").getAbsolutePath(),
                    pythonPath.getAbsolutePath(),
                    pythonFile.getParentFile().getAbsolutePath() + "/.__TAIL_runner__.py"});
        } catch (Exception err)
        {
            pythonProcess = null;
            err.printStackTrace();
        }
    }

    public void destroy() {
        if (pythonProcess != null) {
            pythonProcess.destroy();
        }
    }
}
