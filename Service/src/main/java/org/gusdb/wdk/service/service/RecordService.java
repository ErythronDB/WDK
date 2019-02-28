package org.gusdb.wdk.service.service;

import static org.gusdb.fgputil.FormatUtil.NL;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.report.util.RecordFormatter;
import org.gusdb.wdk.service.annotation.OutSchema;
import org.gusdb.wdk.service.formatter.RecordClassFormatter;
import org.gusdb.wdk.service.request.RecordRequest;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.request.exception.RequestMisformatException;
import org.gusdb.wdk.service.statustype.MultipleChoicesStatusType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/record-types")
public class RecordService extends AbstractWdkService {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(RecordService.class);

  private static final String RECORDCLASS_RESOURCE = "RecordClass with name ";
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @OutSchema("wdk.records.get")
  public JSONArray getRecordClassList(@QueryParam("format") String format) {

    final boolean expand = Optional.ofNullable(format)
        .map(f -> f.equals("expanded"))
        .orElse(false);

    if (expand) 
      return new JSONArray(RecordClassFormatter.getExpandedRecordClassesJson(
        getWdkModel().getAllRecordClassSets(), getWdkModel().getAllQuestions()));
    else 
      return new JSONArray(RecordClassFormatter.getRecordClassNames(
        getWdkModel().getAllRecordClassSets()));
  }

  @GET
  @Path("{recordClassName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRecordClassInfo(
      @PathParam("recordClassName") String recordClassName,
      @QueryParam("expandAttributes") Boolean expandAttributes,
      @QueryParam("expandTables") Boolean expandTables,
      @QueryParam("expandTableAttributes") Boolean expandTableAttributes) {
    return Response.ok(
        RecordClassFormatter.getRecordClassJson(
            getRecordClassOrNotFound(recordClassName), getFlag(expandAttributes),
            getFlag(expandTables), getFlag(expandTableAttributes)).toString()
    ).build();
  }


  // TODO: replace this with a GET (using the path to encode the primary key)
  @POST
  @Path("{recordClassName}/records")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response buildResult(@PathParam("recordClassName") String recordClassName, String body)
      throws WdkModelException, DataValidationException, RequestMisformatException {
    try {
      // get and parse request information
      RecordClass recordClass = getRecordClassOrNotFound(recordClassName);
      RecordRequest request = RecordRequest.createFromJson(recordClass, new JSONObject(body));

      // fetch a list of record instances the passed primary key maps to
      List<RecordInstance> records = RecordClass.getRecordInstances(getSessionUser(), request.getPrimaryKey());

      // if no mapping exists, return not found
      if (records.isEmpty()) {
        throw new NotFoundException(formatNotFound(RECORDCLASS_RESOURCE + recordClassName +
            " has no record with primary keys: " + request.getPrimaryKey().getValuesAsString()));
      }

      // if PK specified maps to multiple records, return Multiple Choices response with IDs from which to choose
      else if (records.size() > 1) {
        String idOptionText = records.stream()
            .map(record -> record.getPrimaryKey().getRawValues().entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", ")))
            .collect(Collectors.joining(NL));
        return Response.status(new MultipleChoicesStatusType())
            .type(MediaType.TEXT_PLAIN).entity(idOptionText).build();
      }

      // PK represents only one record; fetch, format, and return
      else {
        TwoTuple<JSONObject,List<Exception>> recordJsonResult = RecordFormatter.getRecordJson(
            records.get(0), request.getAttributeNames(), request.getTableNames());
        triggerErrorEvents(recordJsonResult.getSecond());
        return Response.ok(recordJsonResult.getFirst().toString()).build();
      }
    }
    catch (JSONException e) {
      throw new RequestMisformatException("Passed request body deemed unacceptable. " + e.getMessage(), e);
    }
    catch (WdkUserException e) {
      // due to checks above during request parsing, this should not happen
      throw new WdkModelException(e);
    }
  }
}
