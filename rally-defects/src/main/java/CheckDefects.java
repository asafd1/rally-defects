import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class CheckDefects
{
	Logger log;

	final int MAX_ROWS = Integer.MAX_VALUE;
	final int MAX_FILES = Integer.MAX_VALUE;

	public static void main(String[] args) throws Exception
	{
		new CheckDefects().checkDefects(args[0], args[1], args[2], args[3], args[4]);
	}

	void checkDefects(String workspace, String project, String user, String password, String dirName) throws Exception
	{
		OnPremRestApi restApi = new OnPremRestApi(new URI("https://rally1.rallydev.com"), user, password, workspace,
			project, "import-qc");

		try
		{
			log = new Logger(new File(dirName, "check.log").getAbsolutePath());
			log.println("Staring check in Workspace '" + workspace + "'");
			File folder = new File(dirName);
			File[] files = folder.listFiles(new FileFilter()
			{
				public boolean accept(File pathname)
				{
					return pathname.getName().endsWith("xlsx") || pathname.getName().endsWith("xls");
				}
			});

			int i = 0;
			for (File f : files)
			{
				checkFile(f, restApi);

				if (++i >= MAX_FILES)
				{
					break;
				}
			}

			log.println("Finished import");
		}
		finally
		{
			restApi.close();
		}
	}

	private void checkDefect(OnPremRestApi restApi, Defect d) throws URISyntaxException, Exception
	{
		QueryRequest defects = new QueryRequest("defect");

		defects.setWorkspace(restApi.workspaceRef);
		defects.setProject(restApi.projectRef);
		defects.setFetch(new Fetch("FormattedID", "Name", "Project", "CreationDate"));
		defects.setQueryFilter(new QueryFilter("QCID", "=", String.valueOf(d.qcid)));
		defects.setOrder("FormattedID ASC");

		defects.setPageSize(1);
		defects.setLimit(1);

		QueryResponse queryResponse = restApi.query(defects);
		if (queryResponse.wasSuccessful())
		{
			if (queryResponse.getResults().size() != 1)
			{
				log.println("could not find defect: " + d.qcid);
			}
		}
		else
		{
			System.err.println("The following errors occurred: ");
			for (String err : queryResponse.getErrors())
			{
				log.println("\t" + err);
			}
		}
	}

	private void checkFile(File f, OnPremRestApi restApi) throws Exception, IOException
	{
		Workbook wb = WorkbookFactory.create(f);
		Sheet sheet = wb.getSheetAt(0);

		int rownum;
		for (rownum = 1; rownum <= sheet.getLastRowNum(); rownum++)
		{
			Defect d = getDefect(sheet, rownum);
			try
			{
				checkDefect(restApi, d);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				log.println("failed to add defect: " + d.qcid);
			}

			if (rownum >= MAX_ROWS)
			{
				break;
			}
		}

		log.println("checked " + (rownum - 1) + " rows from " + f.getName());

	}

	private Defect getDefect(Sheet sheet, int rownum)
	{
		Row row = sheet.getRow(rownum);
		Defect d = new Defect();
		int i = 0;
		d.qcid = Math.round(row.getCell(i++).getNumericCellValue());
		d.name = row.getCell(i++).getStringCellValue();
		d.description = row.getCell(i++).getStringCellValue();
		d.notes = row.getCell(i++).getStringCellValue();
		d.owner = row.getCell(i++).getStringCellValue();
		d.state = row.getCell(i++).getStringCellValue();
		if (d.state.trim().equalsIgnoreCase("canceled"))
		{
			d.state = "Closed";
			d.resolution = "Won't Fix";
		}
		d.severity = row.getCell(i++).getStringCellValue();
		d.submittedBy = row.getCell(i++).getStringCellValue();
		d.environment = row.getCell(i++).getStringCellValue();
		if (d.environment.trim().length() == 0)
		{
			d.environment = "Development";
		}
		d.foundInVersion = row.getCell(i++).getStringCellValue();
		d.targetBuild = row.getCell(i++).getStringCellValue();
		d.category = row.getCell(i++).getStringCellValue();
		d.customerName = row.getCell(i++).getStringCellValue();
		d.externalReferenceID = row.getCell(i++).getStringCellValue();
		d.affectedCustomers = row.getCell(i++).getStringCellValue();
		return d;

	}

	class Defect
	{
		long qcid;
		String name;
		String description;
		String notes;
		String owner;
		String state;
		String resolution;
		String severity;
		String submittedBy;
		String environment;
		String foundInVersion;
		String targetBuild;
		String category;
		String customerName;
		String externalReferenceID;
		String affectedCustomers;
	}
}
