import java.io.*;
import java.util.*;
public class UI {
	
	private static  boolean TESTING = true;

	enum MenuOption {today, print, stats, exit};
		
	static MenuOption getUserChoice() {
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("Enter your choice: ");
			try {
				int choice = input.nextInt();
				if (choice >= 1 && choice <= 5) {
					return MenuOption.values()[choice-1];
				} else {
					System.out.println("Invalid choice, please enter a number between 1 and 5");
				}
			} catch (InputMismatchException e) {
				System.out.println("Invalid choice, please enter a number between 1 and 5");
				input.nextLine();
			}
		}
	}

	public static void main(String[] args) {
		if (TESTING) {
			args = new String[1];
			args[0] = "test.json";
		}
		
		if (args.length < 1) throw new IllegalArgumentException("no argument were given");
		String filepath = args[0];
		Tracker tracker = new Tracker(filepath);

		while (true) {
			// displayMenu(); // TODO
			MenuOption choice = getUserChoice();

			switch (choice) {
			case today:
				tracker.fillToday(
						true // TODO: refactor getUserChoice() to pass available options and reuse it for every menu
				);
				break;
			case stats:
				System.out.println("Tracker statistics:\n" + tracker.getStats());
				break;
			case print:
				System.out.println("Tracker calendar:\n" + tracker.toString());
				break;
			case exit:
				System.out.println("Saving current changes...");
				tracker.save();
				System.out.println("Correctly saved changes.");
				System.out.println("Exiting...");
				System.exit(0);
			}
		}
	}
}
