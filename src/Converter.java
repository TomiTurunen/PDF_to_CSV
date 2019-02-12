
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Converter {

	final int DEFAULT_ACCOUNT_NUMBER = 3000;
	final int COUNTER_ACCONT_NUMBER = 1700;
	final String COUNTER_TRANSACTION_NAME = "Counter transaction";
	final String FIND_START_POSITION_STRING = "Product Amount Unit Unit Price Excluding VAT VAT-%";
	final String FIND_END_POSITION_STRING = "Total Excluding VAT:";
	final String FIND_INVOICE_TOTAL_STRING = "Invoice Total: ";

	public static void main(String args[]) throws IOException {

		Converter converter = new Converter();
		File pdfFile = null;
		Scanner reader = new Scanner(System.in);
		while (pdfFile == null || !pdfFile.exists()) {
			System.out.println("Give PDF file, that you want convert: ");
			String fileString = "";

			try {
				fileString = reader.nextLine();
				pdfFile = new File(fileString);
				if (!pdfFile.exists()) {
					System.out.println("File not found. Try again.");
				}
			} catch (NoSuchElementException e) {
				System.out.println("File not found. Try again.");
			}
		}
		reader.close();

		String pdfInTextFormat = converter.convertGivenPDFToText(pdfFile);

		// Calculate invoiceTotal
		Double invoiceTotal = Double.parseDouble(pdfInTextFormat.substring(pdfInTextFormat.indexOf(converter.FIND_INVOICE_TOTAL_STRING)
						+ converter.FIND_INVOICE_TOTAL_STRING.length(), pdfInTextFormat.length()-2).split(" ")[0]);

		// Get row datas to Array
		String usableRowsString = pdfInTextFormat.substring(
				pdfInTextFormat.indexOf(converter.FIND_START_POSITION_STRING)
				+ converter.FIND_START_POSITION_STRING.length(),
				pdfInTextFormat.indexOf(converter.FIND_END_POSITION_STRING));
		
		// End of line is for removing empty rows
		String[] CSVRowsArray = Arrays.asList(usableRowsString.split("[\\r\\n]+")).stream()
				.filter(str -> !str.isEmpty()).collect(Collectors.toList()).toArray(new String[0]);

		List<CSVRow> CSVRowList = converter.getCSVRowList(CSVRowsArray);

		// Create csv file
		converter.doCSVFile(CSVRowList, invoiceTotal, pdfFile);

	}

	private String convertGivenPDFToText(File pdfFile) throws IOException {
		// Loading an existing document
		PDDocument document = PDDocument.load(pdfFile);

		// Retrieving text from PDF document using PDFTextStripper
		PDFTextStripper pdfStripper = new PDFTextStripper();
		String text = pdfStripper.getText(document);
		document.close();
		return text;
	}

	private List<CSVRow> getCSVRowList(String[] CSVRowsArray) {
		// Set Rows to list of CSVRow type objects
		List<CSVRow> CSVRowList = new ArrayList<>();
		for (String row : CSVRowsArray) {
			String[] contentArray = row.split(" ");
			if (contentArray.length < 6) {
				// Skip invalid rows
				System.out.println("Row is not valid:" + row);
				continue;
			}

			CSVRow newCSVRow = new CSVRow();
			newCSVRow.setName(contentArray[0]);
			newCSVRow.setSum(Double.parseDouble((contentArray[1])) * Double.parseDouble((contentArray[3])) * -1);
			newCSVRow.setVat(Integer.parseInt(contentArray[5]));
			// Do your stuff here
			CSVRowList.add(newCSVRow);
		}
		Collections.reverse(CSVRowList);
		return CSVRowList;
	}

	private void doCSVFile(List<CSVRow> CSVRowList, Double invoiceTotal, File pdfFile) {

		// For format using numbers , instead .
		DecimalFormat formatter = new DecimalFormat();
		formatter.applyPattern("###.###");
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator(',');

		PrintWriter pw = null;
		String csvFileName = "";
		try {
			csvFileName = Util.replaceSuffix(pdfFile.getName(), ".csv");
			String csvPath = pdfFile.getAbsoluteFile().getParent() + File.separator + csvFileName;
			File csvFile = new File(csvPath);
			if (!csvFile.canWrite() && csvFile.exists()) {
				System.out.println("File is open. Please close file and try again.");
				return;
			}
			pw = new PrintWriter(csvFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		StringBuilder builder = new StringBuilder();
		// Print title row
		String ColumnNamesList = "Account number;Product;Sum;Dimension;Dimension item;VAT %;VAT type";
		builder.append(ColumnNamesList + "\n");
		builder.append(COUNTER_ACCONT_NUMBER + ";" + COUNTER_TRANSACTION_NAME + ";" + formatter.format(invoiceTotal));
		builder.append('\n');
		
		// Build content
		for (CSVRow csvRow : CSVRowList) {
			builder.append(
					DEFAULT_ACCOUNT_NUMBER + ";" + csvRow.getName() + ";" + formatter.format(csvRow.getSum()) + ";;;" +
					csvRow.getVat() + ";" + csvRow.getVatType() + '\n');
		}

		pw.write(builder.toString());
		pw.close();
		System.out.println(csvFileName + " created succesfully.");
	}

}