package com.softwareleaf.confluence;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.softwareleaf.confluence.model.*;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A class that is capable of making requests to the confluence API.
 * <p/>
 * Example Usage:
 * <pre>{@code
 *     String myURL = "http://confluence.organisation.org";
 *     String username = "...";
 *     String password = "...";
 *
 *     ConfluenceClient client = ConfluenceClient.builder()
 *          .baseURL(myURL)
 *          .username(username)
 *          .password(password)
 *          .build();
 *
 *     // search confluence instance by title and space key
 *     ContentResultList search =
 *     confluenceClient.getContentBySpaceKeyAndTitle("DEV", "A page or blog in DEV");
 * }</pre>
 *
 * @author Jonathon Hope
 */
public class ConfluenceClient {
    /**
     * The default base url is the production confluence instance.
     */
    public static final String BASE_URL = "http://localhost:8090";
    // default account credentials
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    /**
     * the Logger instance used by this class.
     */
    private static final Logger logger = Logger.getLogger(ConfluenceClient.class.getName());

    /**
     * The ConfluenceAPI endpoint.
     */
    private ConfluenceAPI confluenceAPI;

    /**
     * Constructor.
     */
    private ConfluenceClient(Builder builder) {
        this.confluenceAPI = builder.confluenceAPI;
    }

    /**
     * Fetch a single piece of content.
     *
     * @param id the id of the page or blog post to fetch.
     * @return the Content instance.
     */
    public Content getContentById(String id) {
        return confluenceAPI.getContentById(id);
    }

    /**
     * Fetch a results object containing a paginated list of content.
     *
     * @return an instance of {@code getContentResults} wrapping the list
     * of {@code Content} instances obtained from the API call.
     */
    public ContentResultList getContentResults() {
        return confluenceAPI.getContentResults();
    }

    /**
     * Perform a search for content, by space key and title.
     *
     * @param key   the space key to search under.
     * @param title the title of the piece of content to search for.
     * @return an instance of {@code getContentResults} wrapping the list
     * of {@code Content} instances obtained from the API call.
     */
    public ContentResultList getContentBySpaceKeyAndTitle(final String key, final String title) {
        return confluenceAPI.getContentBySpaceKeyAndTitle(key, title);
    }

    /**
     * Used for converting the storage format of a piece of content.
     *
     * @param storage   the storage instance to convert.
     * @param convertTo the representation to convert to.
     * @return an instance of {@code Storage} that contains the result of
     * the conversion request.
     * @see <a href="https://confluence.atlassian.com/display/DOC/Confluence+Storage+Format">
     * Confluence Storage Format</a>
     */
    public Storage convertContent(final Storage storage, final Storage.Representation convertTo) {
        return confluenceAPI.postContentConverstion(storage, convertTo.toString());
    }

    /**
     * Performs a POST request with the body of the request containing the
     * {@param content}, thus creating a new page or blog post on confluence.
     *
     * @param content  the content to post to confluence.
     * @param callback this handle provides a means of inquiring about
     *                 the success or failure of the invocation.
     */
    public void postContentWithCallback(final Content content, final Callback<Content> callback) {
        confluenceAPI.postContentWithCallback(content, callback);
    }

    /**
     * Performs a POST request with the body of the request containing the
     * {@param content}, thus creating a new page or blog post on confluence.
     *
     * @param content the content to post to confluence.
     */
    public Content postContent(final Content content) {
        System.out.println(content.toString());
        return confluenceAPI.postContent(content);
    }

    /**
     * DELETE Content
     * <p/>
     * Trashes or purges a piece of Content, based on its {@literal ContentType} and {@literal ContentStatus}.
     * <p/>
     * There are three cases:
     * If the content is trashable and its status is {@literal ContentStatus#CURRENT}, it will be trashed.
     * If the content is trashable, its status is {@literal ContentStatus#TRASHED} and the "status" query parameter in
     * the request is "trashed", the content will be purged from the trash and deleted permanently.
     * If the content is not trashable it will be deleted permanently without being trashed.
     *
     * @param id the id of the page of blog post to be deleted.
     */
    public void deleteContentById(final String id) {
        NoContent noContent = confluenceAPI.deleteContentById(id);
        logger.fine("Response: " + noContent);
    }

    /**
     * Obtain a list of available spaces.
     *
     * @return a list of spaces available on confluence.
     */
    public List<Space> getSpaces() {
        Space[] results = confluenceAPI.getSpaces().getSpaces();
        return Arrays.stream(results).collect(Collectors.toList());
    }

    /**
     * Fetch all content from a confluence space.
     *
     * @param spaceKey the key that identifies the target Space.
     * @return a list of all content in the given Space identified by {@param spaceKey}.
     */
    public List<Content> getAllSpaceContent(final String spaceKey) {
        Content[] results = confluenceAPI.getAllSpaceContent(spaceKey,
                ImmutableMap.of(
                        "expand", "ancestors,body.storage",
                        "limit", "1000"))
                .getContents();
        return Arrays.stream(results).collect(Collectors.toList());
    }

    /**
     * Obtain a list of root content of a space.
     *
     * @param spaceKey    the space key of the Space.
     * @param contentType the type of content to return.
     * @return a list of Content instances obtained from the root.
     */
    public List<Content> getRootContentBySpaceKey(final String spaceKey, final Type contentType) {
        Content[] resultList = confluenceAPI
                .getRootContentBySpaceKey(spaceKey, contentType.toString())
                .getContents();
        return Arrays.stream(resultList).collect(Collectors.toList());
    }

    /**
     * Factory object for chaining the construction of a {@code ConfluenceClient}.
     *
     * @return an instance of the internal Builder class.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A Builder factory for implementing the Builder Pattern.
     */
    public static class Builder {
        /**
         * This is the reference to the concrete REST API Client generated by Retrofit.
         */
        private ConfluenceAPI confluenceAPI;
        /**
         * The username forms the first part of the credential used to authenticate requests.
         */
        private String username;
        /**
         * The password forms the second part of the credential used to authenticate requests.
         */
        private String password;
        /**
         * By default, {@link #BASE_URL} will be used as the url of the confluence instance; when
         * this is set, requests will be made to this base URL instead.
         */
        private String alternativeBaseURL;

        // prevent direct instantiation by external classes.
        private Builder() {
        }

        /**
         * Set the username parameter.
         *
         * @param username the username.
         * @return {@code this}.
         */
        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the password parameter.
         *
         * @param password the password matching the username.
         * @return {@code this}.
         */
        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        /**
         * By default, {@link #BASE_URL BaseURL} will be used as the url of the confluence instance; when
         * this is set, requests will be made to this base URL instead.
         *
         * @param url the alternative Base url of the confluence instance to make requests to.
         * @return {@code this}.
         */
        public Builder baseURL(final String url) {
            this.alternativeBaseURL = url;
            return this;
        }

        /**
         * Build and return a configured ConfluenceClient instance.
         *
         * @return a configured {@code ConfluenceClient} instance.
         */
        public ConfluenceClient build() {
            // determine if we are using the production confluence or not.
            final String URL = alternativeBaseURL == null ? BASE_URL : alternativeBaseURL;
            // determine the user credentials to use.
            final String username = this.username == null ? DEFAULT_USERNAME : this.username;
            final String password = this.password == null ? DEFAULT_PASSWORD : this.password;
            /*
             * The Confluence REST API requires HTTP Basic authentication using a
             * username and password pair, for a given Confluence user.
             * We therefore need to first encode the credentials using a Base64 encoder
             * and set up an interceptor that adds the requisite HTTP headers to each request.
             */
            final String credentials = username + ":" + password;
            // encode in base64.
            final String encodedCredentials = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(URL)
                    .setConverter(new GsonConverter(new GsonBuilder().disableHtmlEscaping().create()))
                    .setRequestInterceptor(
                            request -> {
                                request.addHeader("Accept", "application/json");
                                request.addHeader("Authorization", encodedCredentials);
                            }
                    )
                    .build();

            // Create an implementation of the API defined by the specified ConfluenceAPI interface
            this.confluenceAPI = restAdapter.create(ConfluenceAPI.class);

            return new ConfluenceClient(this);
        }

    }

}