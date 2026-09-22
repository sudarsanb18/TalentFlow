package menu;

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
        System.out.println("Starting TalentFlow ATS Engine...");
        Menu menu = new Menu();
        menu.displayMenu();
    }
}
