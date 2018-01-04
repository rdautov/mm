package processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * A processor for retrieving tweets from specific users.
 */
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags({"twitter", "retrieve", "source", "MM"})
@CapabilityDescription("This processor retrieves tweets from specific users.")
public class MmTwitterReceiver extends AbstractProcessor {

    /** Processor property. */
    public static final PropertyDescriptor USER_IDS =
            new PropertyDescriptor.Builder().name("Twitter user IDs")
                    .description(
                            "Specifies comma-separated relevant Twitter users/accounts.")
                    .defaultValue("119367092").required(true)
                    .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
                    .build();

    /** Relationship "Success". */
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description(
                    "This is where flow files are sent if the processor execution went well.")
            .build();

    /** List of processor properties. */
    private List<PropertyDescriptor> properties;

    /** List of processor relationships. */
    private Set<Relationship> relationships;

    /**
     * Twitter stream.
     */
    private TwitterStream twitterStream;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final ProcessorInitializationContext context) {

        final Set<Relationship> procRels = new HashSet<Relationship>();
        procRels.add(REL_SUCCESS);
        setRelationships(Collections.unmodifiableSet(procRels));

        final List<PropertyDescriptor> supDescriptors =
                new ArrayList<PropertyDescriptor>();
        supDescriptors.add(USER_IDS);
        setProperties(Collections.unmodifiableList(supDescriptors));

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("w005HES0qRC80dSzISfcXWuYA")
                .setOAuthConsumerSecret(
                        "WWmbn1USHfGUbFYqGEoP1Zo771MZT8YQD9aYhCCQW2i5uEpjmJ")
                .setOAuthAccessToken(
                        "119367092-XTMgigkWeuTOrnP7N4WkKl3jsZtbuu5o7woFerpJ")
                .setOAuthAccessTokenSecret(
                        "mDeunQONPSMhqYwGcLdYZZiTq28TorWFNCeXK8ZNJzExh");

        twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

        StatusListener listener = new MmTwitterListener();
        twitterStream.addListener(listener);

        getLogger().info("Initialisation complete!");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrigger(final ProcessContext aContext,
            final ProcessSession aSession) throws ProcessException {

        String stringIds = aContext.getProperty(USER_IDS).getValue();
        if (!stringIds.trim().isEmpty()) {

            List<String> ids = Arrays.asList(stringIds.split(","));
            List<Long> longIds = ids.stream().map(Long::parseLong)
                    .collect(Collectors.toList());
            long[] followings = longIds.stream().mapToLong(i -> i).toArray();
            FilterQuery query = new FilterQuery();
            query.follow(followings);

            twitterStream.filter(query);
        }

        // BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        // StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        //
        // // add some track terms
        // String stringIds = aContext.getProperty(USER_IDS).getValue();
        // if (!stringIds.trim().isEmpty()) {
        //
        // List<String> ids = Arrays.asList(stringIds.split(","));
        // List<Long> longIds = ids.stream().map(Long::parseLong)
        // .collect(Collectors.toList());
        // endpoint.followings(longIds);
        // }
        //
        // Authentication auth = new OAuth1("w005HES0qRC80dSzISfcXWuYA",
        // "WWmbn1USHfGUbFYqGEoP1Zo771MZT8YQD9aYhCCQW2i5uEpjmJ",
        // "119367092-XTMgigkWeuTOrnP7N4WkKl3jsZtbuu5o7woFerpJ",
        // "mDeunQONPSMhqYwGcLdYZZiTq28TorWFNCeXK8ZNJzExh");
        // // Authentication auth = new BasicAuth(username, password);
        //
        // // Create a new BasicClient. By default gzip is enabled.
        // if (null == client) {
        // client = new ClientBuilder().hosts(Constants.STREAM_HOST)
        // .endpoint(endpoint).connectionTimeout(60000)
        // .authentication(auth)
        // .processor(new StringDelimitedProcessor(queue)).build();
        // // Establish a connection
        // client.connect();
        // getLogger().info("Client created and connected");
        // } else {
        // client.reconnect();
        // getLogger().info("Client reconnected");
        // }
        //
        // try {
        // while (true) {
        //
        // String msg = queue.take();
        //
        // getLogger().info(msg);
        //
        // final JSONObjectParser parser = new JSONObjectParser(); //
        // Gson..createParser(new
        // // StringReader
        // // parser.
        //
        // JSONParser jsonParser = new JSONParser();
        // JSONObject jsonObject = (JSONObject) jsonParser.parse(msg);
        // // get a String from the JSON object
        //
        // // ignore deleted tweets
        // if (!jsonObject.containsKey("delete")) {
        //
        // String text = (String) jsonObject.get("text");
        // // String user = (String) jsonObject.get("user").;
        //
        // getLogger().info(text);
        //
        // FlowFile flowFile = aSession.create();
        // flowFile = aSession.write(flowFile,
        // new OutputStreamCallback() {
        //
        // @Override
        // public void process(final OutputStream aStream)
        // throws IOException {
        //
        // aStream.write(text.getBytes());
        // }
        // });
        // aSession.putAttribute(flowFile, "TwitterAccount", text);
        // // aSession.putAttribute(flowFile, "Tweet", user);
        // aSession.transfer(flowFile, REL_SUCCESS);
        // aSession.commit();
        // }
        // }
        // } catch (InterruptedException e) {
        // getLogger().error(e.getMessage());
        // } catch (ParseException e) {
        // getLogger().error(e.getMessage());
        // } finally {
        // client.stop();
        // getLogger().info("Stopped the connection");
        // }

        // client.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    /**
     * Setter.
     *
     * @param aRelationships relationships
     */
    public void setRelationships(final Set<Relationship> aRelationships) {
        relationships = aRelationships;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    /**
     * Getter.
     *
     * @return properties
     */
    public List<PropertyDescriptor> getProperties() {
        return properties;
    }

    /**
     * Setter.
     *
     * @param aProperties properties
     */
    public void setProperties(final List<PropertyDescriptor> aProperties) {
        properties = aProperties;
    }

}
