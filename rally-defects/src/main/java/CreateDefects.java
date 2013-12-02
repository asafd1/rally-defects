import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class CreateDefects
{
	Logger log;

	final int MAX_ROWS = Integer.MAX_VALUE;
	final int MAX_FILES = Integer.MAX_VALUE;

	public static void main(String[] args) throws Exception
	{
		new CreateDefects().importDefects(args[0], args[1], args[2], args[3], args[4]);
	}

	void importDefects(String workspace, String project, String user, String password, String dirName) throws Exception
	{
		OnPremRestApi restApi = new OnPremRestApi(new URI("https://rally1.rallydev.com"), user, password, workspace,
			project, "import-qc");

		try
		{
			log = new Logger(new File(dirName, "import.log").getAbsolutePath());
			log.println("Staring import into Workspace '" + workspace + "'");
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
				importFile(f, restApi);

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

	private void importFile(File f, OnPremRestApi restApi) throws Exception, IOException
	{
		Workbook wb = WorkbookFactory.create(f);
		Sheet sheet = wb.getSheetAt(0);

		int rownum;
		for (rownum = 1; rownum <= sheet.getLastRowNum(); rownum++)
		{
			Defect d = getDefect(sheet, rownum);
			try
			{
				createDefect(restApi, d);
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

		log.println("imported " + (rownum - 1) + " rows from " + f.getName());

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
		if (d.state.trim().equalsIgnoreCase("deferred"))
		{
			d.state = "Open";
			d.notes += " - QC Deferred";
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

	private String getUserReference(RallyRestApi restApi, String name) throws Exception
	{
		// Read User
		QueryRequest userRequest = new QueryRequest("User");
		userRequest.setFetch(new Fetch("UserName", "Subscription", "DisplayName"));
		userRequest.setQueryFilter(new QueryFilter("UserName", "=", name));
		QueryResponse userQueryResponse = restApi.query(userRequest);
		JsonArray userQueryResults = userQueryResponse.getResults();
		JsonElement userQueryElement = userQueryResults.get(0);
		JsonObject userQueryObject = userQueryElement.getAsJsonObject();
		String userRef = userQueryObject.get("_ref").getAsString();
		return userRef;
	}

	private void setDefectProperties(RallyRestApi restApi, JsonObject newDefect, Defect d) throws Exception
	{
		// note: custom fields must be prefixed with 'c_' and fields with spaces in them should be set without the
		// spaces (e.g. 'Submitted By' should be sent as 'SubmittedBy')
		newDefect.addProperty("c_QCID", d.qcid);
		newDefect.addProperty("Name", d.name);
		newDefect.addProperty("Description", d.description);
		newDefect.addProperty("Notes", d.notes);
		newDefect.addProperty("Owner", getUserReference(restApi, d.owner));
		newDefect.addProperty("State", d.state);
		newDefect.addProperty("Resolution", d.resolution);
		newDefect.addProperty("Severity", d.severity);
		newDefect.addProperty("SubmittedBy", getUserReference(restApi, d.submittedBy));
		newDefect.addProperty("Environment", d.environment);
		newDefect.addProperty("c_FoundInVersion", d.foundInVersion);
		newDefect.addProperty("TargetBuild", d.targetBuild);
		newDefect.addProperty("c_AffectedCustomers", d.affectedCustomers);
	}

	private void createDefect(OnPremRestApi restApi, Defect d) throws Exception
	{
		JsonObject newDefect = new JsonObject();

		newDefect.addProperty("Workspace", restApi.workspaceRef);
		newDefect.addProperty("Project", restApi.projectRef);

		setDefectProperties(restApi, newDefect, d);

		log.print("Creating defect: " + d.qcid + " - " + d.name + "...");
		CreateRequest createRequest = new CreateRequest("defect", newDefect);
		CreateResponse createResponse = restApi.create(createRequest);
		if (!createResponse.wasSuccessful())
		{
			log.printRaw("failed: " + Arrays.toString(createResponse.getErrors()));
		}
		else
		{
			log.printRaw("success: " + Arrays.toString(createResponse.getWarnings()));
		}
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
