package com.nirima.jenkins.plugins.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.LxcConf;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * Base for docker templates - does not include Jenkins items like labels.
 */
public abstract class DockerTemplateBase
{
    private static final Logger LOGGER = Logger.getLogger(DockerTemplateBase.class.getName());
    
    public final String         image;
    
    /**
     * Field dockerCommand
     */
    public final String         dockerCommand;
    
    /**
     * Field lxcConfString
     */
    public final String         lxcConfString;
    
    public final String         hostname;
    
    public final String[]       dnsHosts;
    public final String[]       volumes;
    public final String         volumesFrom;
    
    public final String         bindPorts;
    public final boolean        bindAllPorts;
    
    public final boolean        privileged;
    
    public DockerTemplateBase(String image, String dnsString, String dockerCommand, String volumesString, String volumesFrom, String lxcConfString, String hostname, String bindPorts, boolean bindAllPorts, boolean privileged
    
    )
    {
        this.image = image;
        
        this.dockerCommand = dockerCommand;
        this.lxcConfString = lxcConfString;
        this.privileged = privileged;
        this.hostname = hostname;
        
        this.bindPorts = bindPorts;
        this.bindAllPorts = bindAllPorts;
        
        this.dnsHosts = this.splitAndFilterEmpty(dnsString);
        this.volumes = this.splitAndFilterEmpty(volumesString);
        this.volumesFrom = volumesFrom;
    }
    
    protected Object readResolve()
    {
        return this;
    }
    
    private String[] splitAndFilterEmpty(String s)
    {
        List<String> temp = new ArrayList<String>();
        for (String item : s.split(" "))
        {
            if (!item.isEmpty())
                temp.add(item);
        }
        
        return temp.toArray(new String[temp.size()]);
        
    }
    
    public String getDnsString()
    {
        return Joiner.on(" ").join(this.dnsHosts);
    }
    
    public String getVolumesString()
    {
        return Joiner.on(" ").join(this.volumes);
    }
    
    public String getVolumesFrom()
    {
        return this.volumesFrom;
    }
    
    public String getDisplayName()
    {
        return "Image of " + this.image;
    }
    
    public InspectContainerResponse provisionNew(DockerClient dockerClient) throws DockerException
    {
        
        CreateContainerCmd cc = this.createContainerConfig(dockerClient);
        
        CreateContainerResponse ccr = cc.exec();
        String id = ccr.getId();

        StartContainerCmd scc = dockerClient.startContainerCmd(id);
        
        // Launch it.. :
        
        this.createHostConfig(scc);
        
        scc.exec();
        
        return dockerClient.inspectContainerCmd(id).exec();
    }
    
    protected String[] getDockerCommandArray()
    {
        String[] dockerCommandArray = new String[0];
        
        if (this.dockerCommand != null && !this.dockerCommand.isEmpty())
        {
            dockerCommandArray = this.dockerCommand.split(" ");
        }
        return dockerCommandArray;
    }
    
    protected Ports getPortMappings()
    {
        if (Strings.isNullOrEmpty(this.bindPorts))
        {
            return new Ports();
        }
        return PortMapping.parse(this.bindPorts);
    }
    
    public CreateContainerCmd createContainerConfig(DockerClient dc)
    {
        
        CreateContainerCmd cc = dc.createContainerCmd(this.image);
        
        
        if (this.hostname != null && !this.hostname.isEmpty())
        {
            cc.withHostName(this.hostname);
        }
        String[] cmd = this.getDockerCommandArray();
        if (cmd.length > 0)
            cc.withCmd(cmd);

        cc.withPortSpecs("22/tcp");
        
        if (this.dnsHosts.length > 0)
            cc.withDns(this.dnsHosts);
        
        if (this.volumesFrom != null && !this.volumesFrom.isEmpty())
            cc.withVolumesFrom(this.volumesFrom);
        
        return cc;
    }
    
    public void createHostConfig(StartContainerCmd scc)
    {
        scc.withPortBindings(this.getPortMappings());
        scc.withPublishAllPorts(this.bindAllPorts);
        scc.withPrivileged(this.privileged);
        
        if (this.dnsHosts.length > 0)
            scc.withDns(this.dnsHosts);
        
        List<LxcConf> lxcConf = this.getLxcConf();
        
        if (!lxcConf.isEmpty())
            scc.withLxcConf(lxcConf.toArray(new LxcConf[0]));
        
        if (!Strings.isNullOrEmpty(this.volumesFrom))
            scc.withVolumesFrom(this.volumesFrom);
    }
    
    @Override
    public String toString()
    {
        return Objects.toStringHelper(this).add("image", this.image).toString();
    }
    
    protected List<LxcConf> getLxcConf()
    {
        
        List<LxcConf> temp = new ArrayList<LxcConf>();
        if (this.lxcConfString == null)
            return temp;
        for (String item : this.lxcConfString.split(" "))
        {
            String[] keyValuePairs = item.split("=");
            if (keyValuePairs.length == 2)
            {
                LOGGER.info("lxc-conf option: " + keyValuePairs[0] + "=" + keyValuePairs[1]);
                LxcConf optN = new LxcConf();
                optN.setKey(keyValuePairs[0]);
                optN.setValue(keyValuePairs[1]);
                temp.add(optN);
            }
            else
            {
                LOGGER.warning("Specified option: " + item + " is not in the form X=Y, please correct.");
            }
        }
        return temp;
    }
}
