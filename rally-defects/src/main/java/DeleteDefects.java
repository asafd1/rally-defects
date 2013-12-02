import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;

public class DeleteDefects
{
	public Logger log;

	public static void main(String[] args) throws Exception
	{
		new DeleteDefects().deleteDefects(args[0], args[1], args[2], args[3]);
	}

	private void deleteDefects(String workspace, String project, String user, String password)
		throws URISyntaxException, Exception
	{
		OnPremRestApi restApi = new OnPremRestApi(new URI("https://rally1.rallydev.com"), user, password, workspace,
			project, "delete-app");

		try
		{
			QueryRequest defects = new QueryRequest("defect");

			defects.setWorkspace(restApi.workspaceRef);
			defects.setProject(restApi.projectRef);
			defects.setFetch(new Fetch("FormattedID", "Name", "Project", "CreationDate"));
			// defects.setQueryFilter(new QueryFilter("State", "<", "Fixed"));
			defects.setOrder("FormattedID ASC");

			defects.setPageSize(200);
			defects.setLimit(1);

			QueryResponse queryResponse = restApi.query(defects);
			if (queryResponse.wasSuccessful())
			{
				log.println(String.format("\nTotal results: %d", queryResponse.getTotalResultCount()));
				int i = 0;
				for (JsonElement result : queryResponse.getResults())
				{
					i++;
					JsonObject defect = result.getAsJsonObject();

					if (!deleteDefect(restApi, defect))
					{
						break;
					}
				}
				log.println("deleted " + i + " defects");

			}
			else
			{
				System.err.println("The following errors occurred: ");
				for (String err : queryResponse.getErrors())
				{
					System.err.println("\t" + err);
				}
			}

		}
		finally
		{
			restApi.close();
		}
	}

	private boolean deleteDefect(OnPremRestApi restApi, JsonObject defect) throws IOException
	{
		log.println("Deleting defect: " + defect.get("FormattedID") + " - " + defect.get("Name").getAsString());

		String project = defect.get("Project").getAsJsonObject().get("Name").getAsString();

		if (!project.equalsIgnoreCase("my-project"))
		{
			log.println("defect is not in my project: " + defect);
			return false;
		}

		String ref = defect.get("_ref").getAsString();

		DeleteRequest deleteRequest = new DeleteRequest(ref);
		DeleteResponse deleteResponse = restApi.delete(deleteRequest);
		if (deleteResponse.wasSuccessful())
		{
			return true;
		}
		log.println("failed to delete.");
		return false;
	}

}
