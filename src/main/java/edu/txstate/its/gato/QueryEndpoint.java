package edu.txstate.its.gato;

import info.magnolia.context.MgnlContext;
import info.magnolia.rest.AbstractEndpoint;
import info.magnolia.rest.service.node.v1.RepositoryMarshaller;
import info.magnolia.rest.service.node.v1.RepositoryNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.RepositoryException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Endpoint for running JCR queries.
 *
 * @param <D> The endpoint definition
 */
@Api(value = "/query/v1", description = "The query API")
@Path("/query/v1")
public class QueryEndpoint<D extends QueryEndpointDefinition> extends AbstractEndpoint<D> {

  private static final String STATUS_MESSAGE_OK = "OK";
  private static final String STATUS_MESSAGE_BAD_REQUEST = "Request not understood due to errors or malformed syntax";
  private static final String STATUS_MESSAGE_UNAUTHORIZED = "Unauthorized";
  private static final String STATUS_MESSAGE_ACCESS_DENIED = "Access denied";
  private static final String STATUS_MESSAGE_ERROR_OCCURRED = "Error occurred";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private RepositoryMarshaller marshaller = new RepositoryMarshaller();

  @Inject
  public QueryEndpoint(final D endpointDefinition) {
    super(endpointDefinition);
  }

  /**
   * Returns a node including its properties and child nodes down to a certain depth.
   */
  @GET
  @Path("/{workspace}/{lang:(.+)?}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(value = "Get query results", notes = "Returns an array of nodes from the specified workspace according to the query")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = STATUS_MESSAGE_OK, response = RepositoryNode.class),
    @ApiResponse(code = 400, message = STATUS_MESSAGE_BAD_REQUEST),
    @ApiResponse(code = 401, message = STATUS_MESSAGE_UNAUTHORIZED),
    @ApiResponse(code = 500, message = STATUS_MESSAGE_ERROR_OCCURRED)
  })
  public Response readNode(
      @PathParam("workspace") String workspaceName,
      @PathParam("lang") @DefaultValue("JCR-SQL2") String lang,
      @QueryParam("depth") @DefaultValue("0") int depth,
      @QueryParam("query") @DefaultValue("") String query,
      @QueryParam("excludeNodeTypes") @DefaultValue("") String excludeNodeTypes,
      @QueryParam("hasPermissions") @DefaultValue("") String hasPermissions,
      @QueryParam("includeMetadata") @DefaultValue("false") boolean includeMetadata) throws RepositoryException {

    if (StringUtils.isBlank(query))
      return Response.status(Response.Status.BAD_REQUEST).build();

    if (!"JCR-SQL2".equals(lang) && !"sql".equals(lang) && !"xpath".equals(lang))
      return Response.status(Response.Status.BAD_REQUEST).build();

    try {
      QueryResult qr = MgnlContext.getJCRSession(workspaceName).getWorkspace()
        .getQueryManager().createQuery(query, lang).execute();
      String[] cols = qr.getSelectorNames();
      RowIterator rows = qr.getRows();
      RepositoryNodeList ret = new RepositoryNodeList();
      while (rows.hasNext()) {
        Node n = rows.nextRow().getNode(cols[0]);
        if (StringUtils.isBlank(hasPermissions) || n.getSession().hasPermission(n.getPath(), hasPermissions)) {
          ret.add(
            marshaller.marshallNode(n, depth, splitExcludeNodeTypesString(excludeNodeTypes), includeMetadata)
          );
        }
      }

      return Response.ok(ret).build();
    } catch (Exception e) {
      log.error("error with JCR query REST request", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  protected List<String> splitExcludeNodeTypesString(String excludes) {
    List<String> excludeList = new ArrayList<String>();

    if (excludes != null) {
      excludes = StringUtils.replace(excludes, " ", "");
      excludeList = Arrays.asList(StringUtils.split(excludes, ","));
    }

    return excludeList;
  }
}
