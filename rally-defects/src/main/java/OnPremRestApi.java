import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;

public class OnPremRestApi extends RallyRestApi
{
	String workspaceRef;
	String projectRef;

	public OnPremRestApi(URI server, String userName, String password, String workspace, String project,
		String applicationName) throws Exception
	{
		super(server, userName, password);

		SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy()
		{
			public boolean isTrusted(X509Certificate[] certificate, String authType) throws CertificateException
			{
				// trust all certs
				return true;
			}
		}, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sf));

		setApplicationName(applicationName);

		setWorkspaceAndProject(workspace, project);
		// setUsers();
	}

	private void setWorkspaceAndProject(String workspace, String project) throws Exception
	{
		// Read Subscription
		QueryRequest subscriptionRequest = new QueryRequest("Subscriptions");
		subscriptionRequest.setFetch(new Fetch("Name", "SubscriptionID", "Workspaces"));

		QueryResponse subscriptionQueryResponse = query(subscriptionRequest);
		JsonArray subscriptionQueryResults = subscriptionQueryResponse.getResults();
		JsonElement subscriptionQueryElement = subscriptionQueryResults.get(0);
		JsonObject subscriptionQueryObject = subscriptionQueryElement.getAsJsonObject();

		// Grab Workspaces Collection
		JsonObject myWorkspaces = subscriptionQueryObject.getAsJsonObject("Workspaces");
		QueryRequest workspacesRequest = new QueryRequest(myWorkspaces);
		workspacesRequest.setFetch(new Fetch("Name", "Workspace"));

		QueryResponse workspacesQueryResponse = query(workspacesRequest);
		JsonArray workspacesQueryResults = workspacesQueryResponse.getResults();

		for (int i = 0; i < workspacesQueryResults.size(); i++)
		{
			JsonObject workspaceObject = workspacesQueryResults.get(i).getAsJsonObject();
			workspaceRef = workspaceObject.get("_ref").getAsString();

			GetRequest workspaceRequest = new GetRequest(workspaceRef);
			workspaceRequest.setFetch(new Fetch("Name", "Projects"));
			GetResponse workspaceResponse = get(workspaceRequest);
			JsonObject workspaceObj = workspaceResponse.getObject();

			String workspaceName = workspaceObj.get("Name").getAsString();
			if (workspaceName.equals(workspace))
			{
				System.out.println("Workspace: " + workspaceName);

				JsonObject myProjects = workspaceObj.getAsJsonObject("Projects");
				QueryRequest projectsRequest = new QueryRequest(myProjects);
				projectsRequest.setFetch(new Fetch("Name", "Project"));

				QueryResponse projectsQueryResponse = query(projectsRequest);
				JsonArray projectsQueryResults = projectsQueryResponse.getResults();

				for (int j = 0; j < projectsQueryResults.size(); j++)
				{
					JsonObject projectObject = projectsQueryResults.get(j).getAsJsonObject();
					projectRef = projectObject.get("_ref").getAsString();
					GetRequest projectRequest = new GetRequest(projectRef);
					projectRequest.setFetch(new Fetch("Name"));
					GetResponse projectResponse = get(projectRequest);

					JsonObject projectObj = projectResponse.getObject();
					String projectName = projectObj.get("Name").getAsString();

					if (projectName.equals(project))
					{
						System.out.println("Project: " + projectName);
						break;
					}
				}

				break;
			}
		}
	}
}
