package com.nirima.jenkins.plugins.docker;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.google.common.base.Splitter;

public class PortMapping
{
    
    private static void fromString(Ports p, String mapping)
    {
        
        boolean isUDP = false;
        int hostPort = 0;
        int containerPort = 0;
        String ip = null;
        
        if (mapping.endsWith("/udp"))
        {
            mapping = mapping.substring(0, mapping.length() - 4);
            isUDP = true;
        }
        
        String[] elements = mapping.split(":");
        
        //
        // ip:hostPort:containerPort |
        // ip::containerPort
        // hostPort:containerPort
        // containerPort
        
        if (elements.length == 1)
        {
            containerPort = Integer.parseInt(elements[0]);
        }
        else if (elements.length == 2)
        {
            hostPort = Integer.parseInt(elements[0]);
            containerPort = Integer.parseInt(elements[1]);
        }
        else if (elements.length == 3)
        {
            ip = elements[0];
            if (elements[1].length() > 0)
            {
                hostPort = Integer.parseInt(elements[1]);
            }
            containerPort = Integer.parseInt(elements[2]);
        }
        
        p.bind(new ExposedPort(isUDP?"udp":"tcp", containerPort), new Binding(ip, hostPort));
    }
    
    public static Ports parse(String mappings)
    {
        Ports p = new Ports();
        Iterable<String> items = Splitter.on(" ").omitEmptyStrings().trimResults().split(mappings);
        
        for (String item : items)
            fromString(p, item);
        
        return p;
        
    }
}
