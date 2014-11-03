package com.nirima.jenkins.plugins.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;

public class MDockerClientBuilder
{
    
    private DockerClientImpl dockerClient = null;
    
    private MDockerClientBuilder(DockerClientImpl dockerClient)
    {
        this.dockerClient = dockerClient;
    }
    
    public static MDockerClientBuilder getInstance()
    {
        return new MDockerClientBuilder(withDefaultDockerCmdExecFactory(DockerClientImpl.getInstance()));
    }
    
    public static MDockerClientBuilder getInstance(DockerClientConfig dockerClientConfig)
    {
        return new MDockerClientBuilder(withDefaultDockerCmdExecFactory(DockerClientImpl.getInstance(dockerClientConfig)));
    }
    
    public static MDockerClientBuilder getInstance(String serverUrl)
    {
        return new MDockerClientBuilder(withDefaultDockerCmdExecFactory(DockerClientImpl.getInstance(serverUrl)));
    }
    
    private static DockerClientImpl withDefaultDockerCmdExecFactory(DockerClientImpl dockerClient)
    {
        
        DockerCmdExecFactory dockerCmdExecFactory = getDefaultDockerCmdExecFactory();
        
        return dockerClient.withDockerCmdExecFactory(dockerCmdExecFactory);
    }
    
    public static DockerCmdExecFactory getDefaultDockerCmdExecFactory()
    {
        return new DockerCmdExecFactoryImpl();
    }
    
    public MDockerClientBuilder withDockerCmdExecFactory(DockerCmdExecFactory dockerCmdExecFactory)
    {
        this.dockerClient = this.dockerClient.withDockerCmdExecFactory(dockerCmdExecFactory);
        return this;
    }
    
    public DockerClient build()
    {
        return this.dockerClient;
    }
}
