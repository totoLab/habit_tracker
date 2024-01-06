import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Tracker {
	
	private TreeMap<LocalDate, Boolean> db;
	private File filePath;
	
	static DateTimeFormatter globalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale( Locale.ITALIAN );

	int MAX_LENGHT_AFTER_PADDING = 9;

	public Tracker(String path) {
		File filePath = new File(path);
		this.filePath = filePath;
		
//		if (!filePath.canWrite()) {
//			throw new IllegalArgumentException("File cannot be re/written, choose another path or execute with higher privileges.");
//		}
		
		if (filePath.exists()) {
			db = restore(filePath);
			System.out.println("Restoring tracker from " + path);
		} else {
			db = new TreeMap<>();
			System.out.println("Creating new tracker " + path);
		}
		
	}
	
	public TreeMap<LocalDate, Boolean> getDb() {
		return new TreeMap<>(db);
	}
	
	// ------------ save / restore database ------------
	public void save() {
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		try {
			mapper.writeValue(
					filePath, db);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("No file found");
		}
	}
	
	public TreeMap<LocalDate, Boolean> restore(File file) { // TreeMap<Date, Boolean> 
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		TreeMap<String, Boolean> value = null;
		try {
			value = mapper.readValue(file, TreeMap.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("No file found");
			System.exit(1);
		}
		return castStringToLocalDate(value);
	}
	
	private TreeMap<LocalDate, Boolean> castStringToLocalDate(TreeMap<String, Boolean> toCast) {
			
		TreeMap<LocalDate, Boolean> ret = new TreeMap<>();
		for (String key : toCast.keySet()) {
			LocalDate date = LocalDate.parse(key, globalFormatter);
			ret.put(date, toCast.get(key));
		}
		
		return ret;
	}

	// ------------ new entries ------------
	void fillDay(String day, boolean value) {
		LocalDate date = LocalDate.parse(day, globalFormatter);
		fillDay(date, value);
	}
	
	private void fillDay(LocalDate day, boolean value) {
		if (db.containsKey(day)) {
			System.out.println("Overwriting value for date " + day);
			// throw new IllegalStateException("Value already exists");
		}
		db.put(day, value);
	}
	
	void fillToday(boolean value) {
		LocalDate today = getTodayDate();
		fillDay(today, value);		
	}
	
	// ------------ dates ------------
	private Comparator<LocalDate> ldc = (o1, o2) -> {
		if ( o1.isAfter(o2) ) return 1;
		else if ( o1.isBefore(o2) ) return -1;
		return 0;
	};
	
	private static LocalDate getTodayDate() {
		return LocalDate.now();
	}
	
	private int daysOfTheMonth(LocalDate date) {
		return date.getMonth()
				.length( date.isLeapYear() );
	}
	
	private List<LocalDate> totalGoodDays() {
		return goodDaysOverRange(
				earliestDayDistance() - 1
				);
	}
	
	/*
	 * @return list of good days from date passed as parameter
	 */
	private List<LocalDate> goodDaysOverRange(LocalDate base, LocalDate upper) {
		return db.keySet().stream()
				.filter( entry -> db.get(entry) )
				.filter( entry -> entry.isEqual(base) || ( entry.isAfter(base) && entry.isBefore(upper.plusDays(1)) ))
				.sorted( ldc )
				.collect( Collectors.toList() );
	}
	
	/*
	 * @return list of good days from today's date
	 */
	private List<LocalDate> goodDaysOverRange(int days) {
		LocalDate base = getTodayDate().minusDays(days);
		return goodDaysOverRange(base, getTodayDate());
	}
	
	private List<Integer> getConsecutives() {
		List<LocalDate> goodDays = goodDaysOverRange(
				earliestDayDistance() - 1
				);
		List<Integer> ret = new ArrayList<>();
		
		if (goodDays.size() < 2) {
			ret.add(goodDays.size());
			return ret;
		}
		
		Iterator<LocalDate> it = goodDays.iterator();
		LocalDate current = it.next();
		int count = 0;
		while (it.hasNext()) {
			LocalDate next = it.next();
			count += 1;
			if (!current.plusDays(1).equals(next)) {
				ret.add(count);
				count = 0;
			}
			current = next;
		}
		count++;
		ret.add(count);
		return ret;
	}

	private int goodDaysPerMonth(LocalDate date) {
		int days = date.lengthOfMonth();
		LocalDate base = date.withDayOfMonth(1);
		return goodDaysOverRange(base, base.plusDays(days - 1)).size();
	}
	
	private List<Integer> yearSearchSpace() {
		Set<LocalDate> keys = db.keySet();
		if (keys.size() < 1) throw new IllegalStateException();

		Supplier<Stream<LocalDate>> dates = () -> keys.stream();
		LocalDate min = dates.get().min(ldc).orElseThrow();
		LocalDate max = dates.get().max(ldc).orElseThrow();
		
		LinkedList<Integer> yearRange = new LinkedList<>();
		for (int i = min.getYear(); i <= max.getYear(); i++) {
			yearRange.add(i);
		}
		return yearRange;
	}
	
	private int earliestDayDistance() {
		return (int) db.keySet().stream().min(ldc).orElseThrow()
				.until(getTodayDate(), ChronoUnit.DAYS);
	}
	
	private int latestDayDistance() {
		return (int) db.keySet().stream().max(ldc).orElseThrow()
				.until(getTodayDate(), ChronoUnit.DAYS);
	}
	
	// ------------ statistics ------------
    DecimalFormat formatter = new DecimalFormat("##.00");
    
	public String getStats() {		
		if (db.keySet().size() == 0) {
			return "No data is filled in";
		}
		
		List<String> stats = new ArrayList<>();
		
		// totals
		Integer total =  db.keySet().size();
		stats.add(mapLike("total", total.toString()));
		
		Integer good = totalGoodDays().size();
		stats.add(mapLike("good days", good.toString()));
		
		Integer bad = total - good;
		stats.add(mapLike("bad days", bad.toString()));

		// good over ranges
		int maxTimeRange = earliestDayDistance();
		for(TimeRange tr : TimeRange.values()) {
			TimeRangeParser trp = new TimeRangeParser(tr, total);
			String descriptor = trp.getIterations() + trp.getType();
			int time = trp.getValue();
			if (time <= maxTimeRange) {
				int goodDays = goodDaysOverRange(time - 1).size();
				Double ratio = (double) goodDays * 100 / time;
				
				int badDays = time - goodDays;
				String comparison = "(" + goodDays +"," + badDays + ")";
				
				stats.add(mapLike(
					descriptor + " good/bad ratio",
					formatter.format(ratio) + "% " + comparison
				));
			}
		}
		
		// streaks
		Supplier<Stream<Integer>> consecutives = () -> getConsecutives().stream();
		Integer current = 0;
		if (goodDaysOverRange(latestDayDistance()).size() >= 1) {
			current = consecutives.get().mapToInt(o->o)
				.reduce((first, second) -> second) // recursively dumping "first" to get the last element
				.getAsInt();
		}
		stats.add(mapLike(
			"current streak",
			current.toString()
		));
		Double average = (double) consecutives.get().mapToInt(o->o).average().getAsDouble();
		stats.add(mapLike(
			"average streak",
			formatter.format(average)
		));
		int[] orderedConsecutives = consecutives.get().mapToInt(o->o).sorted().toArray();
		Integer median = orderedConsecutives[orderedConsecutives.length / 2];
		stats.add(mapLike(
			"median streak",
			formatter.format(median)
		));
		Integer max = consecutives.get().mapToInt(o->o).max().orElseThrow();
		stats.add(mapLike(
			"max streak",
			max.toString()
		));
		return statsListToString(stats);
	}
	
	private String mapLike(String key, String value) {
		return key + ": " + value;
	}
	
	private String statsListToString(List<String> stats) {
		StringBuilder sb = new StringBuilder();
		for (String field : stats) {
			sb.append(field);
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	// ------------ string manipulation ------------
	private String monthToString(LocalDate date) {
		StringBuilder sb = new StringBuilder();
		Month month = date.getMonth();
		sb.append(padStringLeft(month.toString(), MAX_LENGHT_AFTER_PADDING));
		sb.append(" | ");
		for (int day = 1; day <= daysOfTheMonth(date); day++) {
			sb.append(padStringLeft(String.valueOf(day), 2));
			addSpaces(sb, 1);
		}
		return sb.toString();
	}
	
	private void addSpaces(StringBuilder sb, int padding) {
		sb.append("\s".repeat(padding));
	}
	
	private String padStringLeft(String s, int lenght) {
		int n = s.length();
		int padding = lenght - n;
		if (padding < 0 ) throw new IllegalArgumentException("Too little max lenght");
		return s + "\s".repeat(padding);
	}
	
	// ------------ object methods ------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		Set<LocalDate> keys = db.keySet();
		if (keys.size() < 1) return "Empty database";
		
		Supplier<Stream<LocalDate>> dates = () -> keys.stream();
		LocalDate current = dates.get().min(ldc).orElseThrow().withDayOfMonth(1);
		boolean firstMonth = true;
		LocalDate max = dates.get().max(ldc).orElseThrow();
		
		while ( !current.equals(max.plusDays(1)) ) {
			if (current.getDayOfMonth() == 1) {
				sb.append("\n");
				
				int month = current.getMonthValue();
				if (firstMonth || month == 1) {
					addSpaces(sb, MAX_LENGHT_AFTER_PADDING);
					sb.append(" |\n");
					sb.append(
							padStringLeft(
									String.valueOf(current.getYear()), 
									MAX_LENGHT_AFTER_PADDING)
					);
					firstMonth = false;
				} else {
					addSpaces(sb, MAX_LENGHT_AFTER_PADDING);
				}
				sb.append(" |\n");
				sb.append(monthToString(current));
				sb.append("\n");
				
				int goodOfMonth = goodDaysPerMonth(current);
				sb.append(
						padStringLeft(
								" " + goodOfMonth + "/" + current.lengthOfMonth(),
								MAX_LENGHT_AFTER_PADDING)
				);
				
				sb.append(" | ");
			}
			checkAndAddEmoji(sb, current);
			current = current.plusDays(1);
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	private void checkAndAddEmoji(StringBuilder sb, LocalDate date) {
		if (db.containsKey(date)) {
			if (db.get(date)) sb.append("✅");
			else sb.append("❌");
		} else {
			addSpaces(sb, 2);
		}
		addSpaces(sb, 1);
	}
	
	public static void main(String[] args) {
		if (args.length < 1) throw new IllegalArgumentException("Not enough arguments, provide a filepath for the tracker.");
		String filepath = args[0];
		Tracker tracker = new Tracker(filepath);

		while (true) {
			UI.displayMainMenu();
			MenuOption choice = UI.mainMenuChoice();

			switch (choice) {
			case today:
				try {
					tracker.fillToday( UI.yesOrNo( Tracker.getTodayDate().toString() ) );
				} catch (QuitPrompt e) { }
				break;
			case fillDay:
				String userDate;
				try {
					userDate = UI.enterDay();
					tracker.fillDay(userDate, UI.yesOrNo( userDate ));
				} catch (QuitPrompt e) { }
				break;
			case stats:
				System.out.println("Tracker statistics:\n" + tracker.getStats());
				break;
			case print:
				System.out.println("Tracker calendar:\n" + tracker.toString());
				break;
			case exitSaving:
				System.out.println("Saving current changes...");
				tracker.save();
				System.out.println("Correctly saved changes.");
			case exitNotSaving:
				System.out.println("Exiting...");
				System.exit(0);
				break;
			}
		}
	}
	
}

