package com.elmaanum.hermit;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

public class RDF4J {
    public static final String CONFIG_FILE = "src/main/resources/config.json";
    public static FileOutputStream OUTPUT_STREAM = null;
    public static final String ONTOLOGIES = "/semantic-ontologies";

    public static void main(String[] args) throws Exception {
        String environment = args[0];
        String action = args[1];
        String repositoryId = "semantic-network-toys2";
        RemoteRepositoryManager manager = null;
        Repository repo;
        if (environment.equals("local")){
            manager = initRemoteManager("http://localhost:8080/rdf4j-server");
            repo = manager.getRepository(repositoryId);
        } else {
            repo = new SPARQLRepository("http://semantic-bucket.chn7iquynr59.us-east-1-beta.rds.amazonaws.com:8182/sparql");
        }
        repo.initialize();

        if (action.equals("recreate")) {
            OUTPUT_STREAM = new FileOutputStream("./target/recreate_output.txt");

            String directory = args[2];
            if (environment.equals("local")){
                /** remove repos **/
                removeRemoteRepo(repositoryId, manager);
                /** create remote repos **/
                createRemoteRepo(repositoryId, manager);   
            } else {
                deleteAllSPARQL(repo);
            }
            /** add graphs to remote repo from file **/
            getFilesInFolder(repo, directory);
        } else if (action.equals("query")) {
            /** query remote repos via rdf4j **/

        } else if (action.equals("delete")) {
            deleteAllSPARQL(repo);
        }
        else {
            System.out.println("unknown action");
        }
    }


    public static void createRemoteRepo(String repositoryId, RemoteRepositoryManager manager) {
        /** storage configuration **/
        boolean persist = true;
        SailImplConfig backendConfig = new MemoryStoreConfig(persist);
        backendConfig = new ForwardChainingRDFSInferencerConfig(backendConfig);
        SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);

        /** repo configuration **/
        RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
        manager.addRepositoryConfig(repConfig);
    }

    public static void getFilesInFolder(Repository repo, String location) throws URISyntaxException, IOException, JSONException {
        String baseURI = "";

        if (location.equals("project")) {
            File ontologies = new File(Demo.class.getResource(ONTOLOGIES).toURI());
            for (File ontology: ontologies.listFiles()) {
                addToRemoteRepo(repo, ontology, baseURI);
            }
        } else if (location.equals("gitToys")) {
            /** gets ontologies within semantic-network toy folder **/
            File ontologies = new File("../../GitHub/semantic-network/toys");
            for (File ontology: ontologies.listFiles()) {
                addToRemoteRepo(repo, ontology, baseURI);
            }
        } else if (location.equals("gitFull")) {
            /** gets map of paths to diff ontologies in diff folders **/
            JSONObject localGitOntologies = new JSONObject(IOUtil.readString(new File(CONFIG_FILE))).getJSONObject("LOCAL_GIT_ONTOLOGIES_MAP");
            JSONArray keys = localGitOntologies.names();
            for (int i = 0; i < keys.length (); ++i) {
                String key = keys.getString (i);
                String path = localGitOntologies.getString(key);
                addToRemoteRepo(repo, new File(path), baseURI);
            }
        }
    }

    public static void addToRemoteRepo(Repository repo, File ontologyFile, String baseURI) {
        String printString = null;

        try (RepositoryConnection conn = repo.getConnection()) {
            conn.add(ontologyFile, baseURI, RDFFormat.RDFXML);
        }
        catch (RDF4JException e) {
            printString = "Ontology: " +ontologyFile+ "; RDF4JException: " + e + "\n";
        }
        catch (java.io.IOException e) {
            printString = "Ontology: " +ontologyFile+ "; IOException: " + e + "\n";
        }

        if (printString == null){
            printString = "Successfully added: " + ontologyFile + "\n";
        }

        try {
            OUTPUT_STREAM.write(printString.getBytes());
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static RemoteRepositoryManager initRemoteManager(String serverUrl){
        RemoteRepositoryManager manager = new RemoteRepositoryManager(serverUrl);
        manager.initialize();
        return manager;
    }

    public static void removeRemoteRepo(String repositoryId, RemoteRepositoryManager manager) {
        try {
            manager.removeRepository(repositoryId);
        }
        finally {
            manager.shutDown();
        }
    }

    public static void deleteAllSPARQL(Repository repo) {
        try (RepositoryConnection conn = repo.getConnection()) {
            String queryString = "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o } ";
            conn.prepareUpdate(QueryLanguage.SPARQL, queryString).execute();
        }
    }

}