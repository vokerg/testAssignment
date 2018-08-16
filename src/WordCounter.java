import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Title: Description: The application reads the text file tempest.txt from the
 * application directory, counts unique words and lists the top 10 occurrences
 * 
 * @author Dmitri.Grecov
 * 
 */

public class WordCounter {

	private static final int TOP_WORDS = 10;
	private static final String REGEX_SPLIT = "[&.,:;!?\\s\\t\\[\\]]";
	private static final String FILENAME = "tempest.txt";
	private static final String ERROR_CANNOT_BE_READ = "File cannot be read";

	/**
	 * pass "parallel" to use multi-threaded file parser. Leave parameters blank to use single-thread parser
	 */
	public static void main(String[] args) throws IOException {
		boolean isParallel = ((args.length > 0) && (args[0].equals("parallel")));
		new WordCounter().start(isParallel);
	}

	private void start(boolean isParallel) {
		long startTime = System.currentTimeMillis();
		this.execute(isParallel ? parallelFileReader : singleThreadFileReader);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Executed in " + elapsedTime + " milliseconds");
	}

	/**
	 * The method processes the words map and displays 10 most common unique words
	 * in the text
	 * 
	 * @param wordsMapSupplier
	 *            supplies with map with word as a key and number of entries as a
	 *            value
	 */
	public void execute(Supplier<Map<String, Integer>> wordsMapSupplier) {
		Map<String, Integer> wordsMap = wordsMapSupplier.get();
		getMapTopValues(wordsMap, TOP_WORDS).forEach(value -> System.out.println(value));
	}

	private volatile Map<String, Integer> map = new HashMap<String, Integer>();

	private Supplier<Map<String, Integer>> parallelFileReader = () -> {
		Path path = Paths.get(FILENAME);
		try {
			Files.readAllLines(path).parallelStream()
					.forEach(line -> Arrays.stream(line.toLowerCase().split(REGEX_SPLIT)).forEach(word -> {
						if (!word.isEmpty()) {
							synchronized (map) {
								map.put(word, map.containsKey(word) ? map.get(word) + 1 : 1);
							}
						}
					}));
		} catch (IOException e) {
			System.out.println(ERROR_CANNOT_BE_READ);
		}
		return map;
	};

	private Supplier<Map<String, Integer>> singleThreadFileReader = () -> {
		Path path = Paths.get(FILENAME);
		Map<String, Integer> wordMap = new HashMap<String, Integer>();
		try {
			Files.readAllLines(path).stream()
					.forEach(line -> Arrays.stream(line.toLowerCase().split(REGEX_SPLIT)).forEach(word -> {
						if (!word.isEmpty()) {
							wordMap.put(word, wordMap.containsKey(word) ? wordMap.get(word) + 1 : 1);
						}
					}));
		} catch (IOException e) {
			System.out.println(ERROR_CANNOT_BE_READ);
		}
		return wordMap;
	};

	/**
	 * 
	 * The method returns first values of the map as a list of strings in the format
	 * "word (number_of_entries)" The number of returned string is defined by int
	 * parameter.
	 * 
	 * @param Map<String,
	 *            Integer>
	 * @param int
	 * @return List<String>
	 */
	private List<String> getMapTopValues(Map<String, Integer> map, int i) {
		Map<String, Integer> sortedMap = getSortedMap(map);
		return sortedMap.entrySet().stream().limit(i).map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
				.collect(Collectors.toList());
	}

	/**
	 * The method sorts map by the value using stream.sorted and collects it back to
	 * LinkedHashMap
	 * 
	 * @param Map<String,
	 *            Integer>
	 * @return Map<String, Integer>
	 */
	private Map<String, Integer> getSortedMap(Map<String, Integer> map) {
		return map.entrySet().stream().sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
	}
}
