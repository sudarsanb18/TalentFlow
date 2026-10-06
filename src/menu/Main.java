package menu;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Main application class and entry point for TalentFlow ATS.
 * Executes the console interactive menu.
 */
public class Main {

    /**
     * Main method initiating TalentFlow Recruitment System.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        enableUnicodeConsole();
        System.out.println("Starting TalentFlow ATS Engine...");
        Menu menu = new Menu();
        menu.displayMenu();
    }

    /**
     * Prints the tick / cross / warning symbols correctly. On Windows the console normally uses an old code page,
     * so Java turns these symbols into '?'. Switching the console to UTF-8 and writing UTF-8 fixes that.
     */
    private static void enableUnicodeConsole() {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "chcp 65001 > nul").inheritIO().start().waitFor();
            }
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // keep the default console if it cannot be switched
        }
    }
}
