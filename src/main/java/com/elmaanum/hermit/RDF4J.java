package com.elmaanum.hermit;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class RDF4J {
    private static final String ONTOLOGIES = "/semantic-ontologies";
    private static final String GIT_TOY_ONTOLOGIES = "../../Github/semantic-network/toys";
    private static final Map<String, String> LOCAL_GIT_ONTOLOGIES_MAP;
    static
    {
        LOCAL_GIT_ONTOLOGIES_MAP = new HashMap<>();
        LOCAL_GIT_ONTOLOGIES_MAP.put("rsxOntoDefaults", "../../Github/semantic-network/Semantic RSX/RSX-RDF/rsxOntoDefaults.rdf");
        LOCAL_GIT_ONTOLOGIES_MAP.put("rsxml", "../../Github/semantic-network/Semantic RSX/RSX-RDF/rsxml.rdf");
        LOCAL_GIT_ONTOLOGIES_MAP.put("av7c", "../../Github/semantic-network/Semantic Assortment/av7c.rdf");
        LOCAL_GIT_ONTOLOGIES_MAP.put("av7", "../../Github/semantic-network/Semantic Assortment/av7.rdf");
    }
    private static final Map<String, String> BASE_URI_MAP;
    static
    {
        BASE_URI_MAP = new HashMap<>();
        BASE_URI_MAP.put("rsxOntoDefaults", "https://github.com/SPSCommerce/semantic-network/blob/master/Semantic%20RSX/RSX-RDF/rsxOntoDefaults.rdf#");
        BASE_URI_MAP.put("rsxml", "https://github.com/SPSCommerce/semantic-network/blob/master/Semantic%20RSX/RSX-RDF/rsxml.rdf#");
        BASE_URI_MAP.put("av7c", "https://github.com/SPSCommerce/semantic-network/blob/master/Semantic%20Assortment/av7c.rdf#");
        BASE_URI_MAP.put("av7", "https://github.com/SPSCommerce/semantic-network/blob/master/Semantic%20Assortment/av7.rdf#");
    }

    public static void main(String[] args) throws Exception {
        String repositoryId = "semantic-network-toys";
        /** remove repos **/
        removeRemoteRepo(repositoryId);

        /** create remote repos **/
        createRemoteRepo(repositoryId);

        /** init repo **/
        Repository repo = initRepo(repositoryId);

        /** add graphs to remote repo from file **/
        String location = "gitToys";
        getFilesInFolder(repo, location);

        /** query remote repos via rdf4j **/
        // String spsrfValue = "depositorDE";
        // String predicate = "owl:sameAs";
        // queryRemoteRepos(repositoryId, spsrfValue, predicate);
    }

    public static void createRemoteRepo(String repositoryId) {
        /** URL of the remote RDF4J Server we want to access **/
        RemoteRepositoryManager manager = initRemoteManager();

        /** storage configuration **/
        boolean persist = true;
        SailImplConfig backendConfig = new MemoryStoreConfig(persist);
        backendConfig = new ForwardChainingRDFSInferencerConfig(backendConfig);
        SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);

        /** repo configuration **/
        RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
        manager.addRepositoryConfig(repConfig);
    }

    public static Repository initRepo(String repositoryId){
        /** get repo on rdf4j server **/
        RemoteRepositoryManager manager = initRemoteManager();

        /** add to repo **/
        Repository repo = manager.getRepository(repositoryId);
        repo.initialize();
        return repo;
    }
    public static void getFilesInFolder(Repository repo, String location) throws URISyntaxException {
        String baseURI = "";

        if (location == "project") {
            File ontologies = new File(Demo.class.getResource(ONTOLOGIES).toURI());
            for (File ontology: ontologies.listFiles()) {
                System.out.println(ontology);
                addToRemoteRepo(repo, ontology, baseURI);
            }
        } else if (location == "gitToys") {
            /** gets ontologies within semantic-network toy folder **/
            File ontologies = new File("../../GitHub/semantic-network/toys");
            for (File ontology: ontologies.listFiles()) {
                System.out.println(ontology);
                addToRemoteRepo(repo, ontology, baseURI);
            }
        } else if (location == "gitFull") {
            /** gets map of paths to diff ontologies in diff folders **/
            LOCAL_GIT_ONTOLOGIES_MAP.forEach((String key, String path) -> {
                System.out.println(key+": "+ path);
                addToRemoteRepo(repo, new File(path), baseURI);
            });
        }
    }

    public static void addToRemoteRepo(Repository repo, File ontologyFile, String baseURI) {
        try (RepositoryConnection conn = repo.getConnection()) {
            conn.add(ontologyFile, baseURI, RDFFormat.RDFXML);
        }
        catch (RDF4JException e) {
        }
        catch (java.io.IOException e) {}
    }

    public static RemoteRepositoryManager initRemoteManager(){
        String serverUrl = "http://localhost:8080/rdf4j-server";
        RemoteRepositoryManager manager = new RemoteRepositoryManager(serverUrl);
        manager.initialize();
        return manager;
    }

    public static void removeRemoteRepo(String repositoryId) {
        RemoteRepositoryManager manager = initRemoteManager();
        try {
            manager.removeRepository(repositoryId);
        }
        finally {
            manager.shutDown();
        }
    }

    public static String getBaseURI(File ontology) {
        String ontologyStr = ontology.toString();
        // for through map and return if match
        return "";
    }

    public static void queryRemoteRepos(String repositoryId, String spsrfValue, String predicate) {
        RemoteRepositoryManager manager = initRemoteManager();
        Repository repo = manager.getRepository(repositoryId);

        String spsrfNameSpace = "spsrf:<https://github.com/SPSCommerce/semantic-network/blob/master/Semantic%20RSX/RSX-RDF/rsxOntoDefaults.rdf#>";
        try (RepositoryConnection conn = repo.getConnection()){
            String constructQueryString = "PREFIX "+spsrfNameSpace;
            constructQueryString += "CONSTRUCT \n";
            constructQueryString += "WHERE {";
            constructQueryString += "   spsrf:"+spsrfValue+" "+predicate+ "?target";
            constructQueryString += "}";
            GraphQuery constructQuery = conn.prepareGraphQuery(constructQueryString);
            try (GraphQueryResult result = constructQuery.evaluate()) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    System.out.println(st);
                }
            }
        }
    }
}
