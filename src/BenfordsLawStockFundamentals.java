import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;


public class BenfordsLawStockFundamentals {
	
	static File fundamentalsDir = new File("/Users/brianrisk/Downloads/ZFB-complete");
	
	int talliedIndicatorCountThreshold = 40;
	
	public static void main(String [] args) {
		new BenfordsLawStockFundamentals();
		U.p("done");
	}
	
	public BenfordsLawStockFundamentals() {
		ArrayList<Filing> filings = loadFilings();
		U.p("number of filings: " + filings.size());
		int onePercent = filings.size() / 1000;
		
		// calculate theoretical benfords
		Random random = new Random();
		for (Filing filing: filings) {
			filing.createFakeBenford(random.nextLong());
		}
		
		// report on theoretical benford
		Filing.sortBy = Filing.SORT_BY_THEORETICAL_BENFORD;
		Collections.sort(filings);
		double theoreticalOnePercent = filings.get(filings.size() - onePercent).theoreticalBenfordScore;
		U.p("Theoretical 1% score cutoff: " + theoreticalOnePercent);
		
		// report on empirical benford
		Filing.sortBy = Filing.SORT_BY_BENFORD;
		Collections.sort(filings);
		U.p("Emperical 1% score cutoff: " + filings.get(filings.size() - onePercent).benfordScore);
		
		// if we use the theoretical cutoff, what percent do we find for the empirical distribution?
		for (int index = 0; index < filings.size(); index++) {
			Filing filing = filings.get(index);
			if (filing.benfordScore > theoreticalOnePercent) {
				int total = filings.size() - index;
				double percent = (double) total / filings.size();
				U.p("Emperical % found at the theoretical 1%: " + percent);
				break;
			}
		}
		
		
		ArrayList<Filing> outlierFilings = new ArrayList<Filing>(onePercent);
		for (int index = filings.size() - onePercent; index < filings.size(); index++) {
			outlierFilings.add(filings.get(index));
		}
		
		HashMap<String, Company> companies = new HashMap<String, Company>();
		for (Filing filing: outlierFilings) {
			Company company = companies.get(filing.symbol);
			if (company == null) company = new Company(filing.symbol);
			company.addDate(filing.filingDate);
			companies.put(filing.symbol, company);
		}
		
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("benford-outlier-ranges.csv"));
			pw.println("Ticker,start_date,end_date");
			for (Company company: companies.values()) {
				pw.println(company);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	@SuppressWarnings("unused")
	private void printHistograms(ArrayList<Filing> filings) {
		HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
		for (Filing filing: filings) {
			double key = filing.benfordScore;
			key *= 100;
			key = Math.round(key);
			Integer tally = histogram.get((int) key);
			if (tally == null) tally = 0;
			tally++;
			histogram.put((int) key, tally);
		}
		
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("score-histogram.tsv"));
			for (int index = 0; index <= 100; index++) {
				Integer tally = histogram.get(index);
				if (tally == null) tally = 0;
				pw.println(index + "\t" + tally);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		//create fake benford scores
		Random random = new Random();
		for (Filing filing: filings) {
			filing.createFakeBenford(random.nextLong());
		}
		
		HashMap<Integer, Integer> fakeHistogram = new HashMap<Integer, Integer>();
		for (Filing filing: filings) {
			double key = filing.theoreticalBenfordScore;
			key *= 100;
			key = Math.round(key);
			Integer tally = fakeHistogram.get((int) key);
			if (tally == null) tally = 0;
			tally++;
			fakeHistogram.put((int) key, tally);
		}
		
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("fake-score-histogram.tsv"));
			for (int index = 0; index <= 100; index++) {
				Integer tally = fakeHistogram.get(index);
				if (tally == null) tally = 0;
				pw.println(index + "\t" + tally);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private ArrayList<Filing> loadFilings() {
		ArrayList<Filing> filings = new ArrayList<Filing>();
		try {
			// load data
			File [] fundamentalsFiles = fundamentalsDir.listFiles();
			for (File fundamentalsFile: fundamentalsFiles) {

				// get next file if this is not a CSV
				if (!fundamentalsFile.getName().endsWith(".csv")) continue;

				// create reader for the file
				BufferedReader br = new BufferedReader(new FileReader(fundamentalsFile));

				// create a lookup table for header indices
				String line = br.readLine();
				String [] headers = split(line);
				HashMap<String, Integer> indices = new HashMap<String, Integer>();
				for (int index = 0; index < headers.length; index++) {
					indices.put(headers[index], index);
				}
				
				// parse filing data
				line = br.readLine();
				while (line!= null) {
					Filing filing = new Filing(split(line), indices);

					if (filing.isValid && filing.isQuarterlyFiling && filing.talliedIndicatorCount >= talliedIndicatorCountThreshold) {
						filings.add(filing);
					}
					// read next
					line = br.readLine();
				}

				// closing the input stream
				br.close();


			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filings;
	}
	
	private static String [] split(String s) {
		return s.replace('|', '\t').split("\t");
	}

}
