package TestCode;

import Bus.*;
import Message.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import mux.*;

/**
 * Integration Test Harness
 * - Launches BOTH the Command Center GUI and the Passenger Device (PFD) GUI in subprocesses
 * - Starts BuildingMultiplexor and 4 ElevatorMultiplexor instances (in PFD or locally as fallback)
 * - Provides a SoftwareBus client from this JVM to publish system start messages
 *
 * Run from your IDE or command line. Both GUI windows will appear automatically.
 */
public class TestHarness {

    public static void main(String[] args) throws Exception {

        String javaCmd = System.getProperty("java.home") + "\\bin\\java";
        String classpath = System.getProperty("java.class.path");

        System.out.println("Starting Command Center (server) in subprocess...");
        ProcessBuilder pb = new ProcessBuilder(javaCmd, "-cp", classpath, "CommandCenter.ElevatorControlSystem");
        pb.redirectErrorStream(true);
        Process guiProc = pb.start();

        // Capture GUI process output into a buffer and print it
        List<String> guiLines = java.util.Collections.synchronizedList(new ArrayList<>());
        Process finalGuiProc = guiProc;
        Thread outReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(finalGuiProc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    guiLines.add(line);
                    System.out.println("[GUI] " + line);
                }
            } catch (Exception ignored) {}
        });
        outReader.setDaemon(true);
        outReader.start();

        // Also launch passenger device GUI (pfdGUI) so elevator windows appear
        System.out.println("Starting Passenger Device GUI in subprocess...");
        ProcessBuilder pfdPb = new ProcessBuilder(javaCmd, "-cp", classpath, "pfdGUI.gui");
        pfdPb.redirectErrorStream(true);
        Process pfdProc = pfdPb.start();

        List<String> pfdLines = java.util.Collections.synchronizedList(new ArrayList<>());
        Thread pfdOut = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(pfdProc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    pfdLines.add(line);
                    System.out.println("[PFD] " + line);
                }
            } catch (Exception ignored) {}
        });
        pfdOut.setDaemon(true);
        pfdOut.start();

        // Wait for the server socket to open
        Thread.sleep(500);

        // Wait up to 6 seconds for the Command Center to announce readiness
        boolean guiReady = false;
        for (int i = 0; i < 60; i++) {
            synchronized (guiLines) {
                for (String l : guiLines) if (l.contains("COMMAND_CENTER_READY")) guiReady = true;
            }
            if (guiReady) break;
            Thread.sleep(100);
        }

        if (!guiReady) {
            System.err.println("Command Center did not signal ready. Dumping recent GUI output:");
            synchronized (guiLines) {
                int start = Math.max(0, guiLines.size() - 50);
                for (int i = start; i < guiLines.size(); i++) System.err.println("[GUI] " + guiLines.get(i));
            }
            // Try restarting the Command Center once
            System.out.println("Attempting to restart Command Center once...");
            try {
                guiProc.destroyForcibly();
            } catch (Exception ignored) {}
            guiProc = pb.start();
            Process finalGuiProc1 = guiProc;
            Thread restartReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(finalGuiProc1.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        guiLines.add(line);
                        System.out.println("[GUI-restart] " + line);
                    }
                } catch (Exception ignored) {}
            });
            restartReader.setDaemon(true);
            restartReader.start();

            // wait again briefly
            for (int i = 0; i < 60; i++) {
                synchronized (guiLines) {
                    for (String l : guiLines) if (l.contains("COMMAND_CENTER_READY")) guiReady = true;
                }
                if (guiReady) break;
                Thread.sleep(100);
            }

            if (!guiReady) System.err.println("Command Center failed to start after restart attempt.");
        }

     
        List<ElevatorMultiplexor> elevMuxes = new ArrayList<>();
        boolean startedLocalMuxes = false;
        try {
            Thread.sleep(1000); // brief pause for subprocess to initialize
            if (pfdProc == null || !pfdProc.isAlive()) {
                System.out.println("PFD subprocess not running - starting local BuildingMultiplexor and ElevatorMultiplexor instances...");
                // Start Building MUX locally
                BuildingMultiplexor buildingMux = new BuildingMultiplexor();
                // Start 4 elevator MUXes locally
                for (int i = 1; i <= 4; i++) {
                    elevMuxes.add(new ElevatorMultiplexor(i));
                }
                startedLocalMuxes = true;
            } else {
                System.out.println("PFD subprocess running - skipping local MUX startup (PFD will initialize them)");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Use a SoftwareBus client from this JVM to drive tests
        SoftwareBus bus = new SoftwareBus(false);

        System.out.println("Publishing System Start...");
        bus.publish(new Message(SoftwareBusCodes.systemStart, 0, 0));
        Thread.sleep(1000);


        System.out.println("System started. Interact via Command Center or Passenger GUI to drive elevators.");
        Thread.sleep(1000);

        System.out.println("Test complete. Leaving processes running for inspection.");
        System.out.println("If you want to stop the GUI subprocess, terminate the process manually.");
    }
}