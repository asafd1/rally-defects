import java.io.File;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class CheckExcel
{

	public CheckExcel()
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public static void main(String[] args) throws InvalidFormatException, IOException
	{

		CheckExcel me = new CheckExcel();

		me.validateWorkbook(new File("C:/Users/adavid/Documents/QC_CSV/all.xls"));
		System.out.println("done");
	}

	private void validateWorkbook(File f) throws InvalidFormatException, IOException
	{
		Workbook wb = WorkbookFactory.create(f);
		Sheet sheet = wb.getSheetAt(0);
		int rownum;
		for (rownum = 1; rownum <= sheet.getLastRowNum(); rownum++)
		{
			double qcid = sheet.getRow(rownum).getCell(0).getNumericCellValue();
		}
		System.out.println("checked " + rownum + " lines");
	}
}
