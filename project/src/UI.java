import java.util.*;
import java.time.*;
public class UI {
	
	static MenuOption mainMenuChoice() {
		MenuOption[] options = MenuOption.values();
		
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("Enter your choice: ");
			try {
				int choice = input.nextInt();
				int last = options.length;
				if (choice >= 1 && choice <= last) {
					return options[choice - 1];
				} else {
					System.out.println("Invalid choice, please enter a number between 1 and " + last);
				}
			} catch (InputMismatchException e) {
				System.out.println("Invalid choice, please enter a number");
				input.nextLine();
			}
		}
	}

	static boolean yesOrNo(String prompt) {
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.println(prompt + " " + "[y/n]");
			String choice = input.next().toUpperCase();
			return choice.contains("Y");
		}
	}

	static void displayMainMenu() {
		StringBuilder sb = new StringBuilder();
		sb.append("Menu:\n");
		MenuOption[] options =  MenuOption.values();
		for (int i = 0; i < options.length; i++) {
			sb.append((i + 1) + ") " + options[i] + "\n");
		}
		System.out.println(sb.toString().strip());
	}
	
	static String enterDay() {
		StringBuilder sb = new StringBuilder();
		Integer year = enterNumber(1970, 4000, "year");
		sb.append(year.toString());
		sb.append("-");
		Integer month = enterNumber(1, 12, "month");
		sb.append( (month < 10) ?
				"0" + month.toString() :
				month.toString()
		);
		sb.append("-");

		LocalDate test = LocalDate.parse(sb.toString() + "01", Tracker.globalFormatter);
		int maxDays = test.lengthOfMonth();
		Integer day = enterNumber( 1, maxDays, "day" );
		test = test.withDayOfMonth(day);
		return test.toString();
	}
	
	private static Integer enterNumber(int first, int last, String prompt) {
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("Enter your choice for the " + prompt + ": ");
			try {
				int choice = input.nextInt();
				if (choice >= first && choice <= last) {
					return choice;
				} else {
					System.out.println("Invalid choice, please enter a number between " + first + " and " + last);
				}
			} catch (InputMismatchException e) {
				System.out.println("Invalid choice, please enter a number");
				input.nextLine();
			}
		}
	}
	
}
