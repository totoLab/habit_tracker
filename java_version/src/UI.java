import java.util.*;
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
		MenuOption[] options =  MenuOption.values();
		for (int i = 0; i < options.length; i++) {
			System.out.println((i + 1) + ") " + options[i]);
		}
	}
	
}
