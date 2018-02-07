package com.elmaanum.hermit;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.cli.*;

import static com.elmaanum.hermit.RDF4J.initRemoteManager;


public class RDF4J_QUERY {
    public static final String CONFIG_FILE = "src/main/resources/config.json";
    private static FileOutputStream OUTPUT_STREAM = null;
    private static Repository REPOSITORY = null;
    private static RemoteRepositoryManager MANAGER = null;
    private static RepositoryConnection REPO_CONNECTION = null;
    private static Model RDF_MODEL = null;
    private static Value OUTPUT_SUBJECT_VALUE = null;
    private static Value OUTPUT_OBJECT_VALUE = null;
    private static JSONArray OUTPUT_PRED_VALUES = new JSONArray();
    private static JSONObject JSON_OBJECT = new JSONObject();

    public static void main(String[] args) throws Exception {
        CommandLine handledArgs = handleArgs(args);
        startUp(
                handledArgs.getOptionValue("environment"),
                handledArgs.getOptionValue("repoID"),
                handledArgs.getOptionValue("output"),
                handledArgs.getOptionValue("input")
        );
        executeQueriesAndWriteToModel(JSON_OBJECT.getJSONArray("queries"), null);
        writeToRDF();
        shutDown();
    }

    public static void executeQueriesAndWriteToModel(JSONArray jsonArray, TupleQueryResult tupleQueryResult)
            throws JSONException {
        if (tupleQueryResult == null) {
            tupleQueryResult = evaluateTupleQuery(
                    REPO_CONNECTION,
                    jsonArray.getJSONObject(0).getString("query")
            );
        }
        while (tupleQueryResult.hasNext()) {
            BindingSet bindingSet = tupleQueryResult.next();
            if (OUTPUT_OBJECT_VALUE == null) {
                try {
                    OUTPUT_SUBJECT_VALUE = bindingSet.getValue(JSON_OBJECT.getString("returnSubject"));
                    OUTPUT_OBJECT_VALUE = bindingSet.getValue(JSON_OBJECT.getString("returnObject"));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            if (jsonArray.length() > 1) {
                JSONArray nextJArray = new JSONArray(jsonArray.toString());
                nextJArray.remove(0);
                executeQueriesAndWriteToModel(
                        nextJArray,
                        getNextQueryResults(
                                nextJArray.getJSONObject(0).getString("query"),
                                nextJArray.getJSONObject(0).getString("bindString"),
                                bindingSet.getValue(nextJArray.getJSONObject(0).getString("bindValue"))
                        )
                );
            } else {
                OUTPUT_PRED_VALUES.put(bindingSet.getValue(JSON_OBJECT.getString("returnPredicate")));
            }
        }
        if (OUTPUT_OBJECT_VALUE != null && OUTPUT_PRED_VALUES.length() > 0) {
            for (int i = 0; i < OUTPUT_PRED_VALUES.length(); i++) {
                addTripleToModel(OUTPUT_PRED_VALUES.get(i));
            }
            OUTPUT_OBJECT_VALUE = null;
            OUTPUT_PRED_VALUES = new JSONArray();
        }
    }

    public static TupleQueryResult getNextQueryResults(String query, String bindString, Value bindValue) {
        return evaluateTupleQuery(
                REPO_CONNECTION,
                query,
                bindString,
                bindValue
        );
    }

    public static TupleQueryResult evaluateTupleQuery(RepositoryConnection con, String queryString) {
        return con.prepareTupleQuery(queryString).evaluate();
    }

    public static TupleQueryResult evaluateTupleQuery(RepositoryConnection con, String queryString, String setBindString, Value setBindValue) {
        TupleQuery preparedQuery = con.prepareTupleQuery(queryString);
        if (setBindString != null && setBindValue != null) {
            preparedQuery.setBinding(setBindString, setBindValue);
        }
        return preparedQuery.evaluate();
    }

    public static void addTripleToModel(Object predicate) {
        ValueFactory vf = SimpleValueFactory.getInstance();
        RDF_MODEL.add(
                vf.createIRI(OUTPUT_SUBJECT_VALUE.toString()),
                vf.createIRI(predicate.toString()),
                OUTPUT_OBJECT_VALUE
        );
    }

    public static void writeToRDF() {
        Rio.write(RDF_MODEL, OUTPUT_STREAM, RDFFormat.RDFXML);
    }

    public static void startUp(String environment, String repositoryId, String outputFileName, String inputConfigFile)
            throws IOException, JSONException, URISyntaxException {
        if (environment.equals("local")) {
            MANAGER = initRemoteManager("http://localhost:8080/rdf4j-server");
            REPOSITORY = MANAGER.getRepository(repositoryId);
        } else {
            REPOSITORY = new SPARQLRepository("http://semantic-bucket.chn7iquynr59.us-east-1-beta.rds.amazonaws.com:8182/sparql");
        }
        REPOSITORY.initialize();
        REPO_CONNECTION = REPOSITORY.getConnection();
        RDF_MODEL = buildModel();
        OUTPUT_STREAM = new FileOutputStream(outputFileName);
        JSON_OBJECT = new JSONObject(IOUtil.readString(new File(inputConfigFile)));
    }

    public static void shutDown() throws IOException {
        REPOSITORY.shutDown();
        OUTPUT_STREAM.close();
        REPO_CONNECTION.close();
    }

    public static Model buildModel() throws IOException, JSONException {
        ModelBuilder builder = new ModelBuilder();
        JSONObject baseToyURIs = new JSONObject(IOUtil.readString(new File(CONFIG_FILE))).getJSONObject("TOY_BASE_URI_MAP");
        JSONArray keys = baseToyURIs.names();
        for (int i = 0; i < keys.length(); ++i) {
            String prefix = keys.getString(i);
            String uri = baseToyURIs.getString(prefix);
            builder.setNamespace(prefix, uri);
        }
        return builder.build();
    }

    public static CommandLine handleArgs(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(true);
        options.addOption(output);

        Option repoID = new Option("r", "repoID", true, "repository ID");
        options.addOption(repoID);

        Option environment = new Option("e", "environment", true, "environment");
        output.setRequired(true);
        options.addOption(environment);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return null;
        }
        return cmd;
    }
}
