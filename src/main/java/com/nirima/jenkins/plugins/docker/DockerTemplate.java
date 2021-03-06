package com.nirima.jenkins.plugins.docker;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.ItemGroup;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.security.ACL;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.util.StreamTaskListener;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.durabletask.executors.OnceRetentionStrategy;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserListBoxModel;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.trilead.ssh2.Connection;


public class DockerTemplate extends DockerTemplateBase implements Describable<DockerTemplate> {
    private static final Logger LOGGER = Logger.getLogger(DockerTemplate.class.getName());


    public final String labelString;

    // SSH settings
    /**
     * The id of the credentials to use.
     */
    public final String credentialsId;

    /**
     * Minutes before terminating an idle slave
     */
    public final String idleTerminationMinutes;

    /**
     * Field jvmOptions.
     */
    public final String jvmOptions;

    /**
     * Field javaPath.
     */
    public final String javaPath;

    /**
     * Field prefixStartSlaveCmd.
     */
    public final String prefixStartSlaveCmd;

    /**
     *  Field suffixStartSlaveCmd.
     */
    public final String suffixStartSlaveCmd;

    /**
     *  Field remoteFSMapping.
     */
    public final String remoteFsMapping;

    public final String remoteFs; // = "/home/jenkins";

    public final int instanceCap;

    private transient /*almost final*/ Set<LabelAtom> labelSet;
    public transient DockerCloud parent;


    @DataBoundConstructor
    public DockerTemplate(String image, String labelString,
                          String remoteFs,
                          String remoteFsMapping,
                          String credentialsId, String idleTerminationMinutes,
                          String jvmOptions, String javaPath,
                          String prefixStartSlaveCmd, String suffixStartSlaveCmd,
                          String instanceCapStr, String dnsString,
                          String dockerCommand,
                          String volumesString, String volumesFrom,
                          String lxcConfString,
                          String hostname,
                          String bindPorts,
                          boolean bindAllPorts,
                          boolean privileged

    ) {
        super(image, dnsString,dockerCommand,volumesString,volumesFrom,lxcConfString,hostname,
                Objects.firstNonNull(bindPorts, "0.0.0.0:22"), bindAllPorts,
                privileged);


        this.labelString = Util.fixNull(labelString);
        this.credentialsId = credentialsId;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.jvmOptions = jvmOptions;
        this.javaPath = javaPath;
        this.prefixStartSlaveCmd = prefixStartSlaveCmd;
        this.suffixStartSlaveCmd = suffixStartSlaveCmd;
        this.remoteFs =  Strings.isNullOrEmpty(remoteFs)?"/home/jenkins":remoteFs;
        this.remoteFsMapping = remoteFsMapping;

        if (instanceCapStr.equals("")) {
            this.instanceCap = Integer.MAX_VALUE;
        } else {
            this.instanceCap = Integer.parseInt(instanceCapStr);
        }

        this.readResolve();
    }

    private String[] splitAndFilterEmpty(String s) {
        List<String> temp = new ArrayList<String>();
        for (String item : s.split(" ")) {
            if (!item.isEmpty())
                temp.add(item);
        }

        return temp.toArray(new String[temp.size()]);

    }

    public String getInstanceCapStr() {
        if (this.instanceCap==Integer.MAX_VALUE) {
            return "";
        } else {
            return String.valueOf(this.instanceCap);
        }
    }

    public String getDnsString() {
        return Joiner.on(" ").join(this.dnsHosts);
    }

    public String getVolumesString() {
	return Joiner.on(" ").join(this.volumes);
    }

    public String getVolumesFrom() {
        return this.volumesFrom;
    }

    public String getRemoteFsMapping() {
        return this.remoteFsMapping;
    }

    public Descriptor<DockerTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(this.getClass());
    }

    public Set<LabelAtom> getLabelSet(){
        return this.labelSet;
    }

    /**
     * Initializes data structure that we don't persist.
     */
    protected Object readResolve() {
        super.readResolve();

        this.labelSet = Label.parse(this.labelString);
        return this;
    }

    public String getDisplayName() {
        return "Image of " + this.image;
    }

    public DockerCloud getParent() {
        return this.parent;
    }

    private int idleTerminationMinutes() {
        if (this.idleTerminationMinutes == null || this.idleTerminationMinutes.trim().isEmpty()) {
            return 0;
        } else {
            try {
                return Integer.parseInt(this.idleTerminationMinutes);
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.INFO, "Malformed idleTermination value: {0}", this.idleTerminationMinutes);
                return 30;
            }
        }
    }

    public DockerSlave provision(StreamTaskListener listener) throws IOException, Descriptor.FormException, DockerException {
            PrintStream logger = listener.getLogger();


        logger.println("Launching " + this.image );

        int numExecutors = 1;
        Node.Mode mode = Node.Mode.NORMAL;

        RetentionStrategy retentionStrategy = new OnceRetentionStrategy(this.idleTerminationMinutes());

        List<? extends NodeProperty<?>> nodeProperties = new ArrayList();

        InspectContainerResponse containerInspectResponse = this.provisionNew();
        String containerId = containerInspectResponse.getId();

        ComputerLauncher launcher = new DockerComputerLauncher(this, containerInspectResponse);

        // Build a description up:
        String nodeDescription = "Docker Node [" + this.image + " on ";
        try {
            nodeDescription += this.getParent().getDisplayName();
        } catch(Exception ex)
        {
            nodeDescription += "???";
        }
        nodeDescription += "]";

        String slaveName = containerId.substring(0,12);

        try
        {
            slaveName = slaveName + "@" + this.getParent().getDisplayName();
        }
        catch(Exception ex) {
            LOGGER.warning("Error fetching name of cloud");
        }

        return new DockerSlave(this, containerId,
                slaveName,
                nodeDescription,
                this.remoteFs, numExecutors, mode, this.labelString,
                launcher, retentionStrategy, nodeProperties);

    }

    public InspectContainerResponse provisionNew() throws DockerException {
        DockerClient dockerClient = this.getParent().connect();
        return this.provisionNew(dockerClient);
    }

    public int getNumExecutors() {
        return 1;
    }

    @Override
    protected String[] getDockerCommandArray() {
        String[] cmd = super.getDockerCommandArray();

        if( cmd.length == 0 ) {
            //default value to preserve comptability
            cmd = new String[]{"/usr/sbin/sshd", "-D"};
        }

        return cmd;
    }

    @Override
    /**
     * Provide a sensible default - templates are for slaves, and you're mostly going
     * to want port 22 exposed.
     */
    protected Ports getPortMappings() {

        if(Strings.isNullOrEmpty(this.bindPorts) ) {
            return PortMapping.parse("0.0.0.0::22");
        }
        return super.getPortMappings();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DockerTemplate> {

        @Override
        public String getDisplayName() {
            return "Docker Template";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {

            return new SSHUserListBoxModel().withMatching(SSHAuthenticator.matcher(Connection.class),
                    CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context,
                            ACL.SYSTEM, SSHLauncher.SSH_SCHEME));
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("image", this.image)
                .add("parent", this.parent)
                .toString();
    }
}
